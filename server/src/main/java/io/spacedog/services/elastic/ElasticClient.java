package io.spacedog.services.elastic;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.schema.Schema;
import io.spacedog.jobs.Internals;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import joptsimple.internal.Strings;

public class ElasticClient implements SpaceParams {

	Client internalClient;

	public ElasticClient(Client client) {
		this.internalClient = client;
	}

	public Client internal() {
		return internalClient;
	}

	public void close() {
		internalClient.close();
	}

	//
	// prepare
	//

	public IndexRequestBuilder prepareIndex(ElasticIndex index) {
		return internalClient.prepareIndex(index.alias(), index.type());
	}

	public IndexRequestBuilder prepareIndex(ElasticIndex index, String id) {
		return internalClient.prepareIndex(index.alias(), index.type(), id);
	}

	public UpdateRequestBuilder prepareUpdate(ElasticIndex index, String id) {
		return internalClient.prepareUpdate(index.alias(), index.type(), id);
	}

	public SearchRequestBuilder prepareSearch(ElasticIndex... indices) {
		Check.notNullOrEmpty(indices, "indices");
		return internalClient.prepareSearch(ElasticIndex.aliases(indices))//
				.setIndicesOptions(IndicesOptions.fromOptions(false, false, false, false));
	}

	public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
		return internalClient.prepareSearchScroll(scrollId);
	}

	//
	// Search
	//

	public SearchResponse search(ElasticIndex index, Object... terms) {

		if (terms.length % 2 == 1)
			throw Exceptions.illegalArgument(//
					"search terms %s are invalid: one is missing", Arrays.toString(terms));

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.query(bool);

		for (int i = 0; i < terms.length; i = i + 2)
			bool.filter(QueryBuilders.termQuery(terms[i].toString(), terms[i + 1]));

		return search(source, index);
	}

	public SearchResponse search(SearchSourceBuilder source, ElasticIndex... indices) {
		return prepareSearch(indices).setSource(source).get();
	}

	//
	// Index
	//

	public IndexResponse index(ElasticIndex index, Object source) {
		return index(index, source, false);
	}

	public IndexResponse index(ElasticIndex index, Object source, boolean refresh) {
		String sourceString = source instanceof String //
				? source.toString()
				: Json.toString(source);

		return prepareIndex(index).setSource(sourceString, XContentType.JSON)//
				.setRefreshPolicy(ElasticUtils.toPolicy(refresh)).get();
	}

	public IndexResponse index(ElasticIndex index, String id, Object source) {
		return index(index, id, source, false);
	}

	public IndexResponse index(ElasticIndex index, String id, Object source, boolean refresh) {
		String sourceString = source instanceof String //
				? source.toString()
				: Json.toString(source);

		return prepareIndex(index, id).setSource(sourceString, XContentType.JSON)//
				.setRefreshPolicy(ElasticUtils.toPolicy(refresh)).get();
	}

	public IndexResponse index(ElasticIndex index, String id, byte[] source) {
		return index(index, id, source, false);
	}

	public IndexResponse index(ElasticIndex index, String id, byte[] source, boolean refresh) {
		return prepareIndex(index, id).setSource(source, XContentType.JSON)//
				.setRefreshPolicy(ElasticUtils.toPolicy(refresh)).get();
	}

	//
	// Get
	//

	public GetResponse get(ElasticIndex index, String id) {
		return get(index, id, false);
	}

	public GetResponse get(ElasticIndex index, String id, boolean throwNotFound) {
		GetResponse response = prepareGet(index, id).get();

		if (!response.isExists() && throwNotFound)
			throw Exceptions.objectNotFound(index.type(), id);

		return response;
	}

	public GetRequestBuilder prepareGet(ElasticIndex index, String id) {
		return internalClient.prepareGet(index.alias(), index.type(), id);
	}

	public Optional<SearchHit> getUnique(ElasticIndex index, QueryBuilder query) {
		try {
			SearchHits hits = prepareSearch(index)//
					.setQuery(query)//
					.get()//
					.getHits();

			if (hits.getTotalHits() == 0)
				return Optional.empty();

			else if (hits.getTotalHits() == 1)
				return Optional.of(hits.getAt(0));

			throw Exceptions.runtime(//
					"unicity violation for [%s] objects with query [%s]", //
					index.type(), query);

		} catch (IndexNotFoundException e) {
			return Optional.empty();
		}
	}

	public MultiGetResponse getMulti(ElasticIndex index, Set<String> ids) {
		return internalClient.prepareMultiGet().add(index.alias(), index.type(), ids).get();
	}

	//
	// Exists
	//

	public boolean exists(ElasticIndex index, String id) {
		return internalClient.prepareGet(index.alias(), index.type(), id)//
				.setFetchSource(false).get().isExists();
	}

	public boolean exists(QueryBuilder query, ElasticIndex... indices) {
		return internalClient.prepareSearch(ElasticIndex.aliases(indices))//
				.setSize(0)//
				.setQuery(query)//
				.setFetchSource(false)//
				.get()//
				.getHits()//
				.getTotalHits() > 0;
	}

	//
	// Delete
	//

	public boolean delete(ElasticIndex index, String id, boolean refresh, boolean throwNotFound) {
		DeleteResponse response = internalClient.prepareDelete(//
				index.alias(), index.type(), id)//
				.setRefreshPolicy(ElasticUtils.toPolicy(refresh))//
				.get();

		if (ElasticUtils.isDeleted(response))
			return true;

		if (throwNotFound)
			throw Exceptions.objectNotFound(index.type(), id);

		return false;
	}

	public BulkByScrollResponse deleteByQuery(String query, ElasticIndex... indices) {
		QueryBuilder builder = Strings.isNullOrEmpty(query) //
				? QueryBuilders.matchAllQuery()//
				: QueryBuilders.wrapperQuery(query);

		return deleteByQuery(builder, indices);
	}

	public BulkByScrollResponse deleteByQuery(QueryBuilder query, ElasticIndex... indices) {

		if (query == null)//
			query = QueryBuilders.matchAllQuery();

		SearchRequest search = new SearchRequest(ElasticIndex.aliases(indices));
		DeleteByQueryRequest delete = new DeleteByQueryRequest(search)//
				.setTimeout(new TimeValue(60000));

		// TODO
		// do not set source query above since new DeleteByQueryRequest(search)
		// sets an empty SearchSourceBuilder replacing previously set
		// TODO
		// search source size is also forced at 1000 by same ctor
		// i hope it doen't count since this is a bulk delete
		search.source().query(query);

		try {
			return execute(DeleteByQueryAction.INSTANCE, delete).get();

		} catch (ExecutionException | InterruptedException e) {
			throw Exceptions.runtime(e);
		}
	}

	//
	// Others
	//

	public BulkRequestBuilder prepareBulk() {
		return internalClient.prepareBulk();
	}

	public <Request extends ActionRequest, Response extends ActionResponse, RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder>> ActionFuture<Response> execute(
			Action<Request, Response, RequestBuilder> action, Request request) {
		return internalClient.execute(action, request);
	}

	//
	// admin methods
	//

	public boolean exists(ElasticIndex index) {
		try {
			return internalClient.admin().indices()//
					.prepareExists(index.alias())//
					.get()//
					.isExists();

		} catch (IndexNotFoundException e) {
			return false;
		}
	}

	public void createIndex(ElasticIndex index, Schema schema, boolean async) {

		CreateIndexResponse createIndexResponse = internalClient.admin().indices()//
				.prepareCreate(index.toString())//
				.addMapping(index.type(), schema.mapping().toString(), XContentType.JSON)//
				.setSettings(schema.settings(false).toString(), XContentType.JSON)//
				.addAlias(new Alias(index.alias()))//
				.get();

		if (!createIndexResponse.isAcknowledged())
			throw Exceptions.runtime(//
					"creation of index [%s] not acknowledged by the whole cluster", //
					index);

		if (!async)
			ensureIndexIsGreen(index);
	}

	public void ensureIndexIsGreen(ElasticIndex... indices) {
		ensureIndicesAreAtLeastYellow(ElasticIndex.aliases(indices));
	}

	public void ensureAllIndicesAreAtLeastYellow() {
		ensureIndicesAreAtLeastYellow("_all");
	}

	public void ensureIndicesAreAtLeastYellow(String... indices) {
		String indicesString = Arrays.toString(indices);
		Utils.info("[SpaceDog] Ensure indices %s are at least yellow ...", indicesString);

		ClusterHealthResponse response = this.internalClient.admin().cluster()
				.health(Requests.clusterHealthRequest(indices)//
						.timeout(TimeValue.timeValueSeconds(ServerConfig.greenTimeout()))//
						.waitForGreenStatus()//
						.waitForEvents(Priority.LOW)//
						.waitForNoRelocatingShards(true))//
				.actionGet();

		if (ServerConfig.greenCheck()) {
			if (response.isTimedOut())
				throw Exceptions.runtime("ensure indices %s status are at least yellow timed out", //
						indicesString);

			if (response.getStatus().equals(ClusterHealthStatus.RED))
				throw Exceptions.runtime("indices %s failed to turn at least yellow", //
						indicesString);

			if (response.getStatus().equals(ClusterHealthStatus.YELLOW)) {
				String message = String.format(//
						"indices %s status are yellow", indicesString);
				Internals.get().notify(message, message);
			}
		}
		Utils.info("[SpaceDog] indices %s are [%s]", indicesString, response.getStatus());
	}

	public void refreshIndex(ElasticIndex... indices) {
		refreshIndex(ElasticIndex.toString(indices));
	}

	public void refreshIndex(boolean refresh, ElasticIndex... indices) {
		if (refresh)
			refreshIndex(indices);
	}

	public void refreshBackend() {
		backendIndexStream().forEach(index -> refreshIndex(index));
	}

	public void deleteBackendIndices() {

		String[] indices = backendIndices();

		if (!Utils.isNullOrEmpty(indices)) {
			DeleteIndexResponse deleteIndexResponse = internalClient.admin()//
					.indices().prepareDelete(indices).get();

			if (!deleteIndexResponse.isAcknowledged())
				throw Exceptions.runtime(//
						"deletion of all indices of backend [%s] not acknowledged by the whole cluster", //
						Server.backend().id());
		}
	}

	public void deleteIndex(ElasticIndex... indices) {
		internalClient.admin().indices().prepareDelete(ElasticIndex.toString(indices)).get();
	}

	public GetMappingsResponse getBackendMappings() {
		return internalClient.admin().indices()//
				.prepareGetMappings(backendIndices())//
				.get();
	}

	public GetMappingsResponse getMappings(ElasticIndex... indices) {
		return internalClient.admin().indices()//
				.prepareGetMappings(ElasticIndex.aliases(indices))//
				.get();
	}

	public GetSettingsResponse getSettings(ElasticIndex... indices) {
		return internalClient.admin().indices()//
				.prepareGetSettings(ElasticIndex.aliases(indices))//
				.get();
	}

	public void putMapping(ElasticIndex index, ObjectNode mapping) {
		PutMappingResponse putMappingResponse = internalClient.admin().indices()//
				.preparePutMapping(index.alias())//
				.setType(index.type())//
				.setSource(mapping.toString(), XContentType.JSON)//
				.get();

		if (!putMappingResponse.isAcknowledged())
			throw Exceptions.runtime(//
					"mapping [%s] update not acknowledged by cluster", //
					index.type());
	}

	public void putSettings(ElasticIndex index, ObjectNode settings) {
		UpdateSettingsResponse updateSettingsResponse = internalClient.admin().indices()//
				.prepareUpdateSettings(index.alias())//
				.setSettings(settings.toString(), XContentType.JSON)//
				.get();

		if (!updateSettingsResponse.isAcknowledged())
			throw Exceptions.runtime(//
					"mapping [%s] update not acknowledged by cluster", //
					index.type());
	}

	public void deleteAbsolutelyAllIndices() {
		DeleteIndexResponse response = internalClient.admin().indices()//
				.prepareDelete("_all")//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, true))//
				.get();

		if (!response.isAcknowledged())
			throw Exceptions.runtime(//
					"delete all indices not acknowledged by cluster");
	}

	public void closeAbsolutelyAllIndices() {
		CloseIndexResponse closeIndexResponse = internalClient.admin().indices()//
				.prepareClose("_all")//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, true))//
				.get();

		if (!closeIndexResponse.isAcknowledged())
			throw Exceptions.runtime(//
					"close all indices not acknowledged by cluster");
	}

	public ClusterAdminClient cluster() {
		return internalClient.admin().cluster();
	}

	//
	// to index help methods
	//

	public Stream<String> clusterIndexStream() {
		// TODO if too many customers, my cluster might have too many indices
		// for this to work correctly
		return Arrays.stream(internalClient.admin().indices()//
				.prepareGetIndex().get().indices());
	}

	// public Index[] allIndicesForSchema(String schemaName) {
	// return allIndicesStream()//
	// .map(index -> Index.valueOf(index))//
	// .filter(index -> schemaName.equals(index.schemaName))//
	// .toArray(Index[]::new);
	// }

	// public Stream<String> backendIndicesStream() {
	// String prefix = SpaceContext.backendId() + "-";
	// return clusterIndexStream()//
	// .filter(indexName -> indexName.startsWith(prefix));
	// }

	// public String[] backendIndices() {
	// return backendIndicesStream().toArray(String[]::new);
	// }

	/**
	 * TODO use this in the future to distinguish data indices and internal backend
	 * indices. This means all data indices must be renamed with the following
	 * pattern: backendId-data-indexName
	 */
	public String[] backendDataIndices() {
		return filteredBackendIndices(Optional.of("data"));
	}

	// public Index[] clusterSchemaIndices(String schemaName) {
	// ElasticClient elastic = Start.get().getElasticClient();
	// return clusterIndexStream()//
	// .map(index -> elastic.valueOf(index))//
	// .filter(index -> schemaName.equals(index.type()))//
	// .toArray(Index[]::new);
	// }

	public Stream<String> backendIndexStream() {
		return filteredBackendIndexStream(Optional.empty());
	}

	public Stream<String> filteredBackendIndexStream(Optional<String> service) {
		String backendId = Server.backend().id();
		String fullPrefix = service.isPresent() //
				? backendId + '-' + service.get() + '-' //
				: backendId + '-';
		return clusterIndexStream()//
				.filter(indexName -> indexName.startsWith(fullPrefix));
	}

	public String[] backendIndices() {
		return filteredBackendIndices(Optional.empty());
	}

	public String[] filteredBackendIndices(Optional<String> service) {
		return filteredBackendIndexStream(service).toArray(String[]::new);
	}

	//
	// implementation
	//

	private void refreshIndex(String... indices) {
		internalClient.admin().indices().prepareRefresh(indices).get();
	}
}