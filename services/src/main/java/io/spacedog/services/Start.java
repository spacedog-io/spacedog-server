/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.cloud.aws.CloudAwsPlugin;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;

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
		try {
			singleton = new Start();
			singleton.startLocalElastic();
			singleton.initServices();
			singleton.upgrade();
			singleton.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			if (singleton != null) {
				if (singleton.fluent != null)
					singleton.elastic.close();
				if (singleton.elastic != null)
					singleton.elastic.close();
				if (singleton.elasticNode != null)
					singleton.elastic.close();
			}
			System.exit(-1);
		}
	}

	private void upgrade() throws IOException {
		Utils.info("[SpaceDog] Nothing to upgrade");
	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.master", true)//
				.put("node.data", true)//
				.put("cluster.name", CLUSTER_NAME)//
				// disable rebalance to avoid automatic rebalance
				// when a temporary second node appears
				.put("cluster.routing.rebalance.enable", "none")//
				.put("http.enabled", //
						config.isElasticHttpEnabled())//
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
		Client client = elasticNode.client();
		elastic = new ElasticClient(client);

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to YELLOW or GREEN before to initialize anything else
		// wait for 10 seconds maximum

		// elastic.ensureGreen();
		while (true) {
			Thread.sleep(1000);
			ClusterHealthStatus status = client.admin().cluster().prepareHealth().get().getStatus();
			if (!ClusterHealthStatus.RED.equals(status))
				return;
		}
	}

	private void initServices() throws IOException {
		LogResource.get().init();
		CredentialsResource.get().init();
	}

	private void startFluent() throws IOException {
		fluent = new MyFluentServer();
		fluent.configure(Start::configure);
		fluent.start(config.apiPort());
	}

	private static void configure(Routes routes) {
		routes.add(BackendResource.get())//
				.add(AdminResource.get())//
				.add(DataResource.get())//
				.add(SchemaResource.get())//
				.add(CredentialsResource.get())//
				.add(UserResource.get())//
				.add(BatchResource.get())//
				.add(MailResource.get())//
				.add(SnapshotResource.get())//
				.add(LogResource.get())//
				.add(PushResource.get())//
				.add(FileResource.get())//
				.add(ShareResource.get())//
				.add(SearchResource.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(LogResource.filter())//
				.filter(new ServiceErrorFilter())//
				.filter(WebResource.get().filter());
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
		this.config = new StartConfiguration();
	}
}