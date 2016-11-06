/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.services.MailResource.Message;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.Schema;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/api")
public class LafargeCesioResource extends Resource {

	private final Random random = new Random();

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

	//
	// Routes
	//

	@Post("/user/create")
	@Post("/user/create/")
	public Payload userCreate(Context context) {
		Credentials credentials = forceLafargeCredentials();

		try {
			Player player = new Player();
			player.email = extract(EMAIL);
			validateEmail(player.email, SpaceContext.isTest());
			player.country = extractInt(COUNTRY);

			Start.get().getElasticClient().refreshType(//
					credentials.backendId(), PLAYER_TYPE);

			SearchHits hits = DataStore.get().search(//
					credentials.backendId(), PLAYER_TYPE, EMAIL, player.email);

			if (hits.totalHits() > 0) {
				player = Json.mapper().readValue(//
						hits.getAt(0).getSourceAsString(), Player.class);
				return wrapResponse(400, "user exists", player.toNodeWithStringCode());

			} else {

				SearchHits hits2 = Start.get().getElasticClient()//
						.prepareSearch(credentials.backendId(), PLAYER_TYPE)//
						.addSort(SortBuilders.fieldSort(ID).order(SortOrder.DESC))//
						.setFetchSource(false)//
						.setSize(1)//
						.addField(ID)//
						.get()//
						.getHits();

				if (hits2.totalHits() == 0)
					player.id = 1;
				else {
					int lastId = hits2.getAt(0).field(ID).getValue();
					player.id = Integer.valueOf(lastId) + 1;
				}

				player.code = 1000 + random.nextInt(9000);

				DataStore.get().createObject(credentials.backendId(), PLAYER_TYPE, //
						Optional.of("" + player.id), Json.mapper().valueToTree(player), //
						credentials.name());

				sendActivationCode(credentials, player);

				return wrapResponse(201, player.toNode());
			}

		} catch (CesioException e) {
			return wrapResponse(e);
		} catch (Throwable t) {
			t.printStackTrace();
			return wrapResponse(t);
		}
	}

	@Post("/user/login")
	@Post("/user/login/")
	public Payload userLogin() {
		Credentials credentials = forceLafargeCredentials();

		try {
			String email = extract(EMAIL);
			int code = extractInt(CODE);
			Player player = login(credentials, email, code);
			return wrapResponse(201, player.toNodeWithStringCode());

		} catch (CesioException t) {
			return wrapResponse(t);
		} catch (Throwable t) {
			t.printStackTrace();
			return wrapResponse(400, "Connexion Error");
		}
	}

	@Post("/score/set")
	@Post("/score/set/")
	public Payload scoreSet() {
		Credentials credentials = forceLafargeCredentials();

		try {
			String email = extract(EMAIL);
			int code = extractInt(CODE);
			int level = extractInt(LEVEL);
			int value = extractInt(SCORE);

			Player player = login(credentials, email, code);
			Score score = player.score(level);

			if (score == null) {
				player.scores.add(new Score(level, value));
			} else if (value >= score.score) {
				score.score = value;
				score.date = DateTime.now();
			} else
				throw new CesioException("Database saving failed");

			save(player);
			return wrapResponse(201);

		} catch (CesioException e) {
			return wrapResponse(e);
		} catch (Throwable t) {
			t.printStackTrace();
			return wrapResponse(t);
		}
	}

	@Post("/score/get")
	@Post("/score/get/")
	public Payload scoreGet() {
		Credentials credentials = forceLafargeCredentials();

		try {
			String email = extract(EMAIL);
			int code = extractInt(CODE);

			Player player = login(credentials, email, code);

			if (player.scores.isEmpty())
				return wrapResponse(400, "No scores");

			return wrapResponse(201, player.toScoresNode());

		} catch (CesioException e) {
			return wrapResponse(e);
		} catch (Throwable t) {
			t.printStackTrace();
			return wrapResponse(t);
		}

	}

	@Get("/leaderboard")
	@Get("/leaderboard/")
	public Payload leaderboard() {
		Credentials credentials = forceLafargeCredentials();
		ElasticClient elastic = Start.get().getElasticClient();

		List<HighScore> highScores = Lists.newArrayList();
		int size = SpaceContext.isTest() ? 1 : 100;
		int from = 0;

		try {

			elastic.refreshType(credentials.backendId(), PLAYER_TYPE);
			long total = getPlayerCount(credentials);

			while (from < total) {
				getHighScores(credentials, highScores, from, size);
				from = from + size;
			}

			return wrapResponse(201, toLeaderboard(highScores));

		} catch (CesioException e) {
			return wrapResponse(e);
		} catch (Throwable t) {
			t.printStackTrace();
			return wrapResponse(t);
		}

	}

	//
	// Implementation
	//

	private long getPlayerCount(Credentials credentials) {

		return Start.get().getElasticClient()//
				.prepareSearch(credentials.backendId(), PLAYER_TYPE)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.setSize(0)//
				.get()//
				.getHits()//
				.totalHits();
	}

	private void getHighScores(Credentials credentials, List<HighScore> highScores, int from, int size)
			throws IOException, JsonParseException, JsonMappingException {

		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(credentials.backendId(), PLAYER_TYPE)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.setFrom(from).setSize(size)//
				.get()//
				.getHits();

		for (SearchHit searchHit : hits) {
			Player player = Json.mapper().readValue(//
					searchHit.sourceAsString(), Player.class);

			HighScore highScore = player.highScore();
			if (highScore.date != null)
				highScores.add(highScore);
		}
	}

	private void sendActivationCode(Credentials credentials, Player player) {

		Message message = new Message();

		StringBuilder builder = new StringBuilder();

		builder.append("Hi, \n\n");
		builder.append("Please find your unique activation code : \n\n");
		builder.append("Email : ").append(player.email).append("\n");
		builder.append("Code : ").append(player.code).append("\n\n");
		builder.append("You will need it when you will log in to play the game. \n\n");
		builder.append("Enjoy ! \n\n");
		builder.append("The CESIO TEAM \n");
		builder.append("https://goo.gl/AtWpvq \n");

		message.text = builder.toString();
		message.subject = "CESIO GAME - activation code";
		message.from = "cesio@lafargeholcimweb.com";
		message.to = Lists.newArrayList(player.email);

		MailSettings settings = SettingsResource.get().load(MailSettings.class);

		if (settings.smtp != null)
			MailResource.get().emailViaSmtp(credentials, settings.smtp, message);
	}

	private JsonNode toLeaderboard(List<HighScore> highScores) {
		highScores.sort(null);
		ObjectNode results = Json.object();

		ArrayNode today = results.putArray("today");
		ArrayNode week = results.putArray("week");
		ArrayNode forever = results.putArray("forever");

		DateTime startOfDay = DateTime.now().withTimeAtStartOfDay();
		DateTime startOfWeek = startOfDay.withDayOfWeek(DateTimeConstants.MONDAY);

		for (HighScore score : highScores) {

			if (score.date.isAfter(startOfDay)) {
				today.add(score.toNode());
				week.add(score.toNode());

			} else if (score.date.isAfter(startOfWeek))
				week.add(score.toNode());

			forever.add(score.toNode());
		}
		return results;
	}

	private void save(Player player) {
		Credentials credentials = SpaceContext.getCredentials();
		DataStore.get().patchObject(credentials.backendId(), PLAYER_TYPE, //
				"" + player.id, 0l, Json.mapper().valueToTree(player), //
				credentials.name());
	}

	private Credentials forceLafargeCredentials() {
		Credentials credentials = new Credentials(//
				SpaceContext.backendId(), "lafargeadmin", Level.SUPER_ADMIN);
		SpaceContext.setCredentials(credentials);
		return credentials;
	}

	private Player login(Credentials credentials, String email, int code)
			throws JsonParseException, JsonMappingException, IOException {

		Start.get().getElasticClient().refreshType(//
				credentials.backendId(), PLAYER_TYPE);
		SearchHits hits = DataStore.get().search(credentials.backendId(), //
				PLAYER_TYPE, EMAIL, email, CODE, code);

		if (hits.totalHits() > 0) {
			Player player = Json.mapper().readValue(//
					hits.getAt(0).getSourceAsString(), Player.class);
			player.spaceId = hits.getAt(0).id();
			return player;

		} else
			throw new CesioException("Connexion Error");
	}

	private Payload wrapResponse(int status) {
		return wrapResponse(status, "", Json.array());
	}

	private Payload wrapResponse(int status, JsonNode data) {
		return wrapResponse(status, "", data);
	}

	private Payload wrapResponse(int status, String message) {
		return wrapResponse(status, message, Json.array());
	}

	private Payload wrapResponse(int status, String message, JsonNode data) {
		int success = status < 400 ? 1 : 0;
		ObjectNode response = Json.object("response", success, "message", message, "data", data);
		return JsonPayload.json(response, status);
	}

	private Payload wrapResponse(Throwable t) {
		return JsonPayload.json(//
				Json.object("response", 0, "message", t.getMessage(), "data", Json.array()), //
				t instanceof CesioException ? 400 : 500);
	}

	private int extractInt(String paramName) {
		return Integer.valueOf(extract(paramName));
	}

	private String extract(String paramName) {
		String paramValue = SpaceContext.get().context().get(paramName);

		if (paramValue == null)
			throw new CesioException("Missing parameter: %s", paramName);

		if ("".equals(paramValue.trim()))
			throw new CesioException("%s is empty", paramName);

		return paramValue;
	}

	private void validateEmail(String email, boolean forTesting) {
		if (!forTesting && !email.endsWith("@lafargeholcim.com"))
			throw new CesioException("Email is not in domain : lafargeholcim.com");

		String regex = "^[-a-z0-9!#$%&'*+\\/=?^_`{|}~]+(\\.[-a-z0-9!#$%&'*+\\/=?^_`{|}~]+)*"//
				+ "@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

		if (!Pattern.matches(regex, email))
			throw new CesioException("Invalid Email");
	}

	private static class Score {

		@SuppressWarnings("unused")
		public Score() {
		}

		public Score(int level, int value) {
			this.score = value;
			this.level = level;
			this.date = DateTime.now();
		}

		public int score;
		public int level;
		public DateTime date;

		public ObjectNode toNode() {
			return Json.object(SCORE, "" + score, LEVEL, "" + level, //
					DATE, date.toString("yyyy-MM-dd HH:mm:ss"));
		}
	}

	private static class HighScore implements Comparable<HighScore> {
		public String email;
		public int country;
		public int score;
		public DateTime date;

		public ObjectNode toNode() {
			return Json.object("email", email, "country_id", "" + country, "somme", "" + score);
		}

		@Override
		public int compareTo(HighScore hs) {
			return hs.score - score;
		}
	}

	private static class Player {
		public int id;
		public String email;
		public int country;
		public int code;
		public Set<Score> scores = Sets.newHashSet();

		@JsonIgnore
		public String spaceId;

		@JsonIgnore
		public String meta;

		public Score score(int level) {
			for (Score score : scores)
				if (level == score.level)
					return score;

			return null;
		}

		public HighScore highScore() {
			HighScore highScore = new HighScore();
			highScore.email = email;
			highScore.country = country;
			for (Score score : scores) {
				highScore.score += score.score;
				if (highScore.date == null || highScore.date.isBefore(score.date))
					highScore.date = score.date;
			}
			return highScore;
		}

		public ObjectNode toScoresNode() {
			ObjectNode hash = Json.object();
			for (Score score : scores)
				hash.set("" + score.level, score.toNode());
			return hash;
		}

		public ObjectNode toNodeWithStringCode() {
			ObjectNode node = toNode();
			node.put(CODE, "" + code);
			return node;
		}

		public ObjectNode toNode() {
			return Json.object(ID, "" + id, EMAIL, email, //
					COUNTRY, "" + country, CODE, code);
		}
	}

	private class CesioException extends RuntimeException {

		private static final long serialVersionUID = 3166762534873553898L;

		public CesioException(String message, Object... args) {
			super(String.format(message, args));
		}

	}

	//
	// Schemas
	//

	public static Schema playerSchema() {
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

	//
	// Singleton
	//

	private static LafargeCesioResource singleton = new LafargeCesioResource();

	static LafargeCesioResource get() {
		return singleton;
	}

	private LafargeCesioResource() {
	}
}
