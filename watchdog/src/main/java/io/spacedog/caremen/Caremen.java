/**
 * © David Attias 2015
 */
package io.spacedog.caremen;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.CredentialsSettings;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailSettings.SmtpSettings;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SettingsSettings;
import io.spacedog.utils.SettingsSettings.SettingsAcl;
import io.spacedog.utils.SmsSettings;
import io.spacedog.utils.SmsSettings.TwilioSettings;
import io.spacedog.utils.SmsTemplate;
import io.spacedog.utils.StripeSettings;

public class Caremen extends SpaceTest {

	static final SpaceDog DEV;
	static final SpaceDog RECETTE;
	static final SpaceDog PRODUCTION;

	static {
		String testPassword = SpaceEnv.defaultEnv().get("caremen_test_superadmin_password");
		String prodPassword = SpaceEnv.defaultEnv().get("caremen_prod_superadmin_password");

		DEV = SpaceDog.backend("caredev").username("caredev")//
				.password(testPassword).email("platform@spacedog.io");
		RECETTE = SpaceDog.backend("carerec").username("carerec")//
				.password(testPassword).email("platform@spacedog.io");
		PRODUCTION = SpaceDog.backend("caremen").username("caremen")//
				.password(prodPassword).email("platform@spacedog.io");
	}

	private SpaceDog backend;

	@Test
	public void initCaremenBackend() throws IOException {

		backend = DEV;
		SpaceRequest.env().target(SpaceTarget.production);

		// initInstallations();
		// initVehiculeTypes();
		// initStripeSettings();
		// initMailSettings();
		// initSmsSettings();
		// initFareSettings();
		// initAppConfigurationSettings();
		// initReferences();
		// initCredentials();
		// initSettingsSettings();

		// backend.schema().set(buildCourseSchema());
		// backend.schema().set(buildDriverSchema());
		// backend.schema().set(buildCustomerSchema());
		// backend.schema().set(buildCourseLogSchema());
		// backend.schema().set(buildCustomerCompanySchema());
		// backend.schema().set(buildCompanySchema());

		// deleteAllCredentialsButSuperAdmins();
		// createOperatorCredentials();
		// createCashierCredentials();
		// createReminderCredentials();
		// createAppleTestCredentials();
		// createRobots();
	}

	//
	// Settings
	//

	void deleteAllCredentialsButSuperAdmins() {
		backend.credentials().deleteAllButSuperAdmins();
	}

	void initCredentials() {
		CredentialsSettings settings = new CredentialsSettings();
		settings.sessionMaximumLifetime = 60 * 60 * 24 * 7;
		backend.settings().save(settings);
	}

	void initMailSettings() throws IOException {
		MailSettings settings = new MailSettings();

		settings.smtp = new SmtpSettings();
		settings.smtp.host = "mail.caremen.fr";
		settings.smtp.login = "no-reply@caremen.fr";
		settings.smtp.password = SpaceEnv.defaultEnv().get("caremen.smtp.password");
		settings.smtp.sslOnConnect = true;
		settings.smtp.startTlsRequired = true;

		settings.templates = Maps.newHashMap();

		MailTemplate template = new MailTemplate();
		template.from = "CAREMEN <no-reply@caremen.fr>";
		template.to = Lists.newArrayList("{{to}}");
		template.subject = "Votre rattachement au compte entreprise {{company.name}}";
		template.text = "Bonjour {{firstname}} {{lastname}}," //
				+ "\n\nNous avons le plaisir de vous informer que vous pouvez maintenant"
				+ " régler vos courses commandées avec l’application CAREMEN sur le Compte"
				+ " Entreprise de {{company.name}}." //
				+ "\n\nNotez bien que vous pouvez toujours changer et sélectionner votre moyen"
				+ " de paiement avant de confirmer chaque commande de course." //
				+ "\n\nToujours à votre attention," //
				+ "\nLe Service Client CAREMEN\n\n" //
				+ "---\nCeci est un Email envoyé par l’application CAREMEN."
				+ "\nPour plus d’information, contactez le Service Client (bonjour@caremen.fr).";

		template.model = Maps.newHashMap();
		template.model.put("to", "string");
		template.model.put("firstname", "string");
		template.model.put("lastname", "string");
		template.model.put("company", "company");
		template.roles = Collections.singleton("operator");

		settings.templates.put("notif_customer_company", template);

		template = new MailTemplate();
		template.from = "CAREMEN <no-reply@caremen.fr>";
		template.to = Lists.newArrayList("{{to}}");
		template.subject = "Bienvenue chez CAREMEN";
		template.html = Resources.toString(//
				Resources.getResource(this.getClass(), "caremen-welcome.html"), //
				Charset.forName("UTF-8"));
		template.model = Maps.newHashMap();
		template.model.put("to", "string");
		template.model.put("customer", "customer");
		template.roles = Collections.singleton("user");

		settings.templates.put("notif-customer-welcome", template);

		backend.settings().save(settings);
	}

	void initSmsSettings() {
		SmsTemplate template = new SmsTemplate();
		template.to = "{{to}}";
		template.body = "Votre code de validation CAREMEN est le {{code}}";
		template.model = Maps.newHashMap();
		template.model.put("to", "string");
		template.model.put("code", "string");
		template.roles = Collections.singleton("user");

		SmsSettings settings = new SmsSettings();
		settings.templates = Maps.newHashMap();
		settings.templates.put("phone-validation", template);

		settings.twilio = new TwilioSettings();
		SpaceEnv env = SpaceEnv.defaultEnv();
		settings.twilio.accountSid = env.get("caremen.twilio.accountSid");
		settings.twilio.authToken = env.get("caremen.twilio.authToken");
		settings.twilio.defaultFrom = env.get("caremen.twilio.defaultFrom");

		backend.settings().save(settings);
	}

	void initStripeSettings() {
		StripeSettings settings = new StripeSettings();
		settings.rolesAllowedToCharge = Sets.newHashSet("cashier");

		settings.secretKey = backend == DEV //
				? SpaceEnv.defaultEnv().get("caremen.stripe.test.secret.key")//
				: SpaceEnv.defaultEnv().get("caremen.stripe.prod.secret.key");

		backend.settings().save(settings);
	}

	void initFareSettings() {
		ObjectNode classic = Json.object("base", 3, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode premium = Json.object("base", 5, "minimum", 15, "km", 2, "min", 0.45);
		ObjectNode green = Json.object("base", 5, "minimum", 35, "km", 2, "min", 0.45);
		ObjectNode breakk = Json.object("base", 5, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode van = Json.object("base", 5, "minimum", 20, "km", 2, "min", 0.45);
		ObjectNode settings = Json.object("classic", classic, "premium", premium, //
				"green", green, "break", breakk, "van", van, "driverShare", 0.82f);
		SpaceRequest.put("/1/settings/fare").adminAuth(backend).body(settings).go(200, 201);
	}

	void initAppConfigurationSettings() {
		ObjectNode settings = Json.object(//
				"driverAverageSpeedKmPerHour", 15, //
				"courseLogIntervalMeters", 100, //
				"customerWaitingForDriverMaxDurationMinutes", 2, //
				"operatorRefreshTimeoutSeconds", 75);

		SpaceRequest.put("/1/settings/appconfiguration")//
				.adminAuth(backend).body(settings).go(200, 201);
	}

	void initSettingsSettings() {
		SettingsSettings settings = new SettingsSettings();

		// appcustomer settings
		SettingsAcl acl = new SettingsAcl();
		acl.read("key", "user");
		settings.put("appcustomer", acl);

		// appconfiguration settings
		acl = new SettingsAcl();
		acl.read("key", "user", "admin");
		settings.put("appconfiguration", acl);

		// fare settings
		acl = new SettingsAcl();
		acl.read("operator", "cashier", "user");
		acl.update("operator");
		settings.put("fare", acl);

		// references settings
		acl = new SettingsAcl();
		acl.read("key", "user", "operator");
		settings.put("references", acl);

		backend.settings().save(settings);
	}

	void initVehiculeTypes() {

		ObjectNode node = Json.objectBuilder()//
				.object("classic")//
				.put("type", "classic")//
				.put("name", "Berline Classic")//
				.put("description", "Standard")//
				.put("minimumPrice", 10)//
				.put("passengers", 4)//
				.end()//

				.object("premium")//
				.put("type", "premium")//
				.put("name", "Berline Premium")//
				.put("description", "Haut de gamme")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("green")//
				.put("type", "green")//
				.put("name", "Green Berline")//
				.put("description", "Electric cars")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("break")//
				.put("type", "break")//
				.put("name", "Breack")//
				.put("description", "Grand coffre")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("van")//
				.put("type", "van")//
				.put("name", "Van")//
				.put("description", "Mini bus")//
				.put("minimumPrice", 15)//
				.put("passengers", 6)//
				.end()//

				.build();

		SpaceRequest.put("/1/settings/vehiculetypes")//
				.adminAuth(backend).body(node).go(201, 200);
	}

	void initReferences() {

		ObjectNode node = Json.objectBuilder()//

				.node("courseStatuses", //
						Json.array("new-immediate", "driver-is-coming", "ready-to-load", "in-progress", "completed",
								"cancelled", "new-scheduled", "scheduled-assigned", "billed", "unbilled"))//

				.node("driverStatuses", //
						Json.array("working", "not-working", "disabled"))//

				.node("roles", //
						Json.array("key", "user", "customer", "driver", "operator", "admin"))//

				.node("vehiculeTypes", //
						Json.object("classic", "Classic Berline", //
								"premium", "Premium Berline", "green", "Green Berline", //
								"break", "Break", "van", "Van"))//

				.node("companyStatuses", //
						Json.array("enabled", "disabled"))//

				.node("customerStatuses", //
						Json.array("enabled", "disabled"))//

				.build();

		SpaceRequest.put("/1/settings/references")//
				.adminAuth(backend).body(node).go(201, 200);
	}

	//
	// Schemas
	//

	static Schema buildCustomerSchema() {
		return Schema.builder("customer") //

				.acl("user", DataPermission.create, DataPermission.search, //
						DataPermission.update)//
				.acl("operator", DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("credentialsId")//
				.string("status")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("notes").french()//
				.string("phone")//

				.object("billing")//
				.text("name").french()//
				.text("street").french()//
				.string("zipcode")//
				.text("town").french()//
				.close()//

				.close()//
				.build();
	}

	static Schema buildCustomerCompanySchema() {
		return Schema.builder("customercompany") //

				.acl("user", DataPermission.read_all)//
				.acl("operator", DataPermission.create, DataPermission.delete_all, //
						DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.update_all, //
						DataPermission.delete_all, DataPermission.search)//

				.string("companyId")//
				.text("companyName").french()//
				.build();
	}

	void initInstallations() {
		SpaceRequest.delete("/1/schema/installation").adminAuth(backend).go(200, 404);
		SpaceRequest.put("/1/schema/installation").adminAuth(backend).go(201);
		Schema schema = backend.schema().get("installation");
		schema.acl("key", DataPermission.create, DataPermission.read, DataPermission.update, DataPermission.delete);
		schema.acl("user", DataPermission.create, DataPermission.read, DataPermission.update, DataPermission.delete);
		schema.acl("admin", DataPermission.search, DataPermission.update_all, DataPermission.delete_all);
		backend.schema().set(schema);
	}

	static Schema buildCourseSchema() {
		return Schema.builder("course") //

				.acl("user", DataPermission.create, DataPermission.read, //
						DataPermission.search, DataPermission.update)//
				.acl("cashier", DataPermission.search, DataPermission.update_all)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("operator", DataPermission.search, DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("status") //
				.string("requestedVehiculeType") //
				.timestamp("requestedPickupTimestamp") //
				.timestamp("driverIsComingTimestamp") //
				.timestamp("driverIsReadyToLoadTimestamp") //
				.timestamp("cancelledTimestamp") //
				.timestamp("pickupTimestamp") //
				.timestamp("dropoffTimestamp") //
				.text("noteForDriver").french()//
				.floatt("fare") // in euros
				.longg("time") // in millis
				.integer("distance") // in meters

				.object("customer")//
				.string("id")//
				.string("credentialsId")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("phone")//
				.close()

				.object("payment")//
				.string("companyId")//
				.text("companyName").french()//

				.object("stripe")//
				.string("customerId")//
				.string("cardId")//
				.string("paymentId")//
				.close()//

				.close()//

				.object("from")//
				.text("address").french()//
				.geopoint("geopoint")//
				.close()//

				.object("to")//
				.text("address").french()//
				.geopoint("geopoint")//
				.close()//

				.object("driver")//
				.string("driverId")//
				.string("credentialsId")//
				.floatt("gain")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("phone")//
				.string("photo")//

				.object("vehicule")//
				.string("type")//
				.text("brand").french()//
				.text("model").french()//
				.text("color").french()//
				.string("licencePlate")//
				.close()//

				.close()//
				.build();
	}

	static Schema buildDriverSchema() {
		return Schema.builder("driver") //

				.acl("user", DataPermission.search)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("operator", DataPermission.create, DataPermission.search, //
						DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, DataPermission.update_all,
						DataPermission.delete_all)//

				.string("credentialsId")//
				.string("status")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("homeAddress").french()//
				.string("phone")//
				.string("photo")//

				.object("lastLocation")//
				.geopoint("where")//
				.timestamp("when")//
				.close()

				.object("vehicule")//
				.string("type")//
				.text("brand").french()//
				.text("model").french()//
				.text("color").french()//
				.string("licencePlate")//
				.close()//

				.object("RIB")//
				.text("bankName").french()//
				.string("bankCode")//
				.string("accountIBAN")//
				.close()//

				.close()//
				.build();
	}

	static Schema buildCourseLogSchema() {
		return Schema.builder("courselog") //

				.acl("driver", DataPermission.create)//
				.acl("cashier", DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.delete_all)//

				.string("courseId")//
				.string("driverId")//
				.string("status")//
				.geopoint("where")//
				.timestamp("when")//
				.longg("index")//
				.longg("distanceFromLastLog")//

				.close()//
				.build();
	}

	Schema buildCompanySchema() {
		return Schema.builder("company") //

				.acl("operator", DataPermission.create, DataPermission.search, //
						DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("status")//
				.string("vatId")//
				.text("name").french()//
				.text("address").french()//
				.text("notes").french()//
				.string("phone")//
				.string("email")//
				.bool("active")//

				.object("contact")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("role").french()//
				.string("phone")//
				.string("email")//

				.close()//
				.build();
	}

	//
	// Special users
	//

	void createOperatorCredentials() {

		if (backend == DEV || backend == RECETTE) {
			createOperatorCredentials("nico", "nicola.lonzi@gmail.com");
			createOperatorCredentials("dimitri", "dimitri.valax@in-tact.fr");
			createOperatorCredentials("dave", "david@spacedog.io");
			createOperatorCredentials("philou", "philippe.rolland@in-tact.fr");
			createOperatorCredentials("flav", "flavien.dibello@in-tact.fr");
		}

		if (backend == RECETTE) {
			createOperatorCredentials("manu", "emmanuel@walkthisway.fr");
			createOperatorCredentials("xav", "xdorange@gmail.com");
			createOperatorCredentials("auguste", "lcdlocation@wanadoo.fr");
		}

		if (backend == PRODUCTION) {
			createOperatorCredentials("auguste", "lcdlocation@wanadoo.fr");
		}

	}

	void createOperatorCredentials(String username, String email) {
		String password = SpaceEnv.defaultEnv().get("caremen_operator_password");
		resetCredentials(backend, username, password, email, "operator", true);
	}

	void createAppleTestCredentials() {
		// if (backend == PRODUCTION) {
		String password = SpaceEnv.defaultEnv().get("caremen_apple_password");

		Credentials customerCredentials = resetCredentials(backend, "apple@apple.com", //
				password, "apple@apple.com", "customer", false);

		ObjectNode customer = Json.object("firstname", "Apple", "lastname", "Test", //
				"credentialsId", customerCredentials.id(), "phone", "0652802569");

		backend.dataEndpoint().object("customer").node(customer).save();

		Credentials driverCredentials = resetCredentials(backend, "apple@driver.com", password, //
				"apple@driver.com", "driver", true);

		ObjectNode vehicule = Json.object("brand", "Peugeot", "type", "classic", //
				"color", "Noir", "model", "609", "licencePlate", "DF-FF-7SD");

		ObjectNode driver = Json.object("status", "not-working", "firstname", "Apple", //
				"lastname", "Driver", "phone", "0652802569", "vehicule", vehicule, //
				"credentialsId", driverCredentials.id());

		backend.dataEndpoint().object("driver").node(driver).save();
		// }

	}

	void createCashierCredentials() {
		String password = SpaceEnv.defaultEnv().get("caremen_cashier_password");
		resetCredentials(backend, "cashier", password, //
				"plateform@spacedog.io", "cashier", false);
	}

	void createReminderCredentials() {
		String password = SpaceEnv.defaultEnv().get("caremen_reminder_password");
		resetCredentials(backend, "reminder", password, //
				"plateform@spacedog.io", "reminder", false);
	}

	private Credentials resetCredentials(SpaceDog superadmin, String username, //
			String password, String email, String role, boolean admin) {

		superdogDeletesCredentials(superadmin.backendId(), username);

		Credentials credentials = admin //
				? superadmin.credentials().create(username, password, email, true)//
				: superadmin.credentials().create(username, password, email);

		if (role != null)
			superadmin.credentials().setRole(credentials.id(), role);

		return credentials;
	}

	void createRobots() {
		if (backend == PRODUCTION)
			return;

		for (int i = 0; i < 3; i++) {

			String username = "robot-" + i;
			String password = "hi " + username;

			JsonNode node = SpaceRequest.get("/1/credentials")//
					.adminAuth(backend).queryParam("username", username).go(200)//
					.get("results.0.id");

			if (node == null) {
				SpaceDog robot = signUp(backend.backendId(), username, password);
				backend.credentials().setRole(robot.id(), "driver");

				SpaceRequest.post("/1/data/driver").adminAuth(backend)//
						.body("status", "not-working", "firstname", "Robot", //
								"lastname", Integer.toString(i), "phone", "0606060606", //
								"homeAddress", "9 rue Titon 75011 Paris", //
								"credentialsId", robot.id(), "vehicule", //
								Json.object("brand", "Faucon", "model", "Millenium", //
										"type", "classic", "color", "Métal", //
										"licencePlate", "DA-KISS-ME"))//
						.go(201);
			}
		}
	}
}
