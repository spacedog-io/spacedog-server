/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.reindex.BulkByScrollResponse;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.model.DataObject;

public class ElasticPayload implements SpaceFields {

	public static JsonPayload saved(String uriBase, DataObject<?> object) {
		return JsonPayload.saved(object.justCreated(), uriBase, object.type(), //
				object.id()).withVersion(object.version());
	}

	public static JsonPayload saved(String uriBase, IndexResponse response) {
		final boolean created = ElasticUtils.isCreated(response);
		return JsonPayload.saved(created, uriBase, response.getType(), //
				response.getId()).withVersion(response.getVersion());
	}

	public static JsonPayload bulk(BulkByScrollResponse response) {

		return JsonPayload.ok().withFields(//
				"timedOut", response.isTimedOut(), //
				"created", response.getCreated(), //
				"updated", response.getUpdated(), //
				"deleted", response.getDeleted(), //
				"reasonCancelled", response.getReasonCancelled(), //
				"searchFailures", response.getSearchFailures(), //
				"bulkFailures", response.getBulkFailures());
	}

}
