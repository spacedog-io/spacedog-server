/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.common.collect.Maps;

import net.codestory.http.AbstractWebServer;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.internal.Handler;
import net.codestory.http.internal.HttpServerWrapper;
import net.codestory.http.internal.SimpleServerWrapper;
import net.codestory.http.payload.Payload;
import net.codestory.http.routes.Routes;
import net.codestory.http.websockets.WebSocketHandler;

public class Start extends AbstractWebServer<Start> {

	public static class ProductionConfiguration {

		private String homePath;
		private String derFileName;
		private String pemFileName;
		private String crtFileName;

		public ProductionConfiguration(String homePath) {
			this(homePath, null, null, null);
		}

		public ProductionConfiguration(String homePath, String crtFileName, String pemFileName, String derFileName) {
			this.homePath = homePath;
			this.crtFileName = crtFileName;
			this.pemFileName = pemFileName;
			this.derFileName = derFileName;
		}

		public String getHomePath() {
			return homePath;
		}

		public String getDerFileName() {
			return derFileName;
		}

		public String getPemFileName() {
			return pemFileName;
		}

		public String getCrtFileName() {
			return crtFileName;
		}

		public boolean isSsl() {
			return crtFileName != null;
		}
	}

	private Node elasticNode;
	private Client elasticClient;
	private ProductionConfiguration configuration;

	private static Map<String, ProductionConfiguration> configurations = Maps.newHashMap();

	static {
		configurations.put("Local", new ProductionConfiguration("/Users/davattias/dev/spacedog"));
		configurations.put("AWS", new ProductionConfiguration("/home/ec2-user/spacedog"));
		configurations.put("Google", //
				new ProductionConfiguration("/home/davattias/spacedog", //
						"attias.space.certificate.191630.crt", "GandiStandardSSLCA.intermediate.pem",
						"attias.space.pkcs8.der"));
		configurations.put("OVH", //
				new ProductionConfiguration("/root/spacedog", //
						"spacedog.io.certificate.210740.crt", "GandiStandardSSLCA.intermediate.pem",
						"spacedog.io.pkcs8.der"));

	}

	public Node getElasticNode() {
		return elasticNode;
	}

	public Client getElasticClient() {
		return elasticClient;
	}

	private static void configure(Routes routes) {
		routes.add(DataResource.get())//
				.add(SchemaResource.get())//
				.add(UserResource.get())//
				.add(AdminResource.get())//
				.add(BatchResource.get())//
				.add(SearchResource.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(new ServiceErrorFilter());
	}

	public static void main(String[] args) {

		try {
			singleton = new Start();
			singleton.chooseProductionConfiguration();
			singleton.startElastic();
			singleton.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private void chooseProductionConfiguration() {
		for (ProductionConfiguration conf : configurations.values()) {
			if (Files.isDirectory(Paths.get(conf.getHomePath()))) {
				configuration = conf;
				return;
			}
		}
		configuration = configurations.get("Local");
	}

	private void startElastic() throws InterruptedException, ExecutionException, IOException {

		elasticNode = NodeBuilder.nodeBuilder()//
				.local(true)//
				.data(true)//
				.clusterName("spacedog-elastic-cluster")//
				.settings(ImmutableSettings.builder()//
						.put("path.data", //
								Paths.get(configuration.getHomePath()).resolve("data").toAbsolutePath().toString())
						.build())//
				.node();

		elasticClient = elasticNode.client();

		AdminResource.get().initSpacedogIndex();
	}

	private void startFluent() throws IOException {

		singleton.configure(Start::configure);

		if (configuration.isSsl()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");

			Path sslPath = Paths.get(configuration.getHomePath()).resolve("ssl");
			singleton.startSSL(443,
					Arrays.asList(sslPath.resolve(configuration.getCrtFileName()),
							sslPath.resolve(configuration.getPemFileName())),
					sslPath.resolve(configuration.getDerFileName()));

			HttpPermanentRedirect.start(80, "https://spacedog.io");

		} else {
			singleton.start(8080);
			HttpPermanentRedirect.start(9090, "http://127.0.0.1:8080");
		}
	}

	public static Payload executeInternalRequest(Request request, Response response) throws Exception {
		return singleton.routesProvider.get().apply(request, response);
	}

	@Override
	protected HttpServerWrapper createHttpServer(Handler httpHandler, WebSocketHandler webSocketHandler) {
		return new SimpleServerWrapper(httpHandler, webSocketHandler);
	}

	//
	// Singleton
	//

	private static Start singleton;

	static Start get() {
		return singleton;
	}

	private Start() {
	}

}