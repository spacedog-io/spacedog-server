package io.spacedog.server;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.snapshots.SnapshotInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

class ElasticSnapshot implements Comparable<ElasticSnapshot> {

	private static final String PREFIX = "all-utc-";
	private static final DateTimeFormatter snapshotIdFormatter = DateTimeFormat//
			.forPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(DateTimeZone.UTC);

	private SnapshotInfo info;
	private String repositoryId;
	private String id;

	private ElasticSnapshot() {
	}

	private ElasticSnapshot(String repositoryId, SnapshotInfo info) {
		this.repositoryId = repositoryId;
		this.info = info;
	}

	String backend() {
		return id().substring(0, id().indexOf('-'));
	}

	String id() {
		return info == null ? id : info.name();
	}

	String repositoryId() {
		return this.repositoryId;
	}

	SnapshotInfo info() {
		return info;
	}

	void info(SnapshotInfo info) {
		if (this.info != null)
			throw Exceptions.runtime("snapshot [%s] info already set", id());
		this.info = info;
	}

	ObjectNode toJson() {
		return Json.object("id", id(), "repository", repositoryId, //
				"state", info.state().toString(), "type", backend(), //
				"startTime", info.startTime(), "endTime", info.endTime());
	}

	@Override
	// ordered from latest to oldest snapshots
	public int compareTo(ElasticSnapshot o) {
		return Long.valueOf(o.info.startTime()).compareTo(this.info.startTime());
	}

	@Override
	public String toString() {
		return id();
	}

	//
	// Factory methods
	//

	static ElasticSnapshot prepareSnapshot() {

		DateTime now = DateTime.now().withZone(DateTimeZone.UTC);
		ElasticSnapshot snapshot = new ElasticSnapshot();
		snapshot.id = PREFIX + snapshotIdFormatter.print(now);
		snapshot.repositoryId = ElasticRepository.getOrCreateRepositoryFor(now);
		return snapshot;
	}

	static Optional<ElasticSnapshot> find(String snapshotId) {

		return Start.get().getElasticClient().cluster()//
				.prepareGetSnapshots(toRepositoryId(snapshotId))//
				.get().getSnapshots()//
				.stream()//
				.filter(info -> info.name().equals(snapshotId))//
				.findAny()//
				.map(info -> new ElasticSnapshot(toRepositoryId(snapshotId), info));
	}

	static List<ElasticSnapshot> latests(int from, int size) {

		List<ElasticRepository> repositories = ElasticRepository.getAll();
		List<ElasticSnapshot> snapshots = Lists.newArrayList();

		for (ElasticRepository repo : repositories) {
			Start.get().getElasticClient().cluster()//
					.prepareGetSnapshots(repo.id())//
					.get()//
					.getSnapshots()//
					.stream()//
					.filter(info -> info.name().startsWith(PREFIX))//
					.map(info -> new ElasticSnapshot(repo.id(), info))//
					.collect(() -> snapshots, List::add, List::addAll);

			if (snapshots.size() >= from + size)
				break;
		}

		return snapshots.stream().sorted().skip(from).limit(size)//
				.collect(Collectors.toList());
	}

	private static String toRepositoryId(String snapshotId) {
		snapshotId = Utils.removePreffix(snapshotId, PREFIX);
		DateTime dateTime = snapshotIdFormatter.parseDateTime(snapshotId);
		return ElasticRepository.toRepositoryId(dateTime);
	}

}