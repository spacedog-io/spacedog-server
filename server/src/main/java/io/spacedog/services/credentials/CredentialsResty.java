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
import io.spacedog.client.credentials.CredentialsUpdateRequest;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.credentials.SetPasswordRequest;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2")
public class CredentialsResty extends SpaceResty {

	//
	// Routes
	//

	@Get("/login")
	@Get("/login/")
	@Post("/login")
	@Post("/login/")
	public Payload login(Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		if (credentials.hasPasswordBeenChallenged()) {
			long lifetime = getCheckSessionLifetime(credentials, context);
			credentials.setCurrentSession(Session.newSession(lifetime));
			credentials = Services.credentials().update(credentials);
		}

		return JsonPayload.ok()//
				.withFields(ACCESS_TOKEN_FIELD, credentials.accessToken(), //
						EXPIRES_IN_FIELD, credentials.accessTokenExpiresIn(), //
						CREDENTIALS_FIELD, credentials)//
				.build();
	}

	@Get("/logout")
	@Get("/logout/")
	@Post("/logout")
	@Post("/logout/")
	public void logout(Context context) {
		Server.context().credentials().checkAtLeastUser();
		Services.credentials().logout();
	}

	@Get("/credentials")
	@Get("/credentials/")
	public Results getAll(Context context) {
		Server.context().credentials().checkAtLeastAdmin();

		String q = context.get(Q_PARAM);
		String role = context.get(ROLE_PARAM);

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (!Strings.isNullOrEmpty(q)) //
			query.must(QueryBuilders.simpleQueryStringQuery(q)//
					.field(USERNAME_FIELD).field(EMAIL_FIELD).field(ROLES_FIELD).field(TAGS_FIELD));

		if (!Strings.isNullOrEmpty(role)) //
			query.must(QueryBuilders.termQuery(ROLES_FIELD, role));

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
				.query(query)//
				.from(context.query().getInteger(FROM_PARAM, 0))//
				.size(context.query().getInteger(SIZE_PARAM, 10));

		return Services.credentials().search(builder, isRefreshRequested(context));
	}

	@Delete("/credentials")
	@Delete("/credentials/")
	public void deleteAll(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.credentials().deleteAllButSuperAdmins();
	}

	@Post("/credentials")
	@Post("/credentials/")
	public Payload post(CredentialsCreateRequest request, Context context) {

		CredentialsSettings settings = Services.credentials().settings();

		if (!settings.guestSignUpEnabled)
			Server.context().credentials().checkAtLeastUser();

		Credentials credentials = Services.credentials()//
				.create(request, Roles.user);

		JsonPayload payload = JsonPayload.saved(true, "/2", //
				CredentialsService.SERVICE_NAME, credentials.id());

		if (credentials.passwordResetCode() != null)
			payload.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());

		return payload.build();
	}

	@Get("/credentials/me")
	@Get("/credentials/me/")
	public Credentials getMe(Context context) {
		return Server.context().credentials().checkAtLeastUser();
	}

	@Get("/credentials/:id")
	@Get("/credentials/:id/")
	public Credentials getById(String id, Context context) {
		return checkMyselfOrHigherAdminAndGet(id, false);
	}

	@Delete("/credentials/me")
	@Delete("/credentials/me/")
	public void deleteMe() {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		deleteById(credentials.id());
	}

	@Delete("/credentials/:id")
	@Delete("/credentials/:id/")
	public void deleteById(String id) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		// forbidden to delete last backend superadmin
		if (credentials.isSuperAdmin())
			if (!Services.credentials().existsMoreThanOneSuperAdmin())
				throw Exceptions.forbidden(credentials, "backend must at least have one superadmin");

		Services.credentials().delete(id);
	}

	@Put("/credentials/me")
	@Put("/credentials/me/")
	public Payload put(CredentialsUpdateRequest request, Context context) {
		return put(Server.context().credentials().id(), request, context);
	}

	@Put("/credentials/:id")
	@Put("/credentials/:id/")
	public Payload put(String id, CredentialsUpdateRequest request, Context context) {

		Credentials requester = Server.context().credentials();
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		if (requester.isUser())
			requester.checkPasswordHasBeenChallenged();

		if (request.enabled != null //
				|| request.enableDisableAfter != null)
			requester.checkAtLeastAdmin();

		credentials = Services.credentials().update(request, credentials);
		return saved(false, credentials);
	}

	@Post("/credentials/_send_password_reset_email")
	@Post("/credentials/_send_password_reset_email/")
	public Payload postSendPasswordResetEmail(String body, Context context) {
		MapLikeType type = TypeFactory.defaultInstance()//
				.constructMapLikeType(Map.class, String.class, Object.class);
		Map<String, Object> parameters = Json.toPojo(body, type);
		ObjectNode response = Services.credentials()//
				.sendPasswordResetEmail(parameters);
		return JsonPayload.ok().withContent(response).build();
	}

	@Post("/credentials/:id/_reset_password")
	@Post("/credentials/:id/_reset_password/")
	public Payload postResetPassword(String id, Context context) {
		checkAdminAndGet(id);
		Credentials credentials = Services.credentials().resetPassword(id);
		return JsonPayload.saved(false, "/2", CredentialsService.SERVICE_NAME, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode())//
				.build();
	}

	@Post("/credentials/me/_set_password")
	@Post("/credentials/me/_set_password/")
	public Payload postSetMyPassword(SetPasswordRequest request, Context context) {
		return postSetPassword(//
				Server.context().credentials().id(), //
				request, context);
	}

	@Post("/credentials/:id/_set_password")
	@Post("/credentials/:id/_set_password/")
	public Payload postSetPassword(String id, SetPasswordRequest request, Context context) {
		// TODO do we need a password reset expire date to limit the reset
		// time scope
		Credentials credentials = null;
		if (Strings.isNullOrEmpty(request.passwordResetCode())) {
			credentials = checkMyselfOrHigherAdminAndGet(id, true);
			Services.credentials().setPassword(id, request.password());
		} else
			credentials = Services.credentials().setPasswordWithCode(id, //
					request.password(), request.passwordResetCode());

		return saved(false, credentials);
	}

	@Post("/credentials/:id/_password_must_change")
	@Post("/credentials/:id/_password_must_change/")
	public Payload postForcePasswordUpdate(String id, Context context) {
		checkAdminAndGet(id);
		Credentials credentials = Services.credentials().passwordMustChange(id);
		return saved(false, credentials);
	}

	@Post("/credentials/:id/_enable")
	@Post("/credentials/:id/_enable/")
	public Payload postEnable(String id, String body, Context context) {
		return doEnableOrDisable(id, true);
	}

	@Post("/credentials/:id/_disable")
	@Post("/credentials/:id/_disable/")
	public Payload postDisable(String id, String body, Context context) {
		return doEnableOrDisable(id, false);
	}

	@Get("/credentials/:id/roles")
	@Get("/credentials/:id/roles/")
	public Set<String> getRoles(String id, Context context) {
		return checkMyselfOrHigherAdminAndGet(id, false).roles();
	}

	@Delete("/credentials/:id/roles")
	@Delete("/credentials/:id/roles/")
	public Payload deleteAllRoles(String id, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.clearRoles();
		return doUpdate(credentials);
	}

	@Put("/credentials/:id/roles/:role")
	@Put("/credentials/:id/roles/:role/")
	public Payload putRole(String id, String role, Context context) {
		Roles.checkIfValid(role);
		Credentials requester = Server.context().credentials();
		Credentials updated = checkAdminAndGet(id);
		requester.checkCanManage(role);

		if (!updated.roles().contains(role)) {
			updated.addRoles(role);
			updated = Services.credentials().update(updated);
		}
		return saved(false, updated);
	}

	@Delete("/credentials/:id/roles/:role")
	@Delete("/credentials/:id/roles/:role/")
	public Payload deleteRole(String id, String role, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		if (credentials.roles().contains(role)) {
			credentials.removeRoles(role);
			credentials = Services.credentials().update(credentials);
		}
		return saved(false, credentials);
	}

	@Post("/credentials/me/groups")
	@Post("/credentials/me/groups/")
	public Payload postCreateGroup(CredentialsGroupCreateRequest request, Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		String group = credentials.createGroup(request.suffix);
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials, GROUP_FIELD, group);
	}

	@Put("/credentials/:id/groups/:group")
	@Put("/credentials/:id/groups/:group/")
	public Payload putShareGroup(String id, String group, Context context) {
		Credentials credentials = Server.context().credentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkAtLeastUser().checkGroupIsMine(group);
		credentials = Services.credentials().get(id).addGroup(group);
		return doUpdate(credentials);
	}

	@Delete("/credentials/me/groups/:group")
	@Delete("/credentials/me/groups/:group/")
	public Payload deleteRemoveGroup(String group) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		return deleteUnshareGroup(credentials.id(), group);
	}

	@Delete("/credentials/:id/groups/:group")
	@Delete("/credentials/:id/groups/:group/")
	public Payload deleteUnshareGroup(String id, String group) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();

		// if I'm not removing on myself,
		// I need to check I owns this group
		if (!credentials.id().equals(id))
			credentials.checkGroupIsMine(group);

		credentials = Services.credentials().get(id).removeGroup(group);
		return doUpdate(credentials);
	}

	//
	// Internal services
	//

	private Payload doEnableOrDisable(String id, boolean enable) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.doEnableOrDisable(enable);
		return doUpdate(credentials);
	}

	public static long getCheckSessionLifetime(Credentials credentials, Context context) {
		CredentialsSettings settings = Services.credentials().settings();

		long lifetime = context.query()//
				.getLong(LIFETIME_PARAM, settings.sessionMaximumLifetime);

		if (lifetime > settings.sessionMaximumLifetime)
			throw Exceptions.forbidden(credentials, //
					"maximum access token lifetime is [%s] seconds", //
					settings.sessionMaximumLifetime);

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

	private Payload doUpdate(Credentials credentials) {
		Services.credentials().update(credentials);
		return saved(false, credentials);
	}

	private Payload saved(boolean created, Credentials credentials, Object... fields) {
		return JsonPayload.saved(false, "/2", CredentialsService.SERVICE_NAME, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(fields)//
				.build();
	}

}
