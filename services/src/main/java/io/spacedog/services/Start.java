/**
 * © David Attias 2015
 */
package io.spacedog.services;

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

import com.google.common.io.Resources;

import io.spacedog.utils.Exceptions;
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
	private StartConfiguration config;
	private Info info;

	public Node getElasticNode() {
		return elasticNode;
	}

	public ElasticClient getElasticClient() {
		return elastic;
	}

	public StartConfiguration configuration() {
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
		if (info == null)
			try {
				info = Json.mapper().readValue(//
						Resources.toString(//
								Resources.getResource(this.getClass(), "info.json"), //
								Utils.UTF8), //
						Info.class);
			} catch (IOException e) {
				throw Exceptions.runtime(e, "error loading server info file");
			}
		return info;
	}

	private void upgradeAndCleanUp() throws IOException {
		// Utils.info("[SpaceDog] Nothing to upgrade");
		deletePingRequestsFromLogs();
		deleteGetLogRequestsFromLogs();
		SnapshotResource.get().deleteMissingRepositories();
		SnapshotResource.get().deleteObsoleteRepositories();
	}

	private void deleteGetLogRequestsFromLogs() {
		Utils.info("[SpaceDog] Deleting [GET] [/1/log] requests from logs ...");

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery("method", "GET"))//
				.filter(QueryBuilders.termQuery("path", "/1/log"));

		String querySource = new QuerySourceBuilder()//
				.setQuery(queryBuilder)//
				.toString();

		DeleteByQueryResponse response = get().getElasticClient()//
				.deleteByQuery(querySource, Resource.SPACEDOG_BACKEND, LogResource.TYPE);

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

		DeleteByQueryResponse response = get().getElasticClient()//
				.deleteByQuery(querySource, Resource.SPACEDOG_BACKEND, LogResource.TYPE);

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
				// disable rebalance to avoid automatic rebalance
				// when a temporary second node appears
				.put("cluster.routing.rebalance.enable", "none")//
				.put("http.enabled", //
						config.isElasticHttpEnabled())//
				.put("network.host", //
						config.elasticNetworkHost())//
				.put("path.home", //
						config.homePath().toAbsolutePath().toString())
				.put("path.data", //
						config.elasticDataPath().toAbsolutePath().toString());

		if (config.snapshotsPath().isPresent())
			builder.put("path.repo", //
					config.snapshotsPath().get().toAbsolutePath().toString());

		elasticNode = new ElasticNode(builder.build(), //
				DeleteByQueryPlugin.class, //
				CloudAwsPlugin.class);

		elasticNode.start();
		setElasticClient(elasticNode.client());

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to GREEN before to initialize anything else
		// wait for 30 seconds maximum
		elastic.ensureAllIndicesGreen();
	}

	void setElasticClient(Client client) {
		this.elastic = new ElasticClient(client);
	}

	private void initServices() throws IOException {
		LogResource.get().init();
		CredentialsResource.get().init();
	}

	private void startFluent() throws IOException {
		fluent = new MyFluentServer();
		fluent.configure(Start::configure);
		fluent.start(config.serverPort());
	}

	private static void configure(Routes routes) {
		routes.add(BackendResource.get())//
				.add(AdminResource.get())//
				.add(DataResource.get())//
				.add(LafargeCesioResource.get())//
				.add(SchemaResource.get())//
				.add(CredentialsResource.get())//
				.add(LinkedinResource.get())//
				.add(EdfResource.get())//
				.add(UserResource.get())//
				.add(BatchResource.get())//
				.add(MailResource.get())//
				.add(MailTemplateResource.get())//
				.add(SmsResource.get())//
				.add(SmsTemplateResource.get())//
				.add(SnapshotResource.get())//
				.add(LogResource.get())//
				.add(PushResource.get())//
				.add(SendPulseResource.get())//
				.add(StripeResource.get())//
				.add(ShareResource.get())//
				.add(SettingsResource.get())//
				.add(SearchResource.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(LogResource.filter())//
				.filter(SpaceContext.checkAuthorizationFilter())//
				// web filter before error filter
				// so web errors are html pages
				.filter(WebResource.get().filter())//
				.filter(new ServiceErrorFilter())//
				.filter(FileResource.get().filter());
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
		if (singleton == null)
			singleton = new Start();
		return singleton;
	}

	private Start() {
		this.config = new StartConfiguration();
	}
}