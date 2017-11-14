/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.model.CreateCredentialsRequest;
import io.spacedog.model.CredentialsSettings;
import io.spacedog.model.EmailTemplate;
import io.spacedog.model.Schema;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Session;
import io.spacedog.utils.Credentials.Type;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Roles;
import io.spacedog.utils.Usernames;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class CredentialsService extends SpaceService {

	public static final String TYPE = "credentials";
	public static final String SUPERDOG = "superdog";
	private static final String FORGOT_PASSWORD_MAIL_TEMPLATE_NAME = "forgotPassword";

	//
	// Type and schema
	//

	public void initIndex(String backendId) {
		Index index = credentialsIndex().backendId(backendId);
		if (!elastic().exists(index)) {
			String mapping = schema().validate().translate().toString();
			elastic().createIndex(index, mapping, false);
		}
	}

	public static Schema schema() {
		return Schema.builder(TYPE)//
				.string(USERNAME_FIELD)//
				.string(EMAIL_FIELD)//
				.string(ROLES_FIELD).array()//
				.string(GROUP_FIELD)//

				.string(HASHED_PASSWORD_FIELD)//
				.string(PASSWORD_RESET_CODE_FIELD)//
				.bool(PASSWORD_MUST_CHANGE_FIELD)//

				.bool(ENABLED_FIELD)//
				.timestamp(ENABLE_AFTER_FIELD)//
				.timestamp(DISABLE_AFTER_FIELD)//
				.integer(INVALID_CHALLENGES_FIELD)//
				.timestamp(LAST_INVALID_CHALLENGE_AT_FIELD)//

				.timestamp(CREATED_AT_FIELD)//
				.timestamp(UPDATED_AT_FIELD)//

				.object(SESSIONS_FIELD).array()//
				.string(CREATED_AT_FIELD)//
				.string(ACCESS_TOKEN_FIELD)//
				.string(ACCESS_TOKEN_EXPIRES_AT_FIELD)//
				.close()//

				.stash(STASH_FIELD)//
				.build();
	}

	//
	// Routes
	//

	@Get("/1/login")
	@Get("/1/login/")
	@Post("/1/login")
	@Post("/1/login/")
	public Payload login(Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();

		if (credentials.hasPasswordBeenChallenged()) {
			long lifetime = getCheckSessionLifetime(context);
			credentials.setCurrentSession(Session.newSession(lifetime));
			credentials = update(credentials);
		}

		return JsonPayload.ok()//
				.withFields(ACCESS_TOKEN_FIELD, credentials.accessToken(), //
						EXPIRES_IN_FIELD, credentials.accessTokenExpiresIn(), //
						CREDENTIALS_FIELD, credentials.toJson())//
				.build();
	}

	@Get("/1/logout")
	@Get("/1/logout/")
	@Post("/1/logout")
	@Post("/1/logout/")
	public Payload logout(Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		if (credentials.hasCurrentSession()) {
			credentials.deleteCurrentSession();
			update(credentials);
		}
		return JsonPayload.ok().build();
	}

	@Get("/1/credentials")
	@Get("/1/credentials/")
	public Payload getAll(Context context) {
		// TODO add more settings and permissions to control this
		// credentials check
		SpaceContext.credentials().checkAtLeastUser();

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(toQuery(context))//
				.from(context.query().getInteger(FROM_PARAM, 0)) //
				.size(context.query().getInteger(SIZE_PARAM, 10));

		return JsonPayload.ok()//
				.withObject(fromCredentialsSearch(getCredentials(builder)))//
				.build();
	}

	@Delete("/1/credentials")
	@Delete("/1/credentials/")
	public Payload deleteAll(Context context) {
		SpaceContext.credentials().checkAtLeastSuperAdmin();

		// superadmins can only be deleted when backend is deleted
		BoolQueryBuilder query = toQuery(context)//
				.mustNot(QueryBuilders.termQuery(ROLES_FIELD, Type.superadmin));

		// always refresh before and after credentials index updates
		ElasticClient elastic = elastic();
		elastic.refreshType(credentialsIndex());
		elastic.deleteByQuery(query, credentialsIndex());
		elastic.refreshType(credentialsIndex());

		return JsonPayload.ok().build();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(String body, Context context) {

		CredentialsSettings settings = credentialsSettings();

		if (!settings.guestSignUpEnabled)
			SpaceContext.credentials().checkAtLeastUser();

		Credentials credentials = createCredentialsRequestToCredentials(//
				Json.toPojo(body, CreateCredentialsRequest.class), Credentials.Type.user);
		create(credentials);

		JsonPayload payload = JsonPayload.saved(true, "/1", TYPE, credentials.id());
		if (credentials.passwordResetCode() != null)
			payload.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());

		return payload.build();
	}

	@Get("/1/credentials/me")
	@Get("/1/credentials/me/")
	public Payload getMe(Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		return JsonPayload.ok().withObject(credentials.toJson()).build();
	}

	@Get("/1/credentials/:id")
	@Get("/1/credentials/:id/")
	public Payload getById(String id, Context context) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);
		return JsonPayload.ok().withObject(credentials.toJson()).build();
	}

	@Delete("/1/credentials/me")
	@Delete("/1/credentials/me/")
	public Payload deleteMe() {
		String id = SpaceContext.credentials().checkAtLeastUser().id();
		return deleteById(id);
	}

	@Delete("/1/credentials/:id")
	@Delete("/1/credentials/:id/")
	public Payload deleteById(String id) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		// forbidden to delete last backend superadmin
		if (credentials.isSuperAdmin()) {
			if (getSuperAdmins(0, 0).total == 1)
				throw Exceptions.forbidden("backend must at least have one superadmin");
		}

		delete(id);
		return JsonPayload.ok().build();
	}

	@Put("/1/credentials/me")
	@Put("/1/credentials/me/")
	public Payload put(String body, Context context) {
		return put(SpaceContext.credentials().id(), body, context);
	}

	@Put("/1/credentials/:id")
	@Put("/1/credentials/:id/")
	public Payload put(String id, String body, Context context) {
		Credentials requester = SpaceContext.credentials();
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		if (requester.isUser())
			requester.checkPasswordHasBeenChallenged();

		ObjectNode data = Json.readObject(body);
		CredentialsSettings settings = credentialsSettings();

		String username = data.path(USERNAME_FIELD).asText();
		if (!Strings.isNullOrEmpty(username)) {
			Usernames.checkValid(username, settings.usernameRegex());
			credentials.name(username);
		}

		// TODO check email with minimal regex
		String email = data.path(EMAIL_FIELD).asText();
		if (!Strings.isNullOrEmpty(email))
			credentials.email(email);

		String password = data.path(PASSWORD_FIELD).asText();
		if (!Strings.isNullOrEmpty(password)) {
			// check for all not just users
			requester.checkPasswordHasBeenChallenged();
			credentials.changePassword(password, //
					Optional7.of(settings.passwordRegex()));
		}

		JsonNode enabled = data.get(ENABLED_FIELD);
		if (!Json.isNull(enabled)) {
			requester.checkAtLeastAdmin();
			credentials.doEnableOrDisable(enabled.asBoolean());
		}

		JsonNode enableAfter = data.get(ENABLE_AFTER_FIELD);
		if (enableAfter != null) {
			requester.checkAtLeastAdmin();
			credentials.enableAfter(enableAfter.isNull() ? null //
					: DateTime.parse(enableAfter.asText()));
		}

		JsonNode disableAfter = data.get(DISABLE_AFTER_FIELD);
		if (disableAfter != null) {
			requester.checkAtLeastAdmin();
			credentials.disableAfter(disableAfter.isNull() ? null //
					: DateTime.parse(disableAfter.asText()));
		}

		// TODO check if at least one field has been changed
		// before credentials update
		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Post("/1/credentials/forgotPassword")
	@Post("/1/credentials/forgotPassword/")
	public Payload postForgotPassword(String body, Context context) {
		Map<String, Object> parameters = Json.readMap(body);
		String username = Check.notNull(parameters.get(USERNAME_PARAM), "username").toString();

		Credentials credentials = getByName(username, true).get();

		if (!credentials.email().isPresent())
			throw Exceptions.illegalArgument("no email found in credentials [%s][%s]", //
					credentials.type(), credentials.name());

		EmailTemplate template = EmailService.get()//
				.getTemplate(FORGOT_PASSWORD_MAIL_TEMPLATE_NAME)//
				.orElseThrow(() -> Exceptions.illegalArgument(//
						"email template [forgotPassword] not found"));

		// make sure the model has at least the username parameter
		if (template.model == null)
			template.model = Maps.newHashMap();
		template.model.put(USERNAME_PARAM, "string");

		Map<String, Object> mailContext = PebbleTemplating.get()//
				.createContext(template.model, parameters);

		credentials.newPasswordResetCode();
		update(credentials);

		mailContext.put("to", credentials.email().get());
		mailContext.put("credentialsId", credentials.id());
		mailContext.put("passwordResetCode", credentials.passwordResetCode());

		EmailService.get().email(template, mailContext);

		return JsonPayload.ok().build();
	}

	@Delete("/1/credentials/:id/password")
	@Delete("/1/credentials/:id/password/")
	public Payload deletePassword(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		credentials.clearPasswordAndTokens();
		credentials.newPasswordResetCode();
		credentials = update(credentials);

		return JsonPayload.saved(false, "/1", TYPE, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode())//
				.build();
	}

	@Post("/1/credentials/:id/password")
	@Post("/1/credentials/:id/password/")
	public Payload postPassword(String id, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		String passwordResetCode = context.get(PASSWORD_RESET_CODE_FIELD);
		String password = context.get(PASSWORD_FIELD);

		Credentials credentials = getById(id, true).get();
		CredentialsSettings settings = credentialsSettings();
		credentials.changePassword(password, passwordResetCode, //
				Optional7.of(settings.passwordRegex()));
		credentials = update(credentials);

		return saved(false, credentials);
	}

	@Put("/1/credentials/me/password")
	@Put("/1/credentials/me/password/")
	public Payload putMyPassword(String body, Context context) {
		return putPassword(SpaceContext.credentials().id(), body, context);
	}

	@Put("/1/credentials/:id/password")
	@Put("/1/credentials/:id/password/")
	public Payload putPassword(String id, String body, Context context) {

		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, true);

		String password = SpaceContext.get().isJsonContent() && !Strings.isNullOrEmpty(body)//
				? Json.checkString(Json.checkNotNull(Json.readNode(body)))//
				: context.get(PASSWORD_FIELD);

		CredentialsSettings settings = credentialsSettings();
		credentials.changePassword(password, Optional7.of(settings.passwordRegex()));

		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Put("/1/credentials/:id/passwordMustChange")
	@Put("/1/credentials/:id/passwordMustChange/")
	public Payload putPasswordMustChange(String id, String body, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		Boolean passwordMustChange = Json.checkBoolean(//
				Json.checkNotNull(Json.readNode(body)));
		credentials.passwordMustChange(passwordMustChange);

		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Put("/1/credentials/:id/enabled")
	@Put("/1/credentials/:id/enabled/")
	public Payload putEnabled(String id, String body, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		JsonNode enabled = Json.readNode(body);
		if (!enabled.isBoolean())
			throw Exceptions.illegalArgument("body not a boolean but [%s]", body);

		credentials.doEnableOrDisable(enabled.asBoolean());
		credentials = update(credentials);

		return saved(false, credentials);
	}

	@Get("/1/credentials/:id/roles")
	@Get("/1/credentials/:id/roles/")
	public Object getRoles(String id, Context context) {
		return checkMyselfOrHigherAdminAndGet(id, false).roles();
	}

	@Delete("/1/credentials/:id/roles")
	@Delete("/1/credentials/:id/roles/")
	public Payload deleteAllRoles(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.clearRoles();
		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Put("/1/credentials/:id/roles/:role")
	@Put("/1/credentials/:id/roles/:role/")
	public Payload putRole(String id, String role, Context context) {
		Roles.checkIfValid(role);
		Credentials requester = SpaceContext.credentials();
		Credentials updated = checkAdminAndGet(id);
		requester.checkAuthorizedToSet(role);

		if (!updated.roles().contains(role)) {
			updated.addRoles(role);
			updated = update(updated);
		}

		return saved(false, updated);
	}

	@Delete("/1/credentials/:id/roles/:role")
	@Delete("/1/credentials/:id/roles/:role/")
	public Payload deleteRole(String id, String role, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		if (credentials.roles().contains(role)) {
			credentials.removeRoles(role);
			credentials = update(credentials);
			return saved(false, credentials);
		}
		return JsonPayload.error(HttpStatus.NOT_FOUND).build();
	}

	//
	// Internal services
	//

	long getCheckSessionLifetime(Context context) {
		CredentialsSettings settings = credentialsSettings();
		long lifetime = context.query().getLong(LIFETIME_PARAM, settings.sessionMaximumLifetime);
		if (lifetime > settings.sessionMaximumLifetime)
			throw Exceptions.forbidden("maximum access token lifetime is [%s] seconds", //
					settings.sessionMaximumLifetime);
		return lifetime;
	}

	Credentials checkUsernamePassword(String username, String password) {

		if (username.equals(SUPERDOG))
			return checkSuperdog(password);

		return checkRegularUser(username, password);
	}

	Credentials checkRegularUser(String username, String password) {
		try {
			Optional<Credentials> credentials = getByName(username, false);

			if (credentials.isPresent()) {
				if (credentials.get().challengePassword(password))
					return credentials.get();
				else
					updateInvalidChallenges(credentials.get());
			}

		} catch (IndexNotFoundException ignore) {
		}

		throw Exceptions.invalidUsernamePassword();
	}

	private Credentials checkSuperdog(String password) {
		ServerConfiguration conf = Start.get().configuration();
		if (password == null || !password.equals(conf.superdogPassword()))
			throw Exceptions.invalidUsernamePassword();

		return new Credentials(SUPERDOG)//
				.addRoles(Credentials.Type.superdog.toString())//
				.id(SUPERDOG);
	}

	private void updateInvalidChallenges(Credentials credentials) {
		CredentialsSettings settings = credentialsSettings();

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

		try {
			SearchHits hits = elastic().prepareSearch(credentialsIndex())//
					.setQuery(QueryBuilders.termQuery(//
							SESSIONS_ACCESS_TOKEN_FIELD, accessToken))//
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
					"access token [%s] associated with [%s] credentials", //
					accessToken, hits.totalHits());

		} catch (IndexNotFoundException e) {
			throw Exceptions.invalidAccessToken();
		}
	}

	Optional<Credentials> getById(String id, boolean throwNotFound) {
		Credentials credentials = SpaceContext.credentials();

		if (id.equals(credentials.id()))
			return Optional.of(credentials);

		GetResponse response = elastic().get(credentialsIndex(), id);

		if (response.isExists())
			return Optional.of(toCredentials(response));

		if (throwNotFound)
			throw Exceptions.notFound(TYPE, id);
		else
			return Optional.empty();
	}

	Optional<Credentials> getByName(String username, boolean throwNotFound) {

		Optional<SearchHit> searchHit = elastic().getUnique(//
				credentialsIndex(), toQuery(username));

		if (!searchHit.isPresent()) {
			if (throwNotFound)
				throw Exceptions.notFound(TYPE, username);
			else
				return Optional.empty();
		}
		return Optional.of(toCredentials(searchHit.get()));
	}

	Credentials create(Credentials credentials) {
		return create(SpaceContext.backendId(), credentials);
	}

	Credentials create(String backendId, Credentials credentials) {

		// This is the only place where name uniqueness is checked
		if (exists(backendId, credentials.name()))
			throw Exceptions.alreadyExists(TYPE, credentials.name());

		String now = DateTime.now().toString();
		credentials.updatedAt(now);
		credentials.createdAt(now);

		String json = Json.toString(credentials);
		Index index = credentialsIndex().backendId(backendId);

		// refresh index after each index change
		IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
				? elastic().index(index, json, true) //
				: elastic().index(index, credentials.id(), json, true);

		credentials.id(response.getId());
		credentials.version(response.getVersion());
		return credentials;
	}

	Credentials update(Credentials credentials) {
		if (Strings.isNullOrEmpty(credentials.id()))
			throw Exceptions.illegalArgument(//
					"failed to update credentials since id is null");

		// TODO replace 10 by sessionsSizeMax from CredentialsSettings
		credentials.purgeOldSessions(10);
		credentials.updatedAt(DateTime.now().toString());

		// refresh index after each index change
		IndexResponse response = elastic().index(credentialsIndex(), //
				credentials.id(), Json.toString(credentials), //
				true);

		credentials.version(response.getVersion());
		return credentials;
	}

	void delete(String id) {
		// index refresh before not necessary since delete by id
		// index refresh after delete is necessary
		elastic().delete(credentialsIndex(), id, true, true);
	}

	SearchResults<Credentials> getSuperAdmins(int from, int size) {
		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(QueryBuilders.boolQuery()//
						.filter(QueryBuilders.termQuery(ROLES_FIELD, Type.superadmin)))//
				.from(from).size(size);

		return getCredentials(builder);
	}

	Credentials checkAdminAndGet(String id) {
		Credentials requester = SpaceContext.credentials().checkAtLeastAdmin();
		Credentials credentials = getById(id, true).get();
		if (credentials.isGreaterThan(requester))
			throw Exceptions.insufficientCredentials(requester);
		return credentials;
	}

	Credentials checkMyselfOrHigherAdminAndGet(String credentialsId, //
			boolean checkPasswordHasBeenChallenged) {

		Credentials requester = SpaceContext.credentials().checkAtLeastUser();

		if (checkPasswordHasBeenChallenged)
			requester.checkPasswordHasBeenChallenged();

		if (requester.id().equals(credentialsId))
			return requester;

		if (requester.isAtLeastAdmin()) {
			Credentials credentials = getById(credentialsId, true).get();
			if (credentials.isGreaterThan(requester))
				throw Exceptions.insufficientCredentials(requester);
			return credentials;
		}

		throw Exceptions.insufficientCredentials(requester);
	}

	//
	// Implementation
	//

	private Payload saved(boolean created, Credentials credentials) {
		return JsonPayload.saved(false, "/1", TYPE, credentials.id())//
				.withVersion(credentials.version()).withObject(credentials.toJson())//
				.build();
	}

	public Credentials createCredentialsRequestToCredentials(//
			CreateCredentialsRequest request, Credentials.Type type) {

		Credentials requester = SpaceContext.credentials();
		Credentials credentials = new Credentials();

		if (Utils.isNullOrEmpty(request.roles()))
			credentials.addRoles(type.name());
		else {
			requester.checkAuthorizedToSet(request.roles());
			credentials.addRoles(request.roles());
		}

		credentials.group(requester.group());
		if (Strings.isNullOrEmpty(credentials.group()))
			credentials.group(UUID.randomUUID().toString());

		CredentialsSettings settings = credentialsSettings();

		credentials.name(Check.notNullOrEmpty(request.username(), USERNAME_FIELD));
		Usernames.checkValid(credentials.name(), settings.usernameRegex());

		credentials.email(Check.notNullOrEmpty(request.email(), EMAIL_FIELD));

		if (Strings.isNullOrEmpty(request.password()))
			credentials.newPasswordResetCode();
		else
			credentials.changePassword(request.password(), //
					Optional7.of(settings.passwordRegex()));

		return credentials;
	}

	private BoolQueryBuilder toQuery(Context context) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		String username = context.get(USERNAME_FIELD);
		if (!Strings.isNullOrEmpty(username))
			query.filter(QueryBuilders.termQuery(USERNAME_FIELD, username));

		String email = context.get(EMAIL_FIELD);
		if (!Strings.isNullOrEmpty(email))
			query.filter(QueryBuilders.termQuery(EMAIL_FIELD, email));

		String role = context.get("role");
		if (!Strings.isNullOrEmpty(role))
			query.filter(QueryBuilders.termQuery(ROLES_FIELD, role));

		return query;
	}

	private BoolQueryBuilder toQuery(String username) {
		return QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(USERNAME_FIELD, username));
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
		Credentials credentials = Json.toPojo(sourceAsString, Credentials.class);
		credentials.id(id);
		credentials.version(version);
		return credentials;
	}

	private ObjectNode fromCredentialsSearch(SearchResults<Credentials> response) {
		ArrayNode results = Json.array();
		for (Credentials credentials : response.results)
			results.add(credentials.toJson());
		return Json.object("total", response.total, "results", results);
	}

	private boolean exists(String backendId, String username) {
		return elastic().exists(toQuery(username), //
				credentialsIndex().backendId(backendId));
	}

	private SearchResults<Credentials> getCredentials(SearchSourceBuilder builder) {

		Index index = credentialsIndex();
		SearchHits hits = elastic().prepareSearch(index)//
				.setSource(builder.toString()).get().getHits();

		SearchResults<Credentials> response = new SearchResults<>();
		response.type = index.type();
		response.total = hits.totalHits();
		response.results = Lists.newArrayList();

		for (SearchHit hit : hits)
			response.results.add(toCredentials(hit));

		return response;
	}

	public static Index credentialsIndex() {
		return Index.toIndex(TYPE);
	}

	protected CredentialsSettings credentialsSettings() {
		return SettingsService.get()//
				.getAsObject(CredentialsSettings.class);
	}

	//
	// singleton
	//

	private static CredentialsService singleton = new CredentialsService();

	static CredentialsService get() {
		return singleton;
	}

	private CredentialsService() {
		SettingsService.get().registerSettings(CredentialsSettings.class);
	}
}
