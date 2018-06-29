/**
 * © David Attias 2015
 */
package io.spacedog.services.credentials;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Session;
import io.spacedog.client.credentials.CredentialsCreateRequest;
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
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class CredentialsResty extends SpaceResty {

	//
	// Routes
	//

	@Get("/1/login")
	@Get("/1/login/")
	@Post("/1/login")
	@Post("/1/login/")
	public Payload login(Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		if (credentials.hasPasswordBeenChallenged()) {
			long lifetime = getCheckSessionLifetime(context);
			credentials.setCurrentSession(Session.newSession(lifetime));
			credentials = Services.credentials().update(credentials);
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
		Server.context().credentials().checkAtLeastUser();
		Services.credentials().logout();
		return JsonPayload.ok().build();
	}

	@Get("/1/credentials")
	@Get("/1/credentials/")
	public Payload getAll(Context context) {
		Server.context().credentials().checkAtLeastAdmin();

		Credentials.Results all = Services.credentials().getAll(//
				context.get(Q_PARAM), //
				context.query().getInteger(FROM_PARAM, 0), //
				context.query().getInteger(SIZE_PARAM, 10));

		// TODO replace this by automatic jackson serialization
		return JsonPayload.ok().withContent(all.toJson()).build();
	}

	@Delete("/1/credentials")
	@Delete("/1/credentials/")
	public Payload deleteAll(Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.credentials().deleteAllButSuperAdmins();
		return JsonPayload.ok().build();
	}

	@Post("/1/credentials")
	@Post("/1/credentials/")
	public Payload post(CredentialsCreateRequest request, Context context) {

		CredentialsSettings settings = Services.credentials().settings();

		if (!settings.guestSignUpEnabled)
			Server.context().credentials().checkAtLeastUser();

		Credentials credentials = Services.credentials()//
				.create(request, Roles.user);

		JsonPayload payload = JsonPayload.saved(true, "/1", //
				CredentialsService.SERVICE_NAME, credentials.id());

		if (credentials.passwordResetCode() != null)
			payload.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode());

		return payload.build();
	}

	@Get("/1/credentials/me")
	@Get("/1/credentials/me/")
	public Payload getMe(Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
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
		Credentials credentials = Server.context().credentials().checkAtLeastUser();
		return deleteById(credentials.id());
	}

	@Delete("/1/credentials/:id")
	@Delete("/1/credentials/:id/")
	public Payload deleteById(String id) {
		Credentials credentials = checkMyselfOrHigherAdminAndGet(id, false);

		// forbidden to delete last backend superadmin
		if (credentials.isSuperAdmin())
			if (!Services.credentials().existsMoreThanOneSuperAdmin())
				throw Exceptions.forbidden("backend must at least have one superadmin");

		Services.credentials().delete(id);
		return JsonPayload.ok().build();
	}

	@Put("/1/credentials/me")
	@Put("/1/credentials/me/")
	public Payload put(CredentialsUpdateRequest request, Context context) {
		return put(Server.context().credentials().id(), request, context);
	}

	@Put("/1/credentials/:id")
	@Put("/1/credentials/:id/")
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

	@Post("/1/credentials/_send_password_reset_email")
	@Post("/1/credentials/_send_password_reset_email/")
	public Payload postSendPasswordResetEmail(String body, Context context) {
		MapLikeType type = TypeFactory.defaultInstance()//
				.constructMapLikeType(Map.class, String.class, Object.class);
		Map<String, Object> parameters = Json.toPojo(body, type);
		ObjectNode response = Services.credentials()//
				.sendPasswordResetEmail(parameters);
		return JsonPayload.ok().withContent(response).build();
	}

	@Post("/1/credentials/:id/_reset_password")
	@Post("/1/credentials/:id/_reset_password/")
	public Payload postResetPassword(String id, Context context) {
		checkAdminAndGet(id);
		Credentials credentials = Services.credentials().resetPassword(id);
		return JsonPayload.saved(false, "/1", CredentialsService.SERVICE_NAME, credentials.id())//
				.withVersion(credentials.version())//
				.withFields(PASSWORD_RESET_CODE_FIELD, credentials.passwordResetCode())//
				.build();
	}

	@Post("/1/credentials/me/_set_password")
	@Post("/1/credentials/me/_set_password/")
	public Payload postSetMyPassword(SetPasswordRequest request, Context context) {
		return postSetPassword(//
				Server.context().credentials().id(), //
				request, context);
	}

	@Post("/1/credentials/:id/_set_password")
	@Post("/1/credentials/:id/_set_password/")
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

	@Post("/1/credentials/:id/_password_must_change")
	@Post("/1/credentials/:id/_password_must_change/")
	public Payload postForcePasswordUpdate(String id, Context context) {
		checkAdminAndGet(id);
		Credentials credentials = Services.credentials().passwordMustChange(id);
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
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials);
	}

	@Put("/1/credentials/:id/roles/:role")
	@Put("/1/credentials/:id/roles/:role/")
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

	@Delete("/1/credentials/:id/roles/:role")
	@Delete("/1/credentials/:id/roles/:role/")
	public Payload deleteRole(String id, String role, Context context) {
		Credentials credentials = checkAdminAndGet(id);
		if (credentials.roles().contains(role)) {
			credentials.removeRoles(role);
			credentials = Services.credentials().update(credentials);
		}
		return saved(false, credentials);
	}

	//
	// Internal services
	//

	private Payload doEnableOrDisable(String id, boolean enable) {
		Credentials credentials = checkAdminAndGet(id);
		credentials.doEnableOrDisable(enable);
		credentials = Services.credentials().update(credentials);
		return saved(false, credentials);
	}

	public static long getCheckSessionLifetime(Context context) {
		CredentialsSettings settings = Services.credentials().settings();

		long lifetime = context.query()//
				.getLong(LIFETIME_PARAM, settings.sessionMaximumLifetime);

		if (lifetime > settings.sessionMaximumLifetime)
			throw Exceptions.forbidden(//
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

		throw Exceptions.insufficientCredentials(requester);
	}

	//
	// Implementation
	//

	private Payload saved(boolean created, Credentials credentials) {
		return JsonPayload.saved(false, "/1", CredentialsService.SERVICE_NAME, credentials.id())//
				.withVersion(credentials.version()).withContent(credentials.toJson())//
				.build();
	}

}
