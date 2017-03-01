package io.spacedog.admin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.AdminJobs;
import io.spacedog.utils.Check;

public class Snapshot {

	public String run() {

		try {
			// set high timeout to wait for server
			// since snapshot service is slow
			SpaceRequest.env().httpTimeoutMillis(120000);

			String password = SpaceEnv.defaultEnv().get("spacedog_jobs_snapshotall_password");

			SpaceRequest.post("/1/snapshot")//
					.basicAuth("api", "snapshotall", password)//
					.go(202);

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	public String check() {

		String message = null;

		try {
			ObjectNode node = SpaceRequest.get("/1/snapshot/latest")//
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
					"last snapshot started [%s], it should have happen today", start);

			Check.isTrue(difference < 1000 * 60 * 60, //
					"snapshot took [%s], it should take less than one hour", difference);

			return AdminJobs.ok(this, message);

		} catch (Throwable t) {
			return message == null //
					? AdminJobs.error(this, t) //
					: AdminJobs.error(this, message, t);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Snapshot snapshot = new Snapshot();
		snapshot.run();
		Thread.sleep(1000 * 10);
		snapshot.check();
	}
}
