/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

public class Migrate {

	private Node node;
	private ElasticClient elastic;
	private StartConfiguration config;
	private Client client;

	private Migrate() throws IOException {
		this.config = new StartConfiguration();
	}

	public static void main(String[] args) throws Exception {

		System.setProperty("spacedog.configuration.file",
				"/Users/davattias/dev/spacedog/local/spacedog.server.properties");

		Migrate singleton = new Migrate();
		try {
			singleton.startLocalElastic();
			singleton.migrate();

		} catch (Throwable t) {
			t.printStackTrace();
			if (singleton != null) {
				if (singleton.elastic != null)
					singleton.elastic.close();
				if (singleton.node != null)
					singleton.elastic.close();
			}
			System.exit(-1);
		}
	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.local", true)//
				.put("node.data", true)//
				.put("cluster.name", "spacedog-elastic-cluster")//
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

	private void migrate() throws IOException {
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = client.admin().indices()
				.prepareGetMappings("*").get().getMappings();

		Iterator<String> indices = mappings.keysIt();
		while (indices.hasNext()) {
			String index = indices.next();
			Iterator<String> types = mappings.get(index).keysIt();
			while (types.hasNext()) {
				String type = types.next();
				MappingMetaData mapping = mappings.get(index).get(type);
				elastic.createIndex(index, type, mapping.source().string(), 1, 1);
				SearchResponse response = client.prepareSearch(index).setTypes(type).setSize(100).setScroll("1m").get();
				reindex(index, type, response.getHits());
				String scrollId = response.getScrollId();
				do {
					response = client.prepareSearchScroll(scrollId).setScroll("1m").get();
					reindex(index, type, response.getHits());
				} while (response.getHits().getHits().length < 100);
			}

		}
	}

	void reindex(String index, String type, SearchHits hits) {
		for (SearchHit hit : hits) {
			elastic.index(index, type, hit.getId(), hit.source());
			log("%s\t\t%s\t\t%s", index, type, hit.getId());
		}
	}

	private void log(String string, Object... args) {
		System.out.println(String.format(string, args));
	}

}