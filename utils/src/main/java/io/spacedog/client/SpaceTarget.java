package io.spacedog.client;

public enum SpaceTarget {

	local("lvh.me", 8443, 8080, false), //
	staging("spacerepublic.net", 443, 80, true), //
	apiprod("api.spacedog.io", 443, 80, true), //
	production("spacedog.io", 443, 80, true);

	private String host;
	private int port;
	private int redirectedPort;
	private boolean ssl;

	private SpaceTarget(String host, int port, int redirectedPort, boolean ssl) {
		this.host = host;
		this.port = port;
		this.redirectedPort = redirectedPort;
		this.ssl = ssl;
	}

	public String host() {
		return host;
	}

	public int port() {
		return port;
	}

	public int redirectedPort() {
		return redirectedPort;
	}

	public boolean ssl() {
		return ssl;
	}

	public StringBuilder urlBuilder() {
		return new StringBuilder(ssl ? "https://" : "http://")//
				.append(host)//
				.append(port == 443 ? "" : ":" + port);
	}

	public StringBuilder urlBuilder(String backendId) {
		return new StringBuilder(ssl ? "https://" : "http://")//
				.append(backendId)//
				.append('.')//
				.append(host)//
				.append(port == 443 ? "" : ":" + port);
	}

	public StringBuilder redirectedUrlBuilder() {
		return new StringBuilder("http://")//
				.append(host)//
				.append(redirectedPort == 80 ? "" : ":" + redirectedPort);
	}

	public StringBuilder redirectedUrlBuilder(String backendId) {
		return new StringBuilder("http://")//
				.append(backendId)//
				.append('.')//
				.append(host)//
				.append(redirectedPort == 80 ? "" : ":" + redirectedPort);
	}

	public String url() {
		return urlBuilder().toString();
	}

	public String redirectedUrl() {
		return redirectedUrlBuilder().toString();
	}

	public String url(String uri) {
		return urlBuilder().append(uri).toString();
	}

	public String redirectedUrl(String uri) {
		return redirectedUrlBuilder().append(uri).toString();
	}
}
