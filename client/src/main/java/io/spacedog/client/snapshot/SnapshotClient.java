package io.spacedog.client.snapshot;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceParams;

public class SnapshotClient implements SpaceParams {

	public static final String SNAPMAN = "snapman";
	private static final String LATEST = "_latest";

	private SpaceDog dog;

	public SnapshotClient(SpaceDog session) {
		this.dog = session;
	}

	public SpaceSnapshot snapshot() {
		return snapshot(null);
	}

	public SpaceSnapshot snapshot(Boolean waitForCompletion) {
		return dog.post("/2/snapshots")//
				.queryParam("waitForCompletion", waitForCompletion)//
				.go(201, 202).asPojo(SpaceSnapshot.class);
	}

	public SpaceSnapshot restore(String snapshotId) {
		return restore(snapshotId, null);
	}

	public SpaceSnapshot restore(String snapshotId, Boolean waitForCompletion) {
		return dog.post("/2/snapshots/{id}/_restore")//
				.routeParam("id", snapshotId)//
				.queryParam("waitForCompletion", waitForCompletion)//
				.go(200, 202)//
				.asPojo(SpaceSnapshot.class);
	}

	public SpaceSnapshot restoreLatest() {
		return restore(LATEST, null);
	}

	public SpaceSnapshot restoreLatest(Boolean waitForCompletion) {
		return restore(LATEST, waitForCompletion);
	}

	public SpaceSnapshot get(String snapshotId) {
		return dog.get("/2/snapshots/{id}").routeParam("id", snapshotId)//
				.go(200).asPojo(SpaceSnapshot.class);
	}

	public SpaceSnapshot getLatest() {
		return get(LATEST);
	}

	public List<SpaceSnapshot> getAll() {
		return getAll(null, null);
	}

	public List<SpaceSnapshot> getAll(Integer from, Integer size) {
		return dog.get("/2/snapshots").from(from).size(size).go(200)//
				.asPojo(TypeFactory.defaultInstance()//
						.constructCollectionLikeType(ArrayList.class, SpaceSnapshot.class));
	}

	public List<SpaceRepository> getRepositories(Integer from, Integer size) {
		return dog.get("/2/snapshots/repositories").from(from).size(size).go(200)//
				.asPojo(TypeFactory.defaultInstance()//
						.constructCollectionLikeType(ArrayList.class, SpaceRepository.class));
	}

	public boolean openRepository(String repositoryId) {
		return dog.put("/2/snapshots/repositories/{id}")//
				.routeParam("id", repositoryId)//
				.go(200, 201).status() == 201;
	}

	public boolean closeRepository(String repositoryId) {
		return dog.delete("/2/snapshots/repositories/{id}")//
				.routeParam("id", repositoryId)//
				.go(200).get("found").asBoolean();
	}

}
