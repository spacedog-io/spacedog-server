/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Assert;

import io.spacedog.client.SpaceRequest;

public class SuperDogStatus extends Assert {

	public static void main(String[] args) throws Exception {
		SpaceRequest.setTargetHostAndPorts("spacedog.io", 443, 80, true);
		SpaceRequest.get("/v1/admin/account").superdogAuth().go(200);
	}

}
