/**
 * © David Attias 2015
 */
package io.spacedog.services.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

import io.spacedog.server.ServerConfig;
import io.spacedog.utils.Exceptions;

public class SystemFileStore implements FileStore {

	private Path storePath;

	SystemFileStore() {
		storePath = ServerConfig.fileStorePath();
	}

	@Override
	public PutResult put(String backendId, String bucket, InputStream stream, Long length) {
		try {
			PutResult result = new PutResult();
			result.key = UUID.randomUUID().toString();
			Path path = storePath.resolve(backendId).resolve(bucket);
			Files.createDirectories(path);
			HashingInputStream hashedStream = new HashingInputStream(Hashing.md5(), stream);
			Files.copy(hashedStream, path.resolve(result.key));
			result.hash = hashedStream.hash().toString();
			return result;
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to store file in bucket [%s][%s]", backendId, bucket);
		}
	}

	@Override
	public boolean exists(String backendId, String bucket, String key) {
		Path path = storePath.resolve(backendId).resolve(bucket).resolve(key);
		return Files.exists(path);
	}

	@Override
	public InputStream get(String backendId, String bucket, String key) {
		try {
			Path path = storePath.resolve(backendId).resolve(bucket).resolve(key);
			return Files.newInputStream(path);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to get file [%s][%s][%s]", backendId, bucket, key);
		}
	}

	@Override
	public Iterator<String> list(String backendId, String bucket) {
		try {
			Path directory = storePath.resolve(backendId).resolve(bucket);
			return Files.walk(directory)//
					.filter(path -> Files.isRegularFile(path))//
					.map(path -> path.getFileName().toString())//
					.iterator();
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to list bucket [%s][%s]", backendId, bucket);
		}
	}

	// public void deleteAll() {
	// deleteAll(storePath);
	// }

	@Override
	public void deleteAll(String backendId) {
		deleteAll(storePath.resolve(backendId));
	}

	@Override
	public void deleteAll(String backendId, String bucket) {
		deleteAll(storePath.resolve(backendId).resolve(bucket));
	}

	@Override
	public void delete(String backendId, String bucket, String key) {
		delete(storePath.resolve(backendId).resolve(bucket).resolve(key));
	}

	//
	// Implementation
	//

	private void deleteAll(Path directory) {
		try {
			Files.walk(directory)//
					.filter(path -> Files.isRegularFile(path))//
					.forEach(path -> delete(path));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to delete directory [%s]", directory);
		}
	}

	private void delete(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to delete file [%s]", path);
		}
	}

}
