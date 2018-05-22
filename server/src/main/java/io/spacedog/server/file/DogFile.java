package io.spacedog.server.file;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.http.WebPath;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DogFile extends DataObjectBase {

	private String path;
	private String bucketKey;
	private String name;
	private long length;
	private String contentType;
	private String hash;
	private String encryption;
	private Set<String> tags = Sets.newLinkedHashSet();

	public DogFile() {
	}

	public DogFile(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public String getBucketKey() {
		return bucketKey;
	}

	public void setBucketKey(String bucketKey) {
		this.bucketKey = bucketKey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public String getEncryption() {
		return encryption;
	}

	public void setEncryption(String Encryption) {
		this.encryption = Encryption;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getEscapedPath() {
		return WebPath.parse(path).toEscapedString();
	}
}
