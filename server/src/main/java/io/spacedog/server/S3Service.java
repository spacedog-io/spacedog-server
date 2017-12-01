/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;

import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;

import io.spacedog.model.DownloadRequest;
import io.spacedog.model.Permission;
import io.spacedog.model.RolePermissions;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

public class S3Service extends SpaceService {

	private static AmazonS3Client s3 = new AmazonS3Client();
	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	static {
		String awsRegion = Start.get().configuration().awsRegion().orElse("eu-west-1");
		s3.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
	}

	static AmazonS3Client s3Client() {
		return s3;
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

		S3File file = toS3File(bucketSuffix, path);

		if (withContent)
			file.open();

		if (!file.exists())
			return Payload.notFound();

		if (checkOwnership)
			file.checkOwner();

		Payload payload = new Payload(file.metadata().getContentType(), file)//
				.withHeader(SpaceHeaders.ETAG, file.metadata().getETag())//
				.withHeader(SpaceHeaders.SPACEDOG_OWNER, file.owner());

		if (context.query().getBoolean(WITH_CONTENT_DISPOSITION, false))
			payload = payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					file.metadata().getContentDisposition());

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

	public Payload doDelete(String bucketSuffix, WebPath path, boolean checkOwnership) {

		S3File file = toS3File(bucketSuffix, path);
		ArrayNode deleted = Json.array();

		// first try to delete this path as file

		if (file.exists()) {
			if (checkOwnership)
				file.checkOwner();

			file.delete();
			deleted.add(path.toString());
		}

		// second delete all files with this path as prefix

		String next = null;

		do {

			ListObjectsRequest request = new ListObjectsRequest()//
					.withBucketName(file.bucketName())//
					.withPrefix(file.path().toS3Prefix())//
					.withMaxKeys(100);

			if (next != null)
				request.setMarker(next);

			ObjectListing objects = s3.listObjects(request);

			if (!objects.getObjectSummaries().isEmpty()) {
				s3.deleteObjects(new DeleteObjectsRequest(file.bucketName())//
						.withKeys(objects.getObjectSummaries()//
								.stream()//
								.map(summary -> new KeyVersion(summary.getKey()))//
								.collect(Collectors.toList())));
			}

			for (S3ObjectSummary summary : objects.getObjectSummaries())
				deleted.add(fromS3Key(summary.getKey()).toString());

			next = objects.getNextMarker();

		} while (next != null);

		return JsonPayload.ok().withFields("deleted", deleted).build();
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, //
			WebPath path, String fileName, Context context) {
		return doUpload(bucketSuffix, rootUri, credentials, path, fileName, true, context);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, //
			WebPath path, String fileName, boolean enableS3Location, Context context) {

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

		try {
			PutObjectResult putResult = s3.putObject(new PutObjectRequest(bucketName, //
					s3Path.toS3Key(), context.request().inputStream(), metadata));

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

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error reading request input stream");
		}
	}

	public Payload doZip(String bucketSuffix, DownloadRequest request, RolePermissions permissions) {

		Set<S3File> files = toS3Files(bucketSuffix, request);
		Credentials credentials = SpaceContext.credentials();

		if (!permissions.check(credentials, Permission.readAll))
			if (permissions.check(credentials, Permission.readMine))
				for (S3File file : files)
					file.checkOwner(credentials);

		return new Payload("application/octet-stream", //
				new ZipStreamingOutput(files))//
						.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
								contentDisposition(request.fileName));
	}

	//
	// Implementation
	//

	protected String contentDisposition(String fileName) {
		return String.format("attachment; filename=\"%s\"", fileName);
	}

	private class ZipStreamingOutput implements StreamingOutput {

		private Set<S3File> files;

		public ZipStreamingOutput(Set<S3File> files) {
			this.files = files;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			ZipOutputStream zip = new ZipOutputStream(output);
			for (S3File file : files) {
				zip.putNextEntry(new ZipEntry(file.path().removeFirst().toString()));
				file.write(zip);
				zip.flush();
			}
			// TODO put this in a finally block?
			zip.close();
		}

	}

	private S3File toS3File(String bucketSuffix, WebPath path) {
		return new S3File(getBucketName(bucketSuffix), //
				path.addFirst(SpaceContext.backendId()));
	}

	private S3File toS3File(String bucketName, String backendId, String path) {
		WebPath webPath = WebPath.parse(path);
		return new S3File(bucketName, webPath.addFirst(backendId));
	}

	private Set<S3File> toS3Files(String bucketSuffix, DownloadRequest request) {
		String backendId = SpaceContext.backendId();
		String bucketName = getBucketName(bucketSuffix);
		return request.paths.stream()//
				.map(path -> toS3File(bucketName, backendId, path))//
				.collect(Collectors.toSet());
	}

	private String contentType(String fileName, Context context) {
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		return Strings.isNullOrEmpty(fileName) //
				? "application/octet-stream"
				: typeMap.getContentType(fileName);
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
