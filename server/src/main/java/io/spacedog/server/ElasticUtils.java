package io.spacedog.server;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import io.spacedog.utils.Exceptions;

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

	public static SearchSourceBuilder toSearchSourceBuilder(String source) {
		try {
			NamedXContentRegistry registry = Server.get().elasticNode()//
					.injector().getInstance(NamedXContentRegistry.class);

			XContentParser parser = XContentType.JSON.xContent()//
					.createParser(registry, source);

			SearchSourceBuilder builder = SearchSourceBuilder.searchSource();
			builder.parseXContent(parser);
			return builder;

		} catch (Exception e) {
			throw Exceptions.illegalArgument(e, //
					"error parsing search source [%s]", source);
		}
	}
}
