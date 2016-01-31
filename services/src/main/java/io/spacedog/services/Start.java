/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Arrays;
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

	private Node elasticNode;
	private Client elasticClient;
	private MyFluentServer fluent;
	private StartConfiguration config;

	public Node getElasticNode() {
		return elasticNode;
	}

	public Client getElasticClient() {
		return elasticClient;
	}

	public StartConfiguration configuration() {
		return config;
	}

	public static void main(String[] args) {
		try {
			singleton = new Start();
			singleton.startLocalElastic();
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

		elasticNode = NodeBuilder.nodeBuilder()//
				.local(true)//
				.data(true)//
				.clusterName("spacedog-elastic-cluster")//
				.settings(ImmutableSettings.builder()//
						.put("path.data", //
								config.elasticDataPath().toAbsolutePath().toString())
						.build())//
				.node();

		elasticClient = elasticNode.client();
		AccountResource.get().initSpacedogIndex();
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
				.add(PushResource.get())//
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