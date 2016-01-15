package io.spacedog.admin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.client.Space;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Check;

public class Snapshot {

	public void run() {

		try {
			SpaceRequest.post("/v1/dog/snapshot").superdogAuth().go(202);

		} catch (Exception e) {
			e.printStackTrace();
			Space.get().sendNotification(//
					SpaceRequest.getTarget().host() + " snapshot ERROR", //
					Throwables.getStackTraceAsString(e));
		}
	}

	public void check() {

		try {
			ObjectNode node = SpaceRequest.get("/v1/dog/snapshot/latest")//
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

			String message = SpaceRequest.getTarget().host() + " snapshot OK";
			Space.get().sendNotification(message, message);

		} catch (Exception e) {
			e.printStackTrace();
			Space.get().sendNotification(//
					SpaceRequest.getTarget().host() + " snapshot ERROR", //
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
