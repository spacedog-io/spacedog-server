package io.spacedog.server;

import org.elasticsearch.common.Strings;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.KeyValue;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Optional7;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/stripe")
public class StripeService extends SpaceService {

	//
	// Routes
	//

	private static final String STRIPE_CUSTOMER = "customer";
	private static final String CREDENTIALS_STASH_STRIPE_CUSTOMER_ID = "stripeCustomerId";

	@Post("/customers")
	@Post("/customers/")
	public Payload postCustomer() {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();

		if (hasStripeCustomerId(credentials))
			throw Exceptions.illegalArgument(//
					"credentials [%s][%s] already have a stripe customer", //
					credentials.type(), credentials.username());

		StripeSettings settings = stripeSettings();

		SpaceResponse response = SpaceRequest.post("/v1/customers")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.formField("email", credentials.email().get())//
				.go();

		checkStripeError(response);
		updateStripeCustomerId(credentials, response.getString("id"));

		return JsonPayload.created()//
				.withContent(response.asJsonObject()).build();
	}

	@Get("/customers/me")
	@Get("/customers/me/")
	public Payload getCustomer(Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = stripeSettings();

		SpaceResponse response = SpaceRequest.get("/v1/customers/{customerId}")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return JsonPayload.ok().withContent(response.asJsonObject()).build();
	}

	@Delete("/customers/me")
	@Delete("/customers/me/")
	public Payload deleteStripeCustomer() {

		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = stripeSettings();

		removeStripeCustomerId(credentials);

		SpaceResponse response = SpaceRequest.delete("/v1/customers/{customerId}")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return JsonPayload.ok().withContent(response.asJsonObject()).build();
	}

	@Post("/customers/me/sources")
	@Post("/customers/me/sources/")
	public Payload postCard(String body, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		String customerId = getStripeCustomerId(credentials);
		ObjectNode node = Json.readObject(body);
		StripeSettings settings = stripeSettings();
		String sourceToken = Json.checkStringNotNullOrEmpty(node, "source");

		SpaceRequest request = SpaceRequest.post("/v1/customers/{customerId}/sources")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.formField("source", sourceToken);

		Optional7<String> description = Json.checkString(node, "description");
		if (description.isPresent())
			request.formField("metadata[description]", description.get());

		SpaceResponse response = request.go();
		checkStripeError(response);
		return JsonPayload.created().withContent(response.asJsonObject()).build();
	}

	@Delete("/customers/me/sources/:cardId")
	@Delete("/customers/me/sources/:cardId/")
	public Payload deleteStripeCard(String cardId, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = stripeSettings();

		SpaceResponse response = SpaceRequest.delete(//
				"/v1/customers/{customerId}/sources/{cardId}")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.routeParam("cardId", cardId)//
				.go();

		checkStripeError(response);
		return JsonPayload.ok().withContent(response.asJsonObject()).build();
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
		Credentials credentials = SpaceContext.credentials();
		StripeSettings settings = stripeSettings();
		SpaceRequest request = SpaceRequest.post("/v1/charges")//
				.backend("https://api.stripe.com")//
				.basicAuth(settings.secretKey, "");

		if (myself) {
			credentials.checkIfAuthorized(settings.rolesAllowedToPay);

			if (!Strings.isNullOrEmpty(context.get(STRIPE_CUSTOMER)))
				throw Exceptions.illegalArgument(//
						"overriding of my stripe customer is forbidden");

			request.formField(STRIPE_CUSTOMER, getStripeCustomerId(credentials));

		} else
			credentials.checkIfAuthorized(settings.rolesAllowedToCharge);

		for (String key : context.request().query().keys())
			request.formField(key, context.get(key));

		SpaceResponse response = request.go();
		checkStripeError(response);

		return JsonPayload.ok()//
				.withContent(response.asJsonObject()).build();
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
		return credentials.getTag(//
				CREDENTIALS_STASH_STRIPE_CUSTOMER_ID).isPresent();
	}

	private String getStripeCustomerId(Credentials credentials) {

		 Optional7<String> value = credentials.getTag(//
				 CREDENTIALS_STASH_STRIPE_CUSTOMER_ID);

		if (value.isPresent())
			return value.get();

		throw new NotFoundException("credentials [%s][%s] has no stripe customer id", //
				credentials.type(), credentials.username());
	}

	private void updateStripeCustomerId(Credentials credentials, String stripeCustomerId) {
		credentials.setTag(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID, stripeCustomerId);
		CredentialsService.get().update(credentials);
	}

	private void removeStripeCustomerId(Credentials credentials) {
		credentials.removeTag(CREDENTIALS_STASH_STRIPE_CUSTOMER_ID);
		CredentialsService.get().update(credentials);
	}

	private StripeSettings stripeSettings() {
		return SettingsService.get().getAsObject(StripeSettings.class);
	}

	//
	// singleton
	//

	private static StripeService singleton = new StripeService();

	public static StripeService get() {
		return singleton;
	}

	private StripeService() {
		SettingsService.get().registerSettings(StripeSettings.class);
	}
}
