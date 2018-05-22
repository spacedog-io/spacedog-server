package io.spacedog.server.file;

import java.io.InputStream;
import java.util.Iterator;

public interface FileStore {

	public class PutResult {
		public String key;
		public String hash;
	}

	PutResult put(String backendId, String bucket, InputStream bytes, Long length);

	boolean exists(String backendId, String bucket, String key);

	InputStream get(String backendId, String bucket, String key);

	Iterator<String> list(String backendId, String bucket);

	void deleteAll();

	void deleteAll(String backendId);

	void deleteAll(String backendId, String bucket);

	void delete(String backendId, String bucket, String key);
}
