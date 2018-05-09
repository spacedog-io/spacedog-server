package io.spacedog.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.WebPath;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.StreamingOutput;

public class S3File implements Closeable, StreamingOutput {

	private static final String bucketName = S3FileStore.getBucketName("files");

	private String backendId;
	private WebPath path;
	private S3Object s3Object;
	private String rootUri;
	private String fileName;
	private boolean loaded;
	private boolean exists;

	// s3 metadata fields

	private String group;
	private String owner;
	private String eTag;
	private long contentLength;
	private String contentType;
	private String contentDisposition;

	public S3File(WebPath path) {
		this(SpaceContext.backendId(), path);
	}

	public S3File(String backendId, String path) {
		this(backendId, WebPath.parse(path));
	}

	public S3File(String backendId, WebPath path) {
		this.backendId = backendId;
		this.path = path;
	}

	//
	// Getters Setters
	//

	public String bucketName() {
		return bucketName;
	}

	public String backendId() {
		return backendId;
	}

	public String fileName() {
		return fileName;
	}

	public S3File fileName(String name) {
		this.fileName = name;
		return this;
	}

	public long contentLength() {
		return contentLength;
	}

	public S3File contentLength(long contentLength) {
		this.contentLength = contentLength;
		return this;
	}

	public String owner() {
		if (owner == null)
			checkMetadata();
		return owner;
	}

	public String group() {
		if (group == null)
			checkMetadata();
		return group;
	}

	public String rootUri() {
		return this.rootUri;
	}

	public S3File rootUri(String rootUri) {
		this.rootUri = rootUri;
		return this;
	}

	public String contentType() {
		if (contentType == null)
			checkMetadata();
		return contentType;
	}

	public String eTag() {
		if (eTag == null)
			checkMetadata();
		return eTag;
	}

	public String contentDisposition() {
		if (contentDisposition == null)
			checkMetadata();
		return contentDisposition;
	}

	//
	// Other methods
	//

	public S3File owner(Credentials credentials) {
		owner = credentials.id();
		group = credentials.group();
		return this;
	}

	public WebPath path() {
		return path;
	}

	public String s3Key() {
		return path.addFirst(backendId).join();
	}

	public String s3Prefix() {
		return s3Key() + WebPath.SLASH;
	}

	public boolean exists() {
		checkMetadata();
		return exists;
	}

	@Override
	public String toString() {
		return path().toString();
	}

	public String s3Location() {
		return new StringBuilder("https://").append(bucketName)//
				.append(".s3.amazonaws.com")//
				.append(WebPath.SLASH).append(backendId)//
				.append(path.toEscapedString()).toString();
	}

	//
	// S3 calls
	//

	public void open() {
		try {
			if (s3Object == null) {
				loaded = true;
				s3Object = S3FileStore.s3().getObject(bucketName, s3Key());
				digest(s3Object.getObjectMetadata());
			}

		} catch (AmazonS3Exception e) {
			close();
			if (e.getStatusCode() != HttpStatus.NOT_FOUND)
				throw e;
		}
	}

	public void delete() {
		S3FileStore.s3().deleteObject(bucketName, s3Key());
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

	@Override
	public void close() {
		Utils.closeSilently(s3Object);
	}

	//
	// Permissions
	//

	public String checkGroup(Credentials credentials) {
		if (credentials.id().equals(group))
			return group;

		// if forbidden close s3 connection anyway
		// before to throw anything
		close();
		throw Exceptions.insufficientCredentials(credentials);
	}

	public String checkOwner(Credentials credentials) {
		if (credentials.id().equals(owner))
			return owner;

		// if forbidden close s3 connection anyway
		// before to throw anything
		close();
		throw Exceptions.insufficientCredentials(credentials);
	}

	public void checkRead(RolePermissions permissions) {
		checkPermissions(permissions, //
				Permission.read, Permission.readGroup, Permission.readMine);
	}

	public void checkUpdate(RolePermissions permissions) {
		checkPermissions(permissions, //
				Permission.update, Permission.updateGroup, Permission.updateMine);
	}

	public void checkDelete(RolePermissions permissions) {
		checkPermissions(permissions, //
				Permission.delete, Permission.deleteGroup, Permission.deleteMine);
	}

	private void checkPermissions(RolePermissions permissions, //
			Permission all, Permission group, Permission mine) {

		checkPermissions(Collections.singletonList(this), permissions, all, group, mine);
	}

	public static void checkPermissions(List<S3File> files, RolePermissions roles, //
			Permission allPermission, Permission groupPermission, Permission minePermission) {

		Credentials credentials = SpaceContext.credentials();

		if (roles.containsOne(credentials, allPermission))
			return;

		if (roles.containsOne(credentials, groupPermission)) {
			for (S3File file : files)
				if (file.exists())
					file.checkGroup(credentials);
			return;
		}

		if (roles.containsOne(credentials, minePermission)) {
			for (S3File file : files)
				if (file.exists())
					file.checkOwner(credentials);
			return;
		}

		throw Exceptions.insufficientCredentials(credentials);
	}

	//
	// Implementation
	//

	private void checkMetadata() {
		try {
			if (!loaded) {
				loaded = true;
				digest(S3FileStore.s3().getObjectMetadata(bucketName, s3Key()));
			}

		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() != HttpStatus.NOT_FOUND)
				throw e;
		}
	}

	private void digest(ObjectMetadata metadata) {
		eTag = metadata.getETag();
		contentType = metadata.getContentType();
		contentDisposition = metadata.getContentDisposition();
		contentLength = metadata.getContentLength();
		owner = metadata.getUserMetaDataOf(SpaceFields.OWNER_FIELD);
		group = metadata.getUserMetaDataOf(SpaceFields.GROUP_FIELD);
		exists = true;
	}

}