/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;

public class GetAllAccountsTest extends Assert {

	@Test
	public void testGetAllAccounts() throws Exception {

		// prepare
		SpaceDogHelper.printTestHeader();
		SpaceDogHelper.Account aaaa = SpaceDogHelper.resetAccount("aaaa", "aaaa", "hi aaaa", "hello@spacedog.io");
		SpaceDogHelper.Account zzzz = SpaceDogHelper.resetAccount("zzzz", "zzzz", "hi zzzz", "hello@spacedog.io");

		// should succeed to get accounts
		ObjectNode aaaaNode = SpaceRequest.get("/v1/admin/account/aaaa").basicAuth(aaaa).go(200).objectNode();
		ObjectNode zzzzNode = SpaceRequest.get("/v1/admin/account/zzzz").basicAuth(zzzz).go(200).objectNode();

		// fails to get all accounts because of credentials
		SpaceRequest.get("/v1/admin/account").basicAuth(zzzz).go(401);

		// succeed to get all accounts with superdog credentials
		SpaceRequest.get("/v1/admin/account").superdogAuth().go(200)//
				.assertArrayContains(aaaaNode, "results")//
				.assertArrayContains(zzzzNode, "results");

		// should succeed to delete accounts
		SpaceRequest.delete("/v1/admin/account/aaaa").basicAuth(aaaa).go(200);
		SpaceRequest.delete("/v1/admin/account/zzzz").basicAuth(zzzz).go(200);
	}

}
