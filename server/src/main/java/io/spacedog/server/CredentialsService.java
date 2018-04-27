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
import org.elasticsearch.index.query.QueryBuilder;
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

import io.spacedog.client.credentials.CreateCredentialsRequest;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Session;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.credentials.SetPasswordRequest;
import io.spacedog.client.credentials.Usernames;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
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
	private static final String PASSWORD_RESET_EMAIL_TEMPLATE_NAME = "password_reset_email_template";

	//
	// Type and schema
	//

	public void initIndex() {
		Index index = credentialsIndex();
		if (!elastic().exists(index)) {
			String mapping = schema().mapping().toString();
			elastic().createIndex(index, mapping, false);
		}
	}

	public static Schema schema() {
		return Schema.builder(TYPE)//
				.keyword(USERNAME_FIELD)//
				.keyword(EMAIL_FIELD)//
				.keyword(ROLES_FIELD)//
				.keyword(GROUP_FIELD)//
				.keyword(TAGS_FIELD)//

				.keyword(HASHED_PASSWORD_FIELD)//
				.keyword(PASSWORD_RESET_CODE_FIELD)//
				.bool(PASSWORD_MUST_CHANGE_FIELD)//

				.bool(ENABLED_FIELD)//
				.timestamp(ENABLE_AFTER_FIELD)//
				.timestamp(DISABLE_AFTER_FIELD)//
				.integer(INVALID_CHALLENGES_FIELD)//
				.timestamp(LAST_INVALID_CHALLENGE_AT_FIELD)//

				.timestamp(CREATED_AT_FIELD)//
				.timestamp(UPDATED_AT_FIELD)//

				.object(SESSIONS_FIELD)//
				.keyword(CREATED_AT_FIELD)//
				.keyword(ACCESS_TOKEN_FIELD)//
				.keyword(ACCESS_TOKEN_EXPIRES_AT_FIELD)//
				.closeObject()//

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
		SpaceContext.credentials().checkAtLeastAdmin();

		String q = context.get(Q_PARAM);
		QueryBuilder query = Strings.isNullOrEmpty(q) //
				? QueryBuilders.matchAllQuery() //
				: QueryBuilders.simpleQueryStringQuery(q);

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(query)//
				.from(context.query().getInteger(FROM_PARAM, 0)) //
				.size(context.query().getInteger(SIZE_PARAM, 10));

		return JsonPayload.ok()//
				.withContent(fromCredentialsSearch(getCredentials(builder)))//
				.build();
	}

	@Delete("/1/credentials")
	@Delete("/1/credentials/")
	public Payload deleteAll(Context context) {
		SpaceContext.credentials().checkAtLeastSuperAdmin();

		// superadmins can not be deleted this way
		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.mustNot(QueryBuilders.termQuery(ROLES_FIELD, Roles.superadmin));

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

		Credentials credentials = create(//
				Json.toPojo(body, CreateCredentialsRequest.class), //
				Roles.user);

		JsonPayload payload = JsonPayload.saved(true, "/1", TYPE, credentials.id());
		if (credentials.passwordResetCode() != null)
			payload.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());

		return payload.build();
	}

	@Get("/1/credentials/me")
	@Get("/1/credentials/me/")
	public Payload getMe(Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		return JsonPayload.ok().withContent(credentials.toJson()).build();
	}

	@Get("/1/credentials/:id")
	@Get("/1/credentials/:id/")
	public Payload getById(String id, Context context) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);
		return JsonPayload.ok().withContent(credentials.toJson()).build();
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
			checkAlreadyExists(username);
			credentials.name(username);
		}

		// TODO check email with minimal regex
		String email = data.path(EMAIL_FIELD).asText();
		if (!Strings.isNullOrEmpty(email))
			credentials.email(email);

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

	@Post("/1/credentials/_send_password_reset_email")
	@Post("/1/credentials/_send_password_reset_email/")
	public Payload postSendPasswordResetEmail(String body, Context context) {
		Map<String, Object> parameters = Json.readMap(body);
		String username = Check.notNull(parameters.get(USERNAME_PARAM), "username").toString();

		Credentials credentials = getByName(username, true).get();

		if (!credentials.email().isPresent())
			throw Exceptions.illegalArgument("credentials [%s][%s] has no email", //
					credentials.type(), credentials.username());

		EmailTemplate template = EmailService.get()//
				.getTemplate(PASSWORD_RESET_EMAIL_TEMPLATE_NAME)//
				.orElseThrow(() -> Exceptions.illegalArgument(//
						"email template [%s] not found", //
						PASSWORD_RESET_EMAIL_TEMPLATE_NAME));

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

	@Post("/1/credentials/:id/_reset_password")
	@Post("/1/credentials/:id/_reset_password/")
	public Payload postResetPassword(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);

		credentials.clearPasswordAndTokens();
		credentials.newPasswordResetCode();
		credentials = update(credentials);

		return JsonPayload.saved(false, "/1", TYPE, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode())//
				.build();
	}

	@Post("/1/credentials/me/_set_password")
	@Post("/1/credentials/me/_set_password/")
	public Payload postSetMyPassword(String body, Context context) {
		return postSetPassword(SpaceContext.credentials().id(), body, context);
	}

	@Post("/1/credentials/:id/_set_password")
	@Post("/1/credentials/:id/_set_password/")
	public Payload postSetPassword(String id, String body, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		Credentials credentials = null;
		SetPasswordRequest request = Json.toPojo(body, SetPasswordRequest.class);
		Optional7<String> passwordRegex = Optional7.of(//
				credentialsSettings().passwordRegex());

		if (Strings.isNullOrEmpty(request.passwordResetCode())) {
			credentials = checkMyselfOrHigherAdminAndGet(id, true);
			credentials.changePassword(request.password(), passwordRegex);

		} else {
			credentials = getById(id, true).get();
			credentials.changePassword(request.password(), //
					request.passwordResetCode(), passwordRegex);
		}

		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Post("/1/credentials/:id/_password_must_change")
	@Post("/1/credentials/:id/_password_must_change/")
	public Payload postForcePasswordUpdate(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.passwordMustChange(true);
		credentials = update(credentials);
		return saved(false, credentials);
	}

	@Post("/1/credentials/:id/_enable")
	@Post("/1/credentials/:id/_enable/")
	public Payload postEnable(String id, String body, Context context) {
		return doEnableOrDisable(id, true);
	}

	@Post("/1/credentials/:id/_disable")
	@Post("/1/credentials/:id/_disable/")
	public Payload postDisable(String id, String body, Context context) {
		return doEnableOrDisable(id, false);
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
		requester.checkCanManage(role);

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

	public Payload doEnableOrDisable(String credentialsId, boolean enable) {
		Credentials credentials = checkAdminAndGet(credentialsId);
		credentials.doEnableOrDisable(enable);
		credentials = update(credentials);
		return saved(false, credentials);
	}

	long getCheckSessionLifetime(Context context) {
		CredentialsSettings settings = credentialsSettings();
		long lifetime = context.query().getLong(LIFETIME_PARAM, settings.sessionMaximumLifetime);
		if (lifetime > settings.sessionMaximumLifetime)
			throw Exceptions.forbidden("maximum access token lifetime is [%s] seconds", //
					settings.sessionMaximumLifetime);
		return lifetime;
	}

	Credentials checkUsernamePassword(String username, String password) {

		if (username.equals(Credentials.SUPERDOG.username()))
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
		ServerConfiguration conf = Server.get().configuration();
		if (password == null || !password.equals(conf.superdogPassword()))
			throw Exceptions.invalidUsernamePassword();

		return Credentials.SUPERDOG;
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

			if (hits.getTotalHits() == 0)
				throw Exceptions.invalidAccessToken();

			if (hits.getTotalHits() == 1) {
				Credentials credentials = toCredentials(hits.getAt(0));
				credentials.setCurrentSession(accessToken);

				if (credentials.accessTokenExpiresIn() == 0)
					throw Exceptions.accessTokenHasExpired();

				return credentials;
			}

			throw Exceptions.runtime(//
					"access token [%s] associated with [%s] credentials", //
					accessToken, hits.getTotalHits());

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

	public Credentials create(Credentials credentials) {

		checkAlreadyExists(credentials.username());

		String now = DateTime.now().toString();
		credentials.updatedAt(now);
		credentials.createdAt(now);

		Index index = credentialsIndex();

		// refresh index after each index change
		IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
				? elastic().index(index, credentials, true) //
				: elastic().index(index, credentials.id(), credentials, true);

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
				credentials.id(), credentials, true);

		credentials.version(response.getVersion());
		return credentials;
	}

	void delete(String id) {
		// index refresh before not necessary since delete by id
		// index refresh after delete is necessary
		elastic().delete(credentialsIndex(), id, true, true);
	}

	public SearchResults<Credentials> getSuperAdmins(int from, int size) {
		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(QueryBuilders.boolQuery()//
						.filter(QueryBuilders.termQuery(ROLES_FIELD, Roles.superadmin)))//
				.from(from).size(size);

		return getCredentials(builder);
	}

	Credentials checkAdminAndGet(String id) {
		Credentials requester = SpaceContext.credentials().checkAtLeastAdmin();
		Credentials credentials = getById(id, true).get();
		requester.checkCanManage(credentials);
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
			requester.checkCanManage(credentials);
			return credentials;
		}

		throw Exceptions.insufficientCredentials(requester);
	}

	//
	// Implementation
	//

	private Payload saved(boolean created, Credentials credentials) {
		return JsonPayload.saved(false, "/1", TYPE, credentials.id())//
				.withVersion(credentials.version()).withContent(credentials.toJson())//
				.build();
	}

	public Credentials create(CreateCredentialsRequest request, String defaultRole) {

		Credentials requester = SpaceContext.credentials();
		Credentials credentials = new Credentials();

		if (Utils.isNullOrEmpty(request.roles()))
			credentials.addRoles(defaultRole);
		else {
			requester.checkCanManage(request.roles());
			credentials.addRoles(request.roles());
		}

		credentials.group(requester.group());
		if (Strings.isNullOrEmpty(credentials.group()))
			credentials.group(UUID.randomUUID().toString());

		CredentialsSettings settings = credentialsSettings();

		credentials.name(Check.notNullOrEmpty(request.username(), USERNAME_FIELD));
		Usernames.checkValid(credentials.username(), settings.usernameRegex());

		credentials.email(Check.notNullOrEmpty(request.email(), EMAIL_FIELD));

		if (Strings.isNullOrEmpty(request.password()))
			credentials.newPasswordResetCode();
		else
			credentials.changePassword(request.password(), //
					Optional7.of(settings.passwordRegex()));

		create(credentials);
		return credentials;
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

	private void checkAlreadyExists(String username) {
		if (elastic().exists(toQuery(username), credentialsIndex()))
			throw Exceptions.alreadyExists(TYPE, username);
	}

	private SearchResults<Credentials> getCredentials(SearchSourceBuilder builder) {

		Index index = credentialsIndex();
		SearchHits hits = elastic().prepareSearch(index)//
				.setSource(builder).get().getHits();

		SearchResults<Credentials> response = new SearchResults<>();
		response.type = index.type();
		response.total = hits.getTotalHits();
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

	public static CredentialsService get() {
		return singleton;
	}

	private CredentialsService() {
		SettingsService.get().registerSettings(CredentialsSettings.class);
	}
}
