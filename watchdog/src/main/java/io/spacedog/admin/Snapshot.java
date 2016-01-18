package io.spacedog.admin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.utils.Check;
import io.spacedog.utils.Internals;

public class Snapshot {

	public void run() {

		try {
			SpaceRequest.post("/v1/snapshot").superdogAuth().go(202);

		} catch (Exception e) {
			e.printStackTrace();

			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					SpaceRequestConfiguration.get().target().host() + " snapshot ERROR", //
					Throwables.getStackTraceAsString(e));
		}
	}

	public void check() {

		try {
			ObjectNode node = SpaceRequest.get("/v1/snapshot/latest")//
					.superdogAuth()//
					.go(200)//
					.assertEquals("SUCCESS", "state")//
					.objectNode();

			DateTime now = DateTime.now();
			DateTime start = new DateTime(node.get("startTime").asLong());
			DateTime end = new DateTime(node.get("endTime").asLong());

			Check.isTrue(DateTimeComparator.getDateOnlyInstance().compare(now, start) == 0, //
					"last snapshot took place [%s], it should have happen today");

			long difference = end.toLocalTime().millisOfDay().getDifferenceAsLong(start);
			Check.isTrue(difference < 1000 * 60 * 60, //
					"snapshot took [%s], it should take less than one hour", difference);

			String message = SpaceRequestConfiguration.get().target().host() + " snapshot OK";
			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					message, message);

		} catch (Exception e) {

			e.printStackTrace();

			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					SpaceRequestConfiguration.get().target().host() + " snapshot ERROR", //
					Throwables.getStackTraceAsString(e));
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Snapshot snapshot = new Snapshot();
		snapshot.run();
		Thread.sleep(1000 * 10);
		snapshot.check();
	}
}
