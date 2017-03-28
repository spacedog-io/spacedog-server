package io.spacedog.services;

import org.elasticsearch.common.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.core.Json8;
import io.spacedog.model.StripeSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.SpaceException;
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

	private static final String STRIPE_CUSTOMER = "customer";
	private static final String CREDENTIALS_STASH_STRIPE_CUSTOMER_ID = "stripeCustomerId";

	@Post("/customers")
	@Post("/customers/")
	public Payload postCustomer() {
		Credentials credentials = SpaceContext.checkUserCredentials();

		if (hasStripeCustomerId(credentials))
			throw Exceptions.illegalArgument(//
					"credentials [%s][%s] already have a stripe customer", //
					credentials.level(), credentials.name());

		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest.post("/v1/customers")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.formField("email", credentials.email().get())//
				.go();

		checkStripeError(response);
		updateStripeCustomerId(credentials, response.getString("id"));
		return JsonPayload.json(response.objectNode(), 201);
	}

	@Get("/customers/me")
	@Get("/customers/me/")
	public Payload getCustomer(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);

		SpaceResponse response = SpaceRequest.get("/v1/customers/{customerId}")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
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

		removeStripeCustomerId(credentials);

		SpaceResponse response = SpaceRequest.delete("/v1/customers/{customerId}")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
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
		ObjectNode node = Json8.readObject(body);
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);
		String sourceToken = Json8.checkStringNotNullOrEmpty(node, "source");

		SpaceRequest request = SpaceRequest.post("/v1/customers/{customerId}/sources")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.formField("source", sourceToken);

		Json8.checkString(node, "description").ifPresent(//
				description -> request.formField("metadata[description]", description));

		SpaceResponse response = request.go();
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
						"/v1/customers/{customerId}/sources/{cardId}")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.routeParam("cardId", cardId)//
				.go();

		checkStripeError(response);
		return JsonPayload.json(response.objectNode());
	}

	@Post("/charges")
	@Post("/charges/")
	public Payload postCharges(Context context) {
		return charge(false, context);
	}

	@Post("/charges/me")
	@Post("/charges/me/")
	public Payload postChargesMe(Context context) {
		return charge(true, context);
	}

	//
	// Implementation
	//

	private Payload charge(boolean myself, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		StripeSettings settings = SettingsResource.get().load(StripeSettings.class);
		SpaceRequest request = SpaceRequest.post("/v1/charges")//
				.baseUrl("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "");

		if (myself) {
			credentials.checkRoles(settings.rolesAllowedToPay);

			if (!Strings.isNullOrEmpty(context.get(STRIPE_CUSTOMER)))
				throw Exceptions.illegalArgument(//
						"overriding of my stripe customer is forbidden");

			request.formField(STRIPE_CUSTOMER, getStripeCustomerId(credentials));

		} else
			credentials.checkRoles(settings.rolesAllowedToCharge);

		for (String key : context.request().query().keys())
			request.formField(key, context.get(key));

		SpaceResponse response = request.go();
		checkStripeError(response);
		return JsonPayload.json(response.objectNode(), 200);
	}

	private void checkStripeError(SpaceResponse response) {
		int status = response.status();
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

	private void updateStripeCustomerId(Credentials credentials, String stripeCustomerId) {
		credentials.addToStash(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID, stripeCustomerId);
		CredentialsResource.get().update(credentials);
	}

	private void removeStripeCustomerId(Credentials credentials) {
		credentials.removeFromStash(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID);
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
