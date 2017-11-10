package io.spacedog.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.Utils;
import io.spacedog.utils.WebPath;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.StreamingOutput;

public class S3File implements Closeable, StreamingOutput {

	private String bucketName;
	private WebPath path;
	private ObjectMetadata metadata;
	private S3Object s3Object;

	public S3File(String bucketName, WebPath path) {
		this.bucketName = bucketName;
		this.path = path;
	}

	public ObjectMetadata metadata() {
		try {
			if (metadata == null)
				metadata = S3Service.s3Client().getObjectMetadata(bucketName, path.toS3Key());

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() != HttpStatus.NOT_FOUND)
				throw e;
		}
		return metadata;
	}

	public void open() {
		try {
			if (s3Object == null) {
				s3Object = S3Service.s3Client().getObject(bucketName, path.toS3Key());
				metadata = s3Object.getObjectMetadata();
			}

		} catch (AmazonS3Exception e) {
			close();
			if (e.getStatusCode() != HttpStatus.NOT_FOUND)
				throw e;
		}
	}

	@Override
	public void write(OutputStream output) throws IOException {
		try {
			open();
			if (exists())
				ByteStreams.copy(s3Object.getObjectContent(), output);
		} finally {
			close();
		}
	}

	public String checkOwner() {
		return checkOwner(SpaceContext.credentials());
	}

	public String checkOwner(Credentials credentials) {
		String owner = metadata().getUserMetaDataOf(SpaceFields.OWNER_FIELD);

		if (credentials.id().equals(owner))
			return owner;

		// if forbidden close s3 connection anyway
		// before to throw anything
		close();
		throw Exceptions.insufficientCredentials(credentials);
	}

	public String owner() {
		return metadata().getUserMetaDataOf(SpaceFields.OWNER_FIELD);
	}

	@Override
	public void close() {
		Utils.closeSilently(s3Object);
	}

	public void delete() {
		S3Service.s3Client().deleteObject(bucketName, path.toS3Key());
	}

	public WebPath path() {
		return path;
	}

	public String bucketName() {
		return bucketName;
	}

	public boolean exists() {
		return metadata() != null;
	}
}