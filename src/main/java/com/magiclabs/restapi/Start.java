package com.magiclabs.restapi;

import net.codestory.http.WebServer;
import net.codestory.http.routes.Routes;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class Start {

	private static Node elasticNode;
	private static Client elasticClient;

	public static Node getElasticNode() {
		return elasticNode;
	}

	public static Client getElasticClient() {
		return elasticClient;
	}

	private static void configure(Routes routes) {
		routes.add(DataResource.get()) //
				.add(SchemaResource.get()) //
				.add(UserResource.get()) //
				.add(AccountResource.get());
	}

	public static void main(String[] args) {
		try {
			elasticNode = NodeBuilder.nodeBuilder().local(true).data(true)
					.clusterName("MagicLabs-ES-Cluster").node();

			elasticClient = elasticNode.client();

			AccountResource.get().initAdminIndex();

			new WebServer().configure(Start::configure).start();

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
