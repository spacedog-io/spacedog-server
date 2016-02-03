package io.spacedog.client;

public enum SpaceTarget {

	local("localhost", 8443, 8080, false), //
	staging("spacerepublic.net", 443, 80, true), //
	production("spacedog.io", 443, 80, true);

	private String host;
	private int sslPort;
	private int nonSslPort;
	private boolean ssl;

	private SpaceTarget(String host, int sslPort, int nonSslPort, boolean ssl) {
		this.host = host;
		this.sslPort = sslPort;
		this.nonSslPort = nonSslPort;
		this.ssl = ssl;
	}

	public String host() {
		return host;
	}

	public int sslPort() {
		return sslPort;
	}

	public int nonSslPort() {
		return nonSslPort;
	}

	public boolean ssl() {
		return ssl;
	}
}
