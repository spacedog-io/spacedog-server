/**
 * © David Attias 2015
 */
package io.spacedog.services.snapshot;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.snapshot.SpaceRepository;
import io.spacedog.client.snapshot.SpaceSnapshot;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/2/snapshots")
public class SnapshotResty extends SpaceResty {

	public static final String SNAPSHOTER_ROLE = "snapshoter";

	//
	// routes
	//

	@Get("")
	@Get("/")
	public List<SpaceSnapshot> getAll(Context context) {
		checkSnapshotOrSuperadmin();
		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);
		return Services.snapshots().getLatest(from, size);
	}

	@Post("")
	@Post("/")
	public Payload postSnapshot(Context context) {
		checkSnapshotOrSuperadmin();
		boolean wait = context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false);
		return JsonPayload.status(wait ? 202 : 201)//
				.withContent(Services.snapshots().snapshot(wait))//
				.build();
	}

	@Get("/repositories")
	@Get("/repositories/")
	public List<SpaceRepository> getRepositories(Context context) {
		checkSnapshotOrSuperadmin();
		return Services.snapshots().getRepositories();
	}

	@Delete("/repositories/:id")
	@Delete("/repositories/:id/")
	public Payload deleteRepository(String id, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		boolean found = Services.snapshots().deleteRepository(id);
		return JsonPayload.ok().withFields("found", found).build();
	}

	@Get("/_latest")
	@Get("/_latest/")
	public SpaceSnapshot getLatest() {
		checkSnapshotOrSuperadmin();
		return Services.snapshots().getLatest(0, 1)//
				.stream().findAny()//
				.orElseThrow(() -> Exceptions.notFound("no snapshot found"));
	}

	@Get("/:id")
	@Get("/:id/")
	public SpaceSnapshot getById(String snapshotId) {
		checkSnapshotOrSuperadmin();
		return Services.snapshots().get(snapshotId)//
				.orElseThrow(() -> Exceptions.notFound("snapshot [%s] not found", snapshotId));
	}

	@Post("/_latest/_restore")
	@Post("/_latest/_restore/")
	public ObjectNode postRestoreLatest(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.snapshots().restoreLatest(//
				context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false));
	}

	@Post("/:id/_restore")
	@Post("/:id/_restore/")
	public ObjectNode postRestoreById(String snapshotId, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.snapshots().restore(snapshotId, //
				context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false));
	}

	//
	// implementation
	//

	private void checkSnapshotOrSuperadmin() {
		Credentials credentials = Server.context().credentials();

		if (credentials.isAtLeastSuperAdmin() || isSnapshot(credentials))
			return;

		throw Exceptions.insufficientPermissions(credentials);
	}

	private boolean isSnapshot(Credentials credentials) {
		return credentials.roles().contains(SNAPSHOTER_ROLE);
	}

}
