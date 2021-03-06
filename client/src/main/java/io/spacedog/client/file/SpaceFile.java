package io.spacedog.client.file;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.http.WebPath;
import io.spacedog.utils.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class SpaceFile extends DataObjectBase {

	private String path;
	private String key;
	private String name;
	private long length;
	private String contentType;
	private String hash;
	private String encryption;
	private boolean snapshot;
	private Set<String> tags = Sets.newLinkedHashSet();

	public SpaceFile() {
	}

	public SpaceFile(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return Utils.isNullOrEmpty(name) ? WebPath.parse(path).last() : name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExportEntry() {
		return Utils.isNullOrEmpty(name) ? path : path + "/" + name;
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

	public boolean getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	public String getEscapedPath() {
		return WebPath.parse(path).toEscapedString();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class FileList {
		public long total;
		public List<SpaceFile> files;
		public String next;
	}

	public static String flatPath(String path) {
		if (path.charAt(0) == '/')
			path = path.substring(1);
		path = path.replace('/', '-');
		return path;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof SpaceFile))
			return false;

		SpaceFile file = (SpaceFile) obj;
		return Objects.equals(path, file.path);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(path);
	}

	@Override
	public String toString() {
		return String.format("SpaceFile[%s]", path);
	}
}
