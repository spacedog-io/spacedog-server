package io.spacedog.client;

public enum SpaceTarget {

	local("lvh.me", 8443, false), //
	staging("spacerepublic.net", 443, true), //
	colibee("colibee.spacerepublic.net", 80, false), //
	production("spacedog.io", 443, true);

	private String host;
	private int port;
	private boolean ssl;

	private SpaceTarget(String host, int port, boolean ssl) {
		this.host = host;
		this.port = port;
		this.ssl = ssl;
	}

	public String host() {
		return host;
	}

	public int port() {
		return port;
	}

	public boolean ssl() {
		return ssl;
	}

	private StringBuilder urlBuilder(String backendId) {
		return new StringBuilder(ssl ? "https://" : "http://")//
				.append(backendId)//
				.append('.')//
				.append(host)//
				.append(port == 443 ? "" : ":" + port);
	}

	public String url(String backendId) {
		return urlBuilder(backendId).toString();
	}

	public String url(String backendId, String uri) {
		return urlBuilder(backendId).append(uri).toString();
	}
}
