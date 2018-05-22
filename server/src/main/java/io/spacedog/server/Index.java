package io.spacedog.server;

import java.util.Arrays;

import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;

public class Index {

	private String backendId;
	private String service;
	private String type;
	private int version = 0;

	public Index(String type) {
		this.type = Check.notNullOrEmpty(type, "type");
	}

	public String backendId() {
		if (backendId == null)
			backendId = Server.backend().backendId();
		return backendId;
	}

	public Index backendId(String backendId) {
		this.backendId = Check.notNullOrEmpty(backendId, "backendId");
		return this;
	}

	public String service() {
		return service;
	}

	public Index service(String service) {
		this.service = Check.notNullOrEmpty(service, "service");
		return this;
	}

	public String type() {
		return type;
	}

	public Index type(String type) {
		this.type = Check.notNullOrEmpty(type, "type");
		return this;
	}

	public static String[] types(Index[] indices) {
		return Arrays.stream(indices)//
				.map(index -> index.type())//
				.toArray(String[]::new);
	}

	public int version() {
		return version;
	}

	public Index version(int version) {
		this.version = version;
		return this;
	}

	public Index version(String version) {
		this.version = Integer.valueOf(Check.notNullOrEmpty(version, "version"));
		return this;
	}

	@Override
	public String toString() {
		String version = String.valueOf(this.version);
		return service == null //
				? String.join("-", backendId(), type, version)//
				: String.join("-", backendId(), service, type, version);
	}

	public static String[] toString(Index... indices) {
		return Arrays.stream(indices)//
				.map(index -> index.toString())//
				.toArray(String[]::new);
	}

	public String alias() {
		return service == null //
				? String.join("-", backendId(), type)//
				: String.join("-", backendId(), service, type);
	}

	public static String[] aliases(Index... indices) {
		return Arrays.stream(indices)//
				.map(index -> index.alias())//
				.toArray(String[]::new);
	}

	public static Index toIndex(String type) {
		return new Index(type);
	}

	public static Index valueOf(String index) {
		String[] parts = index.split("-", 4);

		if (parts.length == 3)
			return new Index(parts[1]).backendId(parts[0]).version(parts[2]);

		if (parts.length == 4)
			return new Index(parts[2]).backendId(parts[0])//
					.service(parts[1]).version(parts[3]);

		throw Exceptions.runtime("index [%s] is invalid", index);
	}
}