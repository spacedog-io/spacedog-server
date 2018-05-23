/**
 * © David Attias 2015
 */
package io.spacedog.server.file;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.spacedog.client.http.WebPath;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import net.codestory.http.constants.HttpStatus;

public class S3FileStore implements FileStore {

	S3FileStore() {
	}

	//
	// Get
	//

	@Override
	public boolean exists(String backendId, String bucket, String id) {
		try {
			s3.getObjectMetadata(bucketName, toS3Key(backendId, bucket, id));
			return true;

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
				return false;
			throw e;
		}
	}

	@Override
	public InputStream get(String backendId, String bucket, String key) {
		S3Object s3Object = null;
		try {
			s3Object = s3.getObject(bucketName, toS3Key(backendId, bucket, key));
			Server.closeAfterAll(s3Object);
			return s3Object.getObjectContent();

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
				return null;

			throw e;
		}
	}

	//
	// Upload
	//

	@Override
	public PutResult put(String backendId, String bucket, InputStream bytes, Long length) {
		PutResult result = new PutResult();
		result.key = UUID.randomUUID().toString();

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(length);

		result.hash = s3.putObject(bucketName, //
				toS3Key(backendId, bucket, result.key), //
				bytes, metadata)//
				.getETag();

		return result;
	}

	//
	// List
	//

	@Override
	public Iterator<String> list(String backendId, String bucket) {

		ObjectListing listing = s3.listObjects(//
				new ListObjectsRequest()//
						.withBucketName(bucketName)//
						.withPrefix(toS3Key(backendId, bucket))//
						.withMaxKeys(1000));

		return new S3Iterator(listing);
	}

	private static class S3Iterator implements Iterator<String> {

		private ObjectListing listing;
		private List<S3ObjectSummary> summaries;
		private int i = 0;

		public S3Iterator(ObjectListing listing) {
			this.listing = listing;
			this.summaries = listing.getObjectSummaries();
		}

		@Override
		public boolean hasNext() {
			return i < summaries.size();
		}

		@Override
		public String next() {
			if (hasNext() == false)
				throw new NoSuchElementException();

			String key = WebPath.parse(summaries.get(i++).getKey()).last();
			fetchNextBatchIfNecessary();
			return key;
		}

		private void fetchNextBatchIfNecessary() {
			if (hasNext() == false //
					&& listing.isTruncated()) {
				listing = s3.listNextBatchOfObjects(listing);
				this.summaries = listing.getObjectSummaries();
			}
		}
	};

	//
	// Delete
	//

	@Override
	public void delete(String backendId, String bucket, String key) {
		s3.deleteObject(bucketName, toS3Key(backendId, bucket, key));
	}

	@Override
	public void deleteAll() {
		doDeleteAll(toS3Key());
	}

	@Override
	public void deleteAll(String backendId) {
		doDeleteAll(toS3Key(backendId));
	}

	@Override
	public void deleteAll(String backendId, String bucket) {
		doDeleteAll(toS3Key(backendId, bucket));
	}

	private void doDeleteAll(String s3KeyPrefix) {

		String next = null;

		do {

			ListObjectsRequest request = new ListObjectsRequest()//
					.withBucketName(bucketName)//
					.withPrefix(s3KeyPrefix)//
					.withMaxKeys(1000);

			if (next != null)
				request.setMarker(next);

			ObjectListing objects = s3.listObjects(request);

			if (!objects.getObjectSummaries().isEmpty())
				s3.deleteObjects(new DeleteObjectsRequest(bucketName)//
						.withKeys(getKeyVersions(objects)));

			next = objects.getNextMarker();

		} while (next != null);

	}

	private List<KeyVersion> getKeyVersions(ObjectListing objects) {
		return objects.getObjectSummaries().stream()//
				.map(summary -> new KeyVersion(summary.getKey()))//
				.collect(Collectors.toList());
	}

	//
	// Implementation
	//

	private String toS3Key(String... keySegments) {
		return String.join("/", keySegments);
	}

	private static final String bucketName = ServerConfig.awsBucketPrefix() + "files";

	private final static AmazonS3 s3 = AmazonS3ClientBuilder.standard()//
			.withRegion(ServerConfig.awsRegionOrDefault())//
			.build();

}
