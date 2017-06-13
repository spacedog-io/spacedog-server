package io.spacedog.services.caremen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
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
import com.google.common.collect.Lists;

import io.spacedog.core.Json8;
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
		Course course = getCourse(courseId, credentials);
		checkAuthorizedToRemoveDriver(credentials, courseId, course);

		long courseVersion = removeDriverFromCourse(courseId, credentials);
		PushLog driverPushLog = pushToClosestDrivers(courseId, course, credentials);
		PushLog customerPushLog = pushDriverHasGivenUpToCustomer(course, credentials);
		textOperatorDriverHasGivenUp(courseId, course, credentials);

		return createPayload(HttpStatus.OK, //
				courseId, courseVersion, driverPushLog, customerPushLog);
	}

	@Post("/1/service/course/:id/_driver_is_coming")
	@Post("/1/service/course/:id/_driver_is_coming")
	public Payload postDriverIsComing(String courseId, String body, Context context) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.checkStatus(NEW_IMMEDIATE, SCHEDULED_ASSIGNED);
		course.status = DRIVER_IS_COMING;
		course.driverIsComingTimestamp = DateTime.now();
		course.driver = new CourseDriver(getDriver(credentials));
		saveCourse(course, credentials);

		pushDriverIsComingToCustumer(course, credentials);
		return JsonPayload.success();
	}

	@Post("/1/service/course/:id/_ready_to_load")
	@Post("/1/service/course/:id/_ready_to_load")
	public Payload postReadyToLoad(String courseId) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.checkStatus(DRIVER_IS_COMING);
		course.checkDriver(credentials.id());
		course.status = READY_TO_LOAD;
		course.driverIsReadyToLoadTimestamp = DateTime.now();
		saveCourse(course, credentials);

		pushReadyToLoadToCustomer(course, credentials);
		return JsonPayload.success();
	}

	@Post("/1/service/course/:id/_in_progress")
	@Post("/1/service/course/:id/_in_progress")
	public Payload postInProgress(String courseId) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.checkStatus(READY_TO_LOAD);
		course.checkDriver(credentials.id());
		course.status = IN_PROGRESS;
		course.pickupTimestamp = DateTime.now();
		saveCourse(course, credentials);

		pushInProgressToCustomer(course, credentials);
		return JsonPayload.success();
	}

	@Post("/1/service/course/:id/_completed")
	@Post("/1/service/course/:id/_completed")
	public Payload postCompleted(String courseId, String body) {

		Credentials credentials = checkDriverCredentials();

		Course course = getCourse(courseId, credentials);
		course.checkStatus(IN_PROGRESS);
		course.checkDriver(credentials.id());
		course.status = COMPLETED;
		course.dropoffTimestamp = DateTime.now();
		saveCourse(course, credentials);

		pushCompletedToCustomer(course, credentials);
		return JsonPayload.success();
	}

	@Post("/1/service/course/:id/_cancelled")
	@Post("/1/service/course/:id/_cancelled")
	public Payload postCancel(String courseId) {

		Credentials credentials = checkCustomerCredentials();

		Course course = getCourse(courseId, credentials);
		course.checkStatus(NEW_IMMEDIATE, NEW_SCHEDULED, //
				SCHEDULED_ASSIGNED, DRIVER_IS_COMING, READY_TO_LOAD);
		course.status = CANCELLED;
		course.cancelledTimestamp = DateTime.now();
		saveCourse(course, credentials);

		pushCancelledToDriver(course, credentials);
		return JsonPayload.success();
	}

	//
	// Implementation
	//

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

	private long removeDriverFromCourse(String courseId, Credentials credentials) {
		ObjectNode patch = Json8.object("driver", null, STATUS, NEW_IMMEDIATE);
		return DataStore.get()
				.patchObject(//
						credentials.backendId(), COURSE, courseId, //
						patch, credentials.name())//
				.getVersion();
	}

	//
	// text messages
	//

	private static DateTimeZone parisZone = DateTimeZone.forID("Europe/Paris");

	private static DateTimeFormatter pickupFormatter = DateTimeFormat//
			.forPattern("dd/MM' à 'HH'h'mm").withZone(parisZone).withLocale(Locale.FRENCH);

	private void textOperatorNewScheduled(Course course, String courseId) {
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

	private void textOperatorNewImmediate(//
			Course course, String courseId, int notifications) {
		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course immédiate. Départ : ")//
				.append(course.from.address).append(". Catégorie : ")//
				.append(course.requestedVehiculeType).append(". Chauffeurs notifiés : ")//
				.append(notifications);

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
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

	//
	// push to drivers
	//

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
				credentials.backendId(), DRIVER_APP_ID_SUFFIX, credentialsIds);

		// push message to drivers
		Optional<Alert> alert = Alert.of("Demande de course immédiate", //
				"Un client vient de commander une course immédiate", //
				"newImmediate.wav");

		ObjectNode message = toPushMessage(courseId, NEW_IMMEDIATE, alert);

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

	//
	// push to customer
	//

	private PushLog pushDriverHasGivenUpToCustomer(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE,
				Alert.of("Chauffeur indisponible", //
						"Votre chauffeur a rencontré un problème	et ne peut pas vous rejoindre."
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

		if (course.customer == null || course.customer.credentialsId == null)
			throw Exceptions.illegalArgument("course has invalid customer data");

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

	// private String checkCustomerCredentialsId(ObjectNode course) {
	// JsonNode node = Json8.get(course, "customer.credentialsId");
	// if (Json8.isNull(node))
	// throw Exceptions.illegalArgument("course has invalid customer data");
	// return node.asText();
	// }

	// private String checkCustomerCredentialsId(Course course) {
	// if (course == null || course.customer == null //
	// || course.customer.credentialsId == null)
	// throw Exceptions.illegalArgument("course has invalid customer data");
	// return course.customer.credentialsId;
	// }

	// private ObjectNode messageToCustomer(String courseId) {
	//
	// ObjectNode apsMessage = Json8.objectBuilder()//
	// .put(ID, courseId)//
	// .put("type", NEW_IMMEDIATE)//
	// .object("aps")//
	// .put("content-available", 1)//
	// .put("sound", "default")//
	// .object("alert")//
	// .put("title", "Chauffeur indisponible")//
	// .put("body",
	// "Votre chauffeur a rencontré un problème et ne peut pas vous rejoindre."
	// + " Nous recherchons un autre chauffeur.")//
	// .build();
	//
	// return Json8.objectBuilder()//
	// .node(PushService.APNS_SANDBOX.name(), apsMessage)//
	// .node(PushService.APNS.name(), apsMessage)//
	// .build();
	// }

	// private JsonNode toCourseDriver(Driver driver) {
	// Course.Driver courseDriver = new Course.Driver();
	// courseDriver.driverId = driver.id;
	// courseDriver.credentialsId = driver.credentialsId;
	// courseDriver.firstname = driver.firstname;
	// courseDriver.lastname = driver.lastname;
	// courseDriver.phone = driver.phone;
	// courseDriver.photo = driver.photo;
	// courseDriver.vehicule = driver.vehicule;
	// return Json8.toNode(courseDriver);
	// }

	// private void checkStatus(ObjectNode course, String... statuses) {
	// if (course == null || !course.hasNonNull(STATUS))
	// throw Exceptions.illegalArgument("course has no status");
	//
	// String status = course.get(STATUS).asText();
	// for (String checkedStatus : statuses)
	// if (checkedStatus.equals(status))
	// return;
	//
	// throw Exceptions.illegalArgument("incompatible course status [%s]",
	// status);
	// }

	// private ObjectNode getCourse(String courseId, Credentials credentials) {
	// return DataStore.get().getObject(credentials.backendId(), "course",
	// courseId);
	// }

	// private void saveCourse(ObjectNode course, Credentials credentials) {
	// DataStore.get().updateObject(credentials.backendId(), course,
	// credentials.name());
	// }

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
