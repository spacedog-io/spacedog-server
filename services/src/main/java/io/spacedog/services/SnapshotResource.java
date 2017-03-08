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
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
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
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
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

	private static final String PLATFORM_SNAPSHOT_PREFIX = "all";
	private static final String BUCKET_SUFFIX = "snapshots";

	//
	// routes
	//

	@Get("")
	@Get("/")
	public Payload getSnapshotAll() {

		checkSnapshotAllOrAdmin();
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();

		JsonBuilder<ObjectNode> payload = JsonPayload.builder()//
				.put("total", snapshots.size())//
				.array("results");

		for (SpaceSnapshot snapshot : snapshots)
			snapshot.addTo(payload);

		return JsonPayload.json(payload);
	}

	@Get("/latest")
	@Get("/latest/")
	public Payload getSnapshotLatest() {

		checkSnapshotAllOrAdmin();
		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		return snapshots.isEmpty() ? JsonPayload.error(404)//
				: JsonPayload.json(snapshots.get(0).toJson());
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload getSnapshotById(String snapshotId) {

		checkSnapshotAllOrAdmin();
		SpaceSnapshot snapshot = doGetSnapshot(snapshotId);
		return JsonPayload.json(snapshot.toJson());
	}

	@Post("")
	@Post("/")
	public Payload postSnapshot(Context context) {

		checkSnapshotAllOrSuperdog();

		String snapshotId = computeSnapshotName(PLATFORM_SNAPSHOT_PREFIX);
		String repoId = checkCurrentRepository();

		// TODO rename correctly the snapshot repository
		CreateSnapshotResponse response = Start.get().getElasticClient().cluster()//
				.prepareCreateSnapshot(repoId, snapshotId)//
				.setIndicesOptions(IndicesOptions.fromOptions(true, true, true, true))//
				.setWaitForCompletion(context.query().getBoolean(PARAM_WAIT_FOR_COMPLETION, false))//
				.setIncludeGlobalState(true)//
				.setPartial(false)//
				.get();

		int status = response.status().getStatus();

		// fix elastic small incoherence in snapshot creation status
		if (status == 200)
			status = 201;

		JsonBuilder<ObjectNode> jsonResponse = JsonPayload.builder(status)//
				.put("id", snapshotId)//
				.put("location", spaceUrl(Backends.rootApi(), "/1", "snapshot", snapshotId).toString());

		if (response.getSnapshotInfo() != null) {
			SpaceSnapshot info = new SpaceSnapshot(repoId, response.getSnapshotInfo());
			info.addTo(jsonResponse, "snapshot");
		}

		return JsonPayload.json(jsonResponse, status);
	}

	@Post("/latest/restore")
	@Post("/latest/restore/")
	public Payload postSnapshotLatestRestore(Context context) {

		SpaceContext.checkSuperDogCredentials();

		List<SpaceSnapshot> snapshots = getAllPlatformSnapshotsFromLatestToOldest();
		if (Utils.isNullOrEmpty(snapshots))
			throw Exceptions.illegalArgument("snapshot repository doesn't contain any snapshot");

		return doRestore(snapshots.get(0), //
				context.query().getBoolean(PARAM_WAIT_FOR_COMPLETION, false));
	}

	@Post("/:id/restore")
	@Post("/:id/restore/")
	public Payload postSnapshotRestoreById(String snapshotId, Context context) {

		SpaceContext.checkSuperDogCredentials();
		SpaceSnapshot snapshot = doGetSnapshot(snapshotId);
		return doRestore(snapshot, //
				context.query().getBoolean(PARAM_WAIT_FOR_COMPLETION, false));
	}

	//
	// implementation
	//

	private void checkSnapshotAllOrAdmin() {
		Credentials credentials = SpaceContext.getCredentials();

		if (credentials.isAtLeastAdmin() || isSnapshotAll(credentials))
			return;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private void checkSnapshotAllOrSuperdog() {
		Credentials credentials = SpaceContext.getCredentials();

		if (credentials.isSuperDog() || isSnapshotAll(credentials))
			return;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private boolean isSnapshotAll(Credentials credentials) {
		return credentials.isTargetingRootApi() //
				&& credentials.roles().contains(SNAPSHOT_ALL);
	}

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
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not yet completed", snapshot.id());

		if (!snapshot.info.state().restorable())
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not restorable, state is [%s]", //
					snapshot.id(), snapshot.info.state().toString());

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

	private String checkCurrentRepository() {

		String currentRepositoryId = SpaceRepository.getCurrentRepositoryId();

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
				throw Exceptions.runtime(//
						"failed to create current snapshot repository: no details available");
		}

		return currentRepositoryId;
	}

	public void deleteMissingRepositories() {

		Utils.info("[SpaceDog] deleting missing repositories ...");

		List<RepositoryMetaData> repositories = Start.get().getElasticClient().cluster()//
				.prepareGetRepositories().get().repositories();

		for (RepositoryMetaData repository : repositories) {
			try {
				Start.get().getElasticClient().cluster()//
						.prepareVerifyRepository(repository.name()).get();

				Utils.info("[SpaceDog] repository [%s] OK", repository.name());

			} catch (RepositoryMissingException e) {

				Start.get().getElasticClient().cluster()//
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

		String currentRepositoryId = SpaceRepository.getCurrentRepositoryId();
		String obsoleteRepositoryId = SpaceRepository.getPreviousRepositoryId(currentRepositoryId, 4);

		while (true) {
			try {

				Start.get().getElasticClient().cluster()//
						.prepareDeleteRepository(obsoleteRepositoryId)//
						.get();

				Utils.info("[SpaceDog] repository [%s] deleted because obsolete", obsoleteRepositoryId);

				obsoleteRepositoryId = SpaceRepository.getPreviousRepositoryId(//
						obsoleteRepositoryId, 1);

			} catch (RepositoryMissingException e) {
				break;
			}
		}
	}

	static class SpaceRepository {

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
					throw Exceptions.runtime(//
							"failed to create snapshot repository [%s] type [fs] at location [%s]", //
							id, location);
				}

				this.type = "fs";
				this.settings = Settings.builder()//
						.put("location", location.toAbsolutePath().toString())//
						.put("compress", true)//
						.build();
			} else {

				this.type = "s3";
				String awsRegion = conf.awsRegion().orElseThrow(//
						() -> Exceptions.runtime("no AWS region configuration"));

				this.settings = Settings.builder()//
						.put("bucket", getBucketName(BUCKET_SUFFIX))//
						.put("region", awsRegion)//
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

		public static String getCurrentRepositoryId() {
			return DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");
		}

		public static String getPreviousRepositoryId(String repositoryId, int i) {
			int yyyy = Integer.parseInt(repositoryId.substring(0, 4));
			int ww = Integer.parseInt(repositoryId.substring(5));
			while (i > 0) {
				i = i - 1;
				ww = ww - 1;
				if (ww == 0) {
					ww = 52;
					yyyy = yyyy - 1;
				}
			}
			return toRepositoryId(yyyy, ww);
		}

		public static String toRepositoryId(int yyyy, int ww) {
			return String.valueOf(yyyy) + '-' + (ww < 10 ? "0" : "") + String.valueOf(ww);
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
