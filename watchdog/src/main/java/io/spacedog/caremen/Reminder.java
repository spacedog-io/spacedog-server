package io.spacedog.caremen;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.admin.AdminJobs;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.sdk.SpaceData.ComplexQuery;
import io.spacedog.sdk.SpaceData.SearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.SpacePush.PushRequest;
import io.spacedog.utils.Check;
import io.spacedog.utils.Json;

public class Reminder {

	DateTimeFormatter shortTimeFormatter = DateTimeFormat.shortTime()//
			.withLocale(Locale.FRANCE).withZone(DateTimeZone.forID("Europe/Paris"));

	public String remindTomorrow() {

		try {
			SpaceDog dog = login();

			DateTime tomorrow = DateTime.now().plusDays(1).withTimeAtStartOfDay();
			DateTime dayAfterTomorrow = tomorrow.plusDays(1).withTimeAtStartOfDay();

			SearchResults<Course> courses = getScheduledAssignedCourses(dog, tomorrow, dayAfterTomorrow);
			Map<String, List<Course>> courseMap = Maps.newHashMap();

			for (Course course : courses.objects()) {
				try {
					checkCourse(course);
					List<Course> driverCourses = courseMap.get(course.driver.driverId);

					if (driverCourses == null) {
						driverCourses = Lists.newArrayList();
						courseMap.put(course.driver.credentialsId, driverCourses);
					}

					driverCourses.add(course);

				} catch (Exception e) {
					handleException(dog, course, e);
				}
			}

			for (Entry<String, List<Course>> entry : courseMap.entrySet())
				push(dog, entry.getKey(), toMessage(entry.getValue()));

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	public String remindToday() {

		try {
			SpaceDog dog = login();

			DateTime oneHourAfterNow = DateTime.now().plusHours(2);
			DateTime twoHoursAfterNow = oneHourAfterNow.plusHours(2);

			SearchResults<Course> courses = getScheduledAssignedCourses(dog, oneHourAfterNow, twoHoursAfterNow);

			for (Course course : courses.objects()) {
				try {
					checkCourse(course);
					push(dog, course.driver.credentialsId, toMessage(course));

				} catch (Exception e) {
					handleException(dog, course, e);
				}
			}

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";

	}

	private void checkCourse(Course course) {
		Check.notNull(course.driver, "course.driver");
		Check.notNull(course.driver.credentialsId, "course.driver.credentialsId");
		Check.notNull(course.requestedPickupTimestamp, "course.requestedPickupTimestamp");
	}

	private void handleException(SpaceDog dog, Course course, Exception e) {
		String url = SpaceEnv.defaultEnv().target().url(dog.backendId(), //
				"/1/data/course/" + course.id());
		AdminJobs.error(this, "Error remindering course " + url, e);
	}

	private SpaceDog login() {
		SpaceEnv env = SpaceEnv.defaultEnv();
		return SpaceDog.backend(env.get("backend_id")) //
				.username("reminder").login(env.get("caremen_reminder_password"));
	}

	private void push(SpaceDog dog, String driverCredentialsId, String message) {

		PushRequest request = new PushRequest();
		request.appId = "caremendriver";
		request.tags = Collections.singletonList(//
				new PushRequest.PushTag("credentialsId", driverCredentialsId));
		request.message = message;

		dog.push().push(request);
	}

	private SearchResults<Course> getScheduledAssignedCourses(SpaceDog dog, //
			DateTime from, DateTime to) {

		ComplexQuery query = new ComplexQuery();
		query.type = "course";
		query.query = Json.objectBuilder()//
				.put("size", 1000)//
				.object("query").object("bool").array("must")//
				.object().object("term").put("status", "scheduled-assigned").end().end()//
				.object().object("range").object("requestedPickupTimestamp")//
				.put("gte", from.toString())//
				.put("lt", to.toString())//
				.build();

		return dog.dataEndpoint().search(query, Course.class);
	}

	private String toMessage(List<Course> courses) {
		StringBuilder builder = new StringBuilder("Vous avez ")//
				.append(courses.size())//
				.append(" course(s) CAREMEN demain à");

		boolean first = true;

		for (Course course : courses) {
			builder.append(first ? " " : ", ")//
					.append(shortTimeFormatter.print(//
							course.requestedPickupTimestamp));
			first = false;
		}

		return builder.append(". Soyez ponctuel.").toString();
	}

	private String toMessage(Course course) {
		return new StringBuilder("Vous avez une course CAREMEN prévue aujourd'hui à ")//
				.append(shortTimeFormatter.print(//
						course.requestedPickupTimestamp))//
				.append(" à l'adresse : ").append(course.to.address)//
				.append(". Soyez ponctuel.").toString();
	}

	public static void main(String[] args) {
		SpaceRequest.env().target(SpaceTarget.production);
		System.setProperty("backend_id", "carerec");
		new Reminder().remindTomorrow();
		new Reminder().remindToday();
	}
}
