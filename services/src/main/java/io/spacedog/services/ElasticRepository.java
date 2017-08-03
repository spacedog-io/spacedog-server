package io.spacedog.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.spacedog.utils.Exceptions;

class ElasticRepository implements Comparable<ElasticRepository> {

	private static final String BUCKET_SUFFIX = "snapshots";

	// using xxxx for week year and ww for week number
	// yyyy doesn't work well between weeks 52 and 01
	private static final DateTimeFormatter repositoryIdFormatter = DateTimeFormat//
			.forPattern("xxxx-ww").withZone(DateTimeZone.UTC);

	private RepositoryMetaData repo;

	private ElasticRepository(RepositoryMetaData repo) {
		this.repo = repo;
	}

	private ElasticRepository(String id) {
		ServerConfiguration conf = Start.get().configuration();

		if (conf.elasticSnapshotsPath().isPresent()) {

			Path location = conf.elasticSnapshotsPath().get().resolve(id);

			try {
				Files.createDirectories(location);
			} catch (IOException e) {
				throw Exceptions.runtime(//
						"error creating snapshot repository [%s] of type [fs] at location [%s]", //
						id, location);
			}

			Settings settings = Settings.builder()//
					.put("location", location.toAbsolutePath().toString())//
					.put("compress", true)//
					.build();

			this.repo = new RepositoryMetaData(id, "fs", settings);

		} else {

			Settings settings = Settings.builder()//
					.put("bucket", Resource.getBucketName(BUCKET_SUFFIX))//
					.put("region", conf.awsRegion())//
					.put("base_path", id)//
					.put("compress", true)//
					.build();

			this.repo = new RepositoryMetaData(id, "s3", settings);
		}
	}

	String id() {
		return this.repo.name();
	}

	Settings settings() {
		return this.repo.settings();
	}

	String type() {
		return this.repo.type();
	}

	static String getCurrentRepositoryId() {
		return toRepositoryId(DateTime.now());
	}

	static String toRepositoryId(DateTime date) {
		return repositoryIdFormatter.print(date);
	}

	static DateTime toDateTime(String repositoryId) {
		return repositoryIdFormatter.parseDateTime(repositoryId);
	}

	static String getPreviousRepositoryId(String repositoryId, int weeks) {
		return toRepositoryId(toDateTime(repositoryId).minusWeeks(weeks));
	}

	static List<ElasticRepository> getAll() {

		return SnapshotResource.elastic().cluster()//
				.prepareGetRepositories()//
				.get()//
				.repositories()//
				.stream()//
				.map(repo -> new ElasticRepository(repo))//
				.sorted()//
				.collect(Collectors.toList());
	}

	@Override
	// ordered from latest to oldest repository
	public int compareTo(ElasticRepository other) {
		return other.id().compareTo(id());
	}

	@Override
	public String toString() {
		return id();
	}

	static String getOrCreateRepositoryFor(DateTime date) {

		String repoId = ElasticRepository.toRepositoryId(date);

		try {
			SnapshotResource.elastic().cluster()//
					.prepareGetRepositories(repoId)//
					.get();

		} catch (RepositoryMissingException e) {

			ElasticRepository repo = new ElasticRepository(repoId);

			PutRepositoryResponse putRepositoryResponse = SnapshotResource.elastic().cluster()//
					.preparePutRepository(repo.id())//
					.setType(repo.type())//
					.setSettings(repo.settings())//
					.get();

			if (!putRepositoryResponse.isAcknowledged())
				throw Exceptions.runtime(//
						"error creating snapshot repository with id [%s]: no details available", //
						repoId);
		}

		return repoId;
	}
}