/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

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

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class S3Resource extends Resource {

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

				// S3Object instances need to be manually closed to release
				// the underlying http connection. This will be done after
				// the request payload is written to the requester. This
				// should solve the aws connection famine.
				closeThisS3ObjectAtTheEnd.set(s3Object);

				metadata = s3Object.getObjectMetadata();
				owner = getOrCheckOwnership(metadata, checkOwnership);
				fileContent = s3Object.getObjectContent();
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

		if (context.query().getBoolean("withContentDisposition", false))
			payload = payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					metadata.getContentDisposition());

		return payload;
	}

	private static ThreadLocal<S3Object> closeThisS3ObjectAtTheEnd = new ThreadLocal<>();

	public static void closeThisThreadS3Object() {
		try {
			S3Object s3Object = closeThisS3ObjectAtTheEnd.get();
			if (s3Object != null)
				s3Object.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Payload doList(String bucketSuffix, WebPath path, Context context) {

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
			payload.with("next", fromS3Key(objects.getNextMarker()).toEscapedString());

		ArrayNode results = Json.array();

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			WebPath objectPath = fromS3Key(summary.getKey());
			results.add(Json.object("path", objectPath.toString(), //
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
		return JsonPayload.ok().with("deleted", deleted).build();
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, WebPath path, byte[] bytes,
			Context context) {
		return doUpload(bucketSuffix, rootUri, credentials, path, bytes, context, true);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, WebPath path, byte[] bytes,
			Context context, boolean enableS3Location) {

		// TODO check if this upload does not replace an older upload
		// in this case, check crdentials and owner rights
		// should return FORBIDDEN if user not the owner of previous file
		// admin can replace whatever they need to replace?

		if (path.size() < 2)
			throw Exceptions.illegalArgument("no prefix in file path [%s]", path.toString());

		String fileName = path.last();
		String bucketName = getBucketName(bucketSuffix);
		String backendId = SpaceContext.backendId();
		WebPath s3Path = path.addFirst(backendId);

		ObjectMetadata metadata = new ObjectMetadata();
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		metadata.setContentType(typeMap.getContentType(fileName));
		metadata.setContentLength(Long.valueOf(context.header("Content-Length")));
		metadata.setContentDisposition(//
				String.format("attachment; filename=\"%s\"", fileName));
		metadata.addUserMetadata("owner", credentials.name());
		metadata.addUserMetadata("owner-type", credentials.type().name());

		PutObjectResult putResult = s3.putObject(new PutObjectRequest(bucketName, //
				s3Path.toS3Key(), new ByteArrayInputStream(bytes), //
				metadata));

		JsonPayload payload = JsonPayload.ok()//
				.with("path", path.toString())//
				.with("location", toSpaceLocation(backendId, rootUri, path))//
				.with("contentType", metadata.getContentType())//
				.with("expirationTime", putResult.getExpirationTime().getTime())//
				.with("etag", putResult.getETag())//
				.with("contentMd5", putResult.getContentMd5());

		if (enableS3Location)
			payload.with("s3", toS3Location(bucketName, s3Path));

		return payload.build();
	}

	//
	// Implementation
	//

	private String getOrCheckOwnership(ObjectMetadata metadata, boolean checkOwnership) {

		String owner = metadata.getUserMetaDataOf("owner");
		Credentials credentials = SpaceContext.credentials();

		if (!checkOwnership)
			return owner;

		String ownerType = metadata.getUserMetaDataOf("owner-type");

		if (credentials.name().equals(owner) //
				&& credentials.type().name().equals(ownerType))
			return owner;

		throw Exceptions.insufficientCredentials(credentials);
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