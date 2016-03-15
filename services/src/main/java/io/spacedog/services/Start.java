/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.Credentials.Level;
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

public class Start {

	public static final String CLUSTER_NAME = "spacedog-elastic-cluster";

	private Node elasticNode;
	private ElasticClient elastic;
	private MyFluentServer fluent;
	private StartConfiguration config;
	private int credentialsCount = 0;
	private int userCount = 0;

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

		if (elastic.existsIndex("spacedog", "account")) {

			SearchHit[] accounts = elastic.prepareSearch("spacedog", "account")//
					.setTypes("account").setSize(100).get()//
					.getHits().getHits();

			for (SearchHit account : accounts) {
				Map<String, Object> source = account.getSource();

				String backendId = source.get(UserResource.BACKEND_ID).toString();

				log("Upgrading account [%s] ...", backendId);

				SearchHit[] users = elastic.prepareSearch(backendId, "user")//
						.setTypes("user").setSize(1000).get()//
						.getHits().getHits();

				for (SearchHit user : users) {
					createUserCredentials(backendId, user);
					upgradeUserData(backendId, user);
				}

				upgradeSuperAdmin(backendId, source);
			}

			elastic.deleteIndex("spacedog", "account");
		}

		log("Credentials created = %s", credentialsCount);
		log("User data created or updated = %s", userCount);

	}

	void upgradeSuperAdmin(String backendId, Map<String, Object> source) {
		String now = DateTime.now().toString();
		String username = source.get(UserResource.USERNAME).toString();
		String hashedPassword = source.get(UserResource.HASHED_PASSWORD).toString();
		String email = source.get(UserResource.EMAIL).toString();

		ObjectNode credentials = Json.object(//
				UserResource.BACKEND_ID, backendId, //
				UserResource.USERNAME, username, //
				UserResource.HASHED_PASSWORD, hashedPassword, //
				UserResource.EMAIL, email, //
				UserResource.CREDENTIALS_LEVEL, Level.SUPER_ADMIN, //
				UserResource.CREATED_AT, now, //
				UserResource.UPDATED_AT, now);

		UserResource.get().indexCredentials(backendId, username, credentials);
		credentialsCount++;

		ObjectNode user = Json.object(//
				UserResource.USERNAME, username, //
				UserResource.EMAIL, email);

		DataStore.get().createObject(backendId, UserResource.USER_TYPE, //
				username, user, username);

		userCount++;
	}

	private void createUserCredentials(String backendId, SearchHit user) {
		Map<String, Object> userMap = user.getSource();

		String username = userMap.get(UserResource.USERNAME).toString();
		String hashedPassword = userMap.get(UserResource.HASHED_PASSWORD).toString();
		String email = userMap.get(UserResource.EMAIL).toString();
		@SuppressWarnings("unchecked")
		Map<String, Object> meta = (Map<String, Object>) userMap.get("meta");
		String createdAt = meta.get(UserResource.CREATED_AT).toString();
		String updatedAt = meta.get(UserResource.UPDATED_AT).toString();

		log("Upgrading backend [%s] user [%s] ...", backendId, username);

		ObjectNode credentials = Json.object(//
				UserResource.BACKEND_ID, backendId, //
				UserResource.USERNAME, username, //
				UserResource.HASHED_PASSWORD, hashedPassword, //
				UserResource.EMAIL, email, //
				UserResource.CREDENTIALS_LEVEL, Level.USER, //
				UserResource.CREATED_AT, createdAt, //
				UserResource.UPDATED_AT, updatedAt);

		UserResource.get().indexCredentials(backendId, username, credentials);
		credentialsCount++;
	}

	private void upgradeUserData(String backendId, SearchHit user) {
		Map<String, Object> userMap = user.getSource();
		String username = userMap.get(UserResource.USERNAME).toString();

		userMap.remove(UserResource.HASHED_PASSWORD);
		userMap.remove(UserResource.PASSWORD_RESET_CODE);
		userMap.remove(UserResource.PASSWORD);
		userMap.remove("groups");

		elastic.prepareIndex(backendId, "user", username).setSource(userMap).get();
		userCount++;
	}

	private static void log(String string, Object... args) {
		System.out.println(String.format(string, args));
	}

	private void startLocalElastic() throws InterruptedException, ExecutionException, IOException {

		Builder builder = Settings.builder()//
				.put("node.master", true)//
				.put("node.data", true)//
				.put("cluster.name", CLUSTER_NAME)//
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
		elastic = new ElasticClient(client);

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
		LogResource.get().init();
		UserResource.get().init();
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
				.add(BackendResource.get())//
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