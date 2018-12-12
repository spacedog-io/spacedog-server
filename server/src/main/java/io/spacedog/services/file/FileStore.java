package io.spacedog.services.file;

import java.io.InputStream;
import java.util.Iterator;

public interface FileStore {

	public class PutResult {
		public String key;
		public String hash;
	}

	PutResult put(String repo, String bucket, Long length, InputStream bytes);

	void restore(String repo, String bucket, String key, Long length, InputStream bytes);

	InputStream get(String repo, String bucket, String key);

	boolean exists(String repo, String bucket, String key);

	boolean check(String repo, String bucket, String key, String hash);

	Iterator<String> list(String repo, String bucket);

	void deleteAll(String repo);

	void deleteAll(String repo, String bucket);

	void delete(String repo, String bucket, String key);
}
