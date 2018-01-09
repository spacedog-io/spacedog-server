package io.spacedog.demo;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import io.spacedog.client.PushRequest;
import io.spacedog.client.ShareEndpoint.ShareMeta;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESBoolQueryBuilder;
import io.spacedog.client.elastic.ESDistanceUnit;
import io.spacedog.client.elastic.ESGeoDistanceSortBuilder;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortBuilders;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.demo.Course.CourseDataObject;
import io.spacedog.demo.Customer.CustomerDataObject;
import io.spacedog.demo.Driver.DriverDataObject;
import io.spacedog.demo.Driver.Results;
import io.spacedog.http.SpaceEnv;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.DataObject;
import io.spacedog.model.EmailTemplate;
import io.spacedog.model.EmailTemplateRequest;
import io.spacedog.model.GeoPoint;
import io.spacedog.model.Permission;
import io.spacedog.model.Roles;
import io.spacedog.model.Schema;
import io.spacedog.model.SmsTemplate;
import io.spacedog.model.SmsTemplateRequest;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class GettingStarted {

	@Test
	public void superdogInitsBackend() {
		superdog().post("/1/admin/clear").go(200);
		superdog().credentials().create("superadmin", "hi superadmin", //
				"platform@spacedog.io", Roles.superadmin);
	}

	@Test
	public void superadminInitsCourseSchema() {

		URL url = Resources.getResource(getClass(), "course.schema.json");
		JsonNode node = Json.readNode(url);
		Schema courseSchema = new Schema("course", Json.checkObject(node));
		superadmin().schemas().set(courseSchema);

		// superadmin gives permissions to users on 'course' objects
		courseSchema.acl(Roles.user, Permission.create, Permission.updateMine, Permission.readMine);
		superadmin().schemas().set(courseSchema);
	}

	@Test
	public void superadminAuthorizesGuestsToSignUp() {
		CredentialsSettings settings = new CredentialsSettings();
		settings.guestSignUpEnabled = true;
		superadmin().settings().save(settings);
	}

	@Test
	public void customerSignsUp() {
		SpaceDog david = guest().credentials()//
				.create("david", "hi dave", "david@spacedog.io");

		Customer source = new Customer();
		source.firstname = "David";
		source.firstname = "Attias";
		source.phone = "+33123456789";

		DataObject<Customer> customer = new CustomerDataObject()//
				.id(david.id()).source(source);

		customer = david.data().save(customer);

		// customer uploads his picture

		byte[] picture = ClassResources.loadAsBytes(this, "mapommme.jpg");
		ShareMeta meta = david.shares().upload(picture);

		// customer saves his picture url to his customer object

		customer.source().photo = meta.location;
		customer = david.data().save(customer);
	}

	@Test
	public void superadminInitsWelcomeEmailTemplate() {

		EmailTemplate template = new EmailTemplate();
		template.name = "customer-welcome";
		template.from = "CAREMEN <no-reply@caremen.fr>";
		template.to = Lists.newArrayList("{{credentials.email}}");
		template.subject = "Bienvenue chez CAREMEN";
		template.html = ClassResources.loadAsString(this, "caremen-welcome.html");
		template.text = "Bienvenue à vous. Nous sommes heureux de pouvoir vous "
				+ "compter parmi les nouveaux utilisateurs de l’application CAREMEN.";
		template.model = Maps.newHashMap();
		template.model.put("credentials", "credentials");
		template.model.put("customer", "customer");
		template.authorizedRoles = Collections.singleton("superadmin");

		superadmin().emails().saveTemplate(template);
	}

	@Test
	public void systemSendsWelcomeEmail() {

		SpaceDog david = david();
		SpaceDog superadmin = superadmin();

		EmailTemplateRequest emailTemplateRequest = new EmailTemplateRequest();
		emailTemplateRequest.templateName = "customer-welcome";
		emailTemplateRequest.parameters = Maps.newHashMap();
		emailTemplateRequest.parameters.put("credentials", david.id());
		emailTemplateRequest.parameters.put("customer", david.id());

		superadmin.emails().send(emailTemplateRequest);

	}

	@Test
	public void superadminCreatesWelcomeSmsTemplate() {

		SmsTemplate smsTemplate = new SmsTemplate();
		smsTemplate.name = "Welcome";
		smsTemplate.to = "{{customer.phone}}";
		smsTemplate.body = "{{customer.firstname}}, welcome to Caremen";
		smsTemplate.model = Maps.newHashMap();
		smsTemplate.model.put("customer", "customer");
		smsTemplate.roles = Collections.singleton("superadmin");

		superadmin().sms().saveTemplate(smsTemplate);

	}

	@Test
	public void systemSendsWelcomeSms() {

		SmsTemplateRequest smsTemplateRequest = new SmsTemplateRequest();
		smsTemplateRequest.templateName = "welcome";
		smsTemplateRequest.parameters = Maps.newHashMap();
		smsTemplateRequest.parameters.put("customer", david().id());

		superadmin().sms().send(smsTemplateRequest);
	}

	@Test
	public void userRequestsCourse() {

		SpaceDog david = david();

		Course source = new Course();
		source.status = "new-immediate";
		source.requestedPickupTimestamp = DateTime.now();
		source.requestedVehiculeType = "green";

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

		DataObject<Course> course = new CourseDataObject().source(source);
		course = david.data().save(course);
	}

	@Test
	public void customerPushesNotificationToNearbyDrivers() {

		// customer searches for nearby drivers

		ESBoolQueryBuilder query = ESQueryBuilders.boolQuery()//
				.must(ESQueryBuilders.termQuery("status", "working"))//
				.must(ESQueryBuilders.termsQuery("vehicule.type", //
						compatibleVehiculeTypes(course().source().requestedVehiculeType)))//
				.must(ESQueryBuilders.rangeQuery("lastLocation.when")//
						.gt("now-30m"));

		ESGeoDistanceSortBuilder sort = ESSortBuilders.geoDistanceSort("lastLocation.where")//
				.point(course().source().from.geopoint.lat, course().source().from.geopoint.lon)//
				.order(ESSortOrder.ASC).unit(ESDistanceUnit.METERS).sortMode("min");

		Results drivers = david().data().searchRequest().type("driver")//
				.source(ESSearchSourceBuilder.searchSource().query(query).sort(sort).size(5))//
				.go(Driver.Results.class);

		// customer pushes notifications to nearby drivers

		ObjectNode message = Json.object("APNS", Json.object("courseId", course().id(), //
				"aps", Json.object("alert", "New course")), //
				"GCM", Json.object("notification", Json.object("body", "New course")),
				Json.object("data", Json.object("courseId", course().id())));

		for (DriverDataObject driver : drivers.results)
			david.push().push(new PushRequest().appId("demo")//
					.credentialsId(driver.source().credentialsId)//
					.data(message));
	}

	@Test
	public void cashierChargesCustomersForCompletedCourse() {

		SpaceDog cashier = cashier();
		Course.Results completedCourses = searchForCompletedCourses();

		for (DataObject<Course> completed : completedCourses.results) {

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

	private Double calculateFare(DataObject<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private Long calculateDistance(DataObject<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private Long calculateTime(DataObject<Course> completed) {
		// TODO Auto-generated method stub
		return null;
	}

	private io.spacedog.demo.Course.Results searchForCompletedCourses() {
		// TODO Auto-generated method stub
		return null;
	}

	private String compatibleVehiculeTypes(String requestedVehiculeType) {
		return null;
	}

	private SpaceDog superadmin;

	private SpaceDog superadmin() {
		if (superadmin == null)
			superadmin = SpaceDog.dog()//
					.username("superadmin")//
					.password("hi superadmin")//
					.login();
		return superadmin;
	}

	private SpaceDog guest() {
		return SpaceDog.dog();
	}

	private SpaceDog david;

	private SpaceDog david() {
		if (david == null)
			david = SpaceDog.dog()//
					.username("david")//
					.password("hi dave")//
					.login();
		return david;
	}

	private SpaceDog superdog() {
		return SpaceDog.dog().username("superdog")//
				.password(SpaceEnv.env().superdogPassword());
	}

	private DataObject<Course> course;

	private DataObject<Course> course() {
		if (course == null)
			course = superadmin().data().fetch(//
					new CourseDataObject().id("myCourse"));
		return course;
	}

	private SpaceDog cashier() {
		return SpaceDog.dog()//
				.username("cashier")//
				.password("hi cashier");
	}
}
