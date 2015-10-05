package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import net.codestory.http.WebServer;
import net.codestory.http.routes.Routes;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class Start {

	private static Node elasticNode;
	private static Client elasticClient;

	private final static String[] LocalhostConf = { "/Users/davattias/dev/spacedog" };

	private final static String[] GoogleConf = { "/home/davattias/spacedog", //
			"attias.space.certificate.191630.crt", //
			"GandiStandardSSLCA.intermediate.pem", //
			"attias.space.pkcs8.der" };

	private final static String[] OvhConf = { "/root/spacedog", //
			"spacedog.io.certificate.210740.crt", //
			"GandiStandardSSLCA.intermediate.pem", //
			"spacedog.io.pkcs8.der" };

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
				.add(AdminResource.get());

		routes.filter(new CrossOriginFilter());
	}

	public static void main(String[] args) {
		try {
			boolean ssl = true;
			String[] conf = OvhConf;

			if (!Files.isDirectory(Paths.get(conf[0]))) {

				conf = GoogleConf;

				if (!Files.isDirectory(Paths.get(conf[0]))) {
					conf = LocalhostConf;
					ssl = false;
				}
			}

			startElastic(conf);
			startFluent(ssl, conf);

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static void startElastic(String[] conf)
			throws InterruptedException, ExecutionException, IOException {

		// Sets ElasticSearch data path
		System.setProperty("es.path.data", Paths.get(conf[0]).resolve("data")
				.toAbsolutePath().toString());

		elasticNode = NodeBuilder.nodeBuilder().local(true).data(true)
				.clusterName("spacedog-elastic-cluster").node();

		elasticClient = elasticNode.client();

		AdminResource.get().initSpacedogIndex();
	}

	private static void startFluent(boolean ssl, String[] conf)
			throws IOException {
		if (ssl) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");

			Path sslPath = Paths.get(conf[0]).resolve("ssl");
			new WebServer().configure(Start::configure)
					.startSSL(
							443,
							Arrays.asList(sslPath.resolve(conf[1]),
									sslPath.resolve(conf[2])),
							sslPath.resolve(conf[3]));

			HttpPermanentRedirect.start(80, "https://spacedog.io");

		} else {
			new WebServer().configure(Start::configure).start(8080);

			HttpPermanentRedirect.start(9090, "http://127.0.0.1:8080");
		}
	}
}
