package io.spacedog.caremen;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.StringLoader;

import io.spacedog.admin.AdminJobs;
import io.spacedog.caremen.FareSettings.VehiculeFareSettings;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.sdk.SpaceData.SearchResults;
import io.spacedog.sdk.SpaceData.TermQuery;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.sdk.SpaceMail.Message;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class Billing {

	// DateTimeFormatter idFormatter =
	// DateTimeFormat.forPattern("yyMMddhhmmss");
	DateTimeFormatter fullFormatter = DateTimeFormat.fullDateTime().withLocale(Locale.FRANCE);
	DateTimeFormatter shortFormatter = DateTimeFormat.shortDateTime().withLocale(Locale.FRANCE);
	DateTimeFormatter dateFormatter = DateTimeFormat.fullDate().withLocale(Locale.FRANCE);
	DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm").withLocale(Locale.FRANCE);
	DecimalFormat decimalFormatter = new DecimalFormat("#.##");

	SpaceEnv env = null;
	SpaceDog dog = null;

	public String charge() {

		try {
			env = SpaceEnv.defaultEnv();
			dog = SpaceDog.backend(env.get("backend_id")) //
					.username("cashier").login(env.get("caremen_cashier_password"));

			TermQuery query = new TermQuery();
			query.type = "course";
			query.size = 50;
			query.terms = Lists.newArrayList("status", "completed");
			SearchResults<Course> courses = dog.dataEndpoint().search(query, Course.class);

			if (courses.total() == 0)
				return "OK";

			FareSettings fareSettings = dog.settings().get(FareSettings.class);
			AppConfigurationSettings appConfSettings = dog.settings()//
					.get(AppConfigurationSettings.class);

			for (Course course : courses.objects()) {
				try {
					computeFare(dog, course, fareSettings);
					chargeBill(dog, course);
					sendReceipt(course, appConfSettings.rateVAT);
					course.status = "billed";
					course.save();

				} catch (Exception e) {
					String url = env.target().url(dog.backendId(), //
							"/1/data/course/" + course.id());
					AdminJobs.error(this, "Error billing course " + url, e);
				}
			}

		} catch (Throwable t) {
			return AdminJobs.error(this, t);
		}

		return "OK";
	}

	@SuppressWarnings("unused")
	private class Receipt {
		public Course course;
		public Driver driver;

		public double rateVAT;
		public String dateReceipt;
		public String dateCourse;
		public String number;
		public String taxes;
		public String fareHT;
		public String fareTTC;

		public void shakeIt() {
			double ht = 100 * course.fare / (100 + rateVAT);
			fareHT = decimalFormatter.format(ht);
			fareTTC = decimalFormatter.format(course.fare);
			taxes = decimalFormatter.format(course.fare - ht);
			DateTime now = DateTime.now();
			// number = idFormatter.print(now);
			dateReceipt = dateFormatter.print(now);
			dateCourse = dateFormatter.print(course.pickupTimestamp) + " à "
					+ timeFormatter.print(course.pickupTimestamp);
		}
	}

	private void sendReceipt(Course course, double rateVAT) {

		Receipt receipt = new Receipt();
		receipt.course = course;
		receipt.driver = dog.dataEndpoint().get(Driver.class, course.driver.driverId);
		receipt.rateVAT = rateVAT;
		receipt.shakeIt();

		@SuppressWarnings("unchecked")
		Map<String, Object> context = Json.mapper().convertValue(receipt, Map.class);

		Message message = new Message();
		message.from = "CAREMEN <no-reply@caremen.fr>";
		message.to = dog.credentials().get(course.customer.credentialsId).email().get();
		message.bcc = "compta@caremen.fr";
		message.subject = "Votre reçu de course CAREMEN";

		try {
			String template = Resources.toString(//
					Resources.getResource(this.getClass(), "customer-receipt.pebble"), //
					Charset.forName("UTF-8"));

			PebbleEngine pebble = new PebbleEngine.Builder().loader(new StringLoader()).build();
			StringWriter writer = new StringWriter();
			pebble.getTemplate(template).evaluate(writer, context);
			message.html = writer.toString();

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error rendering customer receipt template");
		}

		dog.mailEndpoint().send(message);
	}

	void computeFare(SpaceDog dog, Course course, FareSettings fareSettings) {

		if (course.fare != null)
			return;

		Check.notNull(course.requestedVehiculeType, "course.requestedVehiculeType");
		Check.notNull(course.pickupTimestamp, "course.pickupTimestamp");
		Check.notNull(course.dropoffTimestamp, "course.dropoffTimestamp");
		Check.notNull(course.driver, "course.driver");
		Check.notNull(course.driver.driverId, "course.driver.driverId");
		Check.notNull(course.driver.vehicule, "course.driver.vehicule");

		TermQuery query = new TermQuery();
		query.type = "courselog";
		query.size = 1000;
		query.terms = Lists.newArrayList("courseId", course.id(), //
				"driverId", course.driver.driverId, //
				"status", Lists.newArrayList("in-progress", "completed"));
		query.sort = "meta.createdAt";
		query.ascendant = true;

		SearchResults<CourseLog> logs = dog.dataEndpoint().search(query, CourseLog.class);

		Check.isTrue(logs.total() <= 1000, //
				"too many course logs [%s] for course [%s]", logs.total(), course.id());

		List<LatLng> points = Lists.newArrayList();

		for (CourseLog log : logs.objects())
			if (log.where != null)
				points.add(log.where);

		String type = course.requestedVehiculeType;
		VehiculeFareSettings typeFareSettings = fareSettings.getFor(type);
		Check.notNull(typeFareSettings.base, type + ".base");
		Check.notNull(typeFareSettings.km, type + ".km");
		Check.notNull(typeFareSettings.min, type + ".min");
		Check.notNull(typeFareSettings.minimum, type + ".minimum");

		course.distance = (long) SphericalUtil.computeLength(points);
		course.time = course.dropoffTimestamp.getMillis() //
				- course.pickupTimestamp.getMillis();

		course.fare = (typeFareSettings.km * course.distance / 1000) //
				+ (typeFareSettings.min * course.time / 60000) //
				+ typeFareSettings.base;

		course.fare = Double.max(typeFareSettings.minimum, course.fare);
		course.driver.gain = course.fare * fareSettings.driverShare;

		course.save();
	}

	void chargeBill(SpaceDog dog, Course course) {

		if (course.payment.companyId != null)
			return;
		if (course.payment.stripe.paymentId != null)
			return;

		Check.notNull(course.payment, "course.payment");
		Check.notNull(course.payment.stripe, "course.payment.stripe");
		Check.notNull(course.payment.stripe.customerId, "course.payment.stripe.customerId");
		Check.notNull(course.payment.stripe.cardId, "course.payment.stripe.cardId");
		Check.notNull(course.to.address, "course.to.address");

		String fullPickupDateTime = fullFormatter.print(course.pickupTimestamp);
		String shortPickupDateTime = shortFormatter.print(course.pickupTimestamp);

		Map<String, Object> params = Maps.newHashMap();
		params.put("amount", (int) (course.fare * 100));
		params.put("currency", "eur");
		params.put("customer", course.payment.stripe.customerId);
		params.put("source", course.payment.stripe.cardId);
		params.put("description", //
				String.format("Course du [%s] à destination de [%s]", //
						fullPickupDateTime, course.to.address));
		params.put("statement_descriptor", //
				String.format("Caremen %s", shortPickupDateTime));

		ObjectNode payment = dog.stripe().charge(params);
		course.payment.stripe.paymentId = payment.get("id").asText();

		course.save();
	}

	public static void main(String[] args) {
		SpaceRequest.env().target(SpaceTarget.production);
		System.setProperty("backend_id", "caredev");
		new Billing().charge();
	}
}
