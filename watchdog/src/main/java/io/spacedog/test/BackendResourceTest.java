/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class BackendResourceTest extends SpaceTest {

	@Test
	public void rootBackendShallNotBeDeleted() {
		// prepare
		prepareTest();

		// root backend can not be deleted
		SpaceRequest.delete("/1/backend").superdogAuth().go(400);
	}

	@Test
	public void adminCreatesGetsAndDeletesItsBackend() {

		// prepare

		prepareTest();
		SpaceDog aaaa = resetBackend("aaaa", "aaaa", "hi aaaa");
		SpaceDog zzzz = resetBackend("zzzz", "zzzz", "hi zzzz");

		// super admin gets his backend

		JsonNode aaaaSuperAdmin = SpaceRequest.get("/1/backend").adminAuth(aaaa).go(200)//
				.objectNode().get("results").get(0);
		JsonNode zzzzSuperAdmin = SpaceRequest.get("/1/backend").adminAuth(zzzz).go(200)//
				.objectNode().get("results").get(0);

		// superdog browse all backends and finds aaaa and zzzz

		boolean aaaaFound = false, zzzzFound = false;
		int from = 0, size = 100, total = 0;

		do {
			SpaceResponse response = SpaceRequest.get("/1/backend").from(from).size(size)//
					.superdogAuth().go(200);

			aaaaFound = Iterators.contains(response.get("results").elements(), aaaaSuperAdmin);
			zzzzFound = Iterators.contains(response.get("results").elements(), zzzzSuperAdmin);

			total = response.get("total").asInt();
			from = from + size;

		} while (aaaaFound && zzzzFound && from < total);

		// super admin fails to access a backend he does not own

		SpaceRequest.get("/1/backend")//
				.basicAuth("aaaa", zzzz.username(), zzzz.password().get()).go(401);
		SpaceRequest.get("/1/backend")//
				.basicAuth("zzzz", aaaa.username(), aaaa.password().get()).go(401);

		// super admin fails to delete a backend he does not own

		SpaceRequest.delete("/1/backend")//
				.basicAuth("aaaa", zzzz.username(), zzzz.password().get()).go(401);
		SpaceRequest.delete("/1/backend")//
				.basicAuth("zzzz", aaaa.username(), aaaa.password().get()).go(401);

		// super admin can delete his backend

		SpaceRequest.delete("/1/backend").adminAuth(aaaa).go(200);
		SpaceRequest.delete("/1/backend").adminAuth(zzzz).go(200);
	}
}
