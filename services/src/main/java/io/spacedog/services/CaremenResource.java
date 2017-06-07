package io.spacedog.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.core.Json8;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.GeoPoint;
import io.spacedog.model.PushService;
import io.spacedog.model.Settings;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.JsonBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Post;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class CaremenResource extends Resource {

	private static final String CREDENTIALS_ID = "credentialsId";
	private static final String NEW_SCHEDULED = "new-scheduled";
	private static final String NEW_IMMEDIATE = "new-immediate";
	private static final String VERSION = "version";
	private static final String ID = "id";
	private static final String COURSE = "course";

	//
	// Routes
	//

	@Post("/1/service/course")
	@Post("/1/service/course/")
	public Payload postCourse(String body, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();
		checkAuthorizedToCreateCourse(credentials);

		PushLog driverPushLog = null;
		int httpStatus = HttpStatus.CREATED;

		// check course
		Course course = readAndCheckCourse(body);

		// save course
		Payload payload = DataResource.get().post(COURSE, body, context);

		// get course id and version
		ObjectNode courseResponse = (ObjectNode) payload.rawContent();
		String courseId = courseResponse.get(ID).asText();
		long courseVersion = courseResponse.get(VERSION).asLong();

		if (course.requestedPickupTimestamp == null) {
			driverPushLog = pushToClosestDrivers(courseId, course, credentials);
			if (driverPushLog.successes == 0) {
				courseVersion = setStatusToNoDriverAvailable(courseId, credentials);
				httpStatus = HttpStatus.NOT_FOUND;
			}
			textOperatorNewImmediate(course, courseId, driverPushLog.successes);
		} else
			textOperatorNewScheduled(course, courseId);

		return createPayload(httpStatus, courseId, courseVersion, driverPushLog, null);
	}

	@Delete("/1/service/course/:id/driver")
	@Delete("/1/service/course/:id/driver")
	public Payload deleteCourseDriver(String courseId, String body, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		Course course = loadCourse(courseId, credentials);
		checkAuthorizedToRemoveDriver(credentials, courseId, course);

		long courseVersion = removeDriverFromCourse(courseId, credentials);
		PushLog driverPushLog = pushToClosestDrivers(courseId, course, credentials);
		PushLog customerPushLog = pushToCustomer(courseId, course, credentials);

		textOperatorDriverHasGivenUp(courseId, course, credentials);
		return createPayload(HttpStatus.OK, //
				courseId, courseVersion, driverPushLog, customerPushLog);
	}

	//
	// Implementation
	//

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Course {
		public Customer customer;
		public Location from;
		public Location to;
		public String requestedVehiculeType;
		public DateTime requestedPickupTimestamp;
		public String status;
		public Driver driver;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Location {
			public String address;
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
			public String credentialsId;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class AppConfigurationSettings extends Settings {
		public String operatorPhoneNumber;
		public int newCourseRequestDriverPushRadiusInMeters;
	}

	private Course readAndCheckCourse(String body) {
		Course course = null;

		try {
			course = Json8.mapper().readValue(body, Course.class);
		} catch (Exception e) {
			throw Exceptions.illegalArgument(e, "error deserializing course");
		}

		if (course.status.equals(NEW_SCHEDULED))
			Check.notNull(course.requestedPickupTimestamp, "requestedPickupTimestamp");
		else
			Check.isTrue(course.status.equals(NEW_IMMEDIATE), //
					"invalid course request status [%s]", course.status);

		return course;
	}

	private Course loadCourse(String courseId, Credentials credentials) {
		GetResponse response = Start.get().getElasticClient()//
				.get(credentials.backendId(), COURSE, courseId);

		try {
			return Json7.mapper()//
					.readValue(response.getSourceAsBytes(), Course.class);

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading course [%s]", courseId);
		}
	}

	private long setStatusToNoDriverAvailable(String courseId, Credentials credentials) {
		ObjectNode patch = Json8.object("status", "no-driver-available");
		return DataStore.get()
				.patchObject(//
						credentials.backendId(), COURSE, courseId, //
						patch, credentials.name())//
				.getVersion();
	}

	private long removeDriverFromCourse(String courseId, Credentials credentials) {
		ObjectNode patch = Json8.object("driver", null, "status", NEW_IMMEDIATE);
		return DataStore.get()
				.patchObject(//
						credentials.backendId(), COURSE, courseId, //
						patch, credentials.name())//
				.getVersion();
	}

	private static DateTimeZone parisZone = DateTimeZone.forID("Europe/Paris");

	private static DateTimeFormatter pickupFormatter = DateTimeFormat//
			.forPattern("dd/MM' à 'HH'h'mm").withZone(parisZone).withLocale(Locale.FRENCH);

	private void textOperatorNewScheduled(Course course, String courseId) {
		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course programmée.\nDépart le ")//
				.append(pickupFormatter.print(course.requestedPickupTimestamp)) //
				.append(" de ").append(course.from.address)//
				.append(".\nDestination : ").append(course.to.address)//
				.append(".\nCatégorie : ").append(course.requestedVehiculeType)//
				.append(".");

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private void textOperatorNewImmediate(//
			Course course, String courseId, int notifications) {
		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course immédiate.\nDestination : ")//
				.append(course.to.address).append(".\nCatégorie : ")//
				.append(course.requestedVehiculeType).append(".\nChauffeurs notifiés : ")//
				.append(notifications);

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private Payload createPayload(int httpStatus, String courseId, //
			long courseVersion, PushLog driverPushLog, PushLog customerPushLog) {

		JsonBuilder<ObjectNode> builder = JsonPayload.builder(httpStatus)//
				.put(ID, courseId)//
				.put(VERSION, courseVersion);

		if (driverPushLog != null)
			builder.node("driverPushLog", driverPushLog.logItems);

		if (customerPushLog != null)
			builder.node("customerPushLog", customerPushLog.logItems);

		return JsonPayload.json(builder, httpStatus);
	}

	private PushLog pushToClosestDrivers(String courseId, //
			Course course, Credentials credentials) {

		// search for drivers
		List<String> credentialsIds = searchDrivers(//
				credentials.backendId(), course);

		// if requester is a driver, he does not need the push
		if (credentials.roles().contains("driver"))
			credentialsIds.remove(credentials.id());

		// search for installations
		SearchResponse response = searchInstallations(//
				credentials.backendId(), credentialsIds);

		// push message to drivers
		ObjectNode message = messageToDrivers(courseId);

		PushLog pushLog = new PushLog();
		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		return pushLog;
	}

	private ObjectNode messageToDrivers(String courseId) {

		ObjectNode apsMessage = Json8.objectBuilder()//
				.put(ID, courseId)//
				.put("type", NEW_IMMEDIATE)//
				.object("aps")//
				.put("content-available", 1)//
				.put("sound", "newImmediate.wav")//
				.object("alert")//
				.put("title", "Demande de course immédiate")//
				.put("body", "Un client vient de commander une course immédiate")//
				.build();

		return Json8.objectBuilder()//
				.node(PushService.APNS_SANDBOX.name(), apsMessage)//
				.node(PushService.APNS.name(), apsMessage)//
				.build();
	}

	private ObjectNode messageToCustomer(String courseId) {

		ObjectNode apsMessage = Json8.objectBuilder()//
				.put(ID, courseId)//
				.put("type", NEW_IMMEDIATE)//
				.object("aps")//
				.put("content-available", 1)//
				.put("sound", "default")//
				.object("alert")//
				.put("title", "Chauffeur indisponible")//
				.put("body",
						"Votre chauffeur a rencontré un problème et ne peut pas vous rejoindre."
								+ " Nous recherchons un autre chauffeur.")//
				.build();

		return Json8.objectBuilder()//
				.node(PushService.APNS_SANDBOX.name(), apsMessage)//
				.node(PushService.APNS.name(), apsMessage)//
				.build();
	}

	private List<String> searchDrivers(String backendId, Course course) {

		int radius = SettingsResource.get()//
				.load(AppConfigurationSettings.class)//
						.newCourseRequestDriverPushRadiusInMeters;

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("status", "working"))//
				.must(QueryBuilders.termsQuery("vehicule.type", //
						compatibleVehiculeTypes(course.requestedVehiculeType)));

		GeoDistanceSortBuilder sort = SortBuilders.geoDistanceSort("lastLocation.where")//
				.point(course.from.geopoint.lat, course.from.geopoint.lon)//
				.order(SortOrder.ASC).unit(DistanceUnit.METERS).sortMode("min");

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "driver").setQuery(query).setSize(5)//
				.setFetchSource(false).addField(CREDENTIALS_ID)//
				.addSort(sort).get();

		List<String> credentialsIds = new ArrayList<>(5);
		for (SearchHit hit : response.getHits().hits()) {
			if (distance(hit) > radius)
				break;
			credentialsIds.add(hit.field(CREDENTIALS_ID).getValue());
		}

		return credentialsIds;
	}

	private double distance(SearchHit hit) {
		return (double) hit.sortValues()[0];
	}

	private PushLog pushToCustomer(String courseId, //
			Course course, Credentials credentials) {

		// search for installations
		SearchResponse response = searchInstallations(credentials.backendId(), //
				Lists.newArrayList(course.customer.credentialsId));

		// push message to drivers
		ObjectNode message = messageToCustomer(courseId);

		PushLog pushLog = new PushLog();
		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		return pushLog;
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

		throw Exceptions.illegalArgument("invalid vehicule type [%s]", requestedVehiculeType);
	}

	private SearchResponse searchInstallations(String backendId, List<String> credentialsIds) {

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termsQuery("tags.value", credentialsIds));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "installation")//
				.setQuery(query)//
				// each driver might have more than one installation
				// when driver app is reinstalled
				.setSize(100)//
				.get();

		return response;
	}

	private void textOperatorDriverHasGivenUp(String courseId, Course course, Credentials credentials) {

		StringBuilder builder = new StringBuilder("Le chauffeur [")//
				.append(credentials.name())//
				.append("] a renoncé à la course du client [")//
				.append(course.customer.firstname).append(" ")//
				.append(course.customer.lastname)//
				.append("]. La course a été proposée à d'autres chauffeurs.");

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private String operatorPhoneNumber() {
		return SettingsResource.get().load(AppConfigurationSettings.class)//
				.operatorPhoneNumber;
	}

	private void checkAuthorizedToCreateCourse(Credentials credentials) {
		// only customers are allowed to post courses
		Set<String> roles = credentials.roles();
		if (!roles.contains(Credentials.USER) || roles.size() > 1)
			throw Exceptions.insufficientCredentials(credentials);
	}

	private void checkAuthorizedToRemoveDriver(//
			Credentials credentials, String courseId, Course course) {

		if (course.driver == null || course.driver.credentialsId == null)
			throw Exceptions.illegalArgument(//
					"no driver to delete in course [%s]", courseId);

		if (credentials.roles().contains(Credentials.ADMIN))
			return;

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
