package io.spacedog.client.file;

public enum FileStoreType {
	fs, elastic, s3;

	public String toElasticRepoType() {
		return toString().toLowerCase();
	}
}