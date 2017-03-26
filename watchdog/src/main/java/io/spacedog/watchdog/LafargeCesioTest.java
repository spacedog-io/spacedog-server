/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.joda.time.DateTime;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.model.DataPermission;
import io.spacedog.model.MailSettings;
import io.spacedog.model.Schema;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
//import io.spacedog.services.LafargeCesioResource;
import io.spacedog.utils.Json7;

public class LafargeCesioTest extends SpaceTest {

	private static final String VINCE_EMAIL = "attias666@gmail.com";
	private static final String DAVID_EMAIL = "david@spacedog.io";

	@Test
	public void testLafargeHolcimCesioApi() {

		/**
		 * To test the php backend, uncomment these lines and comment the whole
		 * 'prepare' section. Be carefull that the following is specific to the
		 * spacedog backend (1) /api/leaderboard?size=1 (2) set vince score date
		 * to now minus 8 days
		 */

		// SpaceRequest.configuration().target(SpaceTarget.production);
		// SpaceDog test = new SpaceDog(//
		// "cesio", SpaceRequest.configuration().cesioSuperAdminUsername(), //
		// SpaceRequest.configuration().cesioSuperAdminPassword(), //
		// "david@spacedog.io");

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		test.schema().set(playerSchema());

		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = false;
		settings.smtp = new SmtpSettings();
		settings.smtp.startTlsRequired = true;
		settings.smtp.sslOnConnect = true;
		settings.smtp.host = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.host");
		settings.smtp.login = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.login");
		settings.smtp.password = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.password");

		test.settings().save(settings);

		// create account fails since email parameter is missing
		SpaceRequest.post("/api/user/create").backend(test).go(400)//
				.assertEquals(error("Missing parameter: email"));

		// fails since email is empty
		SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", "")//
				.go(400)//
				.assertEquals(error("email is empty"));

		// fails since email domain not lafargeholcim.com
		SpaceRequest.post("/api/user/create").backend(test)//
				// force forTesting header to false
				// to have the production behavior on this
				.forTesting(false)//
				.formField("email", DAVID_EMAIL)//
				.go(400)//
				.assertEquals(error("Email is not in domain : lafargeholcim.com"));

		// fails since email invalid
		SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", "..@lafargeholcim.com")//
				.go(400)//
				.assertEquals(error("Invalid Email"));

		// fails since country parameter is missing
		SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.go(400)//
				.assertEquals(error("Missing parameter: country"));

		// fails since country is empty
		SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("country", "")//
				.go(400)//
				.assertEquals(error("country is empty"));

		// creates a new user account
		JsonNode account = SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("country", "24")//
				.go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertPresent("data.id")//
				.assertEquals(DAVID_EMAIL, "data.email")//
				.assertEquals("24", "data.country")//
				// only in this case, code is returned as an int
				.assertInteger("data.code")//
				.get("data");

		String idDavid = account.get("id").asText();
		int codeDavid = account.get("code").asInt();
		assertTrue(codeDavid >= 1000);
		assertTrue(codeDavid <= 9999);

		// create account fails since already exists
		SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("country", "24")//
				.go(400)//
				.assertEquals(0, "response")//
				.assertEquals("user exists", "message")//
				.assertEquals(idDavid, "data.id")//
				.assertEquals(DAVID_EMAIL, "data.email")//
				.assertEquals("24", "data.country")//
				// in this case, code is returned as a string
				.assertEquals("" + codeDavid, "data.code");

		// creates a new user account
		// new user account has id = last user account id + 1
		int idVince = Integer.valueOf(idDavid) + 1;
		int codeVince = SpaceRequest.post("/api/user/create").backend(test)//
				.formField("email", VINCE_EMAIL)//
				.formField("country", "32")//
				.go(201)//
				.assertEquals("" + idVince, "data.id")//
				.get("data.code").asInt();

		// login fails since email parameter is missing
		SpaceRequest.post("/api/user/login").backend(test).go(400)//
				.assertEquals(error("Missing parameter: email"));

		// login fails since email is empty
		SpaceRequest.post("/api/user/login").backend(test)//
				.formField("email", "")//
				.go(400)//
				.assertEquals(error("email is empty"));

		// login fails since code is missing
		SpaceRequest.post("/api/user/login").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.go(400)//
				.assertEquals(error("Missing parameter: code"));

		// login fails since code is empty
		SpaceRequest.post("/api/user/login").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "")//
				.go(400)//
				.assertEquals(error("code is empty"));

		// login fails since code is invalid
		SpaceRequest.post("/api/user/login").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "XXX")//
				.go(400)//
				.assertEquals(error("Connexion Error"));

		// login succeeds
		SpaceRequest.post("/api/user/login").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals(idDavid, "data.id")//
				.assertEquals(DAVID_EMAIL, "data.email")//
				.assertEquals("24", "data.country")//
				// in this case, code is returned as a string
				.assertEquals("" + codeDavid, "data.code");

		// david gets no scores
		SpaceRequest.post("/api/score/get").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.go(400)//
				.assertEquals(error("No scores"));

		// Leaderboard returns no scores
		SpaceRequest.get("/api/leaderboard").backend(test).go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals(Json7.array(), "data.today")//
				.assertEquals(Json7.array(), "data.week")//
				.assertEquals(Json7.array(), "data.forever");

		// david sets level 1 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.formField("level", "1")//
				.formField("score", "123")//
				.go(201)//
				.assertEquals(success());

		// david sets lower level 1 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.formField("level", "1")//
				.formField("score", "111")//
				.go(400)//
				.assertEquals(error("Database saving failed"));

		// david sets higher level 1 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.formField("level", "1")//
				.formField("score", "133")//
				.go(201)//
				.assertEquals(success());

		// david sets same level 1 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.formField("level", "1")//
				.formField("score", "133")//
				.go(201)//
				.assertEquals(success());

		// david gets score
		SpaceRequest.post("/api/score/get").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals("133", "data.1.score")//
				.assertEquals("1", "data.1.level")//
				.assertString("data.1.date");

		// david sets level 2 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.formField("level", "2")//
				.formField("score", "43")//
				.go(201)//
				.assertEquals(success());

		// david gets score
		SpaceRequest.post("/api/score/get").backend(test)//
				.formField("email", DAVID_EMAIL)//
				.formField("code", "" + codeDavid)//
				.go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals("133", "data.1.score")//
				.assertEquals("1", "data.1.level")//
				.assertString("data.1.date")//
				.assertEquals("43", "data.2.score")//
				.assertEquals("2", "data.2.level")//
				.assertString("data.2.date");

		// gets leaderboard
		SpaceRequest.get("/api/leaderboard").backend(test).go(201);

		// vince sets level 2 score
		SpaceRequest.post("/api/score/set").backend(test)//
				.formField("email", VINCE_EMAIL)//
				.formField("code", "" + codeVince)//
				.formField("level", "2")//
				.formField("score", "143")//
				.go(201)//
				.assertEquals(success());

		// gets leaderboard

		ObjectNode david = Json7.object("email", DAVID_EMAIL, //
				"country_id", "24", "somme", "176");

		ObjectNode vince = Json7.object("email", VINCE_EMAIL, //
				"country_id", "32", "somme", "143");

		ArrayNode today = Json7.array(david, vince);
		ArrayNode week = today;
		ArrayNode forever = today;

		SpaceRequest.get("/api/leaderboard").backend(test).go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals(today, "data.today")//
				.assertEquals(week, "data.week")//
				.assertEquals(forever, "data.forever");

		// admin sets vince only score 8 days in the past
		// to show his highscore only in the forever section
		ArrayNode scores = (ArrayNode) SpaceRequest.get("/1/data/player/" + idVince).adminAuth(test).go(200)//
				.get("scores");
		Json7.set(scores, "0.date", TextNode.valueOf(//
				DateTime.now().minusDays(8).toString()));
		SpaceRequest.put("/1/data/player/" + idVince).adminAuth(test)//
				.body("scores", scores).go(200);

		// gets leaderboard after vince score update

		today = Json7.array(david);
		week = Json7.array(david);
		forever = Json7.array(david, vince);

		SpaceRequest.get("/api/leaderboard").backend(test).go(201)//
				.assertEquals(1, "response")//
				.assertEquals("", "message")//
				.assertEquals(today, "data.today")//
				.assertEquals(week, "data.week")//
				.assertEquals(forever, "data.forever");

	}

	private JsonNode error(String message) {
		return error(message, Json7.array());
	}

	private JsonNode error(String message, JsonNode data) {
		return Json7.object("response", 0, "message", message, "data", data);
	}

	private JsonNode success() {
		return success(Json7.array());
	}

	private JsonNode success(JsonNode data) {
		return Json7.object("response", 1, "message", "", "data", data);
	}

	// fields
	private static final String ID = "id";
	private static final String DATE = "date";
	private static final String LEVEL = "level";
	private static final String SCORE = "score";
	private static final String SCORES = "scores";
	private static final String COUNTRY = "country";
	private static final String CODE = "code";
	private static final String EMAIL = "email";

	// types
	public static final String PLAYER_TYPE = "player";

	private static Schema playerSchema() {
		return Schema.builder(PLAYER_TYPE) //

				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.integer(ID)//
				.string(EMAIL)//
				.integer(CODE)//
				.integer(COUNTRY)//

				.object(SCORES).array()//
				.integer(SCORE)//
				.integer(LEVEL)//
				.timestamp(DATE)//
				.close()//

				.build();
	}

}
