package io.spacedog.services;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/stripe")
public class StripeResty extends SpaceResty {

	@Post("/customers")
	@Post("/customers/")
	public Payload postCustomer() {
		Credentials credentials = Server.context()//
				.credentials().checkAtLeastUser();

		ObjectNode response = Services.stripe()//
				.createCustomerFor(credentials);

		return JsonPayload.created().withContent(response).build();
	}

	@Get("/customers/me")
	@Get("/customers/me/")
	public Payload getCustomer(Context context) {
		Credentials credentials = Server.context()//
				.credentials().checkAtLeastUser();

		ObjectNode response = Services.stripe()//
				.getCustomerFor(credentials);

		return JsonPayload.ok().withContent(response).build();
	}

	@Delete("/customers/me")
	@Delete("/customers/me/")
	public Payload deleteStripeCustomer() {
		Credentials credentials = Server.context()//
				.credentials().checkAtLeastUser();

		ObjectNode response = Services.stripe()//
				.deleteCustomerFor(credentials);

		return JsonPayload.ok().withContent(response).build();
	}

	@Post("/customers/me/sources")
	@Post("/customers/me/sources/")
	public Payload postCard(ObjectNode body, Context context) {
		Credentials credentials = Server.context()//
				.credentials().checkAtLeastUser();

		String sourceToken = Json.checkStringNotNullOrEmpty(body, "source");
		JsonNode descriptionNode = Json.get(body, "description");
		Optional<String> description = Json.isNull(descriptionNode) //
				? Optional.empty()
				: Optional.of(descriptionNode.asText());

		ObjectNode response = Services.stripe().createSourceFor(//
				credentials, sourceToken, description);

		return JsonPayload.created().withContent(response).build();
	}

	@Delete("/customers/me/sources/:cardId")
	@Delete("/customers/me/sources/:cardId/")
	public Payload deleteStripeCard(String cardId, Context context) {
		Credentials credentials = Server.context()//
				.credentials().checkAtLeastUser();

		ObjectNode response = Services.stripe()//
				.deleteSourceFor(credentials, cardId);

		return JsonPayload.ok().withContent(response).build();
	}

	@Post("/charges")
	@Post("/charges/")
	public Payload postCharge(Context context) {
		Credentials credentials = Server.context().credentials();
		StripeSettings settings = Services.stripe().settings();

		credentials.checkIfAuthorized(settings.rolesAllowedToCharge);

		ObjectNode response = Services.stripe()//
				.charge(context.query().keyValues());

		return JsonPayload.ok().withContent(response).build();

	}

	@Post("/charges/me")
	@Post("/charges/me/")
	public Payload postChargeMe(Context context) {
		Credentials credentials = Server.context().credentials();
		StripeSettings settings = Services.stripe().settings();

		credentials.checkIfAuthorized(settings.rolesAllowedToPay);

		ObjectNode response = Services.stripe()//
				.charge(credentials, context.query().keyValues());

		return JsonPayload.ok().withContent(response).build();

	}
}
