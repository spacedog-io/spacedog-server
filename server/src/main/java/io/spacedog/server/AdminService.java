/**
 * © David Attias 2015
 */
package io.spacedog.server;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class AdminService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload ping() {
		ObjectNode payload = (ObjectNode) Json.toJsonNode(Server.get().info());
		return JsonPayload.ok().withObject(payload).build();
	}

	@Get("/1/admin/return500")
	@Get("/1/admin/return500")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	@Post("/1/admin/clear")
	@Post("/1/admin/clear")
	public void reset() {
		ElasticClient elastic = Server.get().elasticClient();
		elastic.deleteBackendIndices();
		deleteFilesAndSharesIfNecessary();
		initBackendIndices(SpaceContext.backendId());
	}

	//
	// Public interface
	//

	public void initBackendIndices(String backendId) {
		Index index = CredentialsService.credentialsIndex().backendId(backendId);
		if (!elastic().exists(index)) {
			CredentialsService.get().initIndex(backendId);
			LogService.get().initIndex(backendId);
		}
	}

	public void deleteFilesAndSharesIfNecessary() {
		ServerConfiguration configuration = Server.get().configuration();

		if (!SpaceContext.isTest() //
				&& configuration.awsRegion().isPresent() //
				&& !configuration.isOffline()) {

			FileService.get().deleteAll();
			ShareService.get().deleteAll();
		}
	}

	//
	// Singleton
	//

	private static AdminService singleton = new AdminService();

	static AdminService get() {
		return singleton;
	}

	private AdminService() {
	}

}