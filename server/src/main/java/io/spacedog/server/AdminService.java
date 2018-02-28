/**
 * © David Attias 2015
 */
package io.spacedog.server;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.WebPath;
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
		return JsonPayload.ok().withContent(payload).build();
	}

	@Get("/1/admin/return500")
	@Get("/1/admin/return500")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	@Post("/1/admin/clear")
	@Post("/1/admin/clear")
	public void clear() {
		elastic().deleteBackendIndices();
		deleteAllFiles();
		initBackendIndices();
	}

	//
	// Public interface
	//

	// TODO
	// Move this somewhere else. ElasticClient ?
	public void initBackendIndices() {
		CredentialsService.get().initIndex();
		LogService.get().initIndex();
	}

	public void deleteAllFiles() {
		ServerConfiguration configuration = Server.get().configuration();

		if (!SpaceContext.isTest() //
				&& configuration.awsRegion().isPresent() //
				&& !configuration.isOffline()) {

			S3Service.get().doDeleteAll(new S3File(WebPath.ROOT));
		}
	}

	//
	// Singleton
	//

	private static AdminService singleton = new AdminService();

	public static AdminService get() {
		return singleton;
	}

	private AdminService() {
	}

}