package io.spacedog.services.caremen;

import java.util.Set;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.core.Json8;
import io.spacedog.jobs.Internals;
import io.spacedog.services.DataResource;
import io.spacedog.services.DataStore;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.Resource;
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

public class CaremenResource extends Resource implements CourseStatus {

	private NotifyCustomer notifyCustomer;
	private NotifyDriver notifyDriver;
	private NotifyOperator notifyOperator;

	public static final String STATUS = "status";
	public static final String CREDENTIALS_ID = "credentialsId";

	//
	//
	// Routes
	//

	@Post("/1/service/course")
	@Post("/1/service/course/")
	public Payload postCourse(String body, Context context) {

		Credentials credentials = checkCustomerCredentials();

		PushLog driverPushLog = null;
		int httpStatus = HttpStatus.CREATED;

		Course course = createNewCourse(body, credentials);
		course.check(NEW_IMMEDIATE, NEW_SCHEDULED);

		if (course.requestedPickupTimestamp == null) {
			driverPushLog = notifyDriver.newImmediate(course, credentials);

			if (driverPushLog.successes == 0) {
				course.meta.version = setStatusToNoDriverAvailable(course.meta.id, credentials);
				httpStatus = HttpStatus.NOT_FOUND;
			}

			notifyOperator.newImmediate(course, driverPushLog.successes);

		} else
			notifyOperator.newScheduled(course);

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
		PushLog driverPushLog = notifyDriver.newImmediate(course, credentials);
		PushLog customerPushLog = notifyCustomer.driverHasGivenUp(course, credentials);
		notifyOperator.driverHasGivenUp(course, credentials);

		saveCourseLogs(courseId, body, context);
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
		PushLog customerPushLog = notifyCustomer.driverIsComing(course, credentials);
		return createPayload(HttpStatus.OK, course, null, customerPushLog);
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
		PushLog customerPushLog = notifyCustomer.readyToLoad(course, credentials);
		return createPayload(HttpStatus.OK, course, null, customerPushLog);
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
		PushLog customerPushLog = notifyCustomer.inProgress(course, credentials);
		return createPayload(HttpStatus.OK, course, null, customerPushLog);
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

		PushLog customerPushLog = notifyCustomer.completed(course, credentials);
		return createPayload(HttpStatus.OK, course, null, customerPushLog);
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

		PushLog driverPushLog = notifyDriver.cancelled(course, credentials);
		return createPayload(HttpStatus.OK, course, driverPushLog, null);
	}

	//
	// Implementation
	//

	private Course createNewCourse(String body, Credentials requester) {

		Course course = readAndCheckCourse(body);
		DataStore.get().createObject("course", course, requester);
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
				.prepareSearch(credentials.backendId(), Driver.TYPE)//
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
		if (!credentials.roles().contains(Driver.TYPE))
			throw Exceptions.forbidden(//
					"credentials [%s] not a driver", credentials.name());
		return credentials;
	}

	private Course getCourse(String courseId, Credentials credentials) {
		GetResponse response = Start.get().getElasticClient()//
				.get(credentials.backendId(), Course.TYPE, courseId);

		Course course = Json7.toPojo(response.getSourceAsString(), Course.class);
		course.meta.id = response.getId();
		course.meta.type = "course";
		course.meta.version = response.getVersion();

		return course;
	}

	private void saveCourse(Course course, Credentials credentials) {
		DataStore.get().updateObject(course, credentials);
	}

	private long setStatusToNoDriverAvailable(String courseId, Credentials credentials) {
		ObjectNode patch = Json8.object(STATUS, "no-driver-available");
		return DataStore.get().patchObject(//
				credentials.backendId(), Course.TYPE, courseId, //
				patch, credentials.name())//
				.getVersion();
	}

	private Course removeDriverFromCourse(Course course, Credentials credentials) {
		ObjectNode patch = Json8.object("driver", null, STATUS, NEW_IMMEDIATE);
		course.meta.version = DataStore.get().patchObject(//
				credentials.backendId(), Course.TYPE, course.meta.id, //
				patch, credentials.name())//
				.getVersion();
		course.status = NEW_IMMEDIATE;
		course.driver = null;
		return course;
	}

	private Payload createPayload(int httpStatus, Course course, //
			PushLog driverPushLog, PushLog customerPushLog) {

		JsonBuilder<ObjectNode> builder = JsonPayload.builder(httpStatus)//
				.put("id", course.meta.id)//
				.put("version", course.meta.version)//
				.node("course", Json8.toNode(course));

		if (driverPushLog != null)
			builder.node("driverPushLog", driverPushLog.logItems);

		if (customerPushLog != null)
			builder.node("customerPushLog", customerPushLog.logItems);

		return JsonPayload.json(builder, httpStatus);
	}

	private Credentials checkCustomerCredentials() {
		Credentials credentials = SpaceContext.checkUserCredentials();
		Set<String> roles = credentials.roles();
		// assistants are authorized
		if (roles.contains("assistant"))
			return credentials;
		// simple users (customers) are authorized
		if (roles.contains(Credentials.USER) && roles.size() == 1)
			return credentials;
		// all other
		throw Exceptions.insufficientCredentials(credentials);
	}

	private void checkIfAuthorizedToRemoveDriver(Course course, Credentials credentials) {

		if (course.driver == null || course.driver.credentialsId == null)
			throw Exceptions.illegalState(//
					"no driver to delete in course [%s]", course.meta.id);

		if (credentials.roles().contains(Credentials.ADMIN))
			return;

		if (credentials.roles().contains(Driver.TYPE) //
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
		notifyCustomer = new NotifyCustomer();
		notifyDriver = new NotifyDriver();
		notifyOperator = new NotifyOperator();
	}
}
