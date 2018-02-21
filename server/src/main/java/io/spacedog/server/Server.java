/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.plugin.cloud.aws.CloudAwsPlugin;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.joda.time.DateTimeZone;

import io.spacedog.utils.ClassResources;
import io.spacedog.utils.DateTimeZones;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.AbstractWebServer;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.internal.Handler;
import net.codestory.http.internal.HttpServerWrapper;
import net.codestory.http.internal.SimpleServerWrapper;
import net.codestory.http.payload.Payload;
import net.codestory.http.routes.Routes;
import net.codestory.http.websockets.WebSocketHandler;

public class Server {

	public static final String CLUSTER_NAME = "spacedog-elastic-cluster";

	private ElasticNode elasticNode;
	private ElasticClient elasticClient;
	private FluentServer fluent;
	private ServerConfiguration config;
	private Info info;

	public ElasticClient elasticClient() {
		return elasticClient;
	}

	public ServerConfiguration configuration() {
		return config;
	}

	public static void main(String[] args) {
		DateTimeZone.setDefault(DateTimeZones.PARIS);
		Server server = new Server();
		server.start();
	}

	public void start() {

		try {
			init();
			startElastic();
			elasticIsStarted();
			startFluent();
			fluentIsStarted();

		} catch (Throwable t) {
			t.printStackTrace();
			if (fluent != null)
				fluent.stop();
			if (elasticClient != null)
				elasticClient.close();
			if (elasticNode != null)
				elasticClient.close();
			System.exit(-1);
		}
	}

	protected void init() {
		this.config = new ServerConfiguration();
		System.setProperty("http.agent", configuration().serverUserAgent());
		String string = ClassResources.loadAsString(this, "info.json");
		info = Json.toPojo(string, Info.class);
	}

	protected void startElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.master", true)//
				.put("node.data", true)//
				.put("cluster.name", CLUSTER_NAME)//
				// disable automatic index creation
				.put("action.auto_create_index", false)//
				// disable dynamic indexing
				.put("index.mapper.dynamic", false)//
				.put("index.max_result_window", 5000)//
				// disable rebalance to avoid automatic rebalance
				// when a temporary second node appears
				.put("cluster.routing.rebalance.enable", "none")//
				.put("http.enabled", //
						config.elasticIsHttpEnabled())//
				.put("network.host", //
						config.elasticNetworkHost())//
				.put("path.home", //
						config.homePath().toAbsolutePath().toString())
				.put("path.data", //
						config.elasticDataPath().toAbsolutePath().toString());

		if (config.elasticSnapshotsPath().isPresent())
			builder.put("path.repo", //
					config.elasticSnapshotsPath().get().toAbsolutePath().toString());

		elasticNode = new ElasticNode(builder.build(), //
				DeleteByQueryPlugin.class, //
				CloudAwsPlugin.class);

		elasticNode.start();
		this.elasticClient = new ElasticClient(elasticNode.client());

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to GREEN before to initialize anything else
		// wait for 60 seconds maximum by default
		// configurable with spacedog.server.green.check and timeout properties
		elasticClient.ensureAllIndicesAreGreen();
	}

	protected void elasticIsStarted() {
		// init templates
		this.elasticClient.internal().admin().indices()//
				.preparePutTemplate("data")//
				.setSource(ClassResources.loadAsBytes(this, "data-template.json"))//
				.get();

		// init root backend indices
		String backendId = config.apiBackend().backendId();
		AdminService.get().initBackendIndices(backendId);
	}

	protected void startFluent() {
		fluent = new FluentServer();
		fluent.configure(routes -> configure(routes));
		fluent.start(config.serverPort());
	}

	protected void fluentIsStarted() {
	}

	protected void configure(Routes routes) {
		routes.add(AdminService.get())//
				.add(DataService.get())//
				.add(SchemaService.get())//
				.add(CredentialsService.get())//
				.add(LinkedinService.get())//
				.add(BatchService.get())//
				.add(EmailService.get())//
				.add(SmsService.get())//
				.add(LogService.get())//
				.add(PushService.get())//
				.add(ApplicationService.get())//
				.add(StripeService.get())//
				.add(SettingsService.get())//
				.add(SearchService.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(LogService.filter())//
				.filter(SpaceContext.checkAuthorizationFilter())//
				// web filter before error filter
				// so web errors are html pages
				.filter(WebService.get().filter())//
				.filter(new ServiceErrorFilter())//
				.filter(FileService.get().filter());
	}

	public static class Info {
		public String version;
		public String baseline;
	}

	public Info info() {
		return info;
	}

	public Payload executeRequest(Request request, Response response) throws Exception {
		return fluent.executeRequest(request, response);
	}

	//
	// Implementation
	//

	private static class FluentServer extends AbstractWebServer<FluentServer> {

		@Override
		protected HttpServerWrapper createHttpServer(Handler httpHandler, WebSocketHandler webSocketHandler) {
			return new SimpleServerWrapper(httpHandler, webSocketHandler, 12, 1, 1);
		}

		public Payload executeRequest(Request request, Response response) throws Exception {
			return routesProvider.get().apply(request, response);
		}
	}

	//
	// Singleton
	//

	private static Server singleton;

	public static Server get() {
		return singleton;
	}

	private Server() {
		if (singleton != null)
			throw Exceptions.runtime("server already running");
		singleton = this;
	}
}