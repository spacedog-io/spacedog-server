/**
 * © David Attias 2015
 */
package io.spacedog.services.file;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.spacedog.client.http.WebPath;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import net.codestory.http.constants.HttpStatus;

public class S3FileStore implements FileStore {

	private final String bucketName;
	private AmazonS3 s3;

	S3FileStore(String bucketName) {
		this.bucketName = bucketName;
		this.s3 = AmazonS3ClientBuilder.standard()//
				.withRegion(ServerConfig.awsRegionOrDefault())//
				.build();
	}

	//
	// Get
	//

	@Override
	public boolean exists(String backendId, String bucket, String key) {
		return getMeta(backendId, bucket, key).isPresent();
	}

	@Override
	public InputStream get(String backendId, String bucket, String key) {
		try {
			S3Object s3Object = s3.getObject(bucketName, toS3Key(backendId, bucket, key));
			Server.closeAfterAll(s3Object);
			return s3Object.getObjectContent();

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
				return null;

			throw e;
		}
	}

	@Override
	public boolean check(String backendId, String bucket, String key, String hash) {
		return getMeta(backendId, bucket, key)//
				.map(meta -> meta.getETag()).orElse("")//
				.equals(hash);
	}

	private Optional<ObjectMetadata> getMeta(String backendId, String bucket, String key) {
		try {
			return Optional.of(s3.getObjectMetadata(bucketName, toS3Key(backendId, bucket, key)));

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND)
				return Optional.empty();

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
		result.hash = uploadToS3(backendId, bucket, result.key, bytes, length).getETag();
		return result;
	}

	@Override
	public void restore(String backendId, String bucket, String key, InputStream bytes, Long length) {
		uploadToS3(backendId, bucket, key, bytes, length);
	}

	private PutObjectResult uploadToS3(String backendId, String bucket, String key, InputStream bytes, Long length) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(length);
		return s3.putObject(bucketName, toS3Key(backendId, bucket, key), bytes, metadata);
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

	private class S3Iterator implements Iterator<String> {

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

	// public void deleteAll() {
	// doDeleteAll(toS3Key());
	// }

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

}
