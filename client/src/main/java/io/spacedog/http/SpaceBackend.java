package io.spacedog.http;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class SpaceBackend {

	// main fields
	private String host;
	private int port;
	private boolean ssl;

	// specific multi backend fields
	private boolean multi = false;
	private String prefix = "";
	private String backendId = defaultBackendId();
	private boolean webApp = false;

	private SpaceBackend() {
	}

	//
	// Getters, setters and common methods
	//

	public String host() {
		return multi ? new UrlBuilder().appendHost().toString() : host;
	}

	public String backendId() {
		return backendId;
	}

	public int port() {
		return port;
	}

	public boolean webApp() {
		return webApp;
	}

	public boolean ssl() {
		return ssl;
	}

	public boolean multi() {
		return multi;
	}

	public String scheme() {
		return ssl ? "https" : "http";
	}

	@Override
	public String toString() {
		if (multi)
			return new UrlBuilder().appendScheme().appendHost("*").appendPort().toString();
		else
			return url();
	}

	//
	// URL methods
	//

	public StringBuilder urlBuilder() {
		return new UrlBuilder().appendScheme().appendHost().appendPort().build();
	}

	private class UrlBuilder {

		private UrlBuilder appendScheme() {
			builder.append(scheme()).append("://");
			return this;
		}

		private UrlBuilder appendHost() {
			if (multi)
				appendHost(defaultBackendId());
			else
				builder.append(host);
			return this;
		}

		private UrlBuilder appendHost(String backendId) {
			builder.append(prefix).append(backendId).append(host);
			return this;
		}

		private UrlBuilder appendPort() {
			if (port != 80 && port != 443)
				builder.append(":").append(port);
			return this;
		}

		public UrlBuilder append(String string) {
			builder.append(string);
			return this;
		}

		private StringBuilder build() {
			return builder;
		}

		@Override
		public String toString() {
			return build().toString();
		}

		private StringBuilder builder = new StringBuilder();

	}

	public String url() {
		return urlBuilder().toString();
	}

	public String url(String uri) {
		return urlBuilder().append(uri).toString();
	}

	//
	// Factory methods
	//

	public static SpaceBackend fromUrl(String url) {
		return fromUrl(url, false);
	}

	public static SpaceBackend fromUrl(String url, boolean webApp) {
		SpaceBackend target = new SpaceBackend();
		target.webApp = webApp;

		// handle scheme
		if (url.startsWith("https://")) {
			url = Utils.removePreffix(url, "https://");
			target.ssl = true;
		} else if (url.startsWith("http://")) {
			url = Utils.removePreffix(url, "http://");
			target.ssl = false;
		} else
			throw Exceptions.illegalArgument("invalid backend url [%s]", url);

		// handle multi backend
		if (url.indexOf('*') < 0) {
			target.multi = false;
			target.host = url;
		} else {
			String[] parts = url.split("\\*", 2);
			target.multi = true;
			target.host = parts[1];
			target.prefix = parts[0];
			target.backendId = defaultBackendId();
		}

		// handle url port
		if (target.host.indexOf(':') < 0) {
			target.port = target.ssl ? 443 : 80;
		} else {
			String[] parts = target.host.split(":", 2);
			target.host = parts[0];
			target.port = Integer.valueOf(parts[1]);
		}

		return target;
	}

	public static SpaceBackend fromDefaults(String name) {
		return defaultTargets.get(name);
	}

	public static SpaceBackend valueOf(String string) {
		return string.startsWith("http") ? fromUrl(string) : fromDefaults(string);
	}

	public Optional7<SpaceBackend> checkAndInstantiate(String requestHostAndPort) {
		String backendhostAndPort = hostAndPort();

		if (multi && requestHostAndPort.startsWith(prefix) //
				&& requestHostAndPort.endsWith(backendhostAndPort)) {

			String backendId = Utils.removeSuffix(//
					Utils.removePreffix(requestHostAndPort, prefix), //
					backendhostAndPort);

			// check if resulting backing id is well formed
			if (backendId.length() > 0 && !backendId.contains("."))
				return Optional7.of(instanciate(backendId));

		} else if (backendhostAndPort.equalsIgnoreCase(requestHostAndPort))
			return Optional7.of(this);

		return Optional7.empty();
	}

	public SpaceBackend instanciate() {
		return instanciate(defaultBackendId());
	}

	public SpaceBackend instanciate(String backendId) {
		checkIsMultiple();

		SpaceBackend backend = new SpaceBackend();
		backend.multi = false;
		backend.host = new UrlBuilder().appendHost(backendId).toString();
		backend.backendId = backendId;
		backend.port = port;
		backend.ssl = ssl;
		backend.webApp = webApp;

		return backend;
	}

	//
	// Default targets
	//

	public static SpaceBackend local = fromUrl("http://*.lvh.me:8443");
	public static SpaceBackend staging = fromUrl("https://*.spacerepublic.net");
	public static SpaceBackend production = fromUrl("https://*.spacedog.io");

	private static Map<String, SpaceBackend> defaultTargets = Maps.newHashMap();

	static {
		defaultTargets.put("local", local);
		defaultTargets.put("staging", staging);
		defaultTargets.put("production", production);
	}

	//
	// Private methods
	//

	private String hostAndPort() {
		return new UrlBuilder().append(host).appendPort().toString();
	}

	private void checkIsMultiple() {
		if (!multi)
			throw Exceptions.illegalArgument("backend [%s] is not multiple", this);
	}

	//
	// Backend id utils
	//

	private static final Pattern BACKEND_ID_PATTERN = Pattern.compile("[a-z0-9]{4,}");

	public static boolean isValid(String backendId) {

		if (!BACKEND_ID_PATTERN.matcher(backendId).matches())
			return false;

		if (backendId.indexOf("spacedog") > -1)
			return false;

		if (backendId.startsWith(defaultBackendId()))
			return false;

		return true;
	}

	public static void checkIsValid(String backendId) {

		if (Strings.isNullOrEmpty(backendId))
			throw new IllegalArgumentException("backend id is null or empty");

		if (!isValid(backendId))
			throw Exceptions.illegalArgument("backend id does not comply to: "//
					+ "is at least 4 characters long, "//
					+ "is only composed of a-z and 0-9 characters, "//
					+ "is lowercase, does not start with '%s', "//
					+ "does not contain 'spacedog'.", //
					defaultBackendId());
	}

	public static String defaultBackendId() {
		return "api";
	}

	public boolean isDefault() {
		return backendId.equals(defaultBackendId());
	}

	public static boolean isDefaultBackendId(String backendId) {
		return defaultBackendId().equals(backendId);
	}
}
