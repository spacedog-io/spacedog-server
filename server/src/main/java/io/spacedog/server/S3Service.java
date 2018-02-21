/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import io.spacedog.http.ContentTypes;
import io.spacedog.http.SpaceHeaders;
import io.spacedog.http.WebPath;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

public class S3Service extends SpaceService {

	private static AmazonS3Client s3 = new AmazonS3Client();

	static {
		String awsRegion = Server.get().configuration().awsRegion().orElse("eu-west-1");
		s3.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
	}

	static AmazonS3Client s3Client() {
		return s3;
	}

	public Payload doGet(boolean withContent, S3File file, Context context) {

		if (withContent)
			file.open();

		if (!file.exists())
			return Payload.notFound();

		Payload payload = new Payload(file.contentType(), file)//
				.withHeader(SpaceHeaders.ETAG, file.eTag())//
				.withHeader(SpaceHeaders.SPACEDOG_OWNER, file.owner())//
				.withHeader(SpaceHeaders.SPACEDOG_GROUP, file.group());

		// Since fluent-http only provides gzip encoding,
		// we only set Content-Length header if Accept-encoding
		// does not contain gzip. In case client accepts gzip,
		// fluent will gzip this file stream and use 'chunked'
		// Transfer-Encoding incompatible with Content-Length header

		if (!context.header(SpaceHeaders.ACCEPT_ENCODING).contains(SpaceHeaders.GZIP))
			payload.withHeader(SpaceHeaders.CONTENT_LENGTH, //
					Long.toString(file.contentLength()));

		if (context.query().getBoolean(WITH_CONTENT_DISPOSITION, false))
			payload = payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					file.contentDisposition());

		return payload;
	}

	public Payload doList(S3File file, Context context) {

		ListObjectsRequest request = new ListObjectsRequest()//
				.withBucketName(file.bucketName())//
				.withPrefix(file.s3Prefix())//
				.withMaxKeys(context.query().getInteger("size", 100));

		String next = context.get("next");
		if (!Strings.isNullOrEmpty(next)) {
			S3File marker = new S3File(file.bucketName(), file.backendId(), next);
			request.setMarker(marker.s3Key());
		}

		ObjectListing objects = s3.listObjects(request);

		// only root path allows empty list
		if (objects.getObjectSummaries().isEmpty() && file.path().size() > 1)
			return JsonPayload.error(404).build();

		JsonPayload payload = JsonPayload.ok();

		if (objects.isTruncated())
			payload.withFields("next", fromS3Key(objects.getNextMarker()).toEscapedString());

		ArrayNode results = Json.array();

		for (S3ObjectSummary summary : objects.getObjectSummaries()) {
			WebPath objectPath = fromS3Key(summary.getKey());
			results.add(Json.object(//
					"path", objectPath.toString(), //
					"location", toSpaceLocation(file.rootUri(), objectPath), //
					"size", summary.getSize(), //
					"lastModified", new DateTime(summary.getLastModified().getTime()).toString(), //
					"etag", summary.getETag()));
		}

		return payload.withResults(results).build();
	}

	public Payload doDelete(S3File file) {

		ArrayNode deleted = Json.array();

		if (file.exists()) {
			file.delete();
			deleted.add(file.path().toString());
		}

		return JsonPayload.ok().withFields("deleted", deleted).build();
	}

	public Payload doDeleteAll(S3File file) {

		if (file.exists())
			throw Exceptions.runtime("file [%s] exists", file);

		String next = null;
		ArrayNode deleted = Json.array();

		do {

			ListObjectsRequest request = new ListObjectsRequest()//
					.withBucketName(file.bucketName())//
					.withPrefix(file.s3Prefix())//
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

	public Payload doUpload(S3File file, Context context) {
		return doUpload(file, false, context);
	}

	public Payload doUpload(S3File file, boolean enableS3Location, Context context) {

		// TODO check if this upload does not replace an older upload
		// in this case, check crdentials and owner rights
		// should return FORBIDDEN if user not the owner of previous file
		// admin can replace whatever they need to replace?

		ObjectMetadata metadata = s3Metadata(file, context);

		try {
			PutObjectResult putResult = s3.putObject(new PutObjectRequest(//
					file.bucketName(), file.s3Key(), //
					context.request().inputStream(), metadata));

			JsonPayload payload = JsonPayload.ok()//
					.withFields("path", file.path().toString())//
					.withFields("location", toSpaceLocation(file.rootUri(), file.path()))//
					.withFields("contentType", metadata.getContentType())//
					.withFields("contentLength", metadata.getContentLength())//
					.withFields("expirationTime", putResult.getExpirationTime())//
					.withFields("etag", putResult.getETag())//
					.withFields("contentMd5", putResult.getContentMd5());

			if (enableS3Location)
				payload.withFields("publicLocation", file.s3Location());

			return payload.build();

		} catch (IOException e) {
			throw Exceptions.runtime(e, "error reading request input stream");
		}
	}

	public Payload doZip(List<S3File> files, String fileName) {

		return new Payload("application/octet-stream", //
				new ZipStreamingOutput(files))//
						.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
								SpaceHeaders.contentDisposition(fileName));
	}

	//
	// Implementation
	//

	private ObjectMetadata s3Metadata(S3File file, Context context) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType(file.fileName(), context));
		metadata.setContentLength(file.contentLength());
		metadata.setContentDisposition(SpaceHeaders.contentDisposition(file.fileName()));
		metadata.addUserMetadata(OWNER_FIELD, file.owner());
		metadata.addUserMetadata(GROUP_FIELD, file.group());
		return metadata;
	}

	protected long checkContentLength(Context context, long sizeLimitInKB) {
		String contentLength = context.header(SpaceHeaders.CONTENT_LENGTH);
		if (Strings.isNullOrEmpty(contentLength))
			throw Exceptions.illegalArgument("Content-Length header is required");

		long length = Long.valueOf(contentLength);
		if (length > sizeLimitInKB * 1024)
			throw Exceptions.illegalArgument(//
					"content is too big, limit is [%s] KB", //
					length, sizeLimitInKB);

		return length;
	}

	private class ZipStreamingOutput implements StreamingOutput {

		private List<S3File> files;

		public ZipStreamingOutput(List<S3File> files) {
			this.files = files;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			ZipOutputStream zip = new ZipOutputStream(output);
			for (int i = 0; i < files.size(); i++) {
				S3File file = files.get(i);
				zip.putNextEntry(new ZipEntry(file.path().last()));
				file.write(zip);
				zip.flush();
			}
			// TODO put this in a finally block?
			zip.close();
		}

	}

	public static List<S3File> toS3Files(String bucketName, List<String> paths) {
		String backendId = SpaceContext.backendId();
		return paths.stream()//
				.map(path -> new S3File(bucketName, backendId, path))//
				.collect(Collectors.toList());
	}

	private String contentType(String fileName, Context context) {
		// TODO
		// use the provided content-type if specific first
		// if none derive from file extension
		return Strings.isNullOrEmpty(fileName) //
				? "application/octet-stream"
				: ContentTypes.parseFileExtension(fileName);
	}

	private WebPath fromS3Key(String s3Key) {
		return WebPath.parse(s3Key).removeFirst();
	}

	private String toSpaceLocation(String root, WebPath path) {
		return spaceUrl(root).append(path.toEscapedString()).toString();
	}

}
