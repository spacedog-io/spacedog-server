package io.spacedog.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.utils.Json;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.Part;
import net.codestory.http.Query;
import net.codestory.http.Request;

public class InternalRequest implements Request {

	private ServiceCall request;

	public InternalRequest(ServiceCall request) {
		this.request = request;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		return type.isInstance(request) //
				? (T) request
				: null;
	}

	@Override
	public String uri() {
		return request.path;
	}

	@Override
	public String method() {
		return request.method.toString();
	}

	@Override
	public String toString() {
		return method() + ' ' + uri();
	}

	@Override
	public String content() throws IOException {
		// TODO check content type to choose
		// the right way to convert to string
		return request.payload == null ? null : Json.toString(request.payload);
	}

	@Override
	public String contentType() {
		return ContentTypes.JSON_UTF8;
	}

	@Override
	public List<String> headerNames() {
		Set<String> headerNames = Sets.newHashSet();
		if (request.headers != null)
			headerNames.addAll(request.headers.keySet());
		return Lists.newArrayList(headerNames.iterator());
	}

	@Override
	public List<String> headers(String name) {
		if (request.headers != null) {
			Object object = request.headers.get(name);
			if (object != null)
				return Collections.singletonList(object.toString());
		}
		return null;
	}

	@Override
	public String header(String name) {
		if (request.headers != null) {
			Object object = request.headers.get(name);
			if (object != null)
				return object.toString();
		}
		return null;
	}

	@Override
	public InputStream inputStream() throws IOException {
		throw new UnsupportedOperationException(//
				"batch wrapped request must not provide any input stream");
	}

	@Override
	public InetSocketAddress clientAddress() {
		return null;
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public Cookies cookies() {
		return new Cookies() {

			@Override
			public Iterator<Cookie> iterator() {
				return null;
			}

			@Override
			public <T> T unwrap(Class<T> type) {
				return InternalRequest.this.unwrap(type);
			}

			@Override
			public Cookie get(String name) {
				return null;
			}
		};
	}

	@Override
	public Query query() {
		return new Query() {

			@Override
			public <T> T unwrap(Class<T> type) {
				return InternalRequest.this.unwrap(type);
			}

			@Override
			public Collection<String> keys() {
				return request.params == null ? Collections.emptyList() //
						: Sets.newHashSet(request.params.keySet());
			}

			@Override
			public Iterable<String> all(String name) {

				if (request.params == null)
					return Collections.emptyList();

				Object object = request.params.get(name);

				if (object == null)
					return Collections.emptyList();

				return Collections.singletonList(object.toString());
			}
		};
	}

	@Override
	public List<Part> parts() {
		throw new UnsupportedOperationException(//
				"batch wrapped request must not provide parts");
	}
}