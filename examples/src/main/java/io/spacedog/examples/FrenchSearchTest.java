package io.spacedog.examples;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;

import io.spacedog.rest.SpaceTest;
import io.spacedog.server.ElasticClient;
import io.spacedog.server.Start;
import io.spacedog.utils.Json;

public class FrenchSearchTest extends SpaceTest {

	private static final String FIELD_NAME = "text";
	private static final String TYPE = "page";
	private static final String INDEX = "books";

	private Client client;
	private IndicesAdminClient indices;

	@Test
	public void test2() throws Throwable {
		try {
			init();

			index("Mes meilleurs voeux pour cette nouvelle année.");
			index("C'est la fête des canards.");
			index("Nous sommes tous des français libres et égaux en droit devant la république unie et indivisible.");
			index("L'avion s'est posé sur une piste libre.");

			refresh();

			search("Meilleur");
			search("Nouvel");
			search("Fetes");
			search("Somme");
			search("Canard");
			search("Egau");
			search("ann");
			search("anne");
			search("anné");
			search("annee");
			search("année");
			search("avion");
			search("pose");

		} finally {
			if (client != null)
				client.close();
		}

	}

	private void refresh() {
		indices.prepareRefresh(INDEX).get();
	}

	private void search(String word) {
		System.out.println("SEARCH FOR << " + word + " >>");
		SearchResponse response = client.prepareSearch(INDEX)//
				.setQuery(QueryBuilders.matchQuery(FIELD_NAME, word))//
				.setFrom(0)//
				.setSize(10)//
				.get();

		for (SearchHit hit : response.getHits().getHits()) {
			System.out.println("\t" + hit.sourceAsMap().get(FIELD_NAME));
		}

	}

	private void index(String page) {
		client.prepareIndex(INDEX, TYPE)//
				.setSource(FIELD_NAME, page)//
				.get();
	}

	private void init() throws JsonProcessingException, IOException {
		Settings clientSettings = Settings.settingsBuilder()//
				.put("cluster.name", Start.CLUSTER_NAME).build();

		client = TransportClient.builder().settings(clientSettings).build()
				.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

		indices = client.admin().indices();

		if (indices.prepareExists(INDEX).get().isExists())
			indices.prepareDelete(INDEX).get();

		new ElasticClient(client).ensureAllIndicesAreGreen();

		URL url = Resources.getResource("io/spacedog/examples/french.analyzer.settings.json");
		JsonNode customfrenchAnalyser = Json.readNode(url);

		indices.prepareCreate(INDEX)//
				.setSettings(customfrenchAnalyser.toString())//
				.addMapping(TYPE, FIELD_NAME, "type=string,analyzer=french")//
				.get();
	}
}
