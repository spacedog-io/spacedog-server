/**
 * © David Attias 2015
 */
package io.spacedog.examples;

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

import com.google.common.io.Resources;

import io.spacedog.services.Utils;

public class ElasticMain {

	public static void main(String[] args) {
		try {

			Node node = NodeBuilder.nodeBuilder().local(true).data(true).clusterName("MagicLabs-ES-Cluster").node();

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
					.prepareGetMappings("test2").addTypes("account").get().getMappings();

			if (mappings.get("test2") == null || mappings.get("test2").get("account") == null) {

				String accountMapping = Resources
						.toString(Resources.getResource("io/spacedog/services/account-mapping.json"), Utils.UTF8);

				PutMappingRequest mappingRequest = new PutMappingRequest("test2").type("account")
						.source(accountMapping);

				indices.putMapping(mappingRequest).get();
			}

			mappings = indices.prepareGetMappings("test2").addTypes("account").get().getMappings();

			System.out.println(mappings.get("test2").get("account").getSourceAsMap());

		} catch (Throwable t) {
			t.printStackTrace();
		}

		System.exit(0);
	}
}
