package io.spacedog.server;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ElasticUtils {

	public static boolean isCreated(DocWriteResponse response) {
		return response.getResult() == Result.CREATED;
	}

	public static boolean isUpdated(DocWriteResponse response) {
		return response.getResult() == Result.UPDATED;
	}

	public static boolean isDeleted(DocWriteResponse response) {
		return response.getResult() == Result.DELETED;
	}

	public static boolean isNotFound(DocWriteResponse response) {
		return response.getResult() == Result.NOT_FOUND;
	}

	public static RefreshPolicy toPolicy(boolean refresh) {
		return refresh ? RefreshPolicy.IMMEDIATE : RefreshPolicy.NONE;
	}

	public static SearchSourceBuilder toSourceBuilder(String body) {
		return null;
	}
}
