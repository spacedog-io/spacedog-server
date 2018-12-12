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

	public SystemFileStore(Path storePath) {
		this.storePath = storePath;
	}

	@Override
	public PutResult put(String repo, String bucket, Long length, InputStream stream) {
		PutResult result = new PutResult();
		result.key = UUID.randomUUID().toString();
		HashingInputStream hashedStream = new HashingInputStream(Hashing.md5(), stream);
		restore(repo, bucket, result.key, length, hashedStream);
		result.hash = hashedStream.hash().toString();
		return result;
	}

	@Override
	public void restore(String repo, String bucket, String key, Long length, InputStream bytes) {
		try {
			Path path = storePath.resolve(repo).resolve(bucket);
			Files.createDirectories(path);
			Files.copy(bytes, path.resolve(key));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "store file in bucket [%s][%s] failed", repo, bucket);
		}
	}

	@Override
	public boolean exists(String repo, String bucket, String key) {
		Path path = storePath.resolve(repo).resolve(bucket).resolve(key);
		return Files.exists(path);
	}

	@Override
	public boolean check(String repo, String bucket, String key, String hash) {
		Path path = storePath.resolve(repo).resolve(bucket).resolve(key);
		if (Files.exists(path)) {
			try (HashingInputStream bytes = new HashingInputStream(Hashing.md5(), Files.newInputStream(path))) {
				ByteStreams.copy(bytes, ByteStreams.nullOutputStream());
				return bytes.hash().toString().equals(hash);
			} catch (IOException e) {
				throw Exceptions.runtime(e, "check file [%s][%s][%s] failed", repo, bucket, key);
			}
		} else
			return false;
	}

	@Override
	public InputStream get(String repo, String bucket, String key) {
		try {
			Path path = storePath.resolve(repo).resolve(bucket).resolve(key);
			return Files.newInputStream(path);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "get file [%s][%s][%s] failed", repo, bucket, key);
		}
	}

	@Override
	public Iterator<String> list(String repo, String bucket) {
		try {
			Path directory = storePath.resolve(repo).resolve(bucket);
			return Files.walk(directory)//
					.filter(path -> Files.isRegularFile(path))//
					.map(path -> path.getFileName().toString())//
					.iterator();
		} catch (IOException e) {
			throw Exceptions.runtime(e, "list bucket [%s][%s] failed", repo, bucket);
		}
	}

	// public void deleteAll() {
	// deleteAll(storePath);
	// }

	@Override
	public void deleteAll(String repo) {
		deleteAll(storePath.resolve(repo));
	}

	@Override
	public void deleteAll(String repo, String bucket) {
		deleteAll(storePath.resolve(repo).resolve(bucket));
	}

	@Override
	public void delete(String repo, String bucket, String key) {
		delete(storePath.resolve(repo).resolve(bucket).resolve(key));
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
