package io.spacedog.services.snapshot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotState;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.file.FileStoreType;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.snapshot.SpaceRepository;
import io.spacedog.client.snapshot.SpaceSnapshot;
import io.spacedog.jobs.Internals;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.services.SpaceService;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Utils;

public class SnapshotService extends SpaceService implements SpaceFields, SpaceParams {

	public List<SpaceSnapshot> getLatest(int from, int size) {

		String backendId = Server.backend().id();
		List<SpaceRepository> repositories = getRepositories();
		List<SpaceSnapshot> snapshots = Lists.newArrayList();

		for (SpaceRepository repo : repositories) {
			Server.get().elasticClient().cluster()//
					.prepareGetSnapshots(repo.id())//
					.get()//
					.getSnapshots()//
					.stream()//
					.filter(info -> info.snapshotId().getName().startsWith(backendId))//
					.map(info -> toSpaceSnapshot(repo.id(), info))//
					.collect(() -> snapshots, List::add, List::addAll);

			if (snapshots.size() >= from + size)
				break;
		}

		return snapshots.stream().sorted().skip(from).limit(size)//
				.collect(Collectors.toList());
	}

	public Optional<SpaceSnapshot> get(String snapshotId) {
		String repositoryId = toRepositoryId(snapshotId);
		return Server.get().elasticClient().cluster()//
				.prepareGetSnapshots(repositoryId)//
				.get().getSnapshots().stream()//
				.filter(info -> info.snapshotId().getName().equals(snapshotId))//
				.findAny().map(info -> toSpaceSnapshot(repositoryId, info));
	}

	public SpaceSnapshot snapshot(boolean waitForCompletion) {
		SpaceSnapshot snapshot = prepareSnapshot();

		// TODO rename correctly the snapshot repository
		CreateSnapshotRequestBuilder snapshotRequest = elastic().cluster()//
				.prepareCreateSnapshot(snapshot.repositoryId, snapshot.id)//
				.setIndicesOptions(IndicesOptions.fromOptions(true, true, true, true))//
				.setWaitForCompletion(true)//
				.setIncludeGlobalState(true)//
				.setPartial(false);

		SnapshotListener listener = new SnapshotListener();

		if (waitForCompletion) {
			CreateSnapshotResponse response = snapshotRequest.get();
			listener.onResponse(response);
			return toSpaceSnapshot(snapshot.repositoryId, response.getSnapshotInfo());
		} else {
			snapshotRequest.execute(listener);
			return snapshot;
		}
	}

	public void restore(String snapshotId, boolean waitForCompletion) {
		SpaceSnapshot snapshot = get(snapshotId).orElseThrow(//
				() -> Exceptions.notFound("snapshot [%s] not found", snapshotId));

		if (!snapshot.completed)
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not yet completed", snapshot.id);

		if (!snapshot.restorable)
			throw Exceptions.illegalArgument(//
					"snapshot [%s] is not restorable, state is [%s]", //
					snapshot.id, snapshot.state);

		// delete all indices of all backends before restore
		// this is prefered to a close operation
		// because it remove indices not present in restored snapshot
		elastic().deleteAbsolutelyAllIndices();

		RestoreSnapshotRequestBuilder restoreRequest = elastic().cluster()//
				.prepareRestoreSnapshot(snapshot.repositoryId, snapshot.id)//
				.setWaitForCompletion(true)//
				.setIndicesOptions(IndicesOptions.fromOptions(false, true, true, true))//
				.setPartial(false)//
				.setIncludeAliases(true)//
				.setRestoreGlobalState(true);

		RestoreListener listener = new RestoreListener();

		if (waitForCompletion)
			listener.onResponse(restoreRequest.get());
		else
			restoreRequest.execute(listener);
	}

	public List<SpaceRepository> getRepositories() {
		return elastic().cluster().prepareGetRepositories().get()//
				.repositories()//
				.stream()//
				.map(repo -> toSpaceRepository(repo))//
				.sorted().collect(Collectors.toList());
	}

	public boolean openRepository(String repositoryId) {

		// check repository id format
		SpaceRepository.toDateTime(repositoryId);

		try {
			SnapshotResty.elastic().cluster()//
					.prepareGetRepositories(repositoryId)//
					.get();

			return false;

		} catch (RepositoryMissingException e) {

			RepositoryMetaData repo = newRepositoryMetadata(repositoryId);

			SnapshotResty.elastic().cluster()//
					.preparePutRepository(repo.name())//
					.setType(repo.type())//
					.setSettings(repo.settings())//
					.get();

			return true;
		}
	}

	public boolean closeRepository(String repositoryId) {

		// check repository id format
		SpaceRepository.toDateTime(repositoryId);

		try {
			elastic().cluster().prepareDeleteRepository(repositoryId).get();
			return true;

		} catch (RepositoryMissingException e) {
			return false;
		}
	}

	public void closeMissingRepositories() {

		Utils.info("[SpaceDog] close missing repositories ...");

		List<RepositoryMetaData> repositories = elastic().cluster()//
				.prepareGetRepositories().get().repositories();

		for (RepositoryMetaData repository : repositories) {
			try {
				elastic().cluster()//
						.prepareVerifyRepository(repository.name()).get();

				Utils.info("[SpaceDog] repository [%s] OK", repository.name());

			} catch (RepositoryMissingException e) {

				closeRepository(repository.name());
				Utils.info("[SpaceDog] repository [%s] closed because missing", repository.name());
			}
		}
	}

	/**
	 * TODO does not delete very obsolete repo if one is missing in between
	 */
	public void closeObsoleteRepositories() {

		Utils.info("[SpaceDog] close obsolete repositories ...");
		String currentRepoId = SpaceRepository.currentRepositoryId();
		String obsoleteRepoId = SpaceRepository.pastRepositoryId(currentRepoId, 4);

		while (closeRepository(obsoleteRepoId)) {
			Utils.info("[SpaceDog] repository [%s] closed because obsolete", obsoleteRepoId);
			obsoleteRepoId = SpaceRepository.pastRepositoryId(obsoleteRepoId, 1);
		}
	}

	//
	// Implementation for files
	//

	private class SnapshotListener implements ActionListener<CreateSnapshotResponse> {
		@Override
		public void onResponse(CreateSnapshotResponse response) {
			// FileBackup fileBackup = new FileBackup(//
			// Server.backend().id(), "backup", filesBackupStore());
			// Services.files().snapshot(fileBackup);
		}

		@Override
		public void onFailure(Exception e) {
			notifyFailure(e, "snapshot");
		}
	}

	private class RestoreListener implements ActionListener<RestoreSnapshotResponse> {
		@Override
		public void onResponse(RestoreSnapshotResponse response) {
			// FileBackup fileBackup = new FileBackup(//
			// Server.backend().id(), "backup", filesBackupStore());
			// Services.files().restore(fileBackup);
		}

		@Override
		public void onFailure(Exception e) {
			notifyFailure(e, "restore");
		}
	}

	private void notifyFailure(Throwable t, String type) {
		t.printStackTrace();
		String title = String.format("backend [%s] %s error", Server.backend(), type);
		Internals.get().notify(title, t);
	}

	// private FileStore filesBackupStore() {
	// FileStoreType storeType = ServerConfig.filesSnapshotsStoreType();
	//
	// if (FileStoreType.S3.equals(storeType))
	// return new S3FileStore(ServerConfig.awsBucketPrefix() +
	// ServerConfig.filesSnapshotsS3Suffix());
	//
	// if (FileStoreType.FS.equals(storeType))
	// return new SystemFileStore(ServerConfig.filesSnapshotsFSPath());
	//
	// throw Exceptions.runtime("file bucket type [%s] is invalid", storeType);
	// }

	//
	// Implementation for data
	//

	private final static String PREFIX = "-utc-";
	private static final DateTimeFormatter SNAPSHOT_ID_FORMATTER = DateTimeFormat//
			.forPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(DateTimeZone.UTC);

	private SpaceRepository toSpaceRepository(RepositoryMetaData repo) {
		Map<String, Object> settings = Maps.newHashMap();
		for (String key : repo.settings().keySet())
			settings.put(key, repo.settings().get(key));

		return new SpaceRepository()//
				.withId(repo.name())//
				.withType(repo.type())//
				.withSettings(settings);
	}

	private static SpaceSnapshot toSpaceSnapshot(String repositoryId, SnapshotInfo info) {
		SpaceSnapshot snapshot = new SpaceSnapshot();
		snapshot.id = info.snapshotId().getName();
		snapshot.repositoryId = repositoryId;
		snapshot.backendId = snapshot.id.substring(0, snapshot.id.indexOf('-'));
		snapshot.state = info.state().toString();
		snapshot.restorable = info.state().restorable();
		snapshot.completed = info.state().completed();
		snapshot.startTime = new DateTime(info.startTime());
		snapshot.endTime = new DateTime(info.endTime());
		snapshot.indices = Sets.newHashSet(info.indices());
		return snapshot;
	}

	private SpaceSnapshot prepareSnapshot() {
		DateTime now = DateTime.now().withZone(DateTimeZone.UTC);
		String repositoryId = SpaceRepository.toRepositoryId(now);
		openRepository(repositoryId);

		SpaceSnapshot snapshot = new SpaceSnapshot();
		snapshot.backendId = Server.backend().id();
		snapshot.id = snapshot.backendId + PREFIX + SNAPSHOT_ID_FORMATTER.print(now);
		snapshot.repositoryId = repositoryId;
		snapshot.state = SnapshotState.IN_PROGRESS.toString();
		snapshot.restorable = false;
		snapshot.completed = false;
		return snapshot;
	}

	private static String toRepositoryId(String snapshotId) {
		String timestamp = Utils.trimUntil(snapshotId, PREFIX);
		DateTime dateTime = SNAPSHOT_ID_FORMATTER.parseDateTime(timestamp);
		return SpaceRepository.toRepositoryId(dateTime);
	}

	private static RepositoryMetaData newRepositoryMetadata(String repositoryId) {

		FileStoreType type = ServerConfig.snapshotsElasticStoreType();
		String location = Server.backend().id() + "/elastic/" + repositoryId;
		Settings settings = null;

		if (type.equals(FileStoreType.fs))
			settings = Settings.builder()//
					.put("location", location)//
					.put("compress", true)//
					.build();

		else if (type.equals(FileStoreType.s3))
			settings = Settings.builder()//
					.put("bucket", ServerConfig.snapshotsS3Bucket())//
					.put("region", ServerConfig.awsRegion().getName())//
					.put("base_path", location)//
					.put("compress", true)//
					.build();

		if (settings == null)
			throw Exceptions.illegalArgument("snapshot repository type [%s] is invalid", type);

		return new RepositoryMetaData(repositoryId, type.toElasticRepoType(), settings);
	}

}
