package io.spacedog.services;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.server.Server;
import io.spacedog.services.elastic.ElasticClient;

public class SpaceService implements SpaceFields, SpaceParams {

	public static ElasticClient elastic() {
		return Server.get().elasticClient();
	}
}
