/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.Console;
import java.net.InetAddress;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

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

		TransportClient client = null;

		try {

			Settings settings = Settings.settingsBuilder()//
					.put("cluster.name", Start.CLUSTER_NAME).build();

			client = TransportClient.builder().settings(settings).build();
			client.addTransportAddress(new InetSocketTransportAddress(//
					InetAddress.getByName(Start.get().configuration().elasticNetworkHost()), //
					9300));

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