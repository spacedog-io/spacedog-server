/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.Console;
import java.nio.file.Paths;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.NodeBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.Utils;

public class CreateSuperdog {

	private static Client client;
	private static ElasticClient elastic;

	public static void main(String[] args) throws Exception {

		String username = "superdog-david";
		String email = "david@spacedog.io";
		String password = System.getProperty("password");

		Console console = System.console();

		if (console != null) {
			username = "superdog-" + console.readLine("Enter username: superdog-");
			email = console.readLine("Enter email: ");
			password = new String(console.readPassword("Enter password: "));
		}

		try {
			Builder builder = Settings.builder()//
					.put("path.home", Paths.get(System.getProperty("user.home"), "spacedog"))//
					.put("http.enabled", false);

			client = NodeBuilder.nodeBuilder()//
					.client(true)//
					.clusterName(Start.CLUSTER_NAME)//
					.settings(builder)//
					.build().start().client();

			elastic = new ElasticClient(client);

			String now = DateTime.now().toString();
			ObjectNode credentials = Json.object(//
					Resource.BACKEND_ID, Backends.ROOT_API, //
					Resource.USERNAME, username, //
					Resource.CREDENTIALS_LEVEL, Level.SUPERDOG.toString(), //
					Resource.EMAIL, email, //
					Resource.HASHED_PASSWORD, Passwords.checkAndHash(password), //
					Resource.CREATED_AT, now, //
					Resource.UPDATED_AT, now);

			elastic.index(Resource.SPACEDOG_BACKEND, CredentialsResource.TYPE, //
					CredentialsResource.toCredentialsId(Backends.ROOT_API, username), //
					credentials.toString());

			Utils.info("Superdog credentials [api-%s] indexed in [spacedog-credentials]", username);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (client != null)
				client.close();
		}
	}
}