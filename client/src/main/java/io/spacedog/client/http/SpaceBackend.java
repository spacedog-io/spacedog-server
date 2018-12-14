package io.spacedog.client.http;

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
	private String id = "";
	private String hostSuffix;
	private int port;
	private boolean multi = false;
	private String name;

	private SpaceBackend() {
	}

	//
	// Getters, setters and common methods
	//

	public String scheme() {
		return ssl ? "https" : "http";
	}

	public String id() {
		return id.isEmpty() ? SpaceEnv.env().backendId() : id;
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

	public String name() {
		return name;
	}

	public SpaceBackend name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return isMulti() //
				? new UrlBuilder().appendScheme().appendHost("*").appendPort().toString()//
				: url();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof SpaceBackend == false)
			return false;

		SpaceBackend other = (SpaceBackend) object;
		return url().equalsIgnoreCase(other.url());
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
			return appendHost(multi ? API : id);
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

		if (!url.toLowerCase().startsWith("http"))
			throw Exceptions.illegalArgument("backend url [%s] is invalid", url);

		// handle scheme
		target.ssl = url.toLowerCase().startsWith("https");
		url = Utils.trimUntil(url, "://");

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
		return defaultBackends.get(name);
	}

	public static SpaceBackend valueOf(String string) {
		SpaceBackend backend = string.startsWith("http") //
				? fromUrl(string)
				: fromDefaults(string);
		if (backend == null)
			throw Exceptions.illegalArgument("backend url [%s] is invalid");
		return backend;
	}

	public Optional7<SpaceBackend> fromRequest(String requestHostAndPort) {
		String backendSuffixAndPort = hostSuffixAndPort();

		if (isMulti() && requestHostAndPort.startsWith(hostPrefix) //
				&& requestHostAndPort.endsWith(backendSuffixAndPort)) {

			String backendId = Utils.trimSuffix(//
					Utils.trimPreffix(requestHostAndPort, hostPrefix), //
					backendSuffixAndPort);

			// check if resulting backeng id is well formed
			if (backendId.length() > 0 && !backendId.contains("."))
				return Optional7.of(fromBackendId(backendId));

		} else if (backendSuffixAndPort.equalsIgnoreCase(requestHostAndPort))
			return Optional7.of(this);

		return Optional7.empty();
	}

	public SpaceBackend fromBackendId(String backendId) {
		if (!isMulti())
			throw Exceptions.illegalArgument(//
					"backend [%s] is not instanciable", this);

		if (SPACEDOG.equalsIgnoreCase(backendId) //
				|| API.equalsIgnoreCase(backendId))
			return this;

		SpaceBackend backend = new SpaceBackend();
		backend.hostPrefix = hostPrefix;
		backend.id = backendId;
		backend.hostSuffix = hostSuffix;
		backend.port = port;
		backend.ssl = ssl;
		return backend;
	}

	//
	// Default backends
	//

	public static SpaceBackend local = fromUrl("http://*.lvh.me:8443").name("local");
	public static SpaceBackend wwwLocal = fromUrl("http://*.www.lvh.me:8443").name("wwwLocal");
	public static SpaceBackend staging = fromUrl("https://*.spacerepublic.net").name("staging");
	public static SpaceBackend wwwStaging = fromUrl("https://*.www.spacerepublic.net").name("wwwStaging");
	public static SpaceBackend production = fromUrl("https://*.spacedog.io").name("production");
	public static SpaceBackend wwwProduction = fromUrl("https://*.www.spacedog.io").name("wwwProduction");

	private static Map<String, SpaceBackend> defaultBackends = Maps.newHashMap();

	static {
		defaultBackends.put(local.name(), local);
		defaultBackends.put(wwwLocal.name(), wwwLocal);
		defaultBackends.put(staging.name(), staging);
		defaultBackends.put(wwwStaging.name(), wwwStaging);
		defaultBackends.put(production.name(), production);
		defaultBackends.put(wwwProduction.name(), wwwProduction);
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
