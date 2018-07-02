package io.spacedog.tutorials;

import java.util.Collections;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataResults;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.elastic.ESBoolQueryBuilder;
import io.spacedog.client.elastic.ESDistanceUnit;
import io.spacedog.client.elastic.ESGeoDistanceSortBuilder;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortBuilders;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.client.push.PushRequest;
import io.spacedog.client.push.PushSettings;
import io.spacedog.client.schema.GeoPoint;
import io.spacedog.client.sms.SmsTemplate;
import io.spacedog.client.sms.SmsTemplateRequest;
import io.spacedog.utils.Json;

public class T3_RequestDriver extends DemoBase {

	@Test
	public void customerCreatesCourse() {

		SpaceDog david = david();

		Course source = new Course();
		source.status = "new-immediate";
		source.requestedPickupTimestamp = DateTime.now();
		source.requestedVehiculeType = "premium";

		source.customer = new Course.Customer();
		source.customer.firstname = david.username();
		source.customer.credentialsId = david.id();

		source.from = new Course.Location();
		source.from.address = "7 avenue du Chateau 92190 Meudon";
		source.from.geopoint = new GeoPoint(48.816673, 2.230358);

		source.to = new Course.Location();
		source.to.address = "Aéroport Paris Charles de Gaulle";
		source.to.geopoint = new GeoPoint(49.008649, 2.548504);

		source.payment = new Course.Payment();
		source.payment.companyId = "LHJKBVCJFCFCK";
		source.payment.companyName = "SpaceDog";

		DataWrap<Course> course = DataWrap.wrap(source)//
				.id("myCourse");

		course = david.data().save(course);
	}

	@Test
	public void superadminSetsPushSettings() {
		PushSettings settings = new PushSettings();
		settings.authorizedRoles = Sets.newHashSet(Roles.superadmin);
		superadmin().settings().save(settings);
	}

	@Test
	public void systemPushesNotificationToNearbyDrivers() {

		SpaceDog superadmin = superadmin();
		DataWrap<Course> course = course();

		// system searches for nearby drivers

		ESBoolQueryBuilder query = ESQueryBuilders.boolQuery()//
				.must(ESQueryBuilders.termQuery("status", "working"))//
				.must(ESQueryBuilders.termsQuery("vehicule.type", //
						course.source().requestedVehiculeType))//
				.must(ESQueryBuilders.rangeQuery("lastLocation.when")//
						.gt("now-30m"));

		ESGeoDistanceSortBuilder sort = ESSortBuilders.geoDistanceSort("lastLocation.where")//
				.point(course.source().from.geopoint.lat, course.source().from.geopoint.lon)//
				.order(ESSortOrder.ASC)//
				.unit(ESDistanceUnit.METERS)//
				.sortMode("min");

		DataResults<Driver> drivers = superadmin.data().prepareSearch().type("driver")//
				.source(ESSearchSourceBuilder.searchSource().query(query).sort(sort).size(5).toString())//
				.go(Driver.class);

		// system pushes notifications to nearby drivers

		ObjectNode apsMessage = Json.object("id", course.id(), "type", "new-immediate", //
				"aps", Json.object("content-available", 1, "sound", "newImmediate.wav", //
						"alert", Json.object("title", "Demande de course immédiate", //
								"body", "Un client vient de commander une course immédiate")));

		ObjectNode gcmMessage = Json.object("data", Json.object("id", course.id(), //
				"type", "new-immediate", "title", "Demande de course immédiate", //
				"body", "Un client vient de commander une course immédiate"));

		ObjectNode message = Json.object("APNS", apsMessage, "GCM", gcmMessage);

		for (DataWrap<Driver> driver : drivers)
			superadmin.push().push(new PushRequest().appId("carerec-driver")//
					.credentialsId(driver.id())//
					.data(message));
	}

	@Test
	public void superadminCreatesWelcomeSmsTemplate() {

		SpaceDog superadmin = superadmin();

		SmsTemplate smsTemplate = new SmsTemplate();
		smsTemplate.name = "Welcome";
		smsTemplate.to = "{{customer.phone}}";
		smsTemplate.body = "{{customer.firstname}}, welcome to Caremen";
		smsTemplate.model = Maps.newHashMap();
		smsTemplate.model.put("customer", "customer");
		smsTemplate.roles = Collections.singleton("superadmin");

		superadmin.sms().saveTemplate(smsTemplate);

	}

	@Test
	public void systemSendsWelcomeSms() {

		SpaceDog superadmin = superadmin();

		SmsTemplateRequest smsTemplateRequest = new SmsTemplateRequest();
		smsTemplateRequest.templateName = "welcome";
		smsTemplateRequest.parameters = Maps.newHashMap();
		smsTemplateRequest.parameters.put("customer", david().id());

		superadmin.sms().send(smsTemplateRequest);
	}

	@Test
	public void cashierChargesCustomersForCompletedCourse() {

		SpaceDog cashier = cashier();

		DataResults<Course> completedCourses = searchForCompletedCourses();

		for (DataWrap<Course> completed : completedCourses) {

			// cashier calculates time, distance and fare if necessary

			if (completed.source().fare == null) {

				completed.source().time = calculateTime(completed);
				completed.source().distance = calculateDistance(completed);
				completed.source().fare = calculateFare(completed);

				cashier.data().save(completed);
			}

			// cashier charges course customer's credit card

			Map<String, Object> params = Maps.newHashMap();
			params.put("amount", (int) (completed.source().fare * 100));
			params.put("currency", "eur");
			params.put("customer", completed.source().payment.stripe.customerId);
			params.put("source", completed.source().payment.stripe.cardId);
			params.put("description", //
					String.format("Course du [%s] à destination de [%s]", //
							completed.source().pickupTimestamp, completed.source().to.address));

			ObjectNode payment = cashier.stripe().charge(params);

			// cashier saves the course new state

			completed.source().payment.stripe.paymentId = payment.get("id").asText();
			completed.source().status = "billed";
			cashier.data().save(completed);
		}
	}

	private Double calculateFare(DataWrap<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private Long calculateDistance(DataWrap<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private Long calculateTime(DataWrap<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private DataResults<Course> searchForCompletedCourses() {
		// TODO Auto-generated method stub
		return null;
	}
}
