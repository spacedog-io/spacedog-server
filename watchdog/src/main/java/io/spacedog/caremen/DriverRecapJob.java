package io.spacedog.caremen;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.StringLoader;

import io.spacedog.admin.AdminJobs;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.sdk.SpaceData.ComplexQuery;
import io.spacedog.sdk.SpaceData.SearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.SpaceMail.Message;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;

public class DriverRecapJob {

	private SpaceEnv env = null;
	private SpaceDog dog = null;
	private boolean testing = false;
	private FareSettings fareSettings;

	public String recap() {

		try {
			env = SpaceEnv.defaultEnv();
			testing = env.get("caremen_recap_test", false);
			dog = login();

			SearchResults<Driver> drivers = getDriversWhoWorkedLastWeek();

			for (Driver driver : drivers.objects()) {

				try {
					computeRecap(driver).ifPresent(recap -> sendRecap(recap));

				} catch (Throwable t) {
					String message = String.format(//
							"Error sending recap to driver [%s]", //
							driverUrl(driver.id()));
					AdminJobs.error(this, message, t);
				}
			}

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	private Optional<DriverRecap> computeRecap(Driver driver) {

		DriverRecap recap = new DriverRecap();
		recap.setDriver(driver);
		recap.setCaremenShare(getFareSettings().driverShare);

		SearchResults<Course> courses = getDriverLastWeekCourses(driver);

		if (courses.total() == 0)
			return Optional.empty();

		if (courses.total() > 1000)
			throw Exceptions.runtime("too many courses for driver [%s][%s %s]", //
					driver.id(), driver.firstname, driver.lastname);

		for (Course course : courses.objects())
			recap.addCourse(course);

		recap.shakeIt();
		return Optional.of(recap);
	}

	private String driverUrl(String driverId) {
		return SpaceEnv.defaultEnv().target().url(dog.backendId(), //
				"/1/data/driver/" + driverId);
	}

	private FareSettings getFareSettings() {
		if (fareSettings == null)
			fareSettings = dog.settings().get(FareSettings.class);
		return fareSettings;
	}

	private void sendRecap(DriverRecap recap) {

		@SuppressWarnings("unchecked")
		Map<String, Object> context = Json.mapper().convertValue(recap, Map.class);

		Message message = new Message();
		message.from = "CAREMEN <no-reply@caremen.fr>";
		message.to = dog.credentials().get(recap.credentialsId).email().get();
		message.bcc = "compta@caremen.fr";
		message.subject = "Vos revenus CAREMEN de la semaine dernière";

		try {
			String template = Resources.toString(//
					Resources.getResource(this.getClass(), "driver-recap.html"), //
					Charset.forName("UTF-8"));

			PebbleEngine pebble = new PebbleEngine.Builder().loader(new StringLoader()).build();
			StringWriter writer = new StringWriter();
			pebble.getTemplate(template).evaluate(writer, context);
			message.html = writer.toString();

			dog.mailEndpoint().send(message);

		} catch (Exception e) {
			AdminJobs.error(this, "Error sending recap of driver [" //
					+ driverUrl(recap.driverId) + "]", e);
		}
	}

	private SearchResults<Course> getDriverLastWeekCourses(Driver driver) {

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("size", 1000)//
				.object("query").object("bool")//

				.array("must_not")//
				.object().object("term").put("status", "disabled").end().end()//
				.end()//

				.array("must")//
				.object().object("term").put("driver.driverId", driver.id()).end().end()//
				.object().object("term").put("status", "billed").end().end()//
				.object().object("range").object("dropoffTimestamp");

		if (testing)
			builder.put("gte", "now/w");
		else
			builder.put("gte", "now-1w/w")//
					.put("lt", "now/w");

		ComplexQuery query = new ComplexQuery();
		query.type = "course";
		query.query = builder.build();

		return dog.dataEndpoint().search(query, Course.class);
	}

	private SpaceDog login() {
		return SpaceDog.backend(env.get("backend_id")).username("recaper") //
				.login(env.get("caremen_recaper_password"));
	}

	private SearchResults<Driver> getDriversWhoWorkedLastWeek() {

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("size", 1000)//
				.object("query").object("bool")//

				.array("must_not")//
				.object().object("term").put("status", "disabled").end().end()//
				.end()//

				.array("must")//
				.object().object("range").object("meta.updatedAt");

		if (testing)
			builder.put("gte", "now/w");
		else
			builder.put("gte", "now-1w/w")//
					.put("lt", "now/w");

		ComplexQuery query = new ComplexQuery();
		query.type = "driver";
		query.query = builder.build();

		return dog.dataEndpoint().search(query, Driver.class);
	}

	public static void main(String[] args) {
		SpaceRequest.env().target(SpaceTarget.production);
		System.setProperty("backend_id", "caredev");
		System.setProperty("caremen_recap_test", "true");
		new DriverRecapJob().recap();
	}
}
