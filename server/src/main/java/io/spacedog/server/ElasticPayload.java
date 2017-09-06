/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceFields;

public class ElasticPayload implements SpaceFields {

	public static JsonPayload saved(String uriBase, IndexResponse response) {
		return JsonPayload.saved(response.isCreated(), uriBase, response.getType(), //
				response.getId()).withVersion(response.getVersion());
	}

	public static JsonPayload saved(String uriBase, UpdateResponse response) {
		return JsonPayload.saved(response.isCreated(), uriBase, response.getType(), //
				response.getId()).withVersion(response.getVersion());
	}

	public static JsonPayload deleted(DeleteByQueryResponse response) {

		if (response.isTimedOut())
			return JsonPayload.error(504, //
					"the delete by query operation timed out, some objects might have been deleted");

		if (response.getTotalFound() != response.getTotalDeleted())
			return JsonPayload.error(500,
					"the delete by query operation failed to delete all objects found, "
							+ "objects found [%s], objects deleted [%s]",
					response.getTotalFound(), response.getTotalDeleted());

		if (response.getShardFailures().length > 0)
			return toPayload(500, response.getShardFailures());

		return JsonPayload.ok()//
				.with("totalDeleted", response.getTotalDeleted());
	}

	public static JsonPayload toPayload(int status, ShardOperationFailedException[] failures) {

		if (status < 400)
			return JsonPayload.status(status);

		ArrayNode errors = Json.array();
		for (ShardOperationFailedException failure : failures)
			errors.add(Json.object("type", failure.getClass().getName(), //
					"message", failure.reason(), "shardId", failure.shardId()));

		return JsonPayload.status(status).withError(errors);
	}

}
