package io.spacedog.client;

public enum SpaceTarget {

	local("localhost", 4444, 8888, false), //
	production("spacedog.io", 443, 80, true);

	private String host;
	private int mainPort;
	private int optionalPort;
	private boolean ssl;

	private SpaceTarget(String host, int mainPort, int optionalPort, boolean ssl) {
		this.host = host;
		this.mainPort = mainPort;
		this.optionalPort = optionalPort;
		this.ssl = ssl;
	}

	public String host() {
		return host;
	}

	public int port() {
		return mainPort;
	}

	public int optionalPort() {
		return optionalPort;
	}

	public boolean ssl() {
		return ssl;
	}
}
