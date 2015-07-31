package com.magiclabs.restapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

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

			Path ssl = Paths.get("/home/davattias/ssl");

			if (Files.isDirectory(ssl)) {

				// Force Fluent HTTP to production mode
				System.setProperty("PROD_MOD", "true");

				new WebServer()
						.configure(Start::configure)
						.startSSL(
								443,
								Arrays.asList(
										ssl.resolve("attias.space.certificate-191630.crt"),
										ssl.resolve("attias.space.intermediate.certificate.crt")),
								ssl.resolve("attias.space.pkcs8.der"));
			} else {
				new WebServer().configure(Start::configure).start(8080);
			}

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
