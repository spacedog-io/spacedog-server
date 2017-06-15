package io.spacedog.services.caremen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import io.spacedog.core.Json8;
import io.spacedog.jobs.Internals;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.PushService;
import io.spacedog.services.DataResource;
import io.spacedog.services.DataStore;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.PushResource;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.Resource;
import io.spacedog.services.SettingsResource;
import io.spacedog.services.SmsResource;
import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.services.SpaceContext;
import io.spacedog.services.Start;
import io.spacedog.services.caremen.Course.CourseDriver;
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

	private static final String CANCELLED = "cancelled";
	private static final String PASSENGER_APP_ID_SUFFIX = "passenger";
	private static final String DRIVER_APP_ID_SUFFIX = "driver";
	// status values

	private static final String NEW_IMMEDIATE = "new-immediate";
	private static final String NEW_SCHEDULED = "new-scheduled";
	private static final String SCHEDULED_ASSIGNED = "scheduled-assigned";
	private static final String DRIVER_IS_COMING = "driver-is-coming";
	private static final String READY_TO_LOAD = "ready-to-load";
	private static final String IN_PROGRESS = "in-progress";
	private static final String COMPLETED = "completed";

	// others

	private static final String STATUS = "status";
	private static final String CREDENTIALS_ID = "credentialsId";
	private static final String VERSION = "version";
	private static final String ID = "id";
	private static final String COURSE = "course";

	//
	// Routes
	//

	@Post("/1/service/course")
	@Post("/1/service/course/")
	public Payload postCourse(String body, Context context) {

		Credentials credentials = checkCustomerCredentials();

		PushLog driverPushLog = null;
		int httpStatus = HttpStatus.CREATED;

		Course course = createNewCourse(body, context);
		course.check(NEW_IMMEDIATE, NEW_SCHEDULED);

		if (course.requestedPickupTimestamp == null) {
			driverPushLog = pushToClosestDrivers(course, credentials);

			if (driverPushLog.successes == 0) {
				course.meta.version = setStatusToNoDriverAvailable(course.meta.id, credentials);
				httpStatus = HttpStatus.NOT_FOUND;
			}

			textOperatorNewImmediate(course, driverPushLog.successes);

		} else
			textOperatorNewScheduled(course);

		return createPayload(httpStatus, course, driverPushLog, null);
	}

	@Delete("/1/service/course/:id/driver")
	@Delete("/1/service/course/:id/driver")
	public Payload deleteCourseDriver(String courseId, String body, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		Course course = getCourse(courseId, credentials);
		course.check(DRIVER_IS_COMING, READY_TO_LOAD);
		checkIfAuthorizedToRemoveDriver(course, credentials);

		course = removeDriverFromCourse(course, credentials);
		PushLog driverPushLog = pushToClosestDrivers(course, credentials);
		PushLog customerPushLog = pushDriverHasGivenUpToCustomer(course, credentials);
		textOperatorDriverHasGivenUp(course, credentials);

		return createPayload(HttpStatus.OK, course, driverPushLog, customerPushLog);
	}

	@Post("/1/service/course/:id/_driver_is_coming")
	@Post("/1/service/course/:id/_driver_is_coming")
	public Payload postDriverIsComing(String courseId, String body, Context context) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.check(NEW_IMMEDIATE, SCHEDULED_ASSIGNED);
		course.status = DRIVER_IS_COMING;
		course.driverIsComingTimestamp = DateTime.now();
		course.driver = new CourseDriver(getDriver(credentials));
		saveCourse(course, credentials);

		saveCourseLogs(courseId, body, context);
		pushDriverIsComingToCustumer(course, credentials);
		return createPayload(course);
	}

	@Post("/1/service/course/:id/_ready_to_load")
	@Post("/1/service/course/:id/_ready_to_load")
	public Payload postReadyToLoad(String courseId, String body, Context context) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.check(DRIVER_IS_COMING);
		course.checkDriver(credentials.id());
		course.status = READY_TO_LOAD;
		course.driverIsReadyToLoadTimestamp = DateTime.now();
		saveCourse(course, credentials);

		saveCourseLogs(courseId, body, context);
		pushReadyToLoadToCustomer(course, credentials);
		return createPayload(course);
	}

	@Post("/1/service/course/:id/_in_progress")
	@Post("/1/service/course/:id/_in_progress")
	public Payload postInProgress(String courseId, String body, Context context) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.check(READY_TO_LOAD);
		course.checkDriver(credentials.id());
		course.status = IN_PROGRESS;
		course.pickupTimestamp = DateTime.now();
		saveCourse(course, credentials);

		saveCourseLogs(courseId, body, context);
		pushInProgressToCustomer(course, credentials);
		return createPayload(course);
	}

	@Post("/1/service/course/:id/_completed")
	@Post("/1/service/course/:id/_completed")
	public Payload postCompleted(String courseId, String body, Context context) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.check(IN_PROGRESS);
		course.checkDriver(credentials.id());

		CourseLog[] courseLogs = saveCourseLogs(courseId, body, context);

		course.status = COMPLETED;
		course.dropoffTimestamp = extractDropoffTimestamp(courseLogs);
		saveCourse(course, credentials);

		pushCompletedToCustomer(course, credentials);
		return createPayload(course);
	}

	@Post("/1/service/course/:id/_cancelled")
	@Post("/1/service/course/:id/_cancelled")
	public Payload postCancel(String courseId) {

		Credentials credentials = checkCustomerCredentials();

		Course course = getCourse(courseId, credentials);
		course.check(NEW_IMMEDIATE, NEW_SCHEDULED, //
				SCHEDULED_ASSIGNED, DRIVER_IS_COMING, READY_TO_LOAD);
		course.status = CANCELLED;
		course.cancelledTimestamp = DateTime.now();
		saveCourse(course, credentials);

		pushCancelledToDriver(course, credentials);
		return createPayload(course);
	}

	//
	// Implementation
	//

	private Course createNewCourse(String body, Context context) {

		Course course = readAndCheckCourse(body);
		Payload payload = DataResource.get().post(COURSE, body, context);
		course.meta = new Course.Meta();
		ObjectNode courseResponse = (ObjectNode) payload.rawContent();
		course.meta.id = courseResponse.get(ID).asText();
		course.meta.version = courseResponse.get(VERSION).asLong();
		return course;
	}

	private Course readAndCheckCourse(String body) {

		Course course = Json7.toPojo(body, Course.class);

		if (course.status.equals(NEW_SCHEDULED))
			Check.notNull(course.requestedPickupTimestamp, "requestedPickupTimestamp");
		else
			Check.isTrue(course.status.equals(NEW_IMMEDIATE), //
					"invalid course request status [%s]", course.status);

		return course;
	}

	private static final CourseLog[] NO_COURSE_LOGS = new CourseLog[0];

	private CourseLog[] saveCourseLogs(String courseId, String body, Context context) {
		if (!Strings.isNullOrEmpty(body)) {

			try {
				CourseLog[] courseLogs = Json8.toPojo(body, CourseLog[].class);
				for (CourseLog courseLog : courseLogs)
					DataResource.get().post("courselog", //
							Json8.toString(courseLog), context);

				return courseLogs;

			} catch (Throwable t) {

				Internals.get().notify(//
						Start.get().configuration()//
								.superdogAwsNotificationTopic().orElse(null), //
						String.format("Error saving logs of course [%s]", //
								courseId), //
						Throwables.getStackTraceAsString(t));
			}
		}

		return NO_COURSE_LOGS;
	}

	private DateTime extractDropoffTimestamp(CourseLog[] courseLogs) {
		for (CourseLog courseLog : courseLogs)
			if (COMPLETED.equals(courseLog.status))
				return courseLog.when;
		return DateTime.now();
	}

	private Driver getDriver(Credentials credentials) {
		TermQueryBuilder query = QueryBuilders.termQuery("credentialsId", credentials.id());

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(credentials.backendId(), "driver")//
				.setQuery(query)//
				.setSize(1)//
				.get();

		if (response.getHits().getTotalHits() != 1)
			throw Exceptions.illegalArgument("credentials [%s] has more than one driver", credentials.name());

		SearchHit hit = response.getHits().getHits()[0];
		Driver driver = Json7.toPojo(hit.sourceAsString(), Driver.class);
		driver.id = hit.id();
		return driver;
	}

	private Credentials checkDriverCredentials() {
		Credentials credentials = SpaceContext.checkUserCredentials();
		if (!credentials.roles().contains("driver"))
			throw Exceptions.forbidden(//
					"credentials [%s] not a driver", credentials.name());
		return credentials;
	}

	private Course getCourse(String courseId, Credentials credentials) {
		GetResponse response = Start.get().getElasticClient()//
				.get(credentials.backendId(), COURSE, courseId);

		Course course = Json7.toPojo(response.getSourceAsString(), Course.class);
		course.meta.id = response.getId();
		course.meta.version = response.getVersion();

		return course;
	}

	private long saveCourse(Course course, Credentials credentials) {
		return Start.get().getElasticClient()//
				.prepareUpdate(credentials.backendId(), COURSE, course.meta.id)//
				.setDoc(Json8.toString(course))//
				.setVersion(course.meta.version)//
				.get()//
				.getVersion();
	}

	private long setStatusToNoDriverAvailable(String courseId, Credentials credentials) {
		ObjectNode patch = Json8.object(STATUS, "no-driver-available");
		return DataStore.get()
				.patchObject(//
						credentials.backendId(), COURSE, courseId, //
						patch, credentials.name())//
				.getVersion();
	}

	private Course removeDriverFromCourse(Course course, Credentials credentials) {
		ObjectNode patch = Json8.object("driver", null, STATUS, NEW_IMMEDIATE);
		course.meta.version = DataStore.get()
				.patchObject(//
						credentials.backendId(), COURSE, course.meta.id, //
						patch, credentials.name())//
				.getVersion();
		course.status = NEW_IMMEDIATE;
		course.driver = null;
		return course;
	}

	//
	// text messages
	//

	private static DateTimeZone parisZone = DateTimeZone.forID("Europe/Paris");

	private static DateTimeFormatter pickupFormatter = DateTimeFormat//
			.forPattern("dd/MM' à 'HH'h'mm").withZone(parisZone).withLocale(Locale.FRENCH);

	private void textOperatorNewScheduled(Course course) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course programmée pour le ")//
				.append(pickupFormatter.print(course.requestedPickupTimestamp)) //
				.append(". Départ : ").append(course.from.address)//
				.append(". Catégorie : ").append(course.requestedVehiculeType)//
				.append(".");

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private void textOperatorNewImmediate(Course course, int notifications) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course immédiate. Départ : ")//
				.append(course.from.address).append(". Catégorie : ")//
				.append(course.requestedVehiculeType).append(". Chauffeurs notifiés : ")//
				.append(notifications);

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private void textOperatorDriverHasGivenUp(Course course, Credentials credentials) {

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

	//
	// push to drivers
	//

	private PushLog pushToClosestDrivers(Course course, Credentials credentials) {

		// search for drivers
		List<String> credentialsIds = searchDrivers(//
				credentials.backendId(), course);

		// if requester is a driver, he does not need the push
		if (credentials.roles().contains("driver"))
			credentialsIds.remove(credentials.id());

		// search for installations
		SearchResponse response = searchInstallations(//
				credentials.backendId(), DRIVER_APP_ID_SUFFIX, credentialsIds);

		// push message to drivers
		Optional<Alert> alert = Alert.of("Demande de course immédiate", //
				"Un client vient de commander une course immédiate", //
				"newImmediate.wav");

		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE, alert);

		PushLog pushLog = new PushLog();
		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		return pushLog;
	}

	private List<String> searchDrivers(String backendId, Course course) {

		int radius = SettingsResource.get()//
				.load(AppConfigurationSettings.class)//
						.newCourseRequestDriverPushRadiusInMeters;

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(STATUS, "working"))//
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

	private SearchResponse searchInstallations(String backendId, String appIdSuffix, List<String> credentialsIds) {

		// appId is caremendriver or caremenpassenger for prod and dev env
		// appId is carerec-driver or carerec-passenger for recette env
		String appId = (backendId.equals("carerec") ? "carerec-" : "caremen") //
				+ appIdSuffix;

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termsQuery("tags.value", credentialsIds))//
				.must(QueryBuilders.termQuery("appId", appId));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "installation")//
				.setQuery(query)//
				// user might have more than one installation
				// when app is reinstalled or if installed on more
				// than one device
				.setSize(100)//
				.get();

		return response;
	}

	private JsonBuilder<ObjectNode> toJsonPayloadBuilder(int httpStatus, Course course) {

		return JsonPayload.builder(httpStatus)//
				.put(ID, course.meta.id)//
				.put(VERSION, course.meta.version)//
				.node("course", Json8.toNode(course));
	}

	private Payload createPayload(Course course) {

		return JsonPayload.json(//
				toJsonPayloadBuilder(HttpStatus.OK, course), //
				HttpStatus.OK);
	}

	private Payload createPayload(int httpStatus, Course course, //
			PushLog driverPushLog, PushLog customerPushLog) {

		JsonBuilder<ObjectNode> builder = toJsonPayloadBuilder(httpStatus, course);

		if (driverPushLog != null)
			builder.node("driverPushLog", driverPushLog.logItems);

		if (customerPushLog != null)
			builder.node("customerPushLog", customerPushLog.logItems);

		return JsonPayload.json(builder, httpStatus);
	}

	//
	// push to customer
	//

	private PushLog pushDriverHasGivenUpToCustomer(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE,
				Alert.of("Chauffeur indisponible", //
						"Votre chauffeur a rencontré un problème et ne peut pas vous rejoindre."
								+ " Nous recherchons un autre chauffeur.",
						"default"));
		return pushToCustomer(course, message, credentials);
	}

	private void pushDriverIsComingToCustumer(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder("Votre chauffeur arrivera à ")//
				.append(course.from.address).append(" dans quelques instants");
		ObjectNode message = toPushMessage(course.meta.id, DRIVER_IS_COMING, //
				Alert.of("Votre chauffeur est en route", body.toString(), "default"));
		pushToCustomer(course, message, credentials);
	}

	private void pushReadyToLoadToCustomer(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder("Votre chauffeur est arrivé à ")//
				.append(course.from.address);
		ObjectNode message = toPushMessage(course.meta.id, READY_TO_LOAD, //
				Alert.of("Votre chauffeur vous attend", body.toString(), "readyToLoad.wav"));
		pushToCustomer(course, message, credentials);
	}

	private void pushInProgressToCustomer(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, IN_PROGRESS, Alert.empty());
		pushToCustomer(course, message, credentials);
	}

	private void pushCompletedToCustomer(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, COMPLETED, Alert.empty());
		pushToCustomer(course, message, credentials);
	}

	private void pushCancelledToDriver(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, CANCELLED, //
				Alert.of("Course annulée", "Le client a annulé la course", "cancelled.wav"));
		pushToDriver(course, message, credentials);
	}

	private static class Alert {
		String title;
		String body;
		String sound;

		static Optional<Alert> of(String title, String body, String sound) {
			Alert alert = new Alert();
			alert.title = title;
			alert.body = body;
			alert.sound = sound;
			return Optional.of(alert);
		}

		static Optional<Alert> empty() {
			return Optional.empty();
		}

	}

	//
	// Push message
	//

	private ObjectNode toPushMessage(String courseId, //
			String type, Optional<Alert> alert) {

		ObjectNode apsMessage = apsMessage(courseId, type, alert);
		ObjectNode gcmMessage = gcmMessage(courseId, type, alert);

		return Json8.objectBuilder()//
				.node(PushService.APNS_SANDBOX.name(), apsMessage)//
				.node(PushService.APNS.name(), apsMessage)//
				.node(PushService.GCM.name(), gcmMessage)//
				.build();
	}

	private ObjectNode apsMessage(String courseId, String type, Optional<Alert> alert) {
		JsonBuilder<ObjectNode> builder = Json8.objectBuilder()//
				.put(ID, courseId)//
				.put("type", type)//
				.object("aps")//
				.put("content-available", 1);

		if (alert.isPresent())
			builder.put("sound", alert.get().sound)//
					.object("alert")//
					.put("title", alert.get().title)//
					.put("body", alert.get().body);

		return builder.build();
	}

	private ObjectNode gcmMessage(String courseId, String type, Optional<Alert> alert) {
		JsonBuilder<ObjectNode> builder = Json8.objectBuilder()//
				.object("data")//
				.put(ID, courseId)//
				.put("type", type);

		if (alert.isPresent())
			builder.put("title", alert.get().title)//
					.put("body", alert.get().body);

		return builder.build();
	}

	private PushLog pushToCustomer(Course course, ObjectNode message, Credentials credentials) {

		SearchResponse response = searchInstallations(credentials.backendId(), //
				PASSENGER_APP_ID_SUFFIX, Lists.newArrayList(course.customer.credentialsId));

		return pushTo(course.customer.credentialsId, "customer", //
				response, message, credentials);
	}

	private PushLog pushToDriver(Course course, ObjectNode message, Credentials credentials) {

		if (course.driver == null || course.driver.credentialsId == null)
			return new PushLog();

		SearchResponse response = searchInstallations(credentials.backendId(), //
				DRIVER_APP_ID_SUFFIX, Lists.newArrayList(course.driver.credentialsId));

		return pushTo(course.driver.credentialsId, "driver", //
				response, message, credentials);
	}

	private PushLog pushTo(String credentialsId, String type, //
			SearchResponse response, ObjectNode message, Credentials credentials) {

		if (response.getHits().getTotalHits() == 0)
			throw Exceptions.illegalArgument(//
					"no installation found for %s with credentials id [%s]", //
					type, credentialsId);

		PushLog pushLog = new PushLog();
		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		if (pushLog.successes == 0)
			throw Exceptions.space(400, "failed to push to %s", type)//
					.details(pushLog.toNode());

		return pushLog;
	}

	private Credentials checkCustomerCredentials() {
		Credentials credentials = SpaceContext.checkUserCredentials();
		Set<String> roles = credentials.roles();
		if (!roles.contains(Credentials.USER) || roles.size() > 1)
			throw Exceptions.insufficientCredentials(credentials);
		return credentials;
	}

	private void checkIfAuthorizedToRemoveDriver(Course course, Credentials credentials) {

		if (course.driver == null || course.driver.credentialsId == null)
			throw Exceptions.illegalState(//
					"no driver to delete in course [%s]", course.meta.id);

		if (credentials.roles().contains(Credentials.ADMIN))
			return;

		if (credentials.roles().contains("driver") //
				&& !credentials.id().equals(course.driver.credentialsId))
			throw Exceptions.forbidden(//
					"you are not the driver of course [%s]", course.meta.id);
	}

	//
	// singleton
	//

	private static CaremenResource singleton = new CaremenResource();

	public static CaremenResource get() {
		return singleton;
	}

	private CaremenResource() {
	}
}
