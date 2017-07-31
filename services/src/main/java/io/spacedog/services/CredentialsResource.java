/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.core.Json8;
import io.spacedog.model.CreateCredentialsRequest;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.MailTemplate;
import io.spacedog.model.Schema;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Session;
import io.spacedog.utils.Credentials.Type;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.Roles;
import io.spacedog.utils.SchemaTranslator;
import io.spacedog.utils.SchemaValidator;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Usernames;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class CredentialsResource extends Resource {

	private static final String FORGOT_PASSWORD_MAIL_TEMPLATE_NAME = "forgotPassword";
	public static final String TYPE = "credentials";

	//
	// init
	//

	void init() {
		Schema schema = Schema.builder(TYPE)//
				.string(FIELD_USERNAME)//
				.string(FIELD_BACKEND_ID)//
				.bool(FIELD_ENABLED)//
				.timestamp(FIELD_ENABLE_AFTER)//
				.timestamp(FIELD_DISABLE_AFTER)//
				.integer(FIELD_INVALID_CHALLENGES)//
				.timestamp(FIELD_LAST_INVALID_CHALLENGE_AT)//
				.string(FIELD_ROLES).array()//
				.string(FIELD_ACCESS_TOKEN)//
				.string(FIELD_ACCESS_TOKEN_EXPIRES_AT)//
				.string(FIELD_HASHED_PASSWORD)//
				.string(FIELD_PASSWORD_RESET_CODE)//
				.bool(FIELD_PASSWORD_MUST_CHANGE)//
				.string(FIELD_EMAIL)//
				.string(FIELD_CREATED_AT)//
				.string(FIELD_UPDATED_AT)//

				.object(FIELD_SESSIONS).array()//
				.string(FIELD_CREATED_AT)//
				.string(FIELD_ACCESS_TOKEN)//
				.string(FIELD_ACCESS_TOKEN_EXPIRES_AT)//
				.close()//

				.stash(FIELD_STASH)//

				.build();

		SchemaValidator.validate(schema.name(), schema.node());
		String mapping = SchemaTranslator.translate(schema.name(), schema.node())//
				.toString();
		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(SPACEDOG_BACKEND, TYPE))
			elastic.putMapping(SPACEDOG_BACKEND, TYPE, mapping);
		else
			elastic.createIndex(SPACEDOG_BACKEND, TYPE, mapping, false);
	}

	//
	// Routes
	//

	@Get("/1/login")
	@Get("/1/login/")
	@Post("/1/login")
	@Post("/1/login/")
	public Payload login(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();

		if (credentials.hasPasswordBeenChallenged()) {
			long lifetime = getCheckSessionLifetime(context);
			credentials.setCurrentSession(Session.newSession(lifetime));
			credentials = update(credentials);
		}

		return JsonPayload.json(//
				JsonPayload.builder()//
						.put(FIELD_ACCESS_TOKEN, credentials.accessToken()) //
						.put(FIELD_EXPIRES_IN, credentials.accessTokenExpiresIn()) //
						.node(FIELD_CREDENTIALS, credentials.toJson()));
	}

	@Get("/1/logout")
	@Get("/1/logout/")
	@Post("/1/logout")
	@Post("/1/logout/")
	public Payload logout(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		if (credentials.hasCurrentSession()) {
			credentials.deleteCurrentSession();
			update(credentials);
		}
		return JsonPayload.success();
	}

	@Get("/1/credentials")
	@Get("/1/credentials/")
	public Payload getAll(Context context) {
		// TODO add more settings and permissions to control this
		// credentials check
		SpaceContext.checkUserCredentials();
		return JsonPayload.json(fromCredentialsSearch(getCredentials(toQuery(context))));
	}

	@Delete("/1/credentials")
	@Delete("/1/credentials/")
	public Payload deleteAll(Context context) {
		SpaceContext.getCredentials().checkAtLeastSuperAdmin();
		ElasticClient elastic = Start.get().getElasticClient();
		BoolQueryBuilder query = toQuery(context).query;

		// superadmins can only be deleted when backend is deleted
		query.mustNot(QueryBuilders.termQuery(FIELD_ROLES, Type.superadmin));
		elastic.deleteByQuery(SPACEDOG_BACKEND, query, TYPE);

		// always refresh after credentials index updates
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);
		return JsonPayload.success();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(String body, Context context) {

		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		if (settings.disableGuestSignUp)
			SpaceContext.checkUserCredentials();

		Credentials credentials = credentialsRequestToCredentials(body);
		create(credentials);

		JsonBuilder<ObjectNode> builder = JsonPayload //
				.builder(true, credentials.backendId(), "/1", TYPE, credentials.id());

		if (credentials.passwordResetCode() != null)
			builder.put(FIELD_PASSWORD_RESET_CODE, credentials.passwordResetCode());

		return JsonPayload.json(builder, HttpStatus.CREATED)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, credentials.id());
	}

	@Get("/1/credentials/me")
	@Get("/1/credentials/me/")
	public Payload getMe(Context context) {
		String id = SpaceContext.checkUserCredentials().id();
		return getById(id, context);
	}

	@Get("/1/credentials/:id")
	@Get("/1/credentials/:id/")
	public Payload getById(String id, Context context) {
		Credentials credentials = checkMyselfOrAdminAndGet(id, false);
		return JsonPayload.json(credentials.toJson());
	}

	@Delete("/1/credentials/me")
	@Delete("/1/credentials/me/")
	public Payload deleteMe() {
		String id = SpaceContext.checkUserCredentials().id();
		return deleteById(id);
	}

	@Delete("/1/credentials/:id")
	@Delete("/1/credentials/:id/")
	public Payload deleteById(String id) {
		Credentials credentials = checkMyselfOrAdminAndGet(id, false);

		// forbidden to delete last backend superadmin
		if (credentials.isSuperAdmin()) {
			if (getBackendSuperAdmins(0, 0).total == 1)
				throw Exceptions.forbidden("backend must at least have one superadmin");
		}

		delete(id);
		return JsonPayload.success();
	}

	@Put("/1/credentials/me")
	@Put("/1/credentials/me/")
	public Payload put(String body, Context context) {
		String id = SpaceContext.checkUserCredentials().id();
		return put(id, body, context);
	}

	@Put("/1/credentials/:id")
	@Put("/1/credentials/:id/")
	public Payload put(String id, String body, Context context) {
		Credentials requester = SpaceContext.checkUserCredentials(id);
		if (requester.isUser())
			requester.checkPasswordHasBeenChallenged();

		ObjectNode data = Json8.readObject(body);
		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);

		String username = data.path(FIELD_USERNAME).asText();
		if (!Strings.isNullOrEmpty(username)) {
			Usernames.checkValid(username, settings.usernameRegex());
			credentials.name(username);
		}

		// TODO check email with minimal regex
		String email = data.path(FIELD_EMAIL).asText();
		if (!Strings.isNullOrEmpty(email))
			credentials.email(email);

		String password = data.path(FIELD_PASSWORD).asText();
		if (!Strings.isNullOrEmpty(password)) {
			// check for all not just users
			requester.checkPasswordHasBeenChallenged();
			Passwords.check(password, settings.passwordRegex());
			credentials.changePassword(password);
		}

		JsonNode enabled = data.get(FIELD_ENABLED);
		if (!Json8.isNull(enabled)) {
			requester.checkAtLeastAdmin();
			credentials.doEnableOrDisable(enabled.asBoolean());
		}

		JsonNode enableAfter = data.get(FIELD_ENABLE_AFTER);
		if (enableAfter != null) {
			requester.checkAtLeastAdmin();
			credentials.enableAfter(enableAfter.isNull() ? null //
					: DateTime.parse(enableAfter.asText()));
		}

		JsonNode disableAfter = data.get(FIELD_DISABLE_AFTER);
		if (disableAfter != null) {
			requester.checkAtLeastAdmin();
			credentials.disableAfter(disableAfter.isNull() ? null //
					: DateTime.parse(disableAfter.asText()));
		}

		// TODO check if at least one field has been changed
		// before credentials update
		credentials = update(credentials);
		return saved(credentials, false);
	}

	@Post("/1/credentials/forgotPassword")
	@Post("/1/credentials/forgotPassword/")
	public Payload postForgotPassword(String body, Context context) {
		Map<String, Object> parameters = Json8.readMap(body);
		String username = Check.notNull(parameters.get(PARAM_USERNAME), "username").toString();

		Credentials credentials = getByName(username, true).get();

		if (!credentials.email().isPresent())
			throw Exceptions.illegalArgument("no email found in credentials [%s][%s]", //
					credentials.type(), credentials.name());

		MailTemplate template = MailTemplateResource.get()//
				.getTemplate(FORGOT_PASSWORD_MAIL_TEMPLATE_NAME)//
				.orElseThrow(() -> Exceptions.illegalArgument(//
						"no [forgotPassword] mail template in mail settings"));

		// make sure the model has at least the username parameter
		if (template.model == null)
			template.model = Maps.newHashMap();
		template.model.put(PARAM_USERNAME, "string");

		Map<String, Object> mailContext = PebbleTemplating.get()//
				.createContext(template.model, parameters);

		credentials.newPasswordResetCode();
		update(credentials);

		mailContext.put("to", credentials.email().get());
		mailContext.put("credentialsId", credentials.id());
		mailContext.put("passwordResetCode", credentials.passwordResetCode());

		MailTemplateResource.get().sendTemplatedMail(template, mailContext);

		return JsonPayload.success();
	}

	@Delete("/1/credentials/:id/password")
	@Delete("/1/credentials/:id/password/")
	public Payload deletePassword(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		credentials.clearPasswordAndTokens();
		credentials.newPasswordResetCode();
		credentials = update(credentials);

		return JsonPayload.json(JsonPayload.builder(false, credentials.backendId(), "/1", TYPE, //
				credentials.id(), credentials.version())//
				.put(FIELD_PASSWORD_RESET_CODE, credentials.passwordResetCode()));
	}

	@Post("/1/credentials/:id/password")
	@Post("/1/credentials/:id/password/")
	public Payload postPassword(String id, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.get(FIELD_PASSWORD_RESET_CODE);
		String password = context.get(FIELD_PASSWORD);

		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		Passwords.check(password, settings.passwordRegex());
		credentials.changePassword(password, passwordResetCode);
		credentials = update(credentials);

		return saved(credentials, false);
	}

	@Put("/1/credentials/me/password")
	@Put("/1/credentials/me/password/")
	public Payload putMyPassword(String body, Context context) {
		String id = SpaceContext.checkUserCredentials().id();
		return putPassword(id, body, context);
	}

	@Put("/1/credentials/:id/password")
	@Put("/1/credentials/:id/password/")
	public Payload putPassword(String id, String body, Context context) {
		Credentials requester = SpaceContext.checkUserCredentials(id);
		requester.checkPasswordHasBeenChallenged();

		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);

		String password = SpaceContext.get().isJsonContent() && !Strings.isNullOrEmpty(body)//
				? Json8.checkString(Json8.checkNotNull(Json8.readNode(body)))//
				: context.get(FIELD_PASSWORD);

		Passwords.check(password, settings.passwordRegex());
		credentials.changePassword(password);

		credentials = update(credentials);
		return saved(credentials, false);
	}

	@Put("/1/credentials/:id/passwordMustChange")
	@Put("/1/credentials/:id/passwordMustChange/")
	public Payload putPasswordMustChange(String id, String body, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		Boolean passwordMustChange = Json8.checkBoolean(//
				Json8.checkNotNull(Json8.readNode(body)));
		credentials.passwordMustChange(passwordMustChange);

		credentials = update(credentials);
		return saved(credentials, false);
	}

	@Put("/1/credentials/:id/enabled")
	@Put("/1/credentials/:id/enabled/")
	public Payload putEnabled(String id, String body, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		JsonNode enabled = Json8.readNode(body);
		if (!enabled.isBoolean())
			throw Exceptions.illegalArgument("body not a boolean but [%s]", body);

		credentials.doEnableOrDisable(enabled.asBoolean());
		credentials = update(credentials);

		return saved(credentials, false);
	}

	@Get("/1/credentials/:id/roles")
	@Get("/1/credentials/:id/roles/")
	public Object getRoles(String id, Context context) {
		return checkMyselfOrAdminAndGet(id, false).roles();
	}

	@Delete("/1/credentials/:id/roles")
	@Delete("/1/credentials/:id/roles/")
	public Payload deleteAllRoles(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.roles().clear();
		credentials = update(credentials);
		return saved(credentials, false);
	}

	@Put("/1/credentials/:id/roles/:role")
	@Put("/1/credentials/:id/roles/:role/")
	public Payload putRole(String id, String role, Context context) {
		Roles.checkIfValid(role);
		Credentials credentials = checkAdminAndGet(id);
		credentials.checkAuthorizedToManage(role);

		if (!credentials.roles().contains(role)) {
			credentials.roles().add(role);
			credentials = update(credentials);
		}

		return saved(credentials, false);
	}

	@Delete("/1/credentials/:id/roles/:role")
	@Delete("/1/credentials/:id/roles/:role/")
	public Payload deleteRole(String id, String role, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		if (credentials.roles().contains(role)) {
			credentials.roles().remove(role);
			credentials = update(credentials);
			return saved(credentials, false);
		}
		return JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	//
	// Internal services
	//

	long getCheckSessionLifetime(Context context) {
		CredentialsSettings settings = SettingsResource.get().load(CredentialsSettings.class);
		long lifetime = context.query().getLong(PARAM_LIFETIME, settings.sessionMaximumLifetime);
		if (lifetime > settings.sessionMaximumLifetime)
			throw Exceptions.forbidden("maximum access token lifetime is [%s] seconds", //
					settings.sessionMaximumLifetime);
		return lifetime;
	}

	Credentials checkUsernamePassword(String username, String password) {
		Optional<Credentials> optional = getByName(username, false);

		if (optional.isPresent()) {

			Credentials credentials = optional.get();

			if (credentials.challengePassword(password))
				return credentials;
			else
				updateInvalidChallenges(credentials);
		}

		throw Exceptions.invalidUsernamePassword();
	}

	private void updateInvalidChallenges(Credentials credentials) {
		CredentialsSettings settings = SettingsResource.get()//
				.load(CredentialsSettings.class);

		if (settings.maximumInvalidChallenges == 0)
			return;

		if (credentials.lastInvalidChallengeAt() != null //
				&& credentials.lastInvalidChallengeAt()//
						.plusMinutes(settings.resetInvalidChallengesAfterMinutes)//
						.isBeforeNow()) {

			credentials.invalidChallenges(0);
			credentials.lastInvalidChallengeAt(null);
		}

		credentials.invalidChallenges(credentials.invalidChallenges() + 1);
		credentials.lastInvalidChallengeAt(DateTime.now());

		if (credentials.invalidChallenges() >= settings.maximumInvalidChallenges)
			credentials.doEnableOrDisable(false);

		update(credentials);
	}

	Credentials checkToken(String accessToken) {

		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setQuery(QueryBuilders.boolQuery()//
						// I'll check backendId later in this method
						// to let superdogs access all backends
						.must(QueryBuilders.termQuery(FIELD_SESSIONS_ACCESS_TOKEN, accessToken)))//
				.get()//
				.getHits();

		if (hits.totalHits() == 0)
			throw Exceptions.invalidAccessToken();

		if (hits.totalHits() == 1) {
			Credentials credentials = toCredentials(hits.getAt(0));
			credentials.setCurrentSession(accessToken);

			if (credentials.accessTokenExpiresIn() == 0)
				throw Exceptions.accessTokenHasExpired();

			return credentials;
		}

		throw Exceptions.runtime(//
				"access token [%s] associated with too many credentials [%s]", //
				accessToken, hits.totalHits());
	}

	Optional<Credentials> getById(String id, boolean throwNotFound) {
		Credentials credentials = SpaceContext.getCredentials();

		if (id.equals(credentials.id()))
			return Optional.of(credentials);

		GetResponse response = Start.get().getElasticClient().get(SPACEDOG_BACKEND, TYPE, id);

		if (response.isExists())
			return Optional.of(toCredentials(response));

		if (throwNotFound)
			throw Exceptions.notFound(TYPE, id);
		else
			return Optional.empty();
	}

	Optional<Credentials> getByName(String username, boolean throwNotFound) {

		Optional<SearchHit> searchHit = Start.get().getElasticClient().get(//
				SPACEDOG_BACKEND, TYPE, toQuery(username));

		if (!searchHit.isPresent()) {
			if (throwNotFound)
				throw Exceptions.notFound(TYPE, username);
			else
				return Optional.empty();
		}
		return Optional.of(toCredentials(searchHit.get()));
	}

	Credentials create(Credentials credentials) {

		// This is the only place where name uniqueness is checked
		if (exists(credentials.backendId(), credentials.name()))
			throw Exceptions.alreadyExists(TYPE, credentials.name());

		try {
			String now = DateTime.now().toString();
			credentials.updatedAt(now);
			credentials.createdAt(now);

			ElasticClient elastic = Start.get().getElasticClient();
			String json = Json8.mapper().writeValueAsString(credentials);

			// refresh index after each index change
			IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
					? elastic.index(SPACEDOG_BACKEND, TYPE, json, true) //
					: elastic.index(SPACEDOG_BACKEND, TYPE, credentials.id(), json, true);

			credentials.id(response.getId());
			credentials.version(response.getVersion());
			return credentials;

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	Credentials update(Credentials credentials) {
		if (Strings.isNullOrEmpty(credentials.id()))
			throw Exceptions.illegalArgument(//
					"failed to update credentials since id is null");

		try {
			// TODO replace 10 by sessionsSizeMax from CredentialsSettings
			credentials.purgeOldSessions(10);
			credentials.updatedAt(DateTime.now().toString());

			// refresh index after each index change
			IndexResponse response = Start.get().getElasticClient().index(//
					SPACEDOG_BACKEND, TYPE, credentials.id(), //
					Json8.mapper().writeValueAsString(credentials), //
					true);

			credentials.version(response.getVersion());
			return credentials;

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	void delete(String id) {
		ElasticClient elastic = Start.get().getElasticClient();
		// index refresh before not necessary since delete by id
		// index refresh after delete is necessary
		elastic.delete(SPACEDOG_BACKEND, TYPE, id, true, true);
	}

	DeleteByQueryResponse deleteAll(String backendId) {
		ElasticClient elastic = Start.get().getElasticClient();

		// need to refresh index before and after delete
		elastic.refreshType(SPACEDOG_BACKEND, TYPE);

		DeleteByQueryResponse response = elastic.deleteByQuery(SPACEDOG_BACKEND,
				QueryBuilders.termQuery(FIELD_BACKEND_ID, backendId), TYPE);

		elastic.refreshType(SPACEDOG_BACKEND, TYPE);
		return response;
	}

	SearchResults<Credentials> getAllSuperAdmins(int from, int size) {
		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(FIELD_ROLES, Type.superadmin));

		return getCredentials(new BoolSearch(SPACEDOG_BACKEND, TYPE, query, from, size));
	}

	SearchResults<Credentials> getBackendSuperAdmins(int from, int size) {
		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(FIELD_ROLES, Type.superadmin));

		return getCredentials(new BoolSearch(SPACEDOG_BACKEND, TYPE, query, from, size));
	}

	Credentials createSuperdog(String username, String password, String email) {
		Usernames.checkValid(username);
		Credentials credentials = new Credentials(Backends.rootApi(), username);
		credentials.roles(Type.superdog.name());
		credentials.email(email);
		Passwords.check(password);
		credentials.changePassword(password);
		return create(credentials);
	}

	Credentials checkAdminAndGet(String id) {
		Credentials requester = SpaceContext.checkAdminCredentials();
		Credentials credentials = getById(id, true).get();
		if (credentials.isGreaterThan(requester))
			throw Exceptions.insufficientCredentials(requester);
		return credentials;
	}

	Credentials checkMyselfOrAdminAndGet(String id, boolean checkPasswordHasBeenChallenged) {
		Credentials requester = SpaceContext.checkUserCredentials();

		if (checkPasswordHasBeenChallenged)
			requester.checkPasswordHasBeenChallenged();

		if (requester.id().equals(id))
			return requester;

		if (requester.isAtLeastAdmin()) {
			Credentials credentials = getById(id, true).get();
			if (credentials.isGreaterThan(requester))
				throw Exceptions.insufficientCredentials(requester);
			return credentials;
		}

		throw Exceptions.insufficientCredentials(requester);
	}

	//
	// Implementation
	//

	private Payload saved(Credentials credentials, boolean created) {
		return JsonPayload.saved(false, credentials.backendId(), "/1", TYPE, //
				credentials.id(), credentials.version());
	}

	private Credentials credentialsRequestToCredentials(String body) {

		Credentials credentials = new Credentials(SpaceContext.backendId());
		CreateCredentialsRequest request = Json8.toPojo(body, CreateCredentialsRequest.class);

		if (Utils.isNullOrEmpty(request.roles()))
			credentials.roles(Type.user.name());
		else
			credentials.roles(request.roles());

		Credentials requester = SpaceContext.getCredentials();
		if (credentials.isGreaterThan(requester))
			throw Exceptions.insufficientCredentials(requester);

		CredentialsSettings settings = SettingsResource.get()//
				.load(CredentialsSettings.class);

		credentials.name(Check.notNullOrEmpty(request.username(), FIELD_USERNAME));
		Usernames.checkValid(credentials.name(), settings.usernameRegex());

		credentials.email(Check.notNullOrEmpty(request.email(), FIELD_EMAIL));

		if (Strings.isNullOrEmpty(request.password()))
			credentials.newPasswordResetCode();
		else {
			Passwords.check(request.password(), settings.passwordRegex());
			credentials.changePassword(request.password());
		}

		return credentials;
	}

	private BoolSearch toQuery(Context context) {
		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(FIELD_BACKEND_ID, SpaceContext.backendId()));

		String username = context.get(FIELD_USERNAME);
		if (!Strings.isNullOrEmpty(username))
			query.filter(QueryBuilders.termQuery(FIELD_USERNAME, username));

		String email = context.get(FIELD_EMAIL);
		if (!Strings.isNullOrEmpty(email))
			query.filter(QueryBuilders.termQuery(FIELD_EMAIL, email));

		String role = context.get("role");
		if (!Strings.isNullOrEmpty(role))
			query.filter(QueryBuilders.termQuery(FIELD_ROLES, role));

		BoolSearch search = new BoolSearch(SPACEDOG_BACKEND, TYPE, query, //
				context.query().getInteger(PARAM_FROM, 0), //
				context.query().getInteger(PARAM_SIZE, 10));

		return search;
	}

	private BoolQueryBuilder toQuery(String username) {
		return QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(FIELD_USERNAME, username));
	}

	private Credentials toCredentials(SearchHit hit) {
		return toCredentials(hit.getSourceAsString(), //
				hit.getId(), hit.getVersion());
	}

	private Credentials toCredentials(GetResponse response) {
		return toCredentials(response.getSourceAsString(), //
				response.getId(), response.getVersion());
	}

	private Credentials toCredentials(String sourceAsString, String id, long version) {
		try {
			Credentials credentials = Json8.mapper()//
					.readValue(sourceAsString, Credentials.class);
			credentials.id(id);
			credentials.version(version);
			return credentials;

		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	private ObjectNode fromCredentialsSearch(SearchResults<Credentials> response) {
		ArrayNode results = Json8.array();
		for (Credentials credentials : response.results)
			results.add(credentials.toJson());
		return Json8.object("total", response.total, "results", results);
	}

	private boolean exists(String backendId, String username) {
		return Start.get().getElasticClient().exists(SPACEDOG_BACKEND, TYPE, //
				toQuery(username));
	}

	private SearchResults<Credentials> getCredentials(BoolSearch query) {
		ElasticClient elastic = Start.get().getElasticClient();
		Check.isTrue(query.from + query.size <= 1000, "from + size is greater than 1000");

		SearchHits hits = elastic//
				.prepareSearch(query.backendId, query.type)//
				.setQuery(query.query)//
				.setFrom(query.from)//
				.setSize(query.size)//
				.get()//
				.getHits();

		SearchResults<Credentials> response = new SearchResults<>();
		response.backendId = query.backendId;
		response.type = query.type;
		response.from = query.from;
		response.size = query.size;
		response.total = hits.totalHits();
		response.results = Lists.newArrayList();

		for (SearchHit hit : hits)
			response.results.add(toCredentials(hit));

		return response;
	}

	//
	// singleton
	//

	private static CredentialsResource singleton = new CredentialsResource();

	static CredentialsResource get() {
		return singleton;
	}

	private CredentialsResource() {
		SettingsResource.get().registerSettingsClass(CredentialsSettings.class);
	}
}
