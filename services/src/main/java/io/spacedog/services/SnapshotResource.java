/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.RestoreInfo;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1")
public class SnapshotResource extends Resource {

	private static final String PLATFORM_SNAPSHOT_PREFIX = "all";
	private static final String WAIT_FOR_COMPLETION = "waitForCompletion";
	private static final String BUCKET_SUFFIX = "snapshots";

	//
	// routes
	//

	@Get("/snapshot")
	@Get("/snapshot/")
	public Payload getSnapshotAll() {

		SpaceContext.checkAdminCredentials(false);
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();

		JsonBuilder<ObjectNode> payload = JsonPayload.minimalBuilder(200)//
				.put("total", snapshots.size())//
				.array("results");

		for (SpaceSnapshot snapshot : snapshots)
			snapshot.addTo(payload);

		return JsonPayload.json(payload);
	}

	@Get("/snapshot/latest")
	@Get("/snapshot/latest/")
	public Payload getSnapshotLatest() {

		SpaceContext.checkAdminCredentials(false);
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		return snapshots.isEmpty() ? JsonPayload.error(404)//
				: JsonPayload.json(snapshots.get(0).toJson());
	}

	@Get("/snapshot/:id")
	@Get("/snapshot/:id/")
	public Payload getSnapshotById(String snapshotId) {

		SpaceContext.checkAdminCredentials(false);
		SpaceSnapshot snapshot = doGetSnapshot(snapshotId);
		return JsonPayload.json(snapshot.toJson());
	}

	@Post("/snapshot")
	@Post("/snapshot/")
	public Payload postSnapshot(Context context) {

		SpaceContext.checkSuperDogCredentials(false);

		String snapshotId = computeSnapshotName(PLATFORM_SNAPSHOT_PREFIX);
		String repoId = getCurrentSnapshotRepository();

		// TODO rename correctly the snapshot repository
		CreateSnapshotResponse response = Start.get().getElasticClient().cluster()//
				.prepareCreateSnapshot(repoId, snapshotId)//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, true))//
				.setWaitForCompletion(context.query().getBoolean(WAIT_FOR_COMPLETION, false))//
				.setIncludeGlobalState(true)//
				.setPartial(false)//
				.get();

		int status = response.status().getStatus();

		// fix elastic small incoherence in snapshot creation status
		if (status == 200)
			status = 201;

		JsonBuilder<ObjectNode> jsonResponse = JsonPayload.minimalBuilder(status)//
				.put("id", snapshotId)//
				.put("location", spaceUrl(Backends.ROOT_API, "/1", "snapshot", snapshotId).toString());

		if (response.getSnapshotInfo() != null) {
			SpaceSnapshot info = new SpaceSnapshot(repoId, response.getSnapshotInfo());
			info.addTo(jsonResponse, "snapshot");
		}

		return JsonPayload.json(jsonResponse, status);
	}

	@Post("/snapshot/latest/restore")
	@Post("/snapshot/latest/restore/")
	public Payload postSnapshotLatestRestore(Context context) {

		SpaceContext.checkSuperDogCredentials(false);

		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		if (Utils.isNullOrEmpty(snapshots))
			throw new IllegalArgumentException("snapshot repository doesn't contain any snapshot");

		return doRestore(snapshots.get(0), //
				context.query().getBoolean(WAIT_FOR_COMPLETION, false));
	}

	@Post("/snapshot/:id/restore")
	@Post("/snapshot/:id/restore/")
	public Payload postSnapshotRestoreById(String snapshotId, Context context) {

		SpaceContext.checkSuperDogCredentials(false);
		SpaceSnapshot snapshot = doGetSnapshot(snapshotId);
		return doRestore(snapshot, //
				context.query().getBoolean(WAIT_FOR_COMPLETION, false));
	}

	//
	// implementation
	//

	private String computeSnapshotName(String prefix) {
		return prefix + "-utc-" + DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-MM-dd-HH-mm-ss-SSS");
	}

	private SpaceSnapshot doGetSnapshot(String snapshotId) {
		Optional<SpaceSnapshot> optional = getAllPlatformSnapshotsFromLatestToOldest().stream()//
				.filter(snapshot -> snapshot.id().equals(snapshotId))//
				.findAny();

		if (!optional.isPresent())
			throw NotFoundException.snapshot(snapshotId);

		return optional.get();
	}

	private Payload doRestore(SpaceSnapshot snapshot, boolean waitForCompletion) {

		if (!snapshot.info.state().completed())
			throw new IllegalArgumentException(String.format(//
					"snapshot [%s] is not yet completed", snapshot.id()));

		if (!snapshot.info.state().restorable())
			throw new IllegalArgumentException(String.format(//
					"snapshot [%s] is not restorable, state is [%s]", //
					snapshot.id(), snapshot.info.state().toString()));

		// close all backend indices before restore
		Start.get().getElasticClient().deleteAllBackendIndices();

		RestoreInfo restore = Start.get().getElasticClient().cluster()//
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

	private List<SpaceSnapshot> getAllPlatformSnapshotsFromLatestToOldest() {

		return Lists.reverse(//
				Start.get().getElasticClient().cluster()//
						.prepareGetRepositories()//
						.get()//
						.repositories()//
						.stream()//
						.flatMap(repo -> Start.get().getElasticClient().cluster()//
								.prepareGetSnapshots(repo.name())//
								.get()//
								.getSnapshots()//
								.stream()//
								.filter(info -> info.name().startsWith(PLATFORM_SNAPSHOT_PREFIX))//
								.map(info -> new SpaceSnapshot(repo.name(), info)))//
						.sorted()//
						.collect(Collectors.toList()));
	}

	private static class SpaceSnapshot implements Comparable<SpaceSnapshot> {

		private SnapshotInfo info;
		private String repoId;

		public SpaceSnapshot(String repositoryId, SnapshotInfo info) {
			this.repoId = repositoryId;
			this.info = info;
		}

		public String backend() {
			return id().substring(0, id().indexOf('-'));
		}

		public String id() {
			return info.name();
		}

		public String repositoryId() {
			return this.repoId;
		}

		@Override
		public int compareTo(SpaceSnapshot o) {
			return Long.valueOf(this.info.startTime()).compareTo(o.info.startTime());
		}

		public void addTo(JsonBuilder<ObjectNode> builder, String fieldname) {
			builder.object(fieldname);
			putTo(builder);
			builder.end();
		}

		public void addTo(JsonBuilder<ObjectNode> builder) {
			builder.object();
			putTo(builder);
			builder.end();
		}

		public ObjectNode toJson() {
			JsonBuilder<ObjectNode> builder = Json.objectBuilder();
			putTo(builder);
			return builder.build();
		}

		public void putTo(JsonBuilder<? extends JsonNode> builder) {
			builder.put("id", id())//
					.put("repository", repoId)//
					.put("state", info.state().toString())//
					.put("type", backend())//
					.put("startTime", info.startTime())//
					.put("endTime", info.endTime());
		}
	}

	private String getCurrentSnapshotRepository() {

		String currentRepositoryId = DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");

		try {
			Start.get().getElasticClient().cluster()//
					.prepareGetRepositories(currentRepositoryId)//
					.get();

		} catch (RepositoryMissingException e) {

			SpaceRepository repo = new SpaceRepository(currentRepositoryId);

			PutRepositoryResponse putRepositoryResponse = Start.get().getElasticClient().cluster()//
					.preparePutRepository(repo.getId())//
					.setType(repo.getType())//
					.setSettings(repo.getSettings())//
					.get();

			if (!putRepositoryResponse.isAcknowledged())
				throw new RuntimeException(//
						"failed to create current snapshot repository: no details available");
		}

		return currentRepositoryId;
	}

	private static class SpaceRepository {

		private String type;
		private Settings settings;
		private String id;

		private SpaceRepository(String id) {
			this.id = id;
			StartConfiguration conf = Start.get().configuration();

			if (conf.snapshotsPath().isPresent()) {

				Path location = conf.snapshotsPath().get().resolve(id);

				try {
					Files.createDirectories(location);
				} catch (IOException e) {
					throw new RuntimeException(String.format(//
							"failed to create snapshot repository [%s] type [fs] at location [%s]", //
							id, location));
				}

				this.type = "fs";
				this.settings = Settings.builder()//
						.put("location", location.toAbsolutePath().toString())//
						.put("compress", true)//
						.build();
			} else {

				this.type = "s3";
				this.settings = Settings.builder()//
						.put("bucket", getBucketName(BUCKET_SUFFIX))//
						.put("region", conf.awsRegion())//
						.put("base_path", this.id)//
						.put("compress", true)//
						.build();
			}
		}

		public String getId() {
			return this.id;
		}

		public Settings getSettings() {
			return this.settings;
		}

		public String getType() {
			return this.type;
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
