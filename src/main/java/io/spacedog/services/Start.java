/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import net.codestory.http.AbstractWebServer;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.internal.Handler;
import net.codestory.http.internal.HttpServerWrapper;
import net.codestory.http.internal.SimpleServerWrapper;
import net.codestory.http.payload.Payload;
import net.codestory.http.routes.Routes;
import net.codestory.http.websockets.WebSocketHandler;

public class Start {

	private static final String DEFAULT_SPACEDOG_HOME = "/home/spacedog";
	private static final String DEFAULT_SPACEDOG_PROPERTIES_FILE = "/etc/spacedog.properties";

	private Node elasticNode;
	private Client elasticClient;
	private MyFluentServer fluent;
	private Properties configuration = new Properties();

	public Node getElasticNode() {
		return elasticNode;
	}

	public Client getElasticClient() {
		return elasticClient;
	}

	public Path getSpaceDogHomePath() {
		String path = configuration.getProperty("spacedog.home");
		return path == null ? Paths.get(DEFAULT_SPACEDOG_HOME) : Paths.get(path);
	}

	public Path getElasticDataPath() {
		String path = configuration.getProperty("spacedog.elastic.data");
		return path == null ? //
				getSpaceDogHomePath().resolve("data") : Paths.get(path);
	}

	public Path getDerPath() {
		String path = configuration.getProperty("spacedog.ssl.der");
		return path == null ? //
				getSpaceDogHomePath().resolve("ssl/spacedog.io.pkcs8.der") : Paths.get(path);
	}

	public Path getPemPath() {
		String path = configuration.getProperty("spacedog.ssl.pem");
		return path == null ? //
				getSpaceDogHomePath().resolve("ssl/GandiStandardSSLCA.intermediate.pem") : Paths.get(path);
	}

	public Path getCrtPath() {
		String path = configuration.getProperty("spacedog.ssl.crt");
		return path == null ? //
				getSpaceDogHomePath().resolve("ssl/spacedog.io.certificate.210740.crt") : Paths.get(path);
	}

	public boolean isSsl() {
		return Files.isReadable(getCrtPath());
	}

	public static void main(String[] args) {
		try {
			singleton = new Start();
			singleton.startElastic();
			singleton.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private void startElastic() throws InterruptedException, ExecutionException, IOException {

		elasticNode = NodeBuilder.nodeBuilder()//
				.local(true)//
				.data(true)//
				.clusterName("spacedog-elastic-cluster")//
				.settings(ImmutableSettings.builder()//
						.put("path.data", //
								getElasticDataPath().toAbsolutePath().toString())
						.build())//
				.node();

		elasticClient = elasticNode.client();
		AdminResource.get().initSpacedogIndex();
	}

	private void startFluent() throws IOException {

		fluent = new MyFluentServer();
		fluent.configure(Start::configure);

		if (isSsl()) {
			fluent.startSSL(443, Arrays.asList(getCrtPath(), getPemPath()), getDerPath());
			HttpPermanentRedirect.start(80, "https://spacedog.io");
		} else {
			fluent.start(8080);
			HttpPermanentRedirect.start(9090, "http://127.0.0.1:8080");
		}
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

	private static class MyFluentServer extends AbstractWebServer<MyFluentServer> {

		@Override
		protected HttpServerWrapper createHttpServer(Handler httpHandler, WebSocketHandler webSocketHandler) {
			return new SimpleServerWrapper(httpHandler, webSocketHandler);
		}

		public Payload executeRequest(Request request, Response response) throws Exception {
			return routesProvider.get().apply(request, response);
		}
	}

	public Payload executeRequest(Request request, Response response) throws Exception {
		return fluent.executeRequest(request, response);
	}

	//
	// Singleton
	//

	private static Start singleton;

	static Start get() {
		return singleton;
	}

	private Start() throws IOException {
		Path confPath = Paths.get(DEFAULT_SPACEDOG_PROPERTIES_FILE);

		if (Files.isReadable(confPath)) {
			System.out.println("[SpaceDog] configuration file = " + confPath);
			configuration.load(Files.newInputStream(confPath));
		}

		if (!Files.isDirectory(getSpaceDogHomePath()))
			throw new RuntimeException(String.format("SpaceDog home path not a directory [%s]", //
					getSpaceDogHomePath()));

		if (!Files.isDirectory(getElasticDataPath()))
			throw new RuntimeException(String.format("SpaceDog data path not a directory [%s]", //
					getElasticDataPath()));

		System.out.println("[SpaceDog] spacedog path = " + getSpaceDogHomePath());
		System.out.println("[SpaceDog] data path = " + getElasticDataPath());

		if (isSsl()) {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");

			System.out.println("[SpaceDog] .crt file = " + getCrtPath());
			System.out.println("[SpaceDog] .pem file = " + getPemPath());
			System.out.println("[SpaceDog] .der file = " + getDerPath());
		}
	}
}