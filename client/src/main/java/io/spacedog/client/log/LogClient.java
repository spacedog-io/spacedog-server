package io.spacedog.client.log;

import org.joda.time.DateTime;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.http.SpaceParams;

public class LogClient implements SpaceParams {

	private SpaceDog dog;

	public LogClient(SpaceDog session) {
		this.dog = session;
	}

	public LogSearchResults get() {
		return get(10);
	}

	public LogSearchResults get(int size) {
		return get(size, false);
	}

	public LogSearchResults get(int size, boolean refresh) {
		return get(null, size, refresh);
	}

	public LogSearchResults get(String q, int size, boolean refresh) {
		return dog.get("/2/logs").size(size).refresh(refresh)//
				.queryParam(Q_PARAM, q).go(200)//
				.asPojo(LogSearchResults.class);
	}

	public LogSearchResults search(ESSearchSourceBuilder builder) {
		return search(builder, false);
	}

	public LogSearchResults search(ESSearchSourceBuilder builder, boolean refresh) {
		return dog.post("/2/logs/_search").refresh(refresh)//
				.bodyJson(builder.toString()).go(200)//
				.asPojo(LogSearchResults.class);
	}

	public void delete(DateTime before) {
		dog.delete("/2/logs").queryParam("before", before).go(200).asVoid();
	}

}
