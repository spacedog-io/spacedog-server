package io.spacedog.caremen;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Optional;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.admin.AdminJobs;
import io.spacedog.caremen.DriverRecap.CourseRecap;
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

	private static DecimalFormat decimalFormatter = new DecimalFormat("#.##");

	private static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("EEEE dd MMMM', 'HH' h 'mm")//
			.withLocale(Locale.FRANCE).withZone(DateTimeZone.forID("Europe/Paris"));

	public String recap() {

		try {
			env = SpaceEnv.defaultEnv();
			testing = env.get("caremen_recap_test", false);
			dog = login();

			SearchResults<Driver> drivers = getDriversWhoWorkedLastMonth();

			for (Driver driver : drivers.objects())
				computeRecap(driver).ifPresent(recap -> sendRecap(recap));

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	private Optional<DriverRecap> computeRecap(Driver driver) {
		try {
			DriverRecap recap = new DriverRecap();
			recap.setDriver(driver);

			SearchResults<Course> courses = getDriverLastMonthCourses(driver);

			if (courses.total() == 0)
				return Optional.empty();

			if (courses.total() > 1000)
				throw Exceptions.runtime("too many courses for driver [%s][%s %s]", //
						driver.id(), driver.firstname, driver.lastname);

			for (Course course : courses.objects())
				recap.addCourse(course);

			return Optional.of(recap);

		} catch (Exception e) {
			String url = SpaceEnv.defaultEnv().target().url(dog.backendId(), //
					"/1/data/driver/" + driver.id());

			AdminJobs.error(this, "Error sending recap to driver " + url, e);
			return Optional.empty();
		}
	}

	private void sendRecap(DriverRecap recap) {

		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Bonjour %s %s,\n\n", recap.firstname, recap.lastname))//
				.append(String.format("Voici le récap des courses que vous avez effectuées en %s :\n\n", //
						recap.month));

		for (CourseRecap course : recap.courses) {
			builder.append(String.format("%s, %s, %s km, %s min, %s €\n\n", //
					dateFormatter.print(course.dropoffTimestamp), course.to, //
					toKm(course.distance), toMin(course.time), toEuro(course.gain)));
		}

		builder.append("\n\n\n").append("Le montant total de vos revenus du mois sont de ")//
				.append(String.format("%s €\n\n", toEuro(recap.gain)))//
				.append("Ces revenus vous seront payés par virement sur votre compte en banque");

		if (recap.rib == null)
			builder.append('.');
		else
			builder.append(String.format("%s :\n\n\tBIC : %s\n\tIBAN : %s", //
					recap.rib.bankName, recap.rib.bankCode, recap.rib.accountIBAN));

		Message message = new Message();
		message.from = "CAREMEN <no-reply@caremen.fr>";
		message.to = dog.credentials().get(recap.credentialsId).email().get();
		message.subject = "Votre récap des courses du mois dernier";
		message.text = builder.toString();

		dog.mailEndpoint().send(message);
	}

	private String toEuro(double gain) {
		return decimalFormatter.format(gain);
	}

	private String toMin(long time) {
		return decimalFormatter.format(time / 60000);
	}

	private String toKm(long distance) {
		return decimalFormatter.format(distance / 1000);
	}

	private SearchResults<Course> getDriverLastMonthCourses(Driver driver) {

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
			builder.put("gte", "now/M");
		else
			builder.put("gte", "now-1M/M")//
					.put("lt", "now/M");

		ComplexQuery query = new ComplexQuery();
		query.type = "course";
		query.query = builder.build();

		return dog.dataEndpoint().search(query, Course.class);
	}

	private SpaceDog login() {
		return SpaceDog.backend(env.get("backend_id")).username("recaper") //
				.login(env.get("caremen_recaper_password"));
	}

	private SearchResults<Driver> getDriversWhoWorkedLastMonth() {

		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("size", 1000)//
				.object("query").object("bool")//

				.array("must_not")//
				.object().object("term").put("status", "disabled").end().end()//
				.end()//

				.array("must")//
				.object().object("range").object("meta.updatedAt");

		if (testing)
			builder.put("gte", "now/M");
		else
			builder.put("gte", "now-1M/M")//
					.put("lt", "now/M");

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
