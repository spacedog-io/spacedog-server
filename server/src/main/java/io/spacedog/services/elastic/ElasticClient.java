package io.spacedog.services.elastic;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.SnapshotClient;
import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.CloseIndexResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

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

	RestHighLevelClient internalClient;

	public ElasticClient(RestHighLevelClient client) {
		this.internalClient = client;
	}

	public RestHighLevelClient internal() {
		return internalClient;
	}

	public void close() {
		try {
			internalClient.close();
		} catch (IOException e) {
			Utils.warn("closing elastic internal client failed", e);
		}
	}

	//
	// Search
	//

	public SearchRequest prepareSearch(ElasticIndex... indices) {
		Check.notNullOrEmpty(indices, "indices");
		return new SearchRequest(ElasticIndex.aliases(indices))//
				.indicesOptions(IndicesOptions.fromOptions(false, false, false, false));
	}

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
		return search(prepareSearch(indices).source(source));
	}

	public SearchResponse search(SearchRequest request) {

		// Force search request to get version data
		// i.e. seqNo and primaryTerm from elasticsearch
		if (request.source() != null)
			request.source().seqNoAndPrimaryTerm(true);

		try {
			return internalClient.search(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public SearchResponse scroll(String scrollId, TimeValue keepAlive) {
		try {
			SearchScrollRequest request = new SearchScrollRequest(scrollId).scroll(keepAlive);
			return internalClient.scroll(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	//
	// Index
	//

	public IndexRequest prepareIndex(ElasticIndex index) {
		return new IndexRequest(index.alias());
	}

	public IndexResponse index(IndexRequest request) {
		try {
			return internalClient.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public IndexResponse index(ElasticIndex index, Object source) {
		return index(index, source, false);
	}

	public IndexResponse index(ElasticIndex index, Object source, boolean refresh) {
		return index(prepareIndex(index, null, null, source, refresh));
	}

	public IndexResponse index(ElasticIndex index, String id, Object source) {
		return index(index, id, source, false);
	}

	public IndexResponse index(ElasticIndex index, String id, Object source, boolean refresh) {
		return index(prepareIndex(index, id, null, source, refresh));
	}

	public IndexResponse index(ElasticIndex index, String id, String version, Object source, boolean refresh) {
		return index(prepareIndex(index, id, version, source, refresh));
	}

	public IndexResponse index(ElasticIndex index, String id, byte[] source) {
		return index(index, id, source, false);
	}

	public IndexResponse index(ElasticIndex index, String id, byte[] source, boolean refresh) {

		IndexRequest request = prepareIndex(index, id, null, null, refresh)//
				.source(source, XContentType.JSON);

		return index(request);
	}

	private IndexRequest prepareIndex(ElasticIndex index, String id, String version, Object source, Boolean refresh) {
		String sourceString = source instanceof String //
				? source.toString()
				: Json.toString(source);

		IndexRequest request = prepareIndex(index)//
				.id(id)//
				.source(sourceString, XContentType.JSON)//
				.setRefreshPolicy(ElasticUtils.toPolicy(refresh));

		if (version != null) {
			ElasticVersion v = ElasticVersion.valueOf(version);
			request.setIfSeqNo(v.seqNo);
			request.setIfPrimaryTerm(v.primaryTerm);
		}

		return request;
	}

	//
	// Update
	//

	public UpdateRequest prepareUpdate(ElasticIndex index, String id) {
		return new UpdateRequest(index.alias(), id);
	}

	public UpdateResponse update(UpdateRequest request) {
		try {
			return internalClient.update(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	//
	// Get
	//

	public GetResponse get(ElasticIndex index, String id) {
		return get(index, id, false);
	}

	public GetResponse get(ElasticIndex index, String id, boolean throwNotFound) {
		GetResponse response = get(prepareGet(index, id));

		if (!response.isExists() && throwNotFound)
			throw Exceptions.objectNotFound(index.type(), id);

		return response;
	}

	public GetRequest prepareGet(ElasticIndex index, String id) {
		return new GetRequest(index.alias(), id);
	}

	public GetResponse get(GetRequest request) {
		try {
			return internalClient.get(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public Optional<SearchHit> getUnique(ElasticIndex index, QueryBuilder query) {
		try {
			SearchSourceBuilder source = SearchSourceBuilder.searchSource().query(query);
			SearchHits hits = search(source, index).getHits();

			if (hits.getTotalHits().value == 0)
				return Optional.empty();

			else if (hits.getTotalHits().value == 1)
				return Optional.of(hits.getAt(0));

			throw Exceptions.runtime(//
					"unicity violation for [%s] objects with query [%s]", //
					index.type(), query);

		} catch (IndexNotFoundException e) {
			return Optional.empty();
		}
	}

	public MultiGetResponse getMulti(ElasticIndex index, Set<String> ids) {
		try {
			MultiGetRequest request = new MultiGetRequest();
			ids.forEach(id -> request.add(index.alias(), id));
			return internalClient.mget(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	//
	// Exists
	//

	public boolean exists(ElasticIndex index, String id) {
		return get(new GetRequest(index.alias(), id)//
				.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE)).isExists();
	}

	public boolean exists(QueryBuilder query, ElasticIndex... indices) {
		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.size(0)//
				.query(query)//
				.fetchSource(false);

		return search(source, indices).getHits()//
				.getTotalHits().value > 0;
	}

	//
	// Delete
	//

	public DeleteResponse delete(DeleteRequest request) {
		try {
			return internalClient.delete(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public boolean delete(ElasticIndex index, String id, boolean refresh, boolean throwNotFound) {
		DeleteResponse response = delete(//
				new DeleteRequest(index.alias(), id)//
						.setRefreshPolicy(ElasticUtils.toPolicy(refresh)));

		if (ElasticUtils.isDeleted(response))
			return true;

		if (throwNotFound)
			throw Exceptions.objectNotFound(index.type(), id);

		return false;
	}

	public BulkByScrollResponse deleteByQuery(DeleteByQueryRequest request) {
		try {
			return internalClient.deleteByQuery(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
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

		DeleteByQueryRequest request = new DeleteByQueryRequest(ElasticIndex.aliases(indices))//
				.setQuery(query).setTimeout(new TimeValue(60000));

		return deleteByQuery(request);
	}

	//
	// admin methods
	//

	public boolean exists(ElasticIndex index) {
		try {
			return internalClient.indices()//
					.exists(new GetIndexRequest(index.alias()), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public void createIndex(ElasticIndex index, Schema schema, boolean async) {

		CreateIndexRequest request = new CreateIndexRequest(index.toString())//
				.mapping(schema.mapping().toString(), XContentType.JSON)//
				.settings(schema.settings(false).toString(), XContentType.JSON)//
				.alias(new Alias(index.alias()));

		try {
			CreateIndexResponse createIndexResponse = internalClient.indices()//
					.create(request, RequestOptions.DEFAULT);

			if (!createIndexResponse.isAcknowledged())
				throw Exceptions.runtime(//
						"creation of index [%s] not acknowledged by the whole cluster", //
						index);

			if (!async)
				ensureIndexIsGreen(index);

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
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

		try {
			ClusterHealthResponse response = this.internalClient.cluster()//
					.health(Requests.clusterHealthRequest(indices)//
							.timeout(TimeValue.timeValueSeconds(ServerConfig.greenTimeout()))//
							.waitForGreenStatus()//
							.waitForEvents(Priority.LOW)//
							.waitForNoRelocatingShards(true), RequestOptions.DEFAULT);

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

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}

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
			AcknowledgedResponse response = deleteIndices(indices);

			if (!response.isAcknowledged())
				throw Exceptions.runtime(//
						"deletion of all indices of backend [%s] not acknowledged by the whole cluster", //
						Server.backend().id());
		}
	}

	public void deleteIndices(ElasticIndex... indices) {
		deleteIndices(ElasticIndex.toString(indices));
	}

	public AcknowledgedResponse deleteIndices(String... indices) {
		try {
			return internalClient.indices()//
					.delete(new DeleteIndexRequest(indices), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public GetMappingsResponse getMappings(String... indices) {
		try {
			return internalClient.indices()//
					.getMapping(new GetMappingsRequest().indices(indices), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public GetMappingsResponse getBackendMappings() {
		return getMappings(backendIndices());
	}

	public GetMappingsResponse getMappings(ElasticIndex... indices) {
		return getMappings(ElasticIndex.aliases(indices));
	}

	public GetSettingsResponse getSettings(ElasticIndex... indices) {
		try {
			return internalClient.indices()//
					.getSettings(new GetSettingsRequest().indices(ElasticIndex.aliases(indices)),
							RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public void putMapping(ElasticIndex index, ObjectNode mapping) {

		try {
			PutMappingRequest request = new PutMappingRequest(index.alias())//
					.source(mapping.toString(), XContentType.JSON);

			AcknowledgedResponse response = internalClient.indices()//
					.putMapping(request, RequestOptions.DEFAULT);

			if (!response.isAcknowledged())
				throw Exceptions.runtime(//
						"mapping [%s] update not acknowledged by cluster", //
						index.type());

		} catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	public void putSettings(ElasticIndex index, ObjectNode settings) {

		try {
			UpdateSettingsRequest request = new UpdateSettingsRequest(index.alias())//
					.settings(settings.toString(), XContentType.JSON);

			AcknowledgedResponse response = internalClient.indices()//
					.putSettings(request, RequestOptions.DEFAULT);

			if (!response.isAcknowledged())
				throw Exceptions.runtime(//
						"mapping [%s] update not acknowledged by cluster", //
						index.type());

		} catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	public void deleteAbsolutelyAllIndices() {

		try {
			DeleteIndexRequest request = new DeleteIndexRequest("_all")//
					.indicesOptions(IndicesOptions.fromOptions(false, true, true, true));

			AcknowledgedResponse response = internalClient.indices()//
					.delete(request, RequestOptions.DEFAULT);

			if (!response.isAcknowledged())
				throw Exceptions.runtime(//
						"delete all indices not acknowledged by cluster");

		} catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	public void closeAbsolutelyAllIndices() {
		try {
			CloseIndexRequest request = new CloseIndexRequest("_all")//
					.indicesOptions(IndicesOptions.fromOptions(false, true, true, true));

			CloseIndexResponse response = internalClient.indices()//
					.close(request, RequestOptions.DEFAULT);

			if (!response.isAcknowledged())
				throw Exceptions.runtime(//
						"close all indices not acknowledged by cluster");

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public ClusterClient cluster() {
		return internalClient.cluster();
	}

	public SnapshotClient snapshot() {
		return internalClient.snapshot();
	}

	//
	// to index help methods
	//

	public Stream<String> clusterIndexStream() {
		// TODO if too many customers, my cluster might have too many indices
		// for this to work correctly
		try {
			return Arrays.stream(internalClient.indices()//
					.get(new GetIndexRequest("_all"), RequestOptions.DEFAULT)//
					.getIndices());
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
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
		try {
			internalClient.indices().refresh(new RefreshRequest(indices), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}
}
