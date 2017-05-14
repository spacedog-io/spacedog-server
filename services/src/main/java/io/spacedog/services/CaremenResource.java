package io.spacedog.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.core.Json8;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.GeoPoint;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class CaremenResource extends Resource {

	//
	// Routes
	//

	@Post("/1/service/course")
	@Post("/1/service/course/")
	public Payload postCourse(String body, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();

		// only real users are allowed to post courses
		Set<String> roles = credentials.roles();
		if (!roles.contains(Credentials.USER) || roles.size() > 1)
			throw Exceptions.insufficientCredentials(credentials);

		try {
			// check course
			Course course = Json8.mapper().readValue(body, Course.class);

			// save course
			Payload payload = DataResource.get().post("course", body, context);

			// get course id and version
			ObjectNode courseResponse = (ObjectNode) payload.rawContent();
			String courseId = courseResponse.get("id").asText();
			long courseVersion = courseResponse.get("version").asLong();

			// push to drivers if immediate
			if (course.requestedPickupTimestamp == null) {
				PushLog pushLog = pushToClosestDrivers(courseId, course, credentials);
				payload = enhancePushPayload(pushLog, courseId, courseVersion);
			}

			// text the operator
			textOperatorNewCourseRequest(course, courseId);

			return payload;

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	@Delete("/1/service/course/:id/driver")
	@Delete("/1/service/course/:id/driver")
	public Payload deleteCourseDriver(String courseId, String body, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		credentials.checkRoles("driver", "admin");

		GetResponse response = Start.get().getElasticClient()//
				.get(credentials.backendId(), "course", courseId);

		Course course = null;

		try {
			course = Json7.mapper()//
					.readValue(response.getSourceAsBytes(), Course.class);

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading course [%s]", courseId);
		}

		checkDriverIsAuthorized(credentials, courseId, course);

		// update course
		ObjectNode patch = Json8.object("driver", null, "status", "new-immediate");
		long courseVersion = DataStore.get()
				.patchObject(//
						credentials.backendId(), "course", courseId, //
						patch, credentials.name())//
				.getVersion();

		// push to drivers
		PushLog pushLog = pushToClosestDrivers(courseId, course, credentials);
		Payload payload = enhancePushPayload(pushLog, courseId, courseVersion);

		// text the operator
		textOperatorDriverHasGivenUp(courseId, course, credentials);
		return payload;
	}

	//
	// Implementation
	//

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Course {
		public String requestedVehiculeType;
		public DateTime requestedPickupTimestamp;
		public Location from;
		public Driver driver;
		public Customer customer;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Location {
			public GeoPoint geopoint;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Driver {
			public String credentialsId;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Customer {
			public String firstname;
			public String lastname;
		}

	}

	private void textOperatorNewCourseRequest(Course course, String courseId) {
		String smsTemplateName = course.requestedPickupTimestamp == null //
				? "new-immediate" : "new-scheduled";

		SmsTemplateResource.get().postTemplatedSms(smsTemplateName, //
				Json8.object("course", courseId).toString());
	}

	private Payload enhancePushPayload(//
			PushLog pushLog, String courseId, long courseVersion) {

		Payload payload = pushLog.toPayload();
		ObjectNode node = (ObjectNode) payload.rawContent();
		node.put("id", courseId);
		node.put("version", courseVersion);
		return payload;
	}

	private PushLog pushToClosestDrivers(String courseId, //
			Course course, Credentials credentials) {

		// search for drivers
		List<String> driverCredentialsIds = searchDrivers(//
				credentials.backendId(), course);

		// search for installations
		SearchResponse response = searchInstallations(//
				credentials.backendId(), driverCredentialsIds);

		// push message to drivers
		PushLog pushLog = new PushLog();
		ObjectNode message = messageToDrivers(courseId);

		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		return pushLog;
	}

	private ObjectNode messageToDrivers(String courseId) {

		ObjectNode apsMessage = Json8.objectBuilder()//
				.put("id", courseId)//
				.put("type", "new-immediate")//
				.object("aps")//
				.put("content-available", 1)//
				.put("sound", "newImmediate.wav")//
				.object("alert")//
				.put("title", "Demande de course immédiate")//
				.put("body", "Un client vient de commander une course immédiate")//
				.build();

		return Json8.objectBuilder()//
				.node("APNS_SANDBOX", apsMessage)//
				.node("APNS", apsMessage)//
				.build();
	}

	private List<String> searchDrivers(String backendId, Course course) {

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("status", "working"))//
				.must(QueryBuilders.termsQuery("vehicule.type", //
						compatibleVehiculeTypes(course.requestedVehiculeType)))//
				.must(QueryBuilders.geoDistanceQuery("lastLocation.where")//
						.distance(5000, DistanceUnit.KILOMETERS)//
						.point(course.from.geopoint.lat, course.from.geopoint.lon));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "driver").setQuery(query).setSize(5)//
				.setFetchSource(false).addField("credentialsId").get();

		List<String> credentialsIds = new ArrayList<>(5);
		for (SearchHit hit : response.getHits().hits())
			credentialsIds.add(hit.field("credentialsId").getValue());

		return credentialsIds;
	}

	private String[] compatibleVehiculeTypes(String requestedVehiculeType) {
		if ("classic".equals(requestedVehiculeType))
			return new String[] { "classic", "premium", "green", "break", "van" };

		if ("premium".equals(requestedVehiculeType))
			return new String[] { "premium", "green", "van" };

		if ("green".equals(requestedVehiculeType))
			return new String[] { "green" };

		if ("break".equals(requestedVehiculeType))
			return new String[] { "break", "van" };

		if ("van".equals(requestedVehiculeType))
			return new String[] { "van" };

		throw Exceptions.runtime("invalid vehicule type [%s]", requestedVehiculeType);
	}

	private SearchResponse searchInstallations(String backendId, List<String> driverIds) {

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("appId", "caremendriver"))//
				.must(QueryBuilders.termsQuery("tags.value", driverIds));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "installation")//
				.setQuery(query).setSize(5).get();

		return response;
	}

	private void textOperatorDriverHasGivenUp(String courseId, Course course, Credentials credentials) {
		StringBuilder builder = new StringBuilder("Le chauffeur ")//
				.append(credentials.name()).append(" a renoncé à la course n° [")//
				.append(courseId).append(" de ").append(course.customer.firstname)//
				.append(" ").append(course.customer.lastname)//
				.append(". La course a été proposé à d'autres chauffeurs.");

		String settings = SettingsResource.get().load("appconfiguration");
		String phone = Json8.readObject(settings).get("operatorPhoneNumber").asText();
		SmsMessage message = new SmsMessage().to(phone).body(builder.toString());
		SmsResource.get().send(message);
	}

	private void checkDriverIsAuthorized(Credentials credentials, String courseId, Course course) {
		if (credentials.roles().contains("driver") //
				&& !credentials.id().equals(course.driver.credentialsId))
			throw Exceptions.forbidden("you are not the driver of course [%s]", courseId);
	}

	//
	// singleton
	//

	private static CaremenResource singleton = new CaremenResource();

	static CaremenResource get() {
		return singleton;
	}

	private CaremenResource() {
	}
}
