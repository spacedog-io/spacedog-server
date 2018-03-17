package io.spacedog.client.push;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.DataWrapAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Installation extends DataObjectBase {

	public static final String TYPE = "installation";

	private String appId;
	private PushProtocol protocol;
	private String token;
	private String endpoint;
	private int badge;
	private Set<String> tags;

	public String appId() {
		return appId;
	}

	public Installation appId(String appId) {
		this.appId = appId;
		return this;
	}

	public PushProtocol protocol() {
		return protocol;
	}

	public Installation protocol(PushProtocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public String token() {
		return token;
	}

	public Installation token(String token) {
		this.token = token;
		return this;
	}

	public String endpoint() {
		return endpoint;
	}

	public Installation endpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	public int badge() {
		return badge;
	}

	public Installation badge(int badge) {
		this.badge = badge;
		return this;
	}

	public Set<String> tags() {
		if (tags == null)
			tags = Sets.newHashSet();
		return tags;
	}

	public Installation tags(String... tags) {
		return tags(Sets.newHashSet(tags));
	}

	public Installation tags(Set<String> tags) {
		this.tags = tags;
		return this;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Wrap extends DataWrapAbstract<Installation> {

		private Installation source;

		@Override
		public Class<Installation> sourceClass() {
			return Installation.class;
		}

		@Override
		public Installation source() {
			return this.source;
		}

		@Override
		public DataWrap<Installation> source(Installation source) {
			this.source = source;
			return this;
		}

		@Override
		public String type() {
			return TYPE;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {
		public long total;
		public ObjectNode aggregations;
		public List<Installation.Wrap> results;
	}

}
