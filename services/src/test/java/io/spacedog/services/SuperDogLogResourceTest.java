package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;

public class SuperDogLogResourceTest extends Assert {

	@Test
	public void superdogsCanBrowseAllAccountLogs() throws Exception {

		SpaceDogHelper.printTestHeader();

		// create a test accounts and users
		Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "vince@dog.com");
		Account test2Account = SpaceDogHelper.resetAccount("test2", "test2", "hi test2", "test2@dog.com", true);
		SpaceDogHelper.createUser(test2Account, "fred", "hi fred", "fred@dog.com");

		// get all test account logs
		SpaceRequest.get("/v1/admin/log/test?size=2")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/v1/user", "results.0.path")//
				.assertEquals("vince", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/v1/admin/account", "results.1.path")//
				.assertEquals("test", "results.1.response.id");

		// get all test2 account logs
		SpaceRequest.get("/v1/admin/log/test2?size=2")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(2, "results")//
				.assertEquals("POST", "results.0.method")//
				.assertEquals("/v1/user", "results.0.path")//
				.assertEquals("fred", "results.0.response.id")//
				.assertEquals("POST", "results.1.method")//
				.assertEquals("/v1/admin/account", "results.1.path")//
				.assertEquals("test2", "results.1.response.id");

		// get all accounts logs
		SpaceRequest.get("/v1/admin/log?size=8")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(8, "results")//
				.assertEquals("GET", "results.0.method")//
				.assertEquals("/v1/admin/log/test2", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/v1/admin/log/test", "results.1.path")//
				.assertEquals("POST", "results.2.method")//
				.assertEquals("/v1/user", "results.2.path")//
				.assertEquals("fred", "results.2.response.id")//
				.assertEquals("POST", "results.3.method")//
				.assertEquals("/v1/admin/account", "results.3.path")//
				.assertEquals("test2", "results.3.response.id")//
				.assertEquals("DELETE", "results.4.method")//
				.assertEquals("/v1/admin/account/test2", "results.4.path")//
				.assertEquals("POST", "results.5.method")//
				.assertEquals("/v1/user", "results.5.path")//
				.assertEquals("vince", "results.5.response.id")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/v1/admin/account", "results.6.path")//
				.assertEquals("test", "results.6.response.id")//
				.assertEquals("DELETE", "results.7.method")//
				.assertEquals("/v1/admin/account/test", "results.7.path");

		// after account deletion, logs are only accessible to superdogs
		SpaceDogHelper.deleteAccount(testAccount);
		SpaceDogHelper.deleteAccount(test2Account);
		SpaceRequest.get("/v1/admin/log?size=3")//
				.superdogAuth()//
				.go(200)//
				.assertSizeEquals(3, "results")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/v1/admin/account/test2", "results.0.path")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/v1/admin/account/test", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/v1/admin/log", "results.2.path")//
				.assertEquals("8", "results.2.query.size");
	}
}
