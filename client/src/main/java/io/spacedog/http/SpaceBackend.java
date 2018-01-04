package io.spacedog.http;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class SpaceBackend {

	public static final String API = "api";
	public static final String SPACEDOG = "spacedog";

	private boolean ssl;
	private String hostPrefix = "";
	private String backendId = "";
	private String hostSuffix;
	private int port;
	private boolean multi = false;

	private SpaceBackend() {
	}

	//
	// Getters, setters and common methods
	//

	public String scheme() {
		return ssl ? "https" : "http";
	}

	public String backendId() {
		return backendId.isEmpty() ? SPACEDOG : backendId;
	}

	public String host() {
		return new UrlBuilder().appendHost().toString();
	}

	public int port() {
		return port;
	}

	private String hostSuffixAndPort() {
		return new UrlBuilder().append(hostSuffix).appendPort().toString();
	}

	public boolean ssl() {
		return ssl;
	}

	public boolean isMulti() {
		return multi;
	}

	@Override
	public String toString() {
		return isMulti() //
				? new UrlBuilder().appendScheme().appendHost("*").appendPort().toString()//
				: url();
	}

	@Override
	public boolean equals(Object object) {
		if (object != null && object instanceof SpaceBackend) {
			SpaceBackend other = (SpaceBackend) object;
			return url().equalsIgnoreCase(other.url());
		}
		return false;
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
			return appendHost(multi ? API : backendId);
		}

		private UrlBuilder appendHost(String backendId) {
			builder.append(hostPrefix).append(backendId).append(hostSuffix);
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
		SpaceBackend target = new SpaceBackend();

		// handle scheme
		if (url.startsWith("https://")) {
			url = Utils.removePreffix(url, "https://");
			target.ssl = true;
		} else if (url.startsWith("http://")) {
			url = Utils.removePreffix(url, "http://");
			target.ssl = false;
		} else
			throw Exceptions.illegalArgument("backend url [%s] is invalid", url);

		// handle multi backend
		String suffixAndPort = null;
		int index = url.indexOf('*');
		if (index < 0) {
			suffixAndPort = url;
		} else {
			target.multi = true;
			target.hostPrefix = url.substring(0, index);
			suffixAndPort = url.substring(index + 1);
		}

		// handle url port
		index = suffixAndPort.indexOf(':');
		if (index < 0) {
			target.hostSuffix = suffixAndPort;
			target.port = target.ssl ? 443 : 80;
		} else {
			target.hostSuffix = suffixAndPort.substring(0, index);
			target.port = Integer.valueOf(suffixAndPort.substring(index + 1));
		}

		return target;
	}

	public static SpaceBackend fromDefaults(String name) {
		return defaultTargets.get(name);
	}

	public static SpaceBackend valueOf(String string) {
		SpaceBackend backend = string.startsWith("http") //
				? fromUrl(string)
				: fromDefaults(string);
		if (backend == null)
			throw Exceptions.illegalArgument("backend url [%s] is invalid");
		return backend;
	}

	public Optional7<SpaceBackend> checkRequest(String requestHostAndPort) {
		String backendSuffixAndPort = hostSuffixAndPort();

		if (isMulti() && requestHostAndPort.startsWith(hostPrefix) //
				&& requestHostAndPort.endsWith(backendSuffixAndPort)) {

			String backendId = Utils.removeSuffix(//
					Utils.removePreffix(requestHostAndPort, hostPrefix), //
					backendSuffixAndPort);

			// check if resulting backing id is well formed
			if (backendId.length() > 0 && !backendId.contains("."))
				return Optional7.of(instanciate(backendId));

		} else if (backendSuffixAndPort.equalsIgnoreCase(requestHostAndPort))
			return Optional7.of(this);

		return Optional7.empty();
	}

	public SpaceBackend instanciate(String backendId) {
		if (!isMulti())
			throw Exceptions.illegalArgument(//
					"backend [%s] is not instanciable", this);

		if (SPACEDOG.equalsIgnoreCase(backendId) //
				|| API.equalsIgnoreCase(backendId))
			return this;

		SpaceBackend backend = new SpaceBackend();
		backend.hostPrefix = hostPrefix;
		backend.backendId = backendId;
		backend.hostSuffix = hostSuffix;
		backend.port = port;
		backend.ssl = ssl;
		return backend;
	}

	//
	// Default targets
	//

	public static SpaceBackend local = fromUrl("http://*.lvh.me:8443");
	public static SpaceBackend wwwLocal = fromUrl("http://*.www.lvh.me:8443");
	public static SpaceBackend staging = fromUrl("https://*.spacerepublic.net");
	public static SpaceBackend wwwStaging = fromUrl("https://*.www.spacerepublic.net");
	public static SpaceBackend production = fromUrl("https://*.spacedog.io");
	public static SpaceBackend wwwProduction = fromUrl("https://*.www.spacedog.io");

	private static Map<String, SpaceBackend> defaultTargets = Maps.newHashMap();

	static {
		defaultTargets.put("local", local);
		defaultTargets.put("wwwLocal", wwwLocal);
		defaultTargets.put("staging", staging);
		defaultTargets.put("wwwStaging", wwwStaging);
		defaultTargets.put("production", production);
		defaultTargets.put("wwwProduction", wwwProduction);
	}

	//
	// Backend id utils
	//

	private static final Pattern BACKEND_ID_PATTERN = Pattern.compile("[a-z0-9]{4,}");

	public static boolean isValid(String backendId) {

		if (!BACKEND_ID_PATTERN.matcher(backendId).matches())
			return false;

		if (backendId.indexOf(SPACEDOG) > -1)
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
					+ "is lowercase, does not contain 'spacedog'.");
	}

}
