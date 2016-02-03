package io.spacedog.admin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Check;

public class Snapshot {

	public void run() {

		try {
			SpaceRequest.post("/v1/snapshot").superdogAuth().go(202);

		} catch (Exception e) {
			AdminJobs.error(this, e);
		}
	}

	public void check() {

		String message = null;

		try {
			ObjectNode node = SpaceRequest.get("/v1/snapshot/latest")//
					.superdogAuth()//
					.go(200)//
					.assertEquals("SUCCESS", "state")//
					.objectNode();

			DateTime now = DateTime.now();
			DateTime start = new DateTime(node.get("startTime").asLong());
			DateTime end = new DateTime(node.get("endTime").asLong());
			long difference = end.getMillis() - start.getMillis();

			message = new StringBuilder("snapshot id = ")//
					.append(node.get("id").asText())//
					.append("\nrepository = ")//
					.append(node.get("repository").asText())//
					.append("\nstate = ")//
					.append(node.get("state").asText())//
					.append("\ntype = ")//
					.append(node.get("type").asText())//
					.append("\nstartTime = ")//
					.append(start.toString())//
					.append("\nendTime = ")//
					.append(end.toString())//
					.append("\nduration = ")//
					.append(difference)//
					.toString();

			Check.isTrue(DateTimeComparator.getDateOnlyInstance().compare(now, start) == 0, //
					"last snapshot took place [%s], it should have happen today");

			Check.isTrue(difference < 1000 * 60 * 60, //
					"snapshot took [%s], it should take less than one hour", difference);

			AdminJobs.ok(this, message);

		} catch (Exception e) {
			if (message == null)
				AdminJobs.error(this, e);
			else
				AdminJobs.error(this, message, e);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Snapshot snapshot = new Snapshot();
		snapshot.run();
		Thread.sleep(1000 * 10);
		snapshot.check();
	}
}
