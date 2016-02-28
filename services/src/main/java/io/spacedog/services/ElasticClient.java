package io.spacedog.services;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryAction;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class ElasticClient {

	private Client internalClient;

	public ElasticClient(Client client) {
		this.internalClient = client;
	}

	public void close() {
		internalClient.close();
	}

	//
	// prepare methods
	//

	public IndexRequestBuilder prepareIndex(String backend, String type) {
		return internalClient.prepareIndex(toAlias(backend, type), type);
	}

	public IndexRequestBuilder prepareIndex(String backend, String type, String id) {
		return internalClient.prepareIndex(toAlias(backend, type), type, id);
	}

	public UpdateRequestBuilder prepareUpdate(String backendId, String type, String id) {
		return internalClient.prepareUpdate(toAlias(backendId, type), type, id);
	}

	public SearchRequestBuilder prepareSearch(String backendId) {
		Check.notNullOrEmpty(backendId, "backendId");
		return internalClient.prepareSearch(toIndices(backendId));
	}

	public SearchRequestBuilder prepareSearch(String backendId, String type) {
		Check.notNullOrEmpty(backendId, "backendId");
		Check.notNullOrEmpty(type, "type");
		return internalClient.prepareSearch(toAlias(backendId, type));
	}

	//
	// shortcut methods
	//

	public IndexResponse index(String backend, String type, String id, byte[] source) {
		return prepareIndex(backend, type, id).setSource(source).get();
	}

	public IndexResponse index(String backend, String type, String id, String source) {
		return prepareIndex(backend, type, id).setSource(source).get();
	}

	public GetResponse get(String backend, String type, String id) {
		return internalClient.prepareGet(toAlias(backend, type), type, id).get();
	}

	public DeleteResponse delete(String backend, String type, String id) {
		return internalClient.prepareDelete(toAlias(backend, type), type, id).get();
	}

	public DeleteByQueryResponse deleteByQuery(String backendId, String query) {

		Check.notNullOrEmpty(backendId, "backendId");

		if (Strings.isNullOrEmpty(query))
			query = Json.objectBuilder().object("query").object("match_all").toString();

		DeleteByQueryRequest delete = new DeleteByQueryRequest(toIndices(backendId))//
				.timeout(new TimeValue(60000))//
				.source(query);

		try {
			return Start.get().getElasticClient().execute(DeleteByQueryAction.INSTANCE, delete).get();
		} catch (ExecutionException | InterruptedException e) {
			throw Exceptions.wrap(e);
		}
	}

	public DeleteByQueryResponse deleteByQuery(String backendId, String type, String query) {

		Check.notNullOrEmpty(backendId, "backendId");
		Check.notNullOrEmpty(type, "type");

		if (Strings.isNullOrEmpty(query))
			query = Json.objectBuilder().object("query").object("match_all").toString();

		DeleteByQueryRequest delete = new DeleteByQueryRequest(toAlias(backendId, type))//
				.timeout(new TimeValue(60000))//
				.source(query);

		try {
			return Start.get().getElasticClient().execute(DeleteByQueryAction.INSTANCE, delete).get();
		} catch (ExecutionException | InterruptedException e) {
			throw Exceptions.wrap(e);
		}
	}

	public MultiGetResponse multiGet(String backend, String type, Set<String> ids) {
		return internalClient.prepareMultiGet().add(toAlias(backend, type), type, ids).get();
	}

	public <Request extends ActionRequest<?>, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> ActionFuture<Response> execute(
			final Action<Request, Response, RequestBuilder> action, //
			final Request request) {
		return internalClient.execute(action, request);
	}

	//
	// admin methods
	//

	public void createIndex(String backendId, String type, String mapping, int shards, int replicas) {

		Settings settings = Settings.builder()//
				.put("number_of_shards", shards)//
				.put("number_of_replicas", replicas)//
				.build();

		CreateIndexResponse createIndexResponse = internalClient.admin().indices()//
				.prepareCreate(toIndex0(backendId, type))//
				.addMapping(type, mapping)//
				.addAlias(new Alias(toAlias(backendId, type)))//
				.setSettings(settings)//
				.get();

		if (!createIndexResponse.isAcknowledged())
			throw Exceptions.wrap(//
					"index [%s] creation not acknowledged by the whole cluster", //
					toIndex0(backendId, type));
	}

	public void refreshType(String backendId, String type) {
		refreshIndex(toAlias(backendId, type));
	}

	public void refreshBackend(String backendId) {
		toIndicesStream(backendId).forEach(indexName -> refreshIndex(indexName));
	}

	public void deleteAllIndices(String backendId) {

		DeleteIndexResponse deleteIndexResponse = internalClient.admin().indices().prepareDelete(toIndices(backendId))
				.get();

		if (!deleteIndexResponse.isAcknowledged())
			throw Exceptions.wrap(//
					"backend [%s] deletion not acknowledged by the whole cluster", //
					backendId);
	}

	public void deleteIndex(String backendId, String type) {
		internalClient.admin().indices().prepareDelete(toAlias(backendId, type)).get();
	}

	public GetMappingsResponse getMappings(String backendId) {
		Check.notNullOrEmpty(backendId, "backendId");
		return internalClient.admin().indices()//
				.prepareGetMappings(toIndices(backendId))//
				.get();
	}

	public GetMappingsResponse getMappings(String backendId, String type) {
		Check.notNullOrEmpty(backendId, "backendId");
		Check.notNullOrEmpty(type, "type");

		return internalClient.admin().indices()//
				.prepareGetMappings(toAlias(backendId, type))//
				.setTypes(type)//
				.get();
	}

	public ObjectNode getSchema(String backendId, String type) {

		String source = getMappings(backendId, type).getMappings()//
				.iterator().next().value.get(type).source().toString();

		return (ObjectNode) Json.readObjectNode(source).get(type).get("_meta");
	}

	public boolean exists(String backendId, String type) {
		try {
			return internalClient.admin().indices()//
					.prepareTypesExists(toAlias(backendId, type))//
					.setTypes(type)//
					.get()//
					.isExists();

		} catch (IndexNotFoundException e) {
			return false;
		}
	}

	public void putMapping(String backendId, String type, String mapping) {
		PutMappingResponse putMappingResponse = internalClient.admin().indices()//
				.preparePutMapping(toAlias(backendId, type))//
				.setType(type)//
				.setSource(mapping)//
				.get();

		if (!putMappingResponse.isAcknowledged())
			throw Exceptions.wrap(//
					"mapping [%s] update not acknowledged by the whole cluster", //
					type);
	}

	public void closeAllBackendIndices() {
		CloseIndexResponse closeIndexResponse = internalClient.admin().indices().prepareClose("_all")//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, false))//
				.get();

		if (!closeIndexResponse.isAcknowledged())
			throw Exceptions.wrap(//
					"close all backends not acknowledged by the whole cluster");
	}

	public ClusterAdminClient cluster() {
		return internalClient.admin().cluster();
	}

	//
	// to index help methods
	//

	public Stream<String> toIndicesStream(String backendId) {

		// TODO if too many customers, my cluster might have too many indices
		// for this to work correctly
		GetIndexResponse response = internalClient.admin().indices().prepareGetIndex().get();

		String prefix = backendId + "-";

		return Arrays.stream(response.indices())//
				.filter(indexName -> indexName.startsWith(prefix));
	}

	public String[] toIndices(String backendId) {
		return toIndicesStream(backendId).toArray(String[]::new);
	}

	public String toIndex0(String backendId, String type) {
		return toIndex(backendId, type, 0);
	}

	public String toIndex(String backendId, String type, int version) {
		return String.join("-", backendId, type, Integer.toString(version));
	}

	public String toAlias(String backendId, String type) {
		return String.join("-", backendId, type);
	}

	//
	// implementation
	//

	private void refreshIndex(String indexName) {
		internalClient.admin().indices().prepareRefresh(indexName).get();
	}
}
