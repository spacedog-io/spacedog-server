package io.spacedog.services.snapshot;

import java.io.InputStream;

import io.spacedog.services.file.FileStore;
import io.spacedog.utils.Utils;

public class FileBackup {

	private String backendId;
	private String repository;
	private FileStore store;

	public FileBackup(String backendId, String repository, FileStore store) {
		this.store = store;
		this.backendId = backendId;
		this.repository = Utils.isNullOrEmpty(repository) //
				? backendId
				: backendId + '-' + repository;
	}

	public String backendId() {
		return backendId;
	}

	public InputStream get(String bucket, String key) {
		return store.get(repository, bucket, key);
	}

	public void restore(String bucket, String key, long length, InputStream bytes) {
		store.restore(repository, bucket, key, length, bytes);
	}
}