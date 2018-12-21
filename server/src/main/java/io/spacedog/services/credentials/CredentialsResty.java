/**
 * © David Attias 2015
 */
package io.spacedog.services.credentials;

import java.util.Map;
import java.util.Set;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Results;
import io.spacedog.client.credentials.Credentials.Session;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.CredentialsGroupCreateRequest;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.credentials.EnableDisableAfterRequest;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.credentials.SetPasswordRequest;
import io.spacedog.client.credentials.Usernames;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/credentials")
public class CredentialsResty extends SpaceResty {

	//
	// Routes
	//

	@Post("/_login")
	@Post("/_login/")
	public Payload login(Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();

		if (credentials.hasPasswordBeenChallenged()) {
			long lifetime = getCheckSessionLifetime(credentials, context);
			credentials.setCurrentSession(Session.newSession(lifetime));
			credentials = Services.credentials().update(credentials);
		}

		return saved(false, credentials, //
				ACCESS_TOKEN_FIELD, credentials.accessToken(), //
				EXPIRES_IN_FIELD, credentials.accessTokenExpiresIn(), //
				CREDENTIALS_FIELD, credentials);
	}

	@Post("/_logout")
	@Post("/_logout/")
	public void logout(Context context) {
		Server.context().credentials().checkAtLeastUser();
		Services.credentials().logout();
	}

	@Get("")
	@Get("/")
	public Results getAll(Context context) {
		Server.context().credentials().checkAtLeastAdmin();

		String q = context.get(Q_PARAM);
		String role = context.get(ROLE_PARAM);
		String username = context.get(USERNAME_PARAM);

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (!Strings.isNullOrEmpty(q)) //
			query.must(QueryBuilders.simpleQueryStringQuery(q)//
					.field(USERNAME_FIELD).field(EMAIL_FIELD).field(ROLES_FIELD).field(TAGS_FIELD));

		if (!Strings.isNullOrEmpty(role)) //
			query.must(QueryBuilders.termQuery(ROLES_FIELD, role));

		if (!Strings.isNullOrEmpty(username)) //
			query.must(QueryBuilders.termQuery(USERNAME_FIELD, username));

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(query)//
				.from(context.query().getInteger(FROM_PARAM, 0))//
				.size(context.query().getInteger(SIZE_PARAM, 10));

		return Services.credentials().search(builder, isRefreshRequested(context));
	}

	@Delete("")
	@Delete("/")
	public void deleteAll(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.credentials().deleteAllButSuperAdmins();
	}

	@Post("")
	@Post("/")
	public Payload post(CredentialsCreateRequest request, Context context) {

		CredentialsSettings settings = Services.credentials().settings();

		if (!settings.guestSignUpEnabled)
			Server.context().credentials().checkAtLeastUser();

		Credentials credentials = Services.credentials()//
				.create(request, Roles.user);

		JsonPayload payload = JsonPayload.saved(true, "/2", Credentials.TYPE, credentials.id());

		if (credentials.passwordResetCode() != null)
			payload.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());

		return payload.build();
	}

	@Get("/me")
	@Get("/me/")
	public Credentials getMe(Context context) {
		return Server.context().credentials().checkAtLeastUser();
	}

	@Get("/:id")
	@Get("/:id/")
	public Credentials getById(String id, Context context) {
		return checkMyselfOrHigherAdminAndGet(id, false);
	}

	@Delete("/me")
	@Delete("/me/")
	public void deleteMe() {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		deleteById(credentials.id());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public void deleteById(String id) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		// forbidden to delete last backend superadmin
		if (credentials.isSuperAdmin())
			if (!Services.credentials().existsMoreThanOneSuperAdmin())
				throw Exceptions.forbidden(credentials, "backend must at least have one superadmin");

		Services.credentials().delete(id);
	}

	@Put("/me/username")
	@Put("/me/username/")
	public Credentials putMyUsername(String body, Context context) {
		return putUsername(Server.context().credentials().id(), body, context);
	}

	@Put("/:id/username")
	@Put("/:id/username/")
	public Credentials putUsername(String id, String body, Context context) {

		Credentials requester = Server.context().credentials();
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		if (requester.isUser())
			requester.checkPasswordHasBeenChallenged();

		String username = Json.checkString(Json.readNode(body));
		if (Strings.isNullOrEmpty(username))
			throw Exceptions.illegalArgument("username is empty");

		CredentialsSettings settings = Services.credentials().settings();
		Usernames.checkValid(username, settings.usernameRegex());
		if (Services.credentials().exists(username))
			throw Exceptions.alreadyExists(Credentials.TYPE, username);

		credentials.username(username);
		return Services.credentials().update(credentials);
	}

	@Put("/me/email")
	@Put("/me/email/")
	public Credentials putMyEmail(String body, Context context) {
		return putEmail(Server.context().credentials().id(), body, context);
	}

	@Put("/:id/email")
	@Put("/:id/email/")
	public Credentials putEmail(String id, String body, Context context) {

		Credentials requester = Server.context().credentials();
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		if (requester.isUser())
			requester.checkPasswordHasBeenChallenged();

		String email = Json.checkString(Json.readNode(body));

		// TODO check email with minimal regex
		if (Strings.isNullOrEmpty(email))
			throw Exceptions.illegalArgument("email is empty");

		credentials.email(email);
		return Services.credentials().update(credentials);
	}

	@Post("/:id/_enable_disable_after")
	@Post("/:id/_enable_disable_after/")
	public Credentials postEnableDisableAfter(String id, //
			EnableDisableAfterRequest enableDisableAfter, Context context) {

		Credentials credentials = checkAdminAndGet(id);
		credentials.enableAfter(enableDisableAfter.enableAfter);
		credentials.disableAfter(enableDisableAfter.disableAfter);
		return Services.credentials().update(credentials);
	}

	@Post("/_send_password_reset_email")
	@Post("/_send_password_reset_email/")
	public Payload postSendPasswordResetEmail(String body, Context context) {
		MapLikeType type = TypeFactory.defaultInstance()//
				.constructMapLikeType(Map.class, String.class, Object.class);
		Map<String, Object> parameters = Json.toPojo(body, type);
		ObjectNode response = Services.credentials()//
				.sendPasswordResetEmail(parameters);
		return JsonPayload.ok().withContent(response).build();
	}

	@Post("/:id/_reset_password")
	@Post("/:id/_reset_password/")
	public Payload postResetPassword(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.resetPassword();
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials, //
				PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());
	}

	@Post("/me/_set_password")
	@Post("/me/_set_password/")
	public Payload postSetMyPassword(SetPasswordRequest request, Context context) {
		return postSetPassword(Server.context().credentials().id(), request, context);
	}

	@Post("/:id/_set_password")
	@Post("/:id/_set_password/")
	public Payload postSetPassword(String id, SetPasswordRequest request, Context context) {
		// TODO do we need a password reset expire date to limit the reset time scope

		Credentials credentials = null;
		CredentialsSettings settings = Services.credentials().settings();
		Optional7<String> regex = Optional7.of(settings.passwordRegex());

		if (Strings.isNullOrEmpty(request.passwordResetCode())) {
			credentials = checkMyselfOrHigherAdminAndGet(id, true);
			credentials.changePassword(request.password(), regex);
		} else {
			credentials = Services.credentials().get(id);
			credentials.changePassword(request.password(), request.passwordResetCode(), regex);
		}

		credentials = Services.credentials().update(credentials);
		return saved(false, credentials);
	}

	@Post("/:id/_password_must_change")
	@Post("/:id/_password_must_change/")
	public Payload postForcePasswordUpdate(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.passwordMustChange(true);
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials);
	}

	@Post("/:id/_enable")
	@Post("/:id/_enable/")
	public Credentials postEnable(String id, String body, Context context) {
		return doEnableOrDisable(id, true);
	}

	@Post("/:id/_disable")
	@Post("/:id/_disable/")
	public Credentials postDisable(String id, String body, Context context) {
		return doEnableOrDisable(id, false);
	}

	@Get("/:id/roles")
	@Get("/:id/roles/")
	public Set<String> getRoles(String id, Context context) {
		return checkMyselfOrHigherAdminAndGet(id, false).roles();
	}

	@Delete("/:id/roles")
	@Delete("/:id/roles/")
	public Credentials deleteAllRoles(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.clearRoles();
		return Services.credentials().update(credentials);
	}

	@Put("/:id/roles/:role")
	@Put("/:id/roles/:role/")
	public Credentials putRole(String id, String role, Context context) {
		Roles.checkIfValid(role);
		Credentials credentials = checkAdminAndGet(id);
		Server.context().credentials().checkCanManage(role);

		if (!credentials.roles().contains(role)) {
			credentials.addRoles(role);
			credentials = Services.credentials().update(credentials);
		}
		return credentials;
	}

	@Delete("/:id/roles/:role")
	@Delete("/:id/roles/:role/")
	public Credentials deleteRole(String id, String role, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		if (credentials.roles().contains(role)) {
			credentials.removeRoles(role);
			credentials = Services.credentials().update(credentials);
		}
		return credentials;
	}

	@Post("/me/groups")
	@Post("/me/groups/")
	public Payload postCreateGroup(CredentialsGroupCreateRequest request, Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		return createGroup(credentials, request.suffix);
	}

	@Post("/:id/groups")
	@Post("/:id/groups/")
	public Payload postCreateGroup(String id, CredentialsGroupCreateRequest request, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		return createGroup(Services.credentials().get(id), request.suffix);
	}

	@Put("/:id/groups/:group")
	@Put("/:id/groups/:group/")
	public Credentials putShareGroup(String id, String group, Context context) {
		Server.context().credentials().checkGroupAdminPermission(group);
		Credentials credentials = Services.credentials().get(id).addGroup(group);
		return Services.credentials().update(credentials);
	}

	@Delete("/me/groups/:group")
	@Delete("/me/groups/:group/")
	public Credentials deleteRemoveGroup(String group) {
		return removeGroup(Server.context().credentials(), group);
	}

	@Delete("/:id/groups/:group")
	@Delete("/:id/groups/:group/")
	public Credentials deleteUnshareGroup(String id, String group) {
		Server.context().credentials().checkGroupAdminPermission(group);
		return removeGroup(Services.credentials().get(id), group);
	}

	//
	// Internal services
	//

	private Credentials doEnableOrDisable(String id, boolean enable) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.doEnableOrDisable(enable);
		return Services.credentials().update(credentials);
	}

	public static long getCheckSessionLifetime(Credentials credentials, Context context) {
		CredentialsSettings settings = Services.credentials().settings();

		long lifetime = context.query()//
				.getLong(LIFETIME_PARAM, settings.sessionMaximumLifetimeInSeconds);

		if (lifetime > settings.sessionMaximumLifetimeInSeconds)
			throw Exceptions.forbidden(credentials, //
					"maximum access token lifetime is [%s] seconds", //
					settings.sessionMaximumLifetimeInSeconds);

		return lifetime;
	}

	Credentials checkAdminAndGet(String id) {
		Credentials requester = Server.context().credentials().checkAtLeastAdmin();
		Credentials credentials = Services.credentials().get(id);
		requester.checkCanManage(credentials);
		return credentials;
	}

	Credentials checkMyselfOrHigherAdminAndGet(String credentialsId, //
			boolean checkPasswordHasBeenChallenged) {

		Credentials requester = Server.context().credentials().checkAtLeastUser();

		if (checkPasswordHasBeenChallenged)
			requester.checkPasswordHasBeenChallenged();

		if (requester.id().equals(credentialsId))
			return requester;

		if (requester.isAtLeastAdmin()) {
			Credentials credentials = Services.credentials().get(credentialsId);
			requester.checkCanManage(credentials);
			return credentials;
		}

		throw Exceptions.insufficientPermissions(requester);
	}

	//
	// Implementation
	//

	private Payload createGroup(Credentials credentials, String suffix) {
		String group = credentials.createGroup(suffix);
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials, GROUP_FIELD, group);
	}

	private Credentials removeGroup(Credentials credentials, String group) {
		if (credentials.hasGroupAccessPermission(group)) {
			credentials.removeGroup(group);
			credentials = Services.credentials().update(credentials);
		}
		return credentials;
	}

	private Payload saved(boolean created, Credentials credentials, Object... fields) {
		return JsonPayload.saved(false, "/2", Credentials.TYPE, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(fields)//
				.build();
	}

}
