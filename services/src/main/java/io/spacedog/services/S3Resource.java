/**
 * © David Attias 2015
 */
package io.spacedog.services;

import static net.codestory.http.constants.Encodings.GZIP;
import static net.codestory.http.constants.Headers.ACCEPT_ENCODING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.joda.time.DateTime;

import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import io.spacedog.model.ZipRequest;
import io.spacedog.utils.ContentTypes;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

public class S3Resource extends Resource {

	private static final String X_AMZ_META = "x-amz-meta-";
	private static final String OWNER_TYPE_META = "owner-type";
	private static final String OWNER_META = "owner";

	private static AmazonS3Client s3 = new AmazonS3Client();

	static {
		String awsRegion = Start.get().configuration().awsRegion().orElse("eu-west-1");
		s3.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
	}

	public Payload doGet(String bucketSuffix, String backendId, WebPath path, Context context) {
		return doGet(bucketSuffix, backendId, path, context, false);
	}

	public Payload doGet(String bucketSuffix, String backendId, WebPath path, Context context, boolean checkOwnership) {
		return doGet(true, bucketSuffix, backendId, path, context, checkOwnership);
	}

	public Payload doGet(boolean withContent, String bucketSuffix, String backendId, WebPath path, Context context) {
		return doGet(withContent, bucketSuffix, backendId, path, context, false);
	}

	private Payload doGet(boolean withContent, String bucketSuffix, String backendId, WebPath path, Context context,
			boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(backendId);

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

		} catch (Throwable t) {

			Utils.closeSilently(s3Object);

			// 404 is OK
			if (t instanceof AmazonS3Exception //
					&& ((AmazonS3Exception) t).getStatusCode() == 404)
				return Payload.notFound();

			throw t;
		}

		Payload payload = new Payload(metadata.getContentType(), fileContent)//
				.withHeader(SpaceHeaders.ETAG, metadata.getETag())//
				.withHeader(SpaceHeaders.SPACEDOG_OWNER, owner);

		// Since fluent-http only provides gzip encoding,
		// we only set Content-Length header if Accept-encoding
		// does not contain gzip. In case client accepts gzip,
		// fluent will gzip this file stream and use 'chunked'
		// Transfer-Encoding incompatible with Content-Length header

		if (!context.header(ACCEPT_ENCODING).contains(GZIP))
			payload.withHeader(SpaceHeaders.CONTENT_LENGTH, //
					Long.toString(metadata.getContentLength()));

		if (context.query().getBoolean(PARAM_WITH_CONTENT_DISPOSITION, false))
			payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					metadata.getContentDisposition());

		return payload;
	}

	private class S3ObjectStreamingOutput implements StreamingOutput {

		private S3Object s3Object;

		public S3ObjectStreamingOutput(S3Object s3Object) {
			this.s3Object = s3Object;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			writeS3ObjectContent(s3Object, output);
		}
	}

	public Payload doDownload(String bucketSuffix, String backendId, ZipRequest request, Context context) {
		request.checkValid();
		return new Payload("application/octet-stream", //
				new ZipStreamingOutput(bucketSuffix, backendId, request))//
						.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
								SpaceHeaders.contentDisposition(request.fileName));
	}

	private class ZipStreamingOutput implements StreamingOutput {

		private String bucketSuffix;
		private String backendId;
		private ZipRequest request;

		public ZipStreamingOutput(String bucketSuffix, String backendId, ZipRequest request) {
			this.bucketSuffix = bucketSuffix;
			this.backendId = backendId;
			this.request = request;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			int index = 1;
			ZipOutputStream zip = new ZipOutputStream(output);
			for (String path : request.paths) {
				WebPath webPath = WebPath.parse(path);
				zip.putNextEntry(new ZipEntry(index++ + "-" + webPath.last()));
				S3Object s3Object = getS3Object(bucketSuffix, backendId, webPath);
				writeS3ObjectContent(s3Object, zip);
				zip.flush();
			}
			zip.close();
		}

	}

	private void writeS3ObjectContent(S3Object s3Object, OutputStream output) throws IOException {
		try {
			ByteStreams.copy(s3Object.getObjectContent(), output);
		} finally {
			Utils.closeSilently(s3Object);
		}
	}

	private S3Object getS3Object(String bucketSuffix, String backendId, WebPath path) {
		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(backendId);
		return s3.getObject(bucketName, s3Path.toS3Key());
	}

	public Payload doList(String bucketSuffix, String backendId, WebPath path, Context context) {

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
			return JsonPayload.error(404);

		JsonBuilder<ObjectNode> response = JsonPayload.builder();

		if (objects.isTruncated())
			response.put("next", fromS3Key(objects.getNextMarker()).toEscapedString());

		response.array("results");

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			WebPath objectPath = fromS3Key(summary.getKey());
			response.object()//
					.put("path", objectPath.toString())//
					.put("size", summary.getSize())//
					.put("lastModified", new DateTime(summary.getLastModified().getTime()).toString())//
					.put("etag", summary.getETag())//
					.end();
		}

		return JsonPayload.json(response);
	}

	public Payload doDelete(String bucketSuffix, Credentials credentials, WebPath path, boolean fileOnly,
			boolean checkOwnership) {

		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(credentials.backendId());

		JsonBuilder<ObjectNode> builder = JsonPayload.builder().array("deleted");

		// first try to delete this path as key

		try {
			ObjectMetadata metadata = s3.getObjectMetadata(bucketName, s3Path.toS3Key());

			getOrCheckOwnership(metadata, checkOwnership);

			s3.deleteObject(bucketName, s3Path.toS3Key());
			builder.add(path.toString());

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
					builder.add(fromS3Key(summary.getKey()).toString());

				next = objects.getNextMarker();

			} while (next != null);

		}
		return JsonPayload.json(builder);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, //
			WebPath path, Context context, long contentLength) {
		return doUpload(bucketSuffix, rootUri, credentials, path, context, contentLength, true);
	}

	public Payload doUpload(String bucketSuffix, String rootUri, Credentials credentials, //
			WebPath path, Context context, long contentLength, boolean enableS3Location) {

		// TODO check if this upload does not replace an older upload
		// in this case, check credentials and owner rights
		// should return FORBIDDEN if user not the owner of previous file
		// admin can replace whatever they need to replace?

		if (path.size() < 2)
			throw Exceptions.illegalArgument("path [%s] has no prefix", path.toString());

		String fileName = path.last();
		String bucketName = getBucketName(bucketSuffix);
		WebPath s3Path = path.addFirst(credentials.backendId());

		ObjectNode payload = Json7.object("success", true, "status", 200, //
				"path", path.toString(), //
				"location", toSpaceLocation(credentials.backendId(), rootUri, path), //
				"contentType", ContentTypes.parseFileExtension(fileName), //
				"contentLength", contentLength);

		if (enableS3Location)
			payload.put("s3", toS3Location(bucketName, s3Path));

		if (isDelayed(contentLength, context))
			return delayUpload(bucketName, fileName, s3Path, credentials, payload);

		ObjectMetadata metadata = new ObjectMetadata();

		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		metadata.setContentType(ContentTypes.parseFileExtension(fileName));
		metadata.setContentLength(contentLength);
		metadata.setContentDisposition(SpaceHeaders.contentDisposition(fileName));
		metadata.addUserMetadata(OWNER_META, credentials.name());
		metadata.addUserMetadata(OWNER_TYPE_META, credentials.level().toString());

		InputStream input = null;
		PutObjectResult result = null;

		try {
			input = context.request().inputStream();
			result = s3.putObject(bucketName, s3Path.toS3Key(), input, metadata);

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error uploading file to s3");

		} finally {
			Utils.closeSilently(input);
		}

		payload.put("etag", result.getETag())//
				.put("contentMd5", result.getContentMd5());

		return JsonPayload.json(payload);
	}

	//
	// Implementation
	//

	// Upload is delayed if file is bigger than 10 MB
	// or if query parameter 'delay' is true.
	private boolean isDelayed(long contentLength, Context context) {
		return contentLength > 10000000 || context.query().getBoolean(PARAM_DELAY, false);
	}

	protected Payload delayUpload(String bucketName, String fileName, WebPath s3Path, //
			Credentials credentials, ObjectNode payload) {

		GeneratePresignedUrlRequest signedRequest = new GeneratePresignedUrlRequest(//
				bucketName, s3Path.toS3Key(), HttpMethod.PUT);

		signedRequest.setContentType(ContentTypes.parseFileExtension(fileName));
		signedRequest.setExpiration( // 5 minutes to start upload
				new Date(System.currentTimeMillis() + 1000 * 60 * 5));

		// Adding Content-Disposition as parameter doesn't work yet
		// signedRequest.addRequestParameter(SpaceHeaders.CONTENT_DISPOSITION, //
		// SpaceHeaders.contentDisposition(fileName));

		signedRequest.addRequestParameter(X_AMZ_META + OWNER_META, credentials.name());
		signedRequest.addRequestParameter(X_AMZ_META + OWNER_TYPE_META, credentials.level().toString());

		URL url = s3.generatePresignedUrl(signedRequest);

		payload.put("status", HttpStatus.ACCEPTED)//
				.put("uploadTo", url.toString())//
				.put("message", "file is big, please HTTP PUT your file to 'uploadTo' location");

		return JsonPayload.json(payload, HttpStatus.ACCEPTED);
	}

	protected long checkContentLength(Context context, long sizeLimitInKB) {
		String contentLength = context.header(SpaceHeaders.CONTENT_LENGTH);
		if (Strings.isNullOrEmpty(contentLength))
			throw Exceptions.illegalArgument("no Content-Length header");

		long length = Long.valueOf(contentLength);
		if (length > sizeLimitInKB * 1024)
			throw Exceptions.illegalArgument(//
					"content length ([%s] bytes) is too big, limit is [%s] KB", //
					length, sizeLimitInKB);

		return length;
	}

	private String getOrCheckOwnership(ObjectMetadata metadata, boolean checkOwnership) {

		String owner = metadata.getUserMetaDataOf(OWNER_META);
		Credentials credentials = SpaceContext.getCredentials();

		if (!checkOwnership)
			return owner;

		String ownerLevel = metadata.getUserMetaDataOf(OWNER_TYPE_META);

		if (credentials.name().equals(owner) //
				&& credentials.level().toString().equals(ownerLevel))
			return owner;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private WebPath fromS3Key(String s3Key) {
		return WebPath.parse(s3Key).removeFirst();
	}

	private String toSpaceLocation(String backendId, String root, WebPath path) {
		return spaceUrl(backendId, root).append(path.toEscapedString()).toString();
	}

	private String toS3Location(String bucketName, WebPath path) {
		return new StringBuilder("https://").append(bucketName)//
				.append(".s3.amazonaws.com").append(path.toEscapedString()).toString();
	}
}
