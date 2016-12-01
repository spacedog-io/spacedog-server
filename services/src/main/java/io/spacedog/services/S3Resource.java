/**
 * © David Attias 2015
 */
package io.spacedog.services;

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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Uris;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class S3Resource extends Resource {

	private static AmazonS3Client s3 = new AmazonS3Client();
	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	static {
		s3.setRegion(Region.getRegion(Regions.fromName(Start.get().configuration().awsRegion())));
	}

	public Payload doGet(String bucketSuffix, String backendId, String[] path, Context context) {
		return doGet(bucketSuffix, backendId, path, context, false);
	}

	public Payload doGet(String bucketSuffix, String backendId, String[] path, Context context,
			boolean checkOwnership) {
		return doGet(true, bucketSuffix, backendId, path, context, checkOwnership);
	}

	public Payload doGet(boolean withContent, String bucketSuffix, String backendId, String[] path, Context context) {
		return doGet(withContent, bucketSuffix, backendId, path, context, false);
	}

	private Payload doGet(boolean withContent, String bucketSuffix, String backendId, String[] path, Context context,
			boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = S3Key.get(backendId).add(path).toString();

		Object fileContent = "";
		ObjectMetadata metadata = null;
		String owner = null;

		try {
			if (withContent) {
				S3Object object = s3.getObject(bucketName, s3Key);
				metadata = object.getObjectMetadata();
				owner = getOrCheckOwnership(metadata, checkOwnership);
				fileContent = object.getObjectContent();
			} else {
				metadata = s3.getObjectMetadata(bucketName, s3Key);
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

	public Payload doList(String bucketSuffix, String backendId, String[] path, Context context) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = S3Key.get(backendId).add(path).toString();

		ListObjectsRequest request = new ListObjectsRequest()//
				.withBucketName(bucketName)//
				.withPrefix(s3Key + SLASH)//
				.withMaxKeys(context.query().getInteger("size", 100));

		if (!Strings.isNullOrEmpty(context.get("next")))
			request.setMarker(S3Key.get(backendId).add(context.get("next")).toString());

		ObjectListing objects = s3.listObjects(request);

		// only root path allows empty list
		if (objects.getObjectSummaries().isEmpty() //
				&& !backendId.equals(s3Key))
			return JsonPayload.error(404);

		JsonBuilder<ObjectNode> response = JsonPayload.builder();

		if (objects.isTruncated())
			response.put("next", toSpaceKeyFromS3Key(backendId, objects.getNextMarker()));

		response.array("results");

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			response.object()//
					.put("path", toSpaceKeyFromS3Key(backendId, summary.getKey()))//
					.put("size", summary.getSize())//
					.put("lastModified", new DateTime(summary.getLastModified().getTime()).toString())//
					.put("etag", summary.getETag())//
					.end();
		}

		return JsonPayload.json(response);
	}

	public Payload doDelete(String bucketSuffix, Credentials credentials, String[] path, boolean fileOnly,
			boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		String s3Key = S3Key.get(credentials.target()).add(path).toString();

		JsonBuilder<ObjectNode> builder = JsonPayload.builder().array("deleted");

		// first try to delete this path as key

		try {
			ObjectMetadata metadata = s3.getObjectMetadata(bucketName, s3Key);

			getOrCheckOwnership(metadata, checkOwnership);

			s3.deleteObject(bucketName, s3Key);
			builder.add(Uris.join(path));

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
					builder.add(toSpaceKeyFromS3Key(credentials.target(), summary.getKey()));

				next = objects.getNextMarker();

			} while (next != null);

		}
		return JsonPayload.json(builder);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, String[] path, byte[] bytes,
			Context context) {
		return doUpload(bucketSuffix, rootUri, credentials, path, bytes, context, true);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, String[] path, byte[] bytes,
			Context context, boolean enableS3Location) {

		// TODO check if this upload does not replace an older upload
		// in this case, check crdentials and owner rights
		// should return FORBIDDEN if user not the owner of previous file
		// admin can replace whatever they need to replace?

		if (path.length < 2)
			throw Exceptions.illegalArgument("no prefix in file path [%s]", Uris.join(path));

		String fileName = path[path.length - 1];
		String bucketName = getBucketName(bucketSuffix);
		String s3Key = S3Key.get(credentials.target()).add(path).toString();

		ObjectMetadata metadata = new ObjectMetadata();
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		metadata.setContentType(typeMap.getContentType(fileName));
		metadata.setContentLength(Long.valueOf(context.header("Content-Length")));
		metadata.setContentDisposition(String.format("attachment; filename=\"%s\"", fileName));
		metadata.addUserMetadata("owner", credentials.name());
		metadata.addUserMetadata("owner-type", credentials.level().toString());

		s3.putObject(new PutObjectRequest(bucketName, //
				s3Key, new ByteArrayInputStream(bytes), //
				metadata));

		JsonBuilder<ObjectNode> builder = JsonPayload.builder()//
				.put("path", Uris.join(path))//
				.put("location", toSpaceLocation(credentials.target(), rootUri, path));

		if (enableS3Location)
			builder.put("s3", toS3Location(bucketName, s3Key));

		return JsonPayload.json(builder);
	}

	//
	// Implementation
	//

	private String getOrCheckOwnership(ObjectMetadata metadata, boolean checkOwnership) {

		String owner = metadata.getUserMetaDataOf("owner");
		Credentials credentials = SpaceContext.getCredentials();

		if (!checkOwnership)
			return owner;

		String ownerLevel = metadata.getUserMetaDataOf("owner-type");

		if (credentials.name().equals(owner) //
				&& credentials.level().toString().equals(ownerLevel))
			return owner;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private String toSpaceKeyFromS3Key(String backendId, String s3Key) {
		return s3Key.substring(backendId.length());
	}

	private String toSpaceLocation(String backendId, String root, String[] path) {
		return spaceUrl(backendId, root)//
				.append(Uris.join(path)).toString();
	}

	private String toS3Location(String bucketName, String s3Key) {
		return new StringBuilder("https://").append(bucketName)//
				.append(".s3.amazonaws.com/").append(s3Key).toString();
	}

	public static class S3Key {

		boolean slashIsNeeded = false;
		private StringBuilder builder;

		public S3Key() {
			this.builder = new StringBuilder();
		}

		public S3Key add(String... names) {
			if (names != null)
				for (String name : names)
					add(name);

			return this;
		}

		public S3Key add(String name) {
			if (Strings.isNullOrEmpty(name))
				return this;

			if (slashIsNeeded)
				if (name.startsWith(SLASH))
					builder.append(name);
				else
					builder.append(SLASH).append(name);
			else if (name.startsWith(SLASH))
				builder.append(name.substring(1));
			else
				builder.append(name);

			slashIsNeeded = !name.endsWith(SLASH);
			return this;
		}

		@Override
		public String toString() {
			return Utils.removeSuffix(builder.toString(), SLASH);
		}

		public static S3Key get(String... names) {
			return new S3Key().add(names);
		}
	}
}
