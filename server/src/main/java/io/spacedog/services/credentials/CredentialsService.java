package io.spacedog.services.credentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.TemplateParameterTypes;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Session;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.credentials.Usernames;
import io.spacedog.client.email.EmailTemplate;
import io.spacedog.client.email.EmailTemplateRequest;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.Index;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceService;
import io.spacedog.services.elastic.ElasticClient;
import io.spacedog.services.elastic.ElasticExportStreamingOutput;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

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

		throw Exceptions.objectNotFound(Credentials.TYPE, id);
	}

	//
	// Get All Credentials
	//

	public Credentials.Results search(SearchSourceBuilder builder) {
		return search(builder, false);
	}

	public Credentials.Results search(SearchSourceBuilder builder, boolean refresh) {

		if (refresh)
			elastic().refreshIndex(index());

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

		return search(builder).total > 1;
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

			Credentials.Results results = search(//
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
			throw Exceptions.alreadyExists(Credentials.TYPE, credentials.username());

		credentials.createdAt(DateTime.now());
		credentials.updatedAt(credentials.createdAt());

		Index index = index();
		ObjectNode source = toElasticSource(credentials);

		// refresh index after each index change
		IndexResponse response = Strings.isNullOrEmpty(credentials.id()) //
				? elastic().index(index, source, true) //
				: elastic().index(index, credentials.id(), source, true);

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

		requester.checkCanManage(request.roles());
		credentials.addRoles(request.roles());
		if (!credentials.isAtLeastUser())
			credentials.addRoles(defaultRole);

		if (!Utils.isNullOrEmpty(request.groups()))
			for (String group : request.groups()) {
				requester.checkGroupAccessPermission(group);
				credentials.addGroup(group);
			}

		CredentialsSettings settings = settings();

		credentials.username(Check.notNullOrEmpty(request.username(), USERNAME_FIELD));
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

	// TODO
	// Should I check username is not changed or still unique?
	//
	public Credentials update(Credentials credentials) {
		if (Strings.isNullOrEmpty(credentials.id()))
			throw Exceptions.illegalArgument("credentials id is null or empty");

		// TODO replace 10 by sessionsSizeMax from CredentialsSettings
		credentials.purgeOldSessions(10);
		credentials.updatedAt(DateTime.now());

		// refresh index after each index change
		IndexResponse response = elastic().index(index(), //
				credentials.id(), toElasticSource(credentials), true);

		credentials.version(response.getVersion());
		return credentials;
	}

	//
	// Password reset email
	//

	public void sendPasswordResetEmail(String username) {
		Map<String, Object> parameters = Maps.newHashMap();
		parameters.put(USERNAME_PARAM, username);
		sendPasswordResetEmail(parameters);
	}

	public ObjectNode sendPasswordResetEmail(Map<String, Object> parameters) {

		// removed because not part of the template model
		Object object = parameters.remove(USERNAME_PARAM);
		String username = Check.notNull(object, USERNAME_PARAM).toString();
		Check.notNullOrEmpty(username, USERNAME_PARAM);

		Credentials credentials = getByUsername(username)//
				.orElseThrow(() -> Exceptions.objectNotFound(Credentials.TYPE, username));

		if (!credentials.email().isPresent())
			throw Exceptions.illegalArgument("[%s][%s] has no email", //
					credentials.type(), credentials.username());

		credentials.newPasswordResetCode();
		update(credentials);

		EmailTemplateRequest request = new EmailTemplateRequest();
		request.templateName = PASSWORD_RESET_EMAIL_TEMPLATE_NAME;
		request.parameters = parameters;
		request.parameters.put("credentials", Json.mapper().convertValue(credentials, Map.class));
		// we need to manually add password reset code
		// since it is ignored by jackson mapper for security reasons
		request.parameters.put("passwordResetCode", credentials.passwordResetCode());

		EmailTemplate template = Services.emails().getTemplate(PASSWORD_RESET_EMAIL_TEMPLATE_NAME);
		Server.context().credentials().checkRoleAccess(template.authorizedRoles);

		if (template.model == null)
			template.model = Maps.newHashMap();
		template.model.put("credentials", TemplateParameterTypes.object);
		template.model.put("passwordResetCode", TemplateParameterTypes.string);

		return Services.emails().send(request, template);
	}

	private static final String PASSWORD_RESET_EMAIL_TEMPLATE_NAME = "password_reset_email_template";

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
	// Import Export
	//

	public StreamingOutput exportNow(QueryBuilder query) {
		SearchResponse response = elastic().prepareSearch(index())//
				.setScroll(ElasticExportStreamingOutput.TIMEOUT)//
				.setSize(ElasticExportStreamingOutput.SIZE)//
				.setQuery(query)//
				.get();

		return new ElasticExportStreamingOutput(response);
	}

	public long importNow(InputStream data, boolean preserveIds) throws IOException {

		long indexed = 0;
		BufferedReader reader = new BufferedReader(//
				new InputStreamReader(data));

		Index index = index();
		String json = reader.readLine();

		while (json != null) {
			ObjectNode object = Json.readObject(json);
			String source = object.get(SOURCE_FIELD).toString();

			if (preserveIds) {
				String id = object.get(ID_FIELD).asText();
				elastic().index(index, id, source);
			} else
				elastic().index(index, source);

			indexed++;
			json = reader.readLine();
		}
		return indexed;
	}

	//
	// Type and schema
	//

	public void initIndex() {
		Index index = index();
		Schema schema = schema();
		if (!elastic().exists(index))
			elastic().createIndex(index, schema, false);
		else
			elastic().putMapping(index, schema.mapping());
	}

	public Schema schema() {
		return Schema.builder(Credentials.TYPE)//

				.dynamicStrict()//
				.dateDetection(false)//

				.keyword(USERNAME_FIELD).subText("simple")//
				.keyword(EMAIL_FIELD).subText("simple")//
				.keyword(ROLES_FIELD)//
				.keyword(GROUPS_FIELD)//
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

	public Index index() {
		return new Index(Credentials.TYPE);
	}

	private BoolQueryBuilder toQuery(String username) {
		return QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(USERNAME_FIELD, username));
	}

	private Credentials toCredentials(SearchHit hit) {
		return fromElasticSource(hit.getSourceAsString(), //
				hit.getId(), hit.getVersion());
	}

	private Credentials toCredentials(GetResponse response) {
		return fromElasticSource(response.getSourceAsString(), //
				response.getId(), response.getVersion());
	}

	private Credentials fromElasticSource(String sourceAsString, String id, long version) {
		ObjectNode source = Json.readObject(sourceAsString);

		Credentials credentials = Json.toPojo(source, Credentials.class)//
				.version(version).id(id);

		JsonNode hashed = source.get(HASHED_PASSWORD_FIELD);

		if (!Json.isNull(hashed))
			credentials.hashedPassword(hashed.asText());

		JsonNode resetCode = source.get(PASSWORD_RESET_CODE_FIELD);

		if (!Json.isNull(resetCode))
			credentials.passwordResetCode(resetCode.asText());

		JsonNode sessions = source.get(SESSIONS_FIELD);

		if (!Json.isNull(sessions))
			credentials.sessions(Json.toPojo(sessions, TypeFactory.defaultInstance()//
					.constructCollectionLikeType(ArrayList.class, Session.class)));

		return credentials;
	}

	private ObjectNode toElasticSource(Credentials credentials) {

		ObjectNode object = Json.toObjectNode(credentials);

		object.remove(ID_FIELD);
		object.remove(VERSION_FIELD);
		object.put(HASHED_PASSWORD_FIELD, credentials.hashedPassword());
		object.put(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());
		object.putPOJO(SESSIONS_FIELD, credentials.sessions());

		return object;
	}
}
