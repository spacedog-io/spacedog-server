package io.spacedog.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceException;
import io.spacedog.utils.StripeSettings;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/stripe")
public class StripeResource extends Resource {

	//
	// Routes
	//

	private static final String CREDENTIALS_STASH_STRIPE_CUSTOMER_ID = "stripeCustomerId";

	@Post("/customers")
	@Post("/customers/")
	public Payload postCustomer(String body) {
		Credentials credentials = SpaceContext.checkUserCredentials();

		if (hasStripeCustomerId(credentials))
			throw Exceptions.illegalArgument(//
					"credentials [%s][%s] already have a stripe customer", //
					credentials.level(), credentials.name());

		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		ObjectNode node = Json.readObject(body);
		String sourceToken = Json.checkStringNotNullOrEmpty(node, "source");

		SpaceResponse response = SpaceRequest
				.post(//
						"https://api.stripe.com/v1/customers")//
				.basicAuth("", settings.secretKey, "")//
				.formField("email", credentials.email().get())//
				.formField("source", sourceToken)//
				.go();

		checkStripeError(response);
		setStripeCustomerId(credentials, response.getString("id"));
		return JsonPayload.json(response.objectNode(), 201);
	}

	@Get("/customers/me")
	@Get("/customers/me/")
	public Payload getCustomer(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest
				.get(//
						"https://api.stripe.com/v1/customers/{customerId}")//
				.basicAuth("", settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return JsonPayload.json(response.objectNode());
	}

	@Delete("/customers/me")
	@Delete("/customers/me/")
	public Payload deleteStripeCustomer() {

		Credentials credentials = SpaceContext.checkUserCredentials();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest
				.delete(//
						"https://api.stripe.com/v1/customers/{customerId}")//
				.basicAuth("", settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return JsonPayload.json(response.objectNode());
	}

	@Post("/customers/me/sources")
	@Post("/customers/me/sources/")
	public Payload postCard(String body, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String customerId = getStripeCustomerId(credentials);
		ObjectNode node = Json.readObject(body);
		String sourceToken = Json.checkStringNotNullOrEmpty(node, "source");
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest
				.post(//
						"https://api.stripe.com/v1/customers/{customerId}/sources")//
				.basicAuth("", settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.formField("source", sourceToken)//
				.go();

		checkStripeError(response);
		return JsonPayload.json(response.objectNode(), 201);
	}

	@Delete("/customers/me/sources/:cardId")
	@Delete("/customers/me/sources/:cardId/")
	public Payload deleteStripeCard(String cardId, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest
				.delete(//
						"https://api.stripe.com/v1/customers/{customerId}/sources/{cardId}")//
				.basicAuth("", settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.routeParam("cardId", cardId)//
				.go();

		checkStripeError(response);
		return JsonPayload.json(response.objectNode());
	}

	//
	// Implementation
	//

	private void checkStripeError(SpaceResponse response) {
		int status = response.httpResponse().getStatus();
		if (status != 200) {
			throw new SpaceException("stripe-error", status, //
					"Stripe error, type=[%s], message=[%s]", //
					response.getString("error.type"), //
					response.getString("error.message"));
		}
	}

	private boolean hasStripeCustomerId(Credentials credentials) {

		JsonNode value = credentials.getFromStash(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID);
		return value != null && value.isTextual();
	}

	private String getStripeCustomerId(Credentials credentials) {

		JsonNode value = credentials.getFromStash(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID);

		if (value != null && value.isTextual())
			return value.asText();

		throw new NotFoundException("no stripe customer for credentials [%s][%s]", //
				credentials.level(), credentials.name());
	}

	private void setStripeCustomerId(Credentials credentials, String stripeCustomerId) {

		credentials.addToStash(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID, stripeCustomerId);
		CredentialsResource.get().update(credentials);
	}

	//
	// singleton
	//

	private static StripeResource singleton = new StripeResource();

	static StripeResource get() {
		return singleton;
	}

	private StripeResource() {
		SettingsResource.get().registerSettingsClass(StripeSettings.class);
	}
}
