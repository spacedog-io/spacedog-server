/**
 * © David Attias 2015
 */
package io.spacedog.services.snapshot;

import java.util.List;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.snapshot.SpaceRepository;
import io.spacedog.client.snapshot.SpaceSnapshot;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.services.JsonPayload;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/2/snapshots")
public class SnapshotResty extends SpaceResty {

	public static final String SNAPMAN = "snapman";

	//
	// routes
	//

	@Get("")
	@Get("/")
	public List<SpaceSnapshot> getAll(Context context) {
		checkAtLeastSnapman();
		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);
		return Services.snapshots().getLatest(from, size);
	}

	@Post("")
	@Post("/")
	public Payload postSnapshot(Context context) {
		checkAtLeastSnapman();
		boolean wait = context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false);
		return JsonPayload.status(wait ? 202 : 201)//
				.withContent(Services.snapshots().snapshot(wait))//
				.build();
	}

	@Get("/repositories")
	@Get("/repositories/")
	public List<SpaceRepository> getRepositories(Context context) {
		checkAtLeastSnapman();
		return Services.snapshots().getRepositories();
	}

	@Put("/repositories/:id")
	@Put("/repositories/:id/")
	public Payload putRepository(String id, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		boolean created = Services.snapshots().openRepository(id);
		return JsonPayload.saved(created).build();
	}

	@Delete("/repositories/:id")
	@Delete("/repositories/:id/")
	public Payload deleteRepository(String id, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		boolean found = Services.snapshots().closeRepository(id);
		return JsonPayload.ok().withFields("found", found).build();
	}

	@Get("/_latest")
	@Get("/_latest/")
	public SpaceSnapshot getLatest() {
		checkAtLeastSnapman();
		return Services.snapshots().getLatest(0, 1)//
				.stream().findAny()//
				.orElseThrow(() -> Exceptions.notFound("no snapshot found"));
	}

	@Get("/:id")
	@Get("/:id/")
	public SpaceSnapshot getById(String snapshotId) {
		checkAtLeastSnapman();
		return Services.snapshots().get(snapshotId)//
				.orElseThrow(() -> Exceptions.notFound("snapshot [%s] not found", snapshotId));
	}

	@Post("/_latest/_restore")
	@Post("/_latest/_restore/")
	public Payload postRestoreLatest(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return Services.snapshots().getLatest(0, 1).stream().findAny()//
				.map(snapshot -> postRestoreById(snapshot.id, context))//
				.orElseThrow(() -> Exceptions.notFound("no snapshot found"));
	}

	@Post("/:id/_restore")
	@Post("/:id/_restore/")
	public Payload postRestoreById(String snapshotId, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		boolean wait = context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false);
		Services.snapshots().restore(snapshotId, wait);
		return wait ? Payload.ok() : new Payload(HttpStatus.ACCEPTED);
	}

	//
	// implementation
	//

	private Credentials checkAtLeastSnapman() {
		Credentials credentials = Server.context().credentials();

		if (credentials.isAtLeastSuperAdmin() //
				|| credentials.roles().contains(SNAPMAN))
			return credentials;

		throw Exceptions.insufficientPermissions(credentials);
	}

}
