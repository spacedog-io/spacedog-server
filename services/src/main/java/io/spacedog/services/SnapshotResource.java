/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.RestoreInfo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/snapshot")
public class SnapshotResource extends Resource {

	public static final String SNAPSHOT_ALL = "snapshotall";

	//
	// routes
	//

	@Get("")
	@Get("/")
	public Payload getSnapshotAll(Context context) {

		checkSnapshotAllOrAdmin();
		int from = context.query().getInteger(PARAM_FROM, 0);
		int size = context.query().getInteger(PARAM_SIZE, 10);
		List<ElasticSnapshot> snapshots = ElasticSnapshot.latests(from, size);

		JsonBuilder<ObjectNode> payload = JsonPayload.builder()//
				.put("total", snapshots.size())//
				.array("results");

		for (ElasticSnapshot snapshot : snapshots)
			payload.node(snapshot.toJson());

		return JsonPayload.json(payload);
	}

	@Get("/latest")
	@Get("/latest/")
	public Payload getSnapshotLatest() {

		checkSnapshotAllOrAdmin();
		List<ElasticSnapshot> snapshots = ElasticSnapshot.latests(0, 1);
		return snapshots.isEmpty() //
				? JsonPayload.error(404)//
				: JsonPayload.json(snapshots.get(0).toJson());
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload getSnapshotById(String snapshotId) {

		checkSnapshotAllOrAdmin();
		Optional<ElasticSnapshot> snapshot = ElasticSnapshot.find(snapshotId);
		return snapshot.isPresent() //
				? JsonPayload.json(snapshot.get().toJson()) //
				: JsonPayload.error(404);
	}

	@Post("")
	@Post("/")
	public Payload postSnapshot(Context context) {

		checkSnapshotAllOrSuperdog();
		ElasticSnapshot snapshot = ElasticSnapshot.prepareSnapshot();

		// TODO rename correctly the snapshot repository
		CreateSnapshotResponse response = elastic().cluster()//
				.prepareCreateSnapshot(snapshot.repositoryId(), snapshot.id())//
				.setIndicesOptions(IndicesOptions.fromOptions(true, true, true, true))//
				.setWaitForCompletion(context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false))//
				.setIncludeGlobalState(true)//
				.setPartial(false)//
				.get();

		int status = response.status().getStatus();

		// fix elastic small incoherence in snapshot creation status
		if (status == 200)
			status = 201;

		JsonBuilder<ObjectNode> payload = JsonPayload.builder(status)//
				.put("id", snapshot.id())//
				.put("location", spaceUrl("/1", "snapshot", snapshot.id()).toString());

		if (response.getSnapshotInfo() != null) {
			snapshot.info(response.getSnapshotInfo());
			payload.node("snapshot", snapshot.toJson());
		}

		return JsonPayload.json(payload, status);
	}

	@Post("/latest/restore")
	@Post("/latest/restore/")
	public Payload postSnapshotLatestRestore(Context context) {

		SpaceContext.credentials().checkSuperDog();

		List<ElasticSnapshot> snapshots = ElasticSnapshot.latests(0, 1);
		if (Utils.isNullOrEmpty(snapshots))
			throw Exceptions.illegalArgument(//
					"snapshot repository doesn't contain any snapshot");

		return doRestore(snapshots.get(0), //
				context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false));
	}

	@Post("/:id/restore")
	@Post("/:id/restore/")
	public Payload postSnapshotRestoreById(String snapshotId, Context context) {

		SpaceContext.credentials().checkSuperDog();
		ElasticSnapshot snapshot = ElasticSnapshot.find(snapshotId)//
				.orElseThrow(() -> NotFoundException.snapshot(snapshotId));
		return doRestore(snapshot, //
				context.query().getBoolean(WAIT_FOR_COMPLETION_PARAM, false));
	}

	//
	// implementation
	//

	private void checkSnapshotAllOrAdmin() {
		Credentials credentials = SpaceContext.credentials();

		if (credentials.isAtLeastAdmin() || isSnapshotAll(credentials))
			return;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private void checkSnapshotAllOrSuperdog() {
		Credentials credentials = SpaceContext.credentials();

		if (credentials.isSuperDog() || isSnapshotAll(credentials))
			return;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private boolean isSnapshotAll(Credentials credentials) {
		return credentials.roles().contains(SNAPSHOT_ALL);
	}

	private Payload doRestore(ElasticSnapshot snapshot, boolean waitForCompletion) {

		if (!snapshot.info().state().completed())
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not yet completed", snapshot.id());

		if (!snapshot.info().state().restorable())
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not restorable, state is [%s]", //
					snapshot.id(), snapshot.info().state().toString());

		// delete all indices of all backends before restore
		// this is prefered to a close operation
		// because it remove indices not present in restored snapshot
		elastic().deleteAbsolutelyAllIndices();

		RestoreInfo restore = elastic().cluster()//
				.prepareRestoreSnapshot(snapshot.repositoryId(), snapshot.id())//
				.setWaitForCompletion(waitForCompletion)//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, true))//
				.setPartial(false)//
				.setIncludeAliases(true)//
				.setRestoreGlobalState(true)//
				.get()//
				.getRestoreInfo();

		if (restore == null)
			return JsonPayload.error(400, //
					"restore of snapshot [%s] failed: retry later", snapshot.id());

		return JsonPayload.json(restore.status().getStatus());
	}

	public void deleteMissingRepositories() {

		Utils.info("[SpaceDog] deleting missing repositories ...");

		List<RepositoryMetaData> repositories = elastic().cluster()//
				.prepareGetRepositories().get().repositories();

		for (RepositoryMetaData repository : repositories) {
			try {
				elastic().cluster()//
						.prepareVerifyRepository(repository.name()).get();

				Utils.info("[SpaceDog] repository [%s] OK", repository.name());

			} catch (RepositoryMissingException e) {

				elastic().cluster()//
						.prepareDeleteRepository(repository.name()).get();

				Utils.info("[SpaceDog] repository [%s] deleted because missing", repository.name());
			}
		}
	}

	/**
	 * TODO does not delete very obsolete repo if one is missing in between
	 */
	public void deleteObsoleteRepositories() {

		Utils.info("[SpaceDog] deleting obsolete repositories ...");

		String currentRepositoryId = ElasticRepository.getCurrentRepositoryId();
		String obsoleteRepositoryId = ElasticRepository.getPreviousRepositoryId(currentRepositoryId, 4);

		while (true) {
			try {

				elastic().cluster()//
						.prepareDeleteRepository(obsoleteRepositoryId)//
						.get();

				Utils.info("[SpaceDog] repository [%s] deleted because obsolete", obsoleteRepositoryId);

				obsoleteRepositoryId = ElasticRepository.getPreviousRepositoryId(//
						obsoleteRepositoryId, 1);

			} catch (RepositoryMissingException e) {
				break;
			}
		}
	}

	//
	// Singleton
	//

	private static SnapshotResource singleton = new SnapshotResource();

	static SnapshotResource get() {
		return singleton;
	}

	private SnapshotResource() {
	}
}
