package io.spacedog.rest;

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

	//
	// Getters
	//

	public String host() {
		return multi ? host(defaultBackendId()) : host;
	}

	public String host(String backendId) {
		checkThisTargetIsMultiBackend(backendId);
		return prefix + backendId + host;
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

	//
	// Other public methods
	//

	public String scheme() {
		return ssl ? "https" : "http";
	}

	public StringBuilder urlBuilder() {
		if (multi)
			return urlBuilder(defaultBackendId());
		StringBuilder builder = new StringBuilder(scheme())//
				.append("://").append(host);//
		return appendPort(builder);
	}

	public StringBuilder urlBuilder(String backendId) {
		checkThisTargetIsMultiBackend(backendId);
		StringBuilder builder = new StringBuilder(scheme()).append("://")//
				.append(prefix).append(backendId).append(host);//
		return appendPort(builder);
	}

	private StringBuilder appendPort(StringBuilder builder) {
		return port == 80 || port == 443 ? builder : builder.append(":").append(port);
	}

	public String url(String uri) {
		return urlBuilder().append(uri).toString();
	}

	public String url(String backendId, String uri) {
		return urlBuilder(backendId).append(uri).toString();
	}

	public Optional7<SpaceBackend> fromHostAndPort(String hostAndPort) {
		String thisBackendhostAndPort = hostAndPort();

		if (multi && hostAndPort.startsWith(prefix) //
				&& hostAndPort.endsWith(thisBackendhostAndPort)) {

			String backendId = Utils.removeSuffix(//
					Utils.removePreffix(hostAndPort, prefix), //
					thisBackendhostAndPort);

			// TODO if the backend id is used to create a new backend
			// in a multi backend server, id must be valid
			// nbut right now, SpaceContext does not handle errors
			// since SpaceContext filter is the first filter
			// Backends.checkIfIdIsValid(backendId);
			return Optional7.of(fromBackendId(backendId));
		}

		if (host.equalsIgnoreCase(hostAndPort))
			return Optional7.of(this);

		return Optional7.empty();
	}

	public SpaceBackend fromBackendId(String backendId) {
		checkThisTargetIsMultiBackend(backendId);

		SpaceBackend backend = new SpaceBackend();
		backend.host = host(backendId);
		backend.backendId = backendId;
		backend.port = port;
		backend.ssl = ssl;
		backend.webApp = webApp;
		return backend;
	}

	@Override
	public String toString() {
		return (multi ? urlBuilder("*") : urlBuilder()).toString();
	}

	//
	// Default targets
	//

	public static SpaceBackend local = fromUrl("http://*.lvh.me:8443");
	public static SpaceBackend staging = fromUrl("https://*.spacerepublic.net");
	public static SpaceBackend colibee = fromUrl("https://connect.colibee.com");
	public static SpaceBackend production = fromUrl("https://*.spacedog.io");

	private static Map<String, SpaceBackend> defaultTargets = Maps.newHashMap();

	static {
		defaultTargets.put("local", local);
		defaultTargets.put("staging", staging);
		defaultTargets.put("colibee", colibee);
		defaultTargets.put("production", production);
	}

	//
	// Private methods
	//

	private String hostAndPort() {
		return appendPort(new StringBuilder(host)).toString();
	}

	private void checkThisTargetIsMultiBackend(String backendId) {
		if (!multi)
			throw Exceptions.illegalArgument(//
					"target [%s] does not contain backend [%s]", //
					this, backendId);
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
			throw new IllegalArgumentException("backend id doesn't comply to: "//
					+ "is at least 4 characters long, "//
					+ "is only composed of a-z and 0-9 characters, "//
					+ "is lowercase, "//
					+ "does not start with 'api', "//
					+ "does not contain 'spacedog'");
	}

	public static String defaultBackendId() {
		return "api";
	}

	public static boolean isDefaultBackendId(String backendId) {
		return defaultBackendId().equals(backendId);
	}
}
