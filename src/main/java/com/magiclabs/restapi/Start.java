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
		routes.add(DataResource.class) //
				.add(MetaResource.class) //
				.add(UserResource.class) //
				.add(AccountResource.class);
	}

	public static void main(String[] args) {
		elasticNode = NodeBuilder.nodeBuilder().local(true).data(true)
				.clusterName("MagicLabs-ES-Cluster").node();

		elasticClient = elasticNode.client();
		new WebServer().configure(Start::configure).start();
	}
}
