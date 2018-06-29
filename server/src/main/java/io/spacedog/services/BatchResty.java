package io.spacedog.services;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.http.SpaceException;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/batch")
public class BatchResty extends SpaceResty {

	// query parameter names

	private static final String STOP_ON_ERROR_QUERY_PARAM = "stopOnError";

	//
	// Routes
	//

	@Post("")
	@Post("/")
	public Payload post(List<ServiceCall> batch, Context context) {

		if (batch.size() > 20)
			throw new SpaceException("batch-limit-exceeded", HttpStatus.BAD_REQUEST, //
					"batch are limited to 20 sub requests");

		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);
		List<ObjectNode> responses = Services.batch().execute(batch, stopOnError);

		return JsonPayload.ok()//
				.withFields("responses", responses)//
				.build();
	}

	@Get("")
	@Get("/")
	public Payload get(Context context) {

		boolean stopOnError = context.query()//
				.getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);

		Map<String, String> params = context.query().keyValues();
		params.remove(STOP_ON_ERROR_QUERY_PARAM);

		Map<String, ObjectNode> objects = Services.batch().get(params, stopOnError);
		return JsonPayload.ok().withContent(objects).build();
	}
}
