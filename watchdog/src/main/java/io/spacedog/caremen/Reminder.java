package io.spacedog.caremen;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.admin.AdminJobs;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.client.SpaceTarget;
import io.spacedog.sdk.SpaceData.SearchResults;
import io.spacedog.sdk.SpaceData.TermQuery;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.SpacePush.PushRequest;
import io.spacedog.utils.Check;

public class Reminder {

	DateTimeFormatter shortTimeFormatter = DateTimeFormat.shortTime().withLocale(Locale.FRANCE);

	public String remindTomorrow() {

		try {
			SpaceRequestConfiguration configuration = SpaceRequestConfiguration.get();

			SpaceDog dog = SpaceDog.login(//
					configuration.getProperty("backendId"), "reminder", //
					configuration.getProperty("caremen.reminder.password"));

			TermQuery query = new TermQuery();
			query.type = "course";
			query.size = 1000;
			query.terms = Lists.newArrayList("status", "scheduled-assigned");
			SearchResults<Course> courses = dog.data().search(query, Course.class);

			Map<String, List<Course>> courseMap = Maps.newHashMap();
			for (Course course : courses.objects()) {
				try {
					Check.notNull(course.requestedPickupTimestamp, "course.requestedPickupTimestamp");
					Check.notNull(course.driver, "course.driver");
					Check.notNull(course.driver.driverId, "course.driver.driverId");
					List<Course> driverCourses = courseMap.get(course.driver.driverId);

					if (driverCourses == null) {
						driverCourses = Lists.newArrayList();
						courseMap.put(course.driver.driverId, driverCourses);
					}

					driverCourses.add(course);

				} catch (Exception e) {
					String url = configuration.target().url(dog.backendId(), //
							"/1/data/course/" + course.id());
					AdminJobs.error(this, "Error remindering course " + url, e);
				}
			}

			for (Entry<String, List<Course>> entry : courseMap.entrySet()) {

				PushRequest request = new PushRequest();
				request.appId = "caremendriver";
				request.tags = Collections.singletonList(//
						new PushRequest.PushTag("credentialsId", entry.getKey()));
				request.message = computeMessageTomorrow(entry.getValue());

				dog.push().push(request);
			}

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	public String remindToday() {

		try {
			SpaceRequestConfiguration configuration = SpaceRequestConfiguration.get();

			SpaceDog dog = SpaceDog.login(//
					configuration.getProperty("backendId"), "reminder", //
					configuration.getProperty("caremen.reminder.password"));

			TermQuery query = new TermQuery();
			query.type = "course";
			query.size = 1000;
			query.terms = Lists.newArrayList("status", "scheduled-assigned");
			SearchResults<Course> courses = dog.data().search(query, Course.class);

			for (Course course : courses.objects()) {
				try {
					Check.notNull(course.driver, "course.driver");
					Check.notNull(course.driver.driverId, "course.driver.driverId");
					Check.notNull(course.requestedPickupTimestamp, "course.requestedPickupTimestamp");

					PushRequest request = new PushRequest();
					request.appId = "caremendriver";
					request.tags = Collections.singletonList(//
							new PushRequest.PushTag("credentialsId", course.driver.driverId));
					request.message = computeMessageToday(course);

					dog.push().push(request);

				} catch (Exception e) {
					String url = configuration.target().url(dog.backendId(), //
							"/1/data/course/" + course.id());
					AdminJobs.error(this, "Error remindering course " + url, e);
				}
			}

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";

	}

	private String computeMessageTomorrow(List<Course> courses) {
		StringBuilder builder = new StringBuilder("Vous avez ")//
				.append(courses.size())//
				.append(" course(s) CAREMEN demain à");

		boolean first = true;

		for (Course course : courses) {
			builder.append(first ? " " : ", ")//
					.append(shortTimeFormatter.print(course.requestedPickupTimestamp));
			first = false;
		}

		return builder.append(". Soyez ponctuel.").toString();
	}

	private String computeMessageToday(Course course) {
		return new StringBuilder("Vous avez une course prévue à ")//
				.append(shortTimeFormatter.print(course.requestedPickupTimestamp))//
				.append(" à l'adresse : ").append(course.to.address)//
				.append(". Soyez ponctuel.").toString();
	}

	public static void main(String[] args) {
		SpaceRequest.configuration().target(SpaceTarget.production);
		System.setProperty("backendId", "caredev");
		System.setProperty("caremen.reminder.password", "hi reminder");
		new Reminder().remindTomorrow();
		new Reminder().remindToday();
	}
}
