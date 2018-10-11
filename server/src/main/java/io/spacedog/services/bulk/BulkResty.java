package io.spacedog.services.bulk;

import java.util.List;
import java.util.Map;

import io.spacedog.client.bulk.ServiceCall;
import io.spacedog.client.bulk.ServiceResponse;
import io.spacedog.client.http.SpaceException;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;

@Prefix("/2/bulk")
public class BulkResty extends SpaceResty {

	// query parameter names

	private static final String STOP_ON_ERROR_QUERY_PARAM = "stopOnError";

	//
	// Routes
	//

	@Post("")
	@Post("/")
	public List<ServiceResponse> post(List<ServiceCall> batch, Context context) {

		if (batch.size() > 20)
			throw new SpaceException("bulk-limit-exceeded", HttpStatus.BAD_REQUEST, //
					"bulk are limited to 20 sub requests");

		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);
		return Services.bulk().execute(batch, stopOnError);
	}

	@Get("")
	@Get("/")
	public Map<String, Object> get(Context context) {

		boolean stopOnError = context.query()//
				.getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);

		Map<String, String> params = context.query().keyValues();
		params.remove(STOP_ON_ERROR_QUERY_PARAM);

		return Services.bulk().get(params, stopOnError);
	}
}
