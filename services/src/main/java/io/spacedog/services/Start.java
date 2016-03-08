/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;

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

	private Node elasticNode;
	private ElasticClient elasticClient;
	private MyFluentServer fluent;
	private StartConfiguration config;

	public Node getElasticNode() {
		return elasticNode;
	}

	public ElasticClient getElasticClient() {
		return elasticClient;
	}

	public StartConfiguration configuration() {
		return config;
	}

	public static void main(String[] args) {
		try {
			singleton = new Start();
			singleton.startLocalElastic();
			singleton.initServices();
			singleton.startFluent();

		} catch (Throwable t) {
			t.printStackTrace();
			if (singleton != null) {
				if (singleton.fluent != null)
					singleton.elasticClient.close();
				if (singleton.elasticClient != null)
					singleton.elasticClient.close();
				if (singleton.elasticNode != null)
					singleton.elasticClient.close();
			}
			System.exit(-1);
		}
	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.master", true)//
				.put("node.data", true)//
				.put("cluster.name", "spacedog-elastic-cluster")//
				// disable rebalance to avoid automatic rebalance
				// when a temporary second node appears
				.put("cluster.routing.rebalance.enable", "none")//
				.put("path.home", //
						config.homePath().toAbsolutePath().toString())
				.put("path.data", //
						config.elasticDataPath().toAbsolutePath().toString());

		if (config.snapshotsPath().isPresent())
			builder.put("path.repo", //
					config.snapshotsPath().get().toAbsolutePath().toString());

		elasticNode = new ElasticNode(builder.build(), //
				Collections.singleton(DeleteByQueryPlugin.class));

		elasticNode.start();
		Client client = elasticNode.client();
		elasticClient = new ElasticClient(client);

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to YELLOW or GREEN before to initialize anything else
		// wait only for 5 seconds maximum

		while (true) {
			Thread.sleep(1000);
			ClusterHealthStatus status = client.admin().cluster().prepareHealth().get().getStatus();
			if (!ClusterHealthStatus.RED.equals(status))
				return;
		}
	}

	private void initServices() throws IOException {
		AccountResource.get().initSpacedogBackend();
	}

	private void startFluent() throws IOException {

		fluent = new MyFluentServer();
		fluent.configure(Start::configure);

		if (config.isSsl()) {
			fluent.startSSL(//
					config.sslPort(), //
					Arrays.asList(//
							config.crtFilePath().get(), //
							config.pemFilePath().get()), //
					config.derFilePath().get());
		} else
			fluent.start(config.sslPort());

		HttpPermanentRedirect.start(config.nonSslPort(), //
				configuration().sslUrl());
	}

	private static void configure(Routes routes) {
		routes.add(DataResource.get())//
				.add(SchemaResource.get())//
				.add(UserResource.get())//
				.add(AccountResource.get())//
				.add(BatchResource.get())//
				.add(MailResource.get())//
				.add(SnapshotResource.get())//
				.add(LogResource.get())//
				// .add(PushResource.get())//
				.add(PushResource2.get())//
				.add(FileResource.get())//
				.add(ShareResource.get())//
				.add(SearchResource.get());

		routes.filter(new CrossOriginFilter())//
				.filter(SpaceContext.filter())//
				.filter(LogResource.filter())//
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
		this.config = new StartConfiguration();
	}
}