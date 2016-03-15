/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.ByteArrayInputStream;
import java.util.Optional;
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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.payload.Payload;

public class AbstractS3Resource extends AbstractResource {

	private static AmazonS3Client s3 = new AmazonS3Client();
	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	static {
		s3.setRegion(Region.getRegion(Regions.fromName(Start.get().configuration().awsRegion())));
	}

	public Object doGet(String bucketSuffix, String backendId, Optional<String> path, Context context) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = path.isPresent() ? String.join(SLASH, backendId, path.get()) : backendId;

		try {
			S3Object object = s3.getObject(bucketName, s3Key);

			context.response().setHeader(SpaceHeaders.CONTENT_TYPE, //
					object.getObjectMetadata().getContentType());
			context.response().setHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					object.getObjectMetadata().getContentDisposition());
			context.response().setHeader(SpaceHeaders.ETAG, //
					object.getObjectMetadata().getETag());
			context.response().setHeader(SpaceHeaders.AMAZON_META_OWNER, //
					object.getObjectMetadata().getUserMetaDataOf("owner"));
			context.response().setHeader(SpaceHeaders.AMZ_META_OWNER_TYPE, //
					object.getObjectMetadata().getUserMetaDataOf("owner-type"));

			return object.getObjectContent();

		} catch (AmazonS3Exception e) {

			// 404 is OK, just continue to the next part of this method
			if (e.getStatusCode() != 404)
				throw e;
		}

		ListObjectsRequest request = new ListObjectsRequest()//
				.withBucketName(bucketName)//
				.withPrefix(s3Key + SLASH)//
				.withMaxKeys(context.query().getInteger("size", 100));

		if (!Strings.isNullOrEmpty(context.get("next")))
			request.setMarker(toS3Key(backendId, context.get("next")));

		ObjectListing objects = s3.listObjects(request);

		if (path.isPresent() && objects.getObjectSummaries().isEmpty())
			return Payloads.error(404);

		JsonBuilder<ObjectNode> response = Payloads.minimalBuilder(200);

		if (objects.isTruncated())
			response.put("next", toSpaceKeyFromS3Key(backendId, objects.getNextMarker()));

		response.array("results");

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			response.object()//
					.put("path", toSpaceKeyFromS3Key(backendId, summary.getKey()))//
					.put("size", summary.getSize())//
					.put("lastModified", new DateTime(summary.getLastModified().getTime()).toString())//
					.end();
		}

		return Payloads.json(response);
	}

	public Payload doDelete(String bucketSuffix, Credentials credentials, Optional<String> path) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = path.isPresent() ? String.join(SLASH, credentials.backendId(), path.get())
				: credentials.backendId();

		JsonBuilder<ObjectNode> builder = Payloads.minimalBuilder(200).array("deleted");

		// first try to delete this path as key

		try {
			ObjectMetadata metadata = s3.getObjectMetadata(bucketName, s3Key);

			if (credentials.isUserAuthenticated()) {

				if (isOwner(credentials, metadata)) {
					s3.deleteObject(bucketName, s3Key);
					return Payloads.success();
				} else
					return Payloads.error(401);

			} else if (credentials.isAdminAuthenticated()) {
				s3.deleteObject(bucketName, s3Key);
				builder.add(path.get());
			}

		} catch (AmazonS3Exception e) {

			if (e.getStatusCode() != 404)
				throw e;
		}

		// second delete all files with this path as prefix

		if (credentials.isAdminAuthenticated()) {

			String next = null;

			do {

				ListObjectsRequest request = new ListObjectsRequest()//
						.withBucketName(bucketName)//
						.withPrefix(s3Key + SLASH)//
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
					builder.add(toSpaceKeyFromS3Key(credentials.backendId(), summary.getKey()));

				next = objects.getNextMarker();

			} while (next != null);

		}
		return Payloads.json(builder);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, String path, String fileName,
			byte[] bytes, Context context) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = toS3Key(credentials.backendId(), path, fileName);

		ObjectMetadata metadata = new ObjectMetadata();
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		metadata.setContentType(typeMap.getContentType(fileName));
		metadata.setContentLength(Long.valueOf(context.header("Content-Length")));
		metadata.setContentDisposition(String.format("attachment; filename=\"%s\"", fileName));
		metadata.addUserMetadata("owner", credentials.name());
		metadata.addUserMetadata("owner-type", credentials.type().toString());

		s3.putObject(new PutObjectRequest(bucketName, //
				s3Key, new ByteArrayInputStream(bytes), //
				metadata));

		return Payloads.json(Payloads.minimalBuilder(200)//
				.put("path", toSpaceKeyFromPath(path, fileName))//
				.put("location", toSpaceLocation(credentials.backendId(), rootUri, path, fileName))//
				.put("s3", toS3Location(bucketName, s3Key)));
	}

	//
	// Implementation
	//

	private boolean isOwner(Credentials credentials, ObjectMetadata metadata) {
		return credentials.name().equals(metadata.getUserMetaDataOf("owner")) //
				&& credentials.type().toString().equals(metadata.getUserMetaDataOf("owner-type"));
	}

	private String toS3Key(String backendId, String path, String name) {
		return Strings.isNullOrEmpty(path)//
				? String.join(SLASH, backendId, name) //
				: String.join(SLASH, backendId, path, name);
	}

	private String toS3Key(String backendId, String spaceKey) {
		return new StringBuilder(backendId).append(spaceKey).toString();
	}

	private String toSpaceKeyFromS3Key(String backendId, String s3Key) {
		return s3Key.substring(backendId.length());
	}

	private String toSpaceKeyFromPath(String path, String fileName) {
		return Strings.isNullOrEmpty(path)//
				? new StringBuilder(SLASH).append(fileName).toString()//
				: new StringBuilder(SLASH).append(path).append(SLASH).append(fileName).toString();
	}

	private String toSpaceLocation(String backendId, String root, String path, String fileName) {
		return spaceUrl(Optional.of(backendId), root)//
				.append(toSpaceKeyFromPath(path, fileName)).toString();
	}

	private String toS3Location(String bucketName, String s3Key) {
		return new StringBuilder("https://").append(bucketName)//
				.append(".s3.amazonaws.com/").append(s3Key).toString();
	}

}
