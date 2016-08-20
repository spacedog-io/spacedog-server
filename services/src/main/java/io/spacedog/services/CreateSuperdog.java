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

import io.spacedog.utils.Utils;

public class CreateSuperdog {

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

		Client client = null;

		try {
			Builder builder = Settings.builder()//
					.put("path.home", Paths.get(System.getProperty("user.home"), "spacedog"))//
					.put("http.enabled", false);

			client = NodeBuilder.nodeBuilder()//
					.client(true)//
					.clusterName(Start.CLUSTER_NAME)//
					.settings(builder)//
					.build()//
					.start()//
					.client();

			Start.get().setElasticClient(client);
			CredentialsResource.get().createSuperdog(username, password, email);
			Utils.info("Superdog credentials [%s] indexed in [spacedog-credentials]", username);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (client != null)
				client.close();
		}
	}
}