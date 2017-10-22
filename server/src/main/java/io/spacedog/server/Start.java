/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.cloud.aws.CloudAwsPlugin;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.joda.time.DateTimeZone;

import io.spacedog.http.SpaceBackend;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
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

	public static final String CLUSTER_NAME = "spacedog-elastic-cluster";

	private Node elasticNode;
	private ElasticClient elastic;
	private MyFluentServer fluent;
	private ServerConfiguration config;
	private Info info;

	public Node getElasticNode() {
		return elasticNode;
	}

	public ElasticClient getElasticClient() {
		return elastic;
	}

	public ServerConfiguration configuration() {
		return config;
	}

	public static void main(String[] args) {
		DateTimeZone.setDefault(DateTimeZone.forID("Europe/Paris"));
		Start start = get();
		System.setProperty("http.agent", start.configuration().serverUserAgent());

		try {
			start.info();
			start.startElasticNode();
			start.initServices();
			start.upgradeAndCleanUp();
			start.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			if (start != null) {
				if (start.fluent != null)
					start.fluent.stop();
				if (start.elastic != null)
					start.elastic.close();
				if (start.elasticNode != null)
					start.elastic.close();
			}
			System.exit(-1);
		}
	}

	public static class Info {
		public String version;
		public String baseline;
	}

	public Info info() {
		if (info == null) {
			String string = ClassResources.loadToString(this, "info.json");
			info = Json.toPojo(string, Info.class);
		}
		return info;
	}

	private void upgradeAndCleanUp() throws IOException {
		Utils.info("[SpaceDog] Nothing to upgrade");
		// deletePingRequestsFromLogs();
		// deleteGetLogRequestsFromLogs();
		// SnapshotResource.get().deleteMissingRepositories();
		// SnapshotResource.get().deleteObsoleteRepositories();
	}

	private void deleteGetLogRequestsFromLogs() {
		Utils.info("[SpaceDog] Deleting [GET] [/1/log] requests from logs ...");

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery("method", "GET"))//
				.filter(QueryBuilders.termQuery("path", "/1/log"));

		String querySource = new QuerySourceBuilder()//
				.setQuery(queryBuilder)//
				.toString();

		ElasticClient elastic = get().getElasticClient();
		Index index = LogService.logIndex().backendId(SpaceBackend.defaultBackendId());

		DeleteByQueryResponse response = elastic.deleteByQuery(querySource, index);
		Utils.info("[SpaceDog] [%s] logs deleted", response.getTotalDeleted());
	}

	private void deletePingRequestsFromLogs() {
		Utils.info("[SpaceDog] Deleting [GET] [/] requests from logs ...");

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery("method", "GET"))//
				.filter(QueryBuilders.termQuery("path", "/"));

		String querySource = new QuerySourceBuilder()//
				.setQuery(queryBuilder)//
				.toString();

		ElasticClient elastic = get().getElasticClient();
		Index index = LogService.logIndex().backendId(SpaceBackend.defaultBackendId());

		DeleteByQueryResponse response = elastic.deleteByQuery(querySource, index);
		Utils.info("[SpaceDog] [%s] logs deleted", response.getTotalDeleted());
	}

	private void startElasticNode() throws InterruptedException, ExecutionException, IOException {

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
		setElasticClient(elasticNode.client());

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to GREEN before to initialize anything else
		// wait for 60 seconds maximum
		elastic.ensureAllIndicesAreGreen();
	}

	void setElasticClient(Client client) {
		this.elastic = new ElasticClient(client);
	}

	private void initServices() throws IOException {
		BackendService.get()//
				.initBackendIndices(SpaceBackend.defaultBackendId(), false);
	}

	private void startFluent() throws IOException {
		fluent = new MyFluentServer();
		fluent.configure(Start::configure);
		fluent.start(config.serverPort());
	}

	private static void configure(Routes routes) {
		routes.add(BackendService.get())//
				.add(AdminService.get())//
				.add(DataService.get())//
				.add(SchemaService.get())//
				.add(CredentialsService.get())//
				.add(LinkedinService.get())//
				.add(BatchService.get())//
				.add(MailService.get())//
				.add(MailTemplateService.get())//
				.add(SmsService.get())//
				.add(SmsTemplateService.get())//
				.add(SnapshotService.get())//
				.add(LogService.get())//
				.add(PushSpaceService.get())//
				.add(ApplicationService.get())//
				.add(StripeService.get())//
				.add(ShareService.get())//
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

	private static class MyFluentServer extends AbstractWebServer<MyFluentServer> {

		@Override
		protected HttpServerWrapper createHttpServer(Handler httpHandler, WebSocketHandler webSocketHandler) {
			return new SimpleServerWrapper(httpHandler, webSocketHandler);
		}

		public Payload executeRequest(Request request, Response response) throws Exception {
			return routesProvider.get().apply(request, response);
		}

		@Override
		protected void handleHttp(Request request, Response response) {
			try {
				super.handleHttp(request, response);
			} finally {
				S3Resource.closeThisThreadS3Object();
			}
		}
	}

	public Payload executeRequest(Request request, Response response) throws Exception {
		return fluent.executeRequest(request, response);
	}

	//
	// Singleton
	//

	private static Start singleton;

	public static Start get() {
		if (singleton == null)
			singleton = new Start();
		return singleton;
	}

	private Start() {
		this.config = new ServerConfiguration();
	}
}