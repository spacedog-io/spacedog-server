package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceRequest;
import io.spacedog.model.Schema;

public class CockTest {

	@Test
	public void init() {

		SpaceRequest.env().target(SpaceBackend.production);

		SpaceDog superadmin = SpaceDog.backendId("cocktest").username("cocktest")//
				.password("hi cocktest").email("platform@spacedog.io");

		// superadmin.admin().deleteBackend("cocktest");
		// superadmin.admin().createMyBackend(false);

		superadmin.schema().set(buildHomeSchema());
		superadmin.schema().set(buildPersonSchema());
	}

	private Schema buildPersonSchema() {
		return Schema.builder("person")//
				.text("firstname").french()//
				.text("lastname").french()//
				.date("born")//
				.string("father").refType("person")//
				.string("mother").refType("person")//
				.build();
	}

	private Schema buildHomeSchema() {
		return Schema.builder("home")//
				.geopoint("where")//
				.string("habitants").array().refType("person")//
				.integer("rooms")//
				.date("builtAt")//
				.timestamp("lastGazReading")//
				.time("doorClosesAt")//
				.object("copro")//
				.object("contact")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("phone").french()//
				.close()//
				.integer("coproNumber")//
				.text("syndic").french()//
				.close()//
				.build();
	}
}
