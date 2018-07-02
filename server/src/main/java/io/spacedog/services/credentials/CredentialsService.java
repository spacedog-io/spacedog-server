package io.spacedog.services.credentials;

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.CredentialsUpdateRequest;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.credentials.Usernames;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.email.EmailTemplateRequest;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.ElasticClient;
import io.spacedog.server.Index;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceService;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

public class CredentialsService extends SpaceService implements SpaceParams, SpaceFields {

	//
	// Get Credentials
	//

	public Credentials me() {
		return Server.context().credentials();
	}

	public boolean exists(String username) {
		return elastic().exists(toQuery(username), index());
	}

	public Optional<Credentials> getByUsername(String username) {
		return elastic()//
				.getUnique(index(), toQuery(username))//
				.map(hit -> toCredentials(hit));
	}

	public Credentials get(String id) {
		Credentials credentials = Server.context().credentials();

		if (id.equals(credentials.id()))
			return credentials;

		GetResponse response = elastic().get(index(), id);

		if (response.isExists())
			return toCredentials(response);

		throw Exceptions.notFound(SERVICE_NAME, id);
	}

	//
	// Get All Credentials
	//

	public Credentials.Results getAll() {
		return getAll((String) null);
	}

	public Credentials.Results getAll(int from, int size) {
		return getAll(null, from, size);
	}

	public Credentials.Results getAll(String q) {
		return getAll(q, 0, 10);
	}

	public Credentials.Results getAll(String q, int from, int size) {

		QueryBuilder query = Strings.isNullOrEmpty(q) //
				? QueryBuilders.matchAllQuery() //
				: QueryBuilders.simpleQueryStringQuery(q);

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(query).from(from).size(size);

		return getAll(builder);
	}

	public Credentials.Results getAll(SearchSourceBuilder builder) {

		SearchHits hits = elastic()//
				.prepareSearch(index())//
				.setSource(builder).get().getHits();

		Credentials.Results response = new Credentials.Results();
		response.total = hits.getTotalHits();
		response.results = Lists.newArrayList();

		for (SearchHit hit : hits)
			response.results.add(toCredentials(hit));

		return response;
	}

	public boolean existsMoreThanOneSuperAdmin() {
		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(QueryBuilders.termQuery(ROLES_FIELD, Roles.superadmin))//
				.size(0);

		return getAll(builder).total > 1;
	}

	//
	// Login logout
	//

	public Credentials logout() {
		Credentials credentials = Server.context().credentials();
		if (credentials.hasCurrentSession()) {
			credentials.deleteCurrentSession();
			credentials = Services.credentials().update(credentials);
		}
		return credentials;
	}

	public Credentials logout(String id, String accessToken) {
		Credentials credentials = get(id);
		credentials.deleteSession(accessToken);
		return Services.credentials().update(credentials);
	}

	public Credentials checkUsernamePassword(String username, String password) {

		if (username.equals(Credentials.SUPERDOG.username()))
			return checkSuperdog(password);

		return checkRegularUser(username, password);
	}

	Credentials checkRegularUser(String username, String password) {
		try {
			Optional<Credentials> credentials = getByUsername(username);

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

	private void updateInvalidChallenges(Credentials credentials) {
		CredentialsSettings settings = settings();

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

	public Credentials checkToken(String accessToken) {

		try {

			Credentials.Results results = getAll(//
					SearchSourceBuilder.searchSource()//
							.query(QueryBuilders.termQuery(//
									SESSIONS_ACCESS_TOKEN_FIELD, accessToken))//
							.size(1));

			if (results.total == 0)
				throw Exceptions.invalidAccessToken();

			if (results.total == 1) {
				Credentials credentials = results.results.get(0);
				credentials.setCurrentSession(accessToken);

				if (credentials.accessTokenExpiresIn() == 0)
					throw Exceptions.accessTokenHasExpired();

				return credentials;
			}

			throw Exceptions.runtime(//
					"access token [%s] associated with [%s] credentials", //
					accessToken, results.total);

		} catch (IndexNotFoundException e) {
			throw Exceptions.invalidAccessToken();
		}
	}

	private Credentials checkSuperdog(String password) {
		if (password == null || !password.equals(ServerConfig.superdogPassword()))
			throw Exceptions.invalidUsernamePassword();

		return Credentials.SUPERDOG;
	}

	//
	// Create Credentials
	//

	public Credentials create(Credentials credentials) {

		if (exists(credentials.username()))
			throw Exceptions.alreadyExists(//
					SERVICE_NAME, credentials.username());

		String now = DateTime.now().toString();
		credentials.updatedAt(now);
		credentials.createdAt(now);

		Index index = index();

		// refresh index after each index change
		IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
				? elastic().index(index, credentials, true) //
				: elastic().index(index, credentials.id(), credentials, true);

		credentials.id(response.getId());
		credentials.version(response.getVersion());
		return credentials;
	}

	public Credentials create(String username, String password, String email, String... roles) {
		CredentialsCreateRequest request = new CredentialsCreateRequest()//
				.username(username).password(password).email(email).roles(roles);
		return create(request, Roles.user);
	}

	public Credentials create(CredentialsCreateRequest request, String defaultRole) {

		Credentials requester = Server.context().credentials();
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

		CredentialsSettings settings = settings();

		credentials.name(Check.notNullOrEmpty(request.username(), USERNAME_FIELD));
		Usernames.checkValid(credentials.username(), settings.usernameRegex());

		credentials.email(Check.notNullOrEmpty(request.email(), EMAIL_FIELD));

		if (Strings.isNullOrEmpty(request.password()))
			credentials.newPasswordResetCode();
		else
			credentials.changePassword(request.password(), //
					Optional7.of(settings.passwordRegex()));

		return create(credentials);
	}

	//
	// Delete Credentials
	//

	public void delete(String id) {
		// index refresh before not necessary since delete by id
		// index refresh after delete is necessary
		elastic().delete(index(), id, true, true);
	}

	public void deleteByUsername(String username) {
		getByUsername(username).ifPresent(//
				credentials -> delete(credentials.id()));
	}

	public void deleteAllButSuperAdmins() {
		// superadmins can not be deleted this way
		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.mustNot(QueryBuilders.termQuery(ROLES_FIELD, Roles.superadmin));

		// always refresh before and after credentials index updates
		ElasticClient elastic = elastic();
		elastic.refreshIndex(index());
		elastic.deleteByQuery(query, index());
		elastic.refreshIndex(index());
	}

	//
	// Update Credentials
	//

	public Credentials update(Credentials credentials) {
		if (Strings.isNullOrEmpty(credentials.id()))
			throw Exceptions.illegalArgument(//
					"failed to update credentials since id is null");

		// TODO replace 10 by sessionsSizeMax from CredentialsSettings
		credentials.purgeOldSessions(10);
		credentials.updatedAt(DateTime.now().toString());

		// refresh index after each index change
		IndexResponse response = elastic().index(index(), //
				credentials.id(), credentials, true);

		credentials.version(response.getVersion());
		return credentials;
	}

	public Credentials update(CredentialsUpdateRequest request, Credentials credentials) {

		CredentialsSettings settings = Services.credentials().settings();

		if (!Strings.isNullOrEmpty(request.username)) {
			Usernames.checkValid(request.username, settings.usernameRegex());
			if (exists(request.username))
				throw Exceptions.alreadyExists(SERVICE_NAME, request.username);
			credentials.name(request.username);
		}

		// TODO check email with minimal regex
		if (!Strings.isNullOrEmpty(request.email))
			credentials.email(request.email);

		if (request.enabled != null)
			credentials.doEnableOrDisable(request.enabled);

		if (request.enableDisableAfter != null) {
			credentials.enableAfter(request.enableDisableAfter.enable);
			credentials.disableAfter(request.enableDisableAfter.disable);
		}

		// TODO check if at least one field has been changed
		// before credentials update
		return update(credentials);
	}

	//
	// Password methods
	//

	public Credentials resetPassword(String id) {
		Credentials credentials = get(id);
		credentials.clearPasswordAndTokens();
		credentials.newPasswordResetCode();
		credentials = update(credentials);
		return credentials;
	}

	public Credentials setPasswordWithCode(String credentialsId, //
			String newPassword, String passwordResetCode) {
		Credentials credentials = Services.credentials().get(credentialsId);
		credentials.changePassword(newPassword, passwordResetCode, //
				Optional7.of(settings().passwordRegex()));
		return update(credentials);
	}

	public Credentials setPassword(String credentialsId, String newPassword) {
		Credentials credentials = Services.credentials().get(credentialsId);
		credentials.changePassword(newPassword, //
				Optional7.of(settings().passwordRegex()));
		return update(credentials);

	}

	public Credentials passwordMustChange(String credentialsId) {
		Credentials credentials = Services.credentials().get(credentialsId);
		credentials.passwordMustChange(true);
		return update(credentials);
	}

	public void sendPasswordResetEmail(String username) {
		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(USERNAME_PARAM, username);
		sendPasswordResetEmail(parameters);
	}

	public ObjectNode sendPasswordResetEmail(Map<String, Object> parameters) {

		// removed because not part of the template model
		Object object = parameters.remove(USERNAME_PARAM);
		String username = Check.notNull(object, "username").toString();
		Check.notNullOrEmpty(username, "username");

		Credentials credentials = getByUsername(username)//
				.orElseThrow(() -> Exceptions.notFound(SERVICE_NAME, username));

		if (!credentials.email().isPresent())
			throw Exceptions.illegalArgument("[%s][%s] has no email", //
					credentials.type(), credentials.username());

		credentials.newPasswordResetCode();
		update(credentials);

		EmailTemplateRequest request = new EmailTemplateRequest();
		request.templateName = PASSWORD_RESET_EMAIL_TEMPLATE_NAME;
		request.parameters = parameters;
		request.parameters.put("credentials", credentials.id());
		// emailContext.put("to", credentials.email().get());
		// emailContext.put("credentialsId", credentials.id());
		// emailContext.put("passwordResetCode", credentials.passwordResetCode());

		EmailTemplate template = Services.emails().getTemplate(PASSWORD_RESET_EMAIL_TEMPLATE_NAME);
		Server.context().credentials().checkIfAuthorized(template.authorizedRoles);

		if (template.model == null)
			template.model = Maps.newHashMap();
		template.model.put("credentials", "credentials");

		return Services.emails().send(request, template);
	}

	//
	// Enable methods
	//

	public void enable(String id) {
		enable(id, true);
	}

	public void disable(String id) {
		enable(id, false);
	}

	public Credentials enable(String id, boolean enable) {
		Credentials credentials = get(id);
		credentials.doEnableOrDisable(enable);
		return update(credentials);
	}

	//
	// Credentials Settings
	//

	public CredentialsSettings settings() {
		return Services.settings().getOrThrow(CredentialsSettings.class);
	}

	public void enableGuestSignUp(boolean enable) {
		CredentialsSettings settings = settings();
		settings.guestSignUpEnabled = enable;
		Services.settings().save(settings);
	}

	//
	//
	//

	public static final String SERVICE_NAME = "credentials";
	private static final String PASSWORD_RESET_EMAIL_TEMPLATE_NAME = "password_reset_email_template";

	//
	// Type and schema
	//

	public void initIndex() {
		Index index = index();
		if (!elastic().exists(index)) {
			String mapping = schema().mapping().toString();
			elastic().createIndex(index, mapping, false);
		}
	}

	public Schema schema() {
		return Schema.builder(SERVICE_NAME)//
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

	public Index index() {
		return new Index(SERVICE_NAME);
	}
}