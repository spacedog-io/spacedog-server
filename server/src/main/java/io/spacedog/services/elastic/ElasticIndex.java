package io.spacedog.services.elastic;

import java.util.Arrays;

import io.spacedog.server.Server;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;

public class ElasticIndex {

	private String backendId;
	private String service;
	private String type;
	private int version = 0;

	public ElasticIndex(String service) {
		this.service = Check.notNullOrEmpty(service, "service");
	}

	public String backendId() {
		if (backendId == null)
			backendId = Server.backend().id();
		return backendId;
	}

	public ElasticIndex backendId(String backendId) {
		this.backendId = Check.notNullOrEmpty(backendId, "backendId");
		return this;
	}

	public String service() {
		return service;
	}

	public ElasticIndex service(String service) {
		this.service = Check.notNullOrEmpty(service, "service");
		return this;
	}

	public String type() {
		return type == null ? service : type;
	}

	public ElasticIndex type(String type) {
		this.type = Check.notNullOrEmpty(type, "type");
		return this;
	}

	public int version() {
		return version;
	}

	public ElasticIndex version(int version) {
		this.version = version;
		return this;
	}

	public ElasticIndex version(String version) {
		this.version = Integer.valueOf(Check.notNullOrEmpty(version, "version"));
		return this;
	}

	@Override
	public String toString() {
		String version = String.valueOf(this.version);
		return type == null //
				? String.join("-", backendId(), service, version)//
				: String.join("-", backendId(), service, type, version);
	}

	@Override
	public boolean equals(Object obj) {
		return obj == null ? false : toString().equals(obj.toString());
	}

	public static String[] toString(ElasticIndex... indices) {
		return Arrays.stream(indices)//
				.map(index -> index.toString())//
				.toArray(String[]::new);
	}

	public String alias() {
		return type == null //
				? String.join("-", backendId(), service)//
				: String.join("-", backendId(), service, type);
	}

	public static String[] aliases(ElasticIndex... indices) {
		return Arrays.stream(indices)//
				.map(index -> index.alias())//
				.toArray(String[]::new);
	}

	public static ElasticIndex valueOf(String index) {
		String[] parts = index.split("-", 4);

		if (parts.length == 3)
			return new ElasticIndex(parts[1]).backendId(parts[0]).version(parts[2]);

		if (parts.length == 4)
			return new ElasticIndex(parts[1]).backendId(parts[0])//
					.type(parts[2]).version(parts[3]);

		throw Exceptions.runtime("index [%s] is invalid", index);
	}
}