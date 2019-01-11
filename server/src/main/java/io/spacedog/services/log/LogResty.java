package io.spacedog.services.log;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.log.LogSearchResults;
import io.spacedog.server.Server;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import io.spacedog.services.elastic.ElasticUtils;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;

@Prefix("/2/logs")
public class LogResty extends SpaceResty {

	public static final String PURGEMAN = "purgeman";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public LogSearchResults getAll(Context context) {
		Server.context().credentials().checkAtLeastAdmin();

		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);
		String q = context.get(Q_PARAM);

		return Services.logs().get(q, from, size, isRefreshRequested(context));
	}

	@Post("/_search")
	@Post("/_search/")
	public LogSearchResults search(String body, Context context) {

		Server.context().credentials().checkAtLeastAdmin();

		return Services.logs().search(//
				ElasticUtils.toSearchSourceBuilder(body), //
				isRefreshRequested(context));
	}

	@Delete("")
	@Delete("/")
	public ObjectNode purge(Context context) {

		checkAtLeastPurgeMan();

		String param = context.request().query().get(BEFORE_PARAM);
		DateTime before = param == null ? DateTime.now().minusDays(7) //
				: DateTime.parse(param);

		return Services.logs().delete(before);
	}

	//
	// Implementation
	//

	private Credentials checkAtLeastPurgeMan() {
		Credentials credentials = Server.context().credentials();

		if (credentials.isAtLeastSuperAdmin() //
				|| credentials.roles().contains(PURGEMAN))
			return credentials;

		throw Exceptions.insufficientPermissions(credentials);
	}

}
