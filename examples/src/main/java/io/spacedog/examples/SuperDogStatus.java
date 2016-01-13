/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Assert;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;

public class SuperDogStatus extends Assert {

	public static void main(String[] args) throws Exception {
		SpaceRequest.setTarget(SpaceTarget.production);
		SpaceRequest.get("/v1/admin/account").superdogAuth().go(200);
	}

}
