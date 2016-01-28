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
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.RestoreInfo;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.services.Start.Configuration;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class SnapshotResource extends AbstractResource {

	private static final String PLATFORM_SNAPSHOT_PREFIX = "all";
	private static final String ELASTIC_ALL = "_all";
	private static final String WAIT_FOR_COMPLETION = "waitForCompletion";

	//
	// routes
	//

	@Get("/snapshot")
	@Get("/snapshot/")
	@Get("/dog/snapshot")
	@Get("/dog/snapshot/")
	public Payload getSnapshotAll() throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkAdminCredentials();
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();

		JsonBuilder<ObjectNode> payload = Payloads.minimalBuilder(200)//
				.put("total", snapshots.size())//
				.array("results");

		for (SpaceSnapshot snapshot : snapshots)
			snapshot.addTo(payload);

		return Payloads.json(payload);
	}

	@Get("/snapshot/latest")
	@Get("/snapshot/latest/")
	@Get("/dog/snapshot/latest")
	@Get("/dog/snapshot/latest/")
	public Payload getSnapshotLatest() throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkAdminCredentials();
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		return snapshots.isEmpty() ? Payloads.error(404)//
				: Payloads.json(snapshots.get(0).toJson());
	}

	@Get("/snapshot/:snapshotId")
	@Get("/snapshot/:snapshotId/")
	@Get("/dog/snapshot/:snapshotId")
	@Get("/dog/snapshot/:snapshotId/")
	public Payload getSnapshotById(String snapshotId) throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkAdminCredentials();
		SpaceSnapshot snapshot = doGetSnapshot(snapshotId);
		return Payloads.json(snapshot.toJson());
	}

	@Post("/snapshot")
	@Post("/snapshot/")
	@Post("/dog/snapshot")
	@Post("/dog/snapshot/")
	public Payload postSnapshot(Context context) throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkSuperDogCredentials();

		String snapshotId = computeSnapshotName(PLATFORM_SNAPSHOT_PREFIX);
		String repoId = getCurrentSnapshotRepository();

		// TODO rename correctly the snapshot repository
		CreateSnapshotResponse response = Start.get().getElasticClient().admin().cluster()//
				.prepareCreateSnapshot(repoId, snapshotId)//
				.setIndicesOptions(IndicesOptions.fromOptions(false, false, true, true))//
				.setWaitForCompletion(context.query().getBoolean(WAIT_FOR_COMPLETION, false))//
				.setIncludeGlobalState(true)//
				.setPartial(true)//
				.get();

		int status = response.status().getStatus();

		// fix elastic small incoherence in snapshot creation status
		if (status == 200)
			status = 201;

		JsonBuilder<ObjectNode> jsonResponse = Payloads.minimalBuilder(status)//
				.put("id", snapshotId)//
				.put("location", spaceUrl("/v1/dog", "snapshot", snapshotId).toString());

		if (response.getSnapshotInfo() != null) {
			SpaceSnapshot info = new SpaceSnapshot(repoId, response.getSnapshotInfo());
			info.addTo(jsonResponse, "snapshot");
		}

		return Payloads.json(jsonResponse, status);
	}

	@Post("/snapshot/latest/restore")
	@Post("/snapshot/latest/restore/")
	@Post("/dog/snapshot/latest/restore")
	@Post("/dog/snapshot/latest/restore/")
	public Payload postSnapshotLatestRestore(Context context)
			throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkSuperDogCredentials();

		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		if (Utils.isNullOrEmpty(snapshots))
			throw new IllegalArgumentException("snapshot repository doesn't contain any snapshot");

		return doRestore(snapshots.get(0), //
				context.query().getBoolean(WAIT_FOR_COMPLETION, false));
	}

	@Post("/snapshot/:snapshotId/restore")
	@Post("/snapshot/:snapshotId/restore/")
	@Post("/dog/snapshot/:snapshotId/restore")
	@Post("/dog/snapshot/:snapshotId/restore/")
	public Payload postSnapshotRestoreById(String snapshotId, Context context)
			throws JsonParseException, JsonMappingException, IOException {

		SpaceContext.checkSuperDogCredentials();
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

		// close all indices before restore
		CloseIndexResponse closeIndexResponse = Start.get().getElasticClient().admin().indices()
				.prepareClose(ELASTIC_ALL)//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, false))//
				.get();

		if (!closeIndexResponse.isAcknowledged())
			throw new RuntimeException("failed to close all indices");

		RestoreInfo restore = Start.get().getElasticClient().admin().cluster()//
				.prepareRestoreSnapshot(snapshot.repositoryId(), snapshot.id())//
				.setWaitForCompletion(waitForCompletion)//
				.setPartial(false)//
				.get()//
				.getRestoreInfo();

		if (restore == null)
			return Payloads.error(400, //
					"restore of snapshot [%s] failed: retry later", snapshot.id());

		return Payloads.json(restore.status().getStatus());
	}

	private List<SpaceSnapshot> getAllPlatformSnapshotsFromLatestToOldest() {

		return Lists.reverse(//
				Start.get().getElasticClient().admin().cluster()//
						.prepareGetRepositories()//
						.get()//
						.repositories()//
						.stream()//
						.flatMap(repo -> Start.get().getElasticClient().admin().cluster()//
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
			Start.get().getElasticClient().admin().cluster()//
					.prepareGetRepositories(currentRepositoryId)//
					.get();

		} catch (RepositoryMissingException e) {

			SpaceRepository repo = new SpaceRepository(currentRepositoryId);

			PutRepositoryResponse putRepositoryResponse = Start.get().getElasticClient().admin().cluster()//
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
			Configuration conf = Start.get().configuration();

			if (conf.getSnapshotsBucketName().isPresent()) {

				this.type = "s3";
				this.settings = ImmutableSettings.builder()//
						.put("bucket", conf.getSnapshotsBucketName().get())//
						.put("region", conf.getSnapshotsBucketRegion().get())//
						.put("base_path", this.id)//
						.put("compress", true)//
						.build();

			} else if (conf.getSnapshotsPath().isPresent()//
					&& Files.isDirectory(conf.getSnapshotsPath().get())) {

				Path location = conf.getSnapshotsPath().get().resolve(id);

				try {
					Files.createDirectories(location);
				} catch (IOException e) {
					throw new RuntimeException(String.format(//
							"failed to create snapshot repository [%s] type [fs] at location [%s]", //
							id, location));
				}

				this.type = "fs";
				this.settings = ImmutableSettings.builder()//
						.put("location", location.toAbsolutePath().toString())//
						.put("compress", true)//
						.build();
			} else
				throw new RuntimeException("no snapshot repository configuration set");
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
