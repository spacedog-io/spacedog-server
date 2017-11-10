/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;

import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import io.spacedog.model.ZipRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

public class S3Service extends SpaceService {

	private static AmazonS3Client s3 = new AmazonS3Client();
	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	static {
		String awsRegion = Start.get().configuration().awsRegion().orElse("eu-west-1");
		s3.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
	}

	public Payload doGet(String bucketSuffix, WebPath path, Context context) {
		return doGet(bucketSuffix, path, context, false);
	}

	public Payload doGet(String bucketSuffix, WebPath path, Context context, boolean checkOwnership) {
		return doGet(true, bucketSuffix, path, context, checkOwnership);
	}

	public Payload doGet(boolean withContent, String bucketSuffix, WebPath path, Context context) {
		return doGet(withContent, bucketSuffix, path, context, false);
	}

	private Payload doGet(boolean withContent, String bucketSuffix, WebPath path, Context context,
			boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(SpaceContext.backendId());

		S3Object s3Object = null;
		Object fileContent = "";
		ObjectMetadata metadata = null;
		String owner = null;

		try {
			if (withContent) {
				s3Object = s3.getObject(bucketName, s3Path.toS3Key());
				metadata = s3Object.getObjectMetadata();
				owner = getOrCheckOwnership(metadata, checkOwnership);
				fileContent = new S3ObjectStreamingOutput(s3Object);
			} else {
				metadata = s3.getObjectMetadata(bucketName, s3Path.toS3Key());
				owner = getOrCheckOwnership(metadata, checkOwnership);
			}

		} catch (AmazonS3Exception e) {

			// 404 is OK
			if (e.getStatusCode() == 404)
				return Payload.notFound();

			throw e;
		}

		Payload payload = new Payload(metadata.getContentType(), fileContent)//
				.withHeader(SpaceHeaders.ETAG, metadata.getETag())//
				.withHeader(SpaceHeaders.SPACEDOG_OWNER, owner);

		if (context.query().getBoolean(WITH_CONTENT_DISPOSITION, false))
			payload = payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					metadata.getContentDisposition());

		return payload;
	}

	public Payload doList(String bucketSuffix, String rootUri, WebPath path, Context context) {

		String backendId = SpaceContext.backendId();
		WebPath s3Path = path.addFirst(backendId);
		String bucketName = getBucketName(bucketSuffix);

		ListObjectsRequest request = new ListObjectsRequest()//
				.withBucketName(bucketName)//
				.withPrefix(s3Path.toS3Prefix())//
				.withMaxKeys(context.query().getInteger("size", 100));

		String next = context.get("next");
		if (!Strings.isNullOrEmpty(next))
			request.setMarker(WebPath.parse(next).addFirst(backendId).toS3Key());

		ObjectListing objects = s3.listObjects(request);

		// only root path allows empty list
		if (objects.getObjectSummaries().isEmpty() && s3Path.size() > 1)
			return JsonPayload.error(404).build();

		JsonPayload payload = JsonPayload.ok();

		if (objects.isTruncated())
			payload.withFields("next", fromS3Key(objects.getNextMarker()).toEscapedString());

		ArrayNode results = Json.array();

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			WebPath objectPath = fromS3Key(summary.getKey());
			results.add(Json.object(//
					"path", objectPath.toString(), //
					"location", toSpaceLocation(backendId, rootUri, objectPath), //
					"size", summary.getSize(), //
					"lastModified", new DateTime(summary.getLastModified().getTime()).toString(), //
					"etag", summary.getETag()));
		}

		return payload.withResults(results).build();
	}

	public Payload doDelete(String bucketSuffix, WebPath path, boolean fileOnly, boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(SpaceContext.backendId());
		ArrayNode deleted = Json.array();

		// first try to delete this path as key

		try {
			ObjectMetadata metadata = s3.getObjectMetadata(bucketName, s3Path.toS3Key());

			getOrCheckOwnership(metadata, checkOwnership);

			s3.deleteObject(bucketName, s3Path.toS3Key());
			deleted.add(path.toString());

		} catch (AmazonS3Exception e) {

			if (e.getStatusCode() != HttpStatus.NOT_FOUND)
				throw e;
		}

		// second delete all files with this path as prefix

		if (!fileOnly) {

			String next = null;

			do {

				ListObjectsRequest request = new ListObjectsRequest()//
						.withBucketName(bucketName)//
						.withPrefix(s3Path.toS3Prefix())//
						.withMaxKeys(100);

				if (next != null)
					request.setMarker(next);

				ObjectListing objects = s3.listObjects(request);

				if (!objects.getObjectSummaries().isEmpty()) {
					s3.deleteObjects(new DeleteObjectsRequest(bucketName)//
							.withKeys(objects.getObjectSummaries()//
									.stream()//
									.map(summary -> new KeyVersion(summary.getKey()))//
									.collect(Collectors.toList())));
				}

				for (S3ObjectSummary summary : objects.getObjectSummaries())
					deleted.add(fromS3Key(summary.getKey()).toString());

				next = objects.getNextMarker();

			} while (next != null);

		}
		return JsonPayload.ok().withFields("deleted", deleted).build();
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, WebPath path, byte[] bytes,
			String fileName, Context context) {
		return doUpload(bucketSuffix, rootUri, credentials, path, bytes, fileName, true, context);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, WebPath path, byte[] bytes,
			String fileName, boolean enableS3Location, Context context) {

		// TODO check if this upload does not replace an older upload
		// in this case, check crdentials and owner rights
		// should return FORBIDDEN if user not the owner of previous file
		// admin can replace whatever they need to replace?

		String bucketName = getBucketName(bucketSuffix);
		String backendId = SpaceContext.backendId();
		WebPath s3Path = path.addFirst(backendId);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType(fileName, context));
		metadata.setContentLength(Long.valueOf(context.header("Content-Length")));
		metadata.setContentDisposition(contentDisposition(fileName));
		metadata.addUserMetadata(OWNER_FIELD, credentials.id());

		PutObjectResult putResult = s3.putObject(new PutObjectRequest(bucketName, //
				s3Path.toS3Key(), new ByteArrayInputStream(bytes), //
				metadata));

		JsonPayload payload = JsonPayload.ok()//
				.withFields("path", path.toString())//
				.withFields("location", toSpaceLocation(backendId, rootUri, path))//
				.withFields("contentType", metadata.getContentType())//
				.withFields("expirationTime", putResult.getExpirationTime())//
				.withFields("etag", putResult.getETag())//
				.withFields("contentMd5", putResult.getContentMd5());

		if (enableS3Location)
			payload.withFields("s3", toS3Location(bucketName, s3Path));

		return payload.build();
	}

	public Payload doDownload(String bucketSuffix, ZipRequest request, Context context) {
		request.checkValid();
		return new Payload("application/octet-stream", //
				new ZipStreamingOutput(bucketSuffix, request))//
						.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
								contentDisposition(request.fileName));
	}

	//
	// Implementation
	//

	private class S3ObjectStreamingOutput implements StreamingOutput {

		private S3Object s3Object;

		public S3ObjectStreamingOutput(S3Object s3Object) {
			this.s3Object = s3Object;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			ByteStreams.copy(s3Object.getObjectContent(), output);
			s3Object.close();
		}

	}

	protected String contentDisposition(String fileName) {
		return String.format("attachment; filename=\"%s\"", fileName);
	}

	private class ZipStreamingOutput implements StreamingOutput {

		private String bucketSuffix;
		private ZipRequest request;
		private String backendId;

		public ZipStreamingOutput(String bucketSuffix, ZipRequest request) {
			this.bucketSuffix = bucketSuffix;
			this.request = request;
			this.backendId = SpaceContext.backendId();
		}

		@Override
		public void write(OutputStream output) throws IOException {
			ZipOutputStream zip = new ZipOutputStream(output);
			for (String path : request.paths) {
				WebPath webPath = WebPath.parse(path);
				S3Object object = getS3Object(bucketSuffix, backendId, webPath);
				zip.putNextEntry(new ZipEntry(webPath.last()));
				ByteStreams.copy(object.getObjectContent(), zip);
				object.close();
				zip.flush();
			}
			zip.close();
		}

	}

	private S3Object getS3Object(String bucketSuffix, String backendId, WebPath path) {
		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(backendId);
		return s3.getObject(bucketName, s3Path.toS3Key());
	}

	private String contentType(String fileName, Context context) {
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		return Strings.isNullOrEmpty(fileName) //
				? "application/octet-stream"
				: typeMap.getContentType(fileName);
	}

	private String getOrCheckOwnership(ObjectMetadata metadata, boolean checkOwnership) {

		String owner = metadata.getUserMetaDataOf(OWNER_FIELD);
		Credentials credentials = SpaceContext.credentials();

		if (checkOwnership && !credentials.id().equals(owner))
			throw Exceptions.insufficientCredentials(credentials);

		return owner;
	}

	private WebPath fromS3Key(String s3Key) {
		return WebPath.parse(s3Key).removeFirst();
	}

	private String toSpaceLocation(String backendId, String root, WebPath path) {
		return spaceUrl(root).append(path.toEscapedString()).toString();
	}

	private String toS3Location(String bucketName, WebPath path) {
		return new StringBuilder("https://").append(bucketName)//
				.append(".s3.amazonaws.com").append(path.toEscapedString()).toString();
	}
}
