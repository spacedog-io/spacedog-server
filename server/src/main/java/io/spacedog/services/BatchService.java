package io.spacedog.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.batch.ServiceResponse;
import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.server.InternalRequest;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class BatchService {

	public List<ServiceResponse> execute(List<ServiceCall> batch) {
		return execute(batch, true);
	}

	public List<ServiceResponse> execute(List<ServiceCall> batch, Boolean stopOnError) {

		List<ServiceResponse> responses = Lists.newArrayList();

		for (int i = 0; i < batch.size(); i++) {
			ServiceCall call = batch.get(i);
			ServiceResponse payload = execute(call);
			responses.add(payload);

			if (stopOnError && payload.success == false)
				break;
		}
		return responses;
	}

	public ServiceResponse execute(ServiceCall call) {

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

		return toServiceAnswer(payload);
	}

	private ServiceResponse toServiceAnswer(Payload payload) {
		ServiceResponse answer = new ServiceResponse();
		answer.success = payload.isSuccess();
		answer.status = payload.code();
		answer.content = Json.toJsonNode(payload.rawContent());
		return answer;
	}

	public Map<String, Object> get(Map<String, String> paths, Boolean stopOnError) {

		Map<String, Object> objects = Maps.newHashMap();

		for (Entry<String, String> entry : paths.entrySet()) {
			ServiceCall call = new ServiceCall(SpaceMethod.GET, "/1" + entry.getValue());
			ServiceResponse payload = execute(call);
			objects.put(entry.getKey(), payload.content);

			if (stopOnError && payload.success == false)
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
}
