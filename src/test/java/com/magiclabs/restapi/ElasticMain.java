package com.magiclabs.restapi;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class ElasticMain {

	public static void main(String[] args) {
		try {

			Node node = NodeBuilder.nodeBuilder().local(true).data(true)
					.clusterName("MagicLabs-ES-Cluster").node();

			Client client = node.client();
			IndicesAdminClient indices = client.admin().indices();

			try {
				indices.prepareDelete("test2").get();

			} catch (IndexMissingException e) {
				// ignored
			}

			try {
				indices.create(new CreateIndexRequest("test2")).get();
			} catch (ExecutionException e) {
				if (e.getCause() instanceof IndexAlreadyExistsException) {
					// ignored
				} else
					throw e;
			}

			ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = indices
					.prepareGetMappings("test2").addTypes("account").get()
					.getMappings();

			if (mappings.get("test2") == null
					|| mappings.get("test2").get("account") == null) {

				PutMappingRequest mappingRequest = new PutMappingRequest(
						"test2").type("account").source(
						AccountResource.ACCOUNT_MAPPING);

				indices.putMapping(mappingRequest).get();
			}

			mappings = indices.prepareGetMappings("test2").addTypes("account")
					.get().getMappings();

			System.out.println(mappings.get("test2").get("account")
					.getSourceAsMap());

		} catch (Throwable t) {
			t.printStackTrace();
		}

		System.exit(0);
	}
}
