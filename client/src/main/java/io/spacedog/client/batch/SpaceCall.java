package io.spacedog.client.batch;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Maps;

import io.spacedog.client.http.SpaceMethod;
import io.spacedog.utils.Exceptions;

public class SpaceCall {
	public SpaceMethod method;
	public String path;
	public Map<String, Object> headers;
	public Map<String, Object> params;
	public Object payload;

	public SpaceCall() {
	}

	public SpaceCall(SpaceMethod method, String path) {
		this.method = method;
		this.path = path;
	}

	public SpaceCall withPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	public SpaceCall withParams(Object... values) {
		if (values.length % 2 != 0)
			throw Exceptions.illegalArgument(//
					"params [%s] not even", Arrays.toString(values));

		if (this.params == null)
			this.params = Maps.newHashMap();

		for (int i = 0; i < values.length - 1; i += 2)
			this.params.put(values[i].toString(), values[i + 1]);

		return this;
	}
}
