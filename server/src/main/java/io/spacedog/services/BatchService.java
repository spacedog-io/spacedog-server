package io.spacedog.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.server.InternalRequest;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.utils.Exceptions;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class BatchService {

	public List<ObjectNode> execute(List<ServiceCall> batch) {
		return execute(batch, true);
	}

	public List<ObjectNode> execute(List<ServiceCall> batch, Boolean stopOnError) {

		List<ObjectNode> responses = Lists.newArrayList();

		for (int i = 0; i < batch.size(); i++) {
			ServiceCall call = batch.get(i);
			Payload payload = execute(call);
			responses.add(toJson(payload, call));

			if (stopOnError && payload.isError())
				break;
		}
		return responses;
	}

	public Payload execute(ServiceCall call) {

		checkBatchCall(call);

		Payload payload = null;
		InternalRequest request = new InternalRequest(call);

		try {
			payload = Server.get().executeRequest(request, null);

		} catch (Throwable t) {
			payload = JsonPayload.error(t).build();
		}

		if (payload == null)
			payload = new Payload(HttpStatus.INTERNAL_SERVER_ERROR);

		return payload;
	}

	public Map<String, ObjectNode> get(Map<String, String> paths, Boolean stopOnError) {

		Map<String, ObjectNode> objects = Maps.newHashMap();

		for (Entry<String, String> entry : paths.entrySet()) {
			ServiceCall call = new ServiceCall(SpaceMethod.GET, "/1" + entry.getValue());
			Payload payload = execute(call);
			objects.put(entry.getKey(), toJson(payload, call));

			if (stopOnError && payload.isError())
				break;
		}
		return objects;
	}

	//
	// Implementation
	//

	private void checkBatchCall(ServiceCall call) {
		// backend service is forbidden in batch request to avoid create/delete backend
		// in batches. Without this restriction, it is possible in certain conditions to
		// create multiple credentials with the same username.
		if (call.path.startsWith("/1/backend"))
			throw Exceptions.illegalArgument(//
					"/backend requests forbidden in batch");
	}

	private static ObjectNode toJson(Payload payload, ServiceCall call) {

		Object rawContent = payload.rawContent();

		if (rawContent == null)
			rawContent = JsonPayload.status(payload.code())//
					.build().rawContent();

		if (rawContent instanceof ObjectNode)
			return (ObjectNode) rawContent;

		throw Exceptions.illegalArgument(//
				"batch sub request [%s][%s] returned a non JSON response", //
				call.method, call.path);
	}

}
