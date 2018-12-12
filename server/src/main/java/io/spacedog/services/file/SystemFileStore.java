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
import com.google.common.io.ByteStreams;

import io.spacedog.utils.Exceptions;

public class SystemFileStore implements FileStore {

	private Path storePath;

	SystemFileStore(Path storePath) {
		this.storePath = storePath;
	}

	@Override
	public PutResult put(String backendId, String bucket, InputStream stream, Long length) {
		PutResult result = new PutResult();
		result.key = UUID.randomUUID().toString();
		HashingInputStream hashedStream = new HashingInputStream(Hashing.md5(), stream);
		restore(backendId, bucket, result.key, hashedStream, length);
		result.hash = hashedStream.hash().toString();
		return result;
	}

	@Override
	public void restore(String backendId, String bucket, String key, InputStream bytes, Long length) {
		try {
			Path path = storePath.resolve(backendId).resolve(bucket);
			Files.createDirectories(path);
			Files.copy(bytes, path.resolve(key));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "store file in bucket [%s][%s] failed", backendId, bucket);
		}
	}

	@Override
	public boolean exists(String backendId, String bucket, String key) {
		Path path = storePath.resolve(backendId).resolve(bucket).resolve(key);
		return Files.exists(path);
	}

	@Override
	public boolean check(String backendId, String bucket, String key, String hash) {
		Path path = storePath.resolve(backendId).resolve(bucket).resolve(key);
		if (Files.exists(path)) {
			try (HashingInputStream bytes = new HashingInputStream(Hashing.md5(), Files.newInputStream(path))) {
				ByteStreams.copy(bytes, ByteStreams.nullOutputStream());
				return bytes.hash().toString().equals(hash);
			} catch (IOException e) {
				throw Exceptions.runtime(e, "check file [%s][%s][%s] failed", backendId, bucket, key);
			}
		} else
			return false;
	}

	@Override
	public InputStream get(String backendId, String bucket, String key) {
		try {
			Path path = storePath.resolve(backendId).resolve(bucket).resolve(key);
			return Files.newInputStream(path);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "get file [%s][%s][%s] failed", backendId, bucket, key);
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
			throw Exceptions.runtime(e, "list bucket [%s][%s] failed", backendId, bucket);
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
			if (Files.isDirectory(directory))
				Files.walk(directory)//
						.filter(path -> Files.isRegularFile(path))//
						.forEach(path -> delete(path));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "delete directory [%s] failed", directory);
		}
	}

	private void delete(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "delete file [%s] failed", path);
		}
	}

}
