/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.utils.Json;

public class UpgradeElasticData {

	private Node node;
	private ElasticClient elastic;
	private StartConfiguration config;
	private Client client;

	private UpgradeElasticData() throws IOException {
		this.config = new StartConfiguration();
	}

	public static void main(String[] args) throws Exception {

		UpgradeElasticData singleton = new UpgradeElasticData();

		try {
			singleton.startLocalElastic();
			singleton.waitForClusterInit();
			singleton.removeReplicas();
			singleton.waitForClusterInit();
			singleton.upgrade();

		} catch (Throwable t) {
			t.printStackTrace();
			if (singleton != null) {
				if (singleton.elastic != null)
					singleton.elastic.close();
				if (singleton.node != null)
					singleton.elastic.close();
			}
		}

	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.master", true)//
				.put("node.data", true)//
				.put("cluster.name", "spacedog-elastic-cluster")//
				// disable rebalance to avoid automatic rebalance
				// when a temporary second node appears
				.put("cluster.routing.rebalance.enable", "none")//
				.put("path.home", //
						config.homePath().toAbsolutePath().toString())
				.put("path.data", //
						config.elasticDataPath().toAbsolutePath().toString());

		if (config.snapshotsPath().isPresent())
			builder.put("path.repo", //
					config.snapshotsPath().get().toAbsolutePath().toString());

		node = new ElasticNode(builder.build(), //
				Collections.singleton(DeleteByQueryPlugin.class));

		node.start();
		client = node.client();
		elastic = new ElasticClient(node.client());
	}

	private void waitForClusterInit() throws InterruptedException {
		ClusterHealthStatus status = null;
		do {
			Thread.sleep(2000);
			status = client.admin().cluster().prepareHealth().get().getStatus();
		} while (ClusterHealthStatus.RED.equals(status));
	}

	private void removeReplicas() {
		client.admin().indices().prepareUpdateSettings("*")//
				.setSettings(Settings.builder().put("number_of_replicas", 0).build())//
				.get();
	}

	private void upgrade() throws IOException {
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = client.admin().indices()
				.prepareGetMappings("*").get().getMappings();

		Iterator<String> indices = mappings.keysIt();
		while (indices.hasNext()) {
			String backendId = indices.next();
			// indices without any '-' are plain old backend indices
			if (backendId.indexOf('-') < 0) {
				Iterator<String> types = mappings.get(backendId).keysIt();
				while (types.hasNext()) {
					String type = types.next();
					MappingMetaData mapping = mappings.get(backendId).get(type);
					log("/%s/%s/mapping: %s", backendId, type, mapping.sourceAsMap());
					OneIndexPerType index = new OneIndexPerType(backendId, type, mapping.source().string());
					index.createIndex();
					index.migrateDocuments();
				}
				client.admin().indices().prepareDelete(backendId).get();
			}
		}
	}

	public class OneIndexPerType {

		private String backendId;
		private String type;
		private String mapping;
		private int reindexed;

		public OneIndexPerType(String backendId, String type, String mapping) {
			this.backendId = backendId;
			this.type = type;
			this.mapping = mapping;
		}

		public void createIndex() {
			ObjectNode json = Json.readObjectNode(mapping);
			// remove any _id mapping field since it is not allowed
			// in 2.x mappings
			((ObjectNode) json.get(type)).remove("_id");

			try {
				elastic.createIndex(backendId, type, json.toString());
			} catch (IndexAlreadyExistsException e) {
				// TODO ignored for testing but to be removed
				// for the final tests and production
			}
		}

		public void migrateDocuments() {
			ObjectNode json = Json.readObjectNode(mapping);
			// remove any _id mapping field since it is not allowed
			// in 2.x mappings
			((ObjectNode) json.get(type)).remove("_id");

			try {
				elastic.createIndex(backendId, type, json.toString(), //
						AbstractResource.SHARDS_DEFAULT, //
						AbstractResource.REPLICAS_DEFAULT);
			} catch (IndexAlreadyExistsException e) {
				// TODO ignored for testing but to be removed
				// for the final tests and production
			}

			SearchResponse response = client.prepareSearch(backendId)//
					.setTypes(type).setSize(100).setScroll(TimeValue.timeValueMinutes(1))//
					.setQuery(QueryBuilders.matchAllQuery()).get();

			reIndex(response.getHits());
			String scrollId = response.getScrollId();

			do {
				response = client.prepareSearchScroll(scrollId)//
						.setScroll(TimeValue.timeValueMinutes(1)).get();
				reIndex(response.getHits());
			} while (response.getHits().getHits().length == 100);

			client.admin().indices().prepareSyncedFlush(elastic.toAlias(backendId, type)).get();
			log("/%s/%s => %s reindexed", elastic.toAlias(backendId, type), type, reindexed);
		}

		void reIndex(SearchHits hits) {
			if (hits.getHits().length > 0) {
				BulkRequestBuilder request = client.prepareBulk();

				for (SearchHit hit : hits) {
					String source = hit.sourceAsString();
					if ("spacedog".equals(backendId) && "account".equals(type))
						source = upgradeAccountSource(source);
					request.add(client.prepareIndex(elastic.toAlias(backendId, type), //
							type, hit.getId()).setSource(source));
				}

				BulkItemResponse[] responses = request.get().getItems();
				for (BulkItemResponse response : responses) {
					if (response.isFailed())
						log("Failure /%s/%s/%s => %s", response.getIndex(), response.getType(), //
								response.getId(), response.getFailure().getCause());
					else
						reindexed++;
				}
			}
		}

		private String upgradeAccountSource(String source) {
			ObjectNode account = Json.readObjectNode(source);
			JsonNode date = Json.get(account, "backendKey.generatedAt");

			if (date.isLong()) {
				Json.set(account, "backendKey.generatedAt", //
						TextNode.valueOf(new DateTime(date.asLong()).toString()));

				log("/%s/%s/%s/backendKey/generatedAt => [%s] => [%s]", backendId, type, //
						account.get("backendId").asText(), date, Json.get(account, "backendKey.generatedAt"));
			}
			return account.toString();
		}
	}

	private static void log(String string, Object... args) {
		System.out.println(String.format(string, args));
	}

}