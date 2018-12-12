package io.spacedog.services.file;

import java.io.InputStream;
import java.util.Iterator;

public interface FileStore {

	public class PutResult {
		public String key;
		public String hash;
	}

	PutResult put(String backendId, String bucket, InputStream bytes, Long length);

	void restore(String backendId, String bucket, String key, InputStream bytes, Long length);

	InputStream get(String backendId, String bucket, String key);

	boolean exists(String backendId, String bucket, String key);

	boolean check(String backendId, String bucket, String key, String hash);

	Iterator<String> list(String backendId, String bucket);

	void deleteAll(String backendId);

	void deleteAll(String backendId, String bucket);

	void delete(String backendId, String bucket, String key);
}
