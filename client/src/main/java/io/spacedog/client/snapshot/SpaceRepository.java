package io.spacedog.client.snapshot;

import java.util.Map;
import java.util.Objects;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SpaceRepository implements Comparable<SpaceRepository> {

	// using xxxx for week year and ww for week number
	// yyyy doesn't work well between weeks 52 and 01
	private static final DateTimeFormatter REPO_ID_FORMATTER = DateTimeFormat//
			.forPattern("xxxx-ww").withZone(DateTimeZone.UTC);

	public static final String TYPE_S3 = "s3";
	public static final String TYPE_FS = "fs";

	private String id;
	private String type;
	private Map<String, Object> settings;

	public String id() {
		return id;
	}

	public SpaceRepository withId(String id) {
		this.id = id;
		return this;
	}

	public String type() {
		return type;
	}

	public SpaceRepository withType(String type) {
		this.type = type;
		return this;
	}

	public Map<String, Object> settings() {
		return settings;
	}

	public SpaceRepository withSettings(Map<String, Object> settings) {
		this.settings = settings;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof SpaceRepository))
			return false;

		SpaceRepository repo = (SpaceRepository) obj;
		return Objects.equals(id, repo.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return String.format("SpaceRepository[%s]", id);
	}

	@Override
	// ordered from latest to oldest repository
	public int compareTo(SpaceRepository other) {
		return other.id.compareTo(id);
	}

	public static String currentRepositoryId() {
		return toRepositoryId(DateTime.now());
	}

	public static String toRepositoryId(DateTime date) {
		return REPO_ID_FORMATTER.print(date);
	}

	public static DateTime toDateTime(String repositoryId) {
		return REPO_ID_FORMATTER.parseDateTime(repositoryId);
	}

	public static String pastRepositoryId(String repositoryId, int weeks) {
		return toRepositoryId(toDateTime(repositoryId).minusWeeks(weeks));
	}
}