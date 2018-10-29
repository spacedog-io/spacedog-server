package io.spacedog.services.stripe;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.common.Strings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.client.stripe.PaymentRequest;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.server.Services;
import io.spacedog.utils.Exceptions;

public class StripeService {

	public ObjectNode createCustomerFor(Credentials credentials) {

		if (hasStripeCustomerId(credentials))
			throw Exceptions.illegalArgument(//
					"credentials [%s][%s] already have a stripe customer", //
					credentials.type(), credentials.username());

		StripeSettings settings = settings();

		SpaceResponse response = SpaceRequest.post("/v1/customers")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "")//
				.formField("email", credentials.email().get())//
				.go();

		checkStripeError(response);
		updateStripeCustomerId(credentials, response.getString("id"));
		return response.asJsonObject();
	}

	public ObjectNode getCustomerFor(Credentials credentials) {
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = settings();

		SpaceResponse response = SpaceRequest.get(//
				"/v1/customers/{customerId}")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return response.asJsonObject();
	}

	public ObjectNode deleteCustomerFor(Credentials credentials) {
		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = settings();

		removeStripeCustomerId(credentials);

		SpaceResponse response = SpaceRequest.delete(//
				"/v1/customers/{customerId}")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.go();

		checkStripeError(response);
		return response.asJsonObject();
	}

	public ObjectNode createSourceFor(Credentials credentials, //
			String sourceToken, Optional<String> description) {

		String customerId = getStripeCustomerId(credentials);
		StripeSettings settings = settings();

		SpaceRequest request = SpaceRequest.post("/v1/customers/{customerId}/sources")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.formField("source", sourceToken);

		if (description.isPresent())
			request.formField("metadata[description]", description.get());

		SpaceResponse response = request.go();

		checkStripeError(response);
		return response.asJsonObject();
	}

	public ObjectNode deleteSourceFor(Credentials credentials, String sourceId) {
		StripeSettings settings = settings();
		String customerId = getStripeCustomerId(credentials);

		SpaceResponse response = SpaceRequest.delete(//
				"/v1/customers/{customerId}/sources/{sourceId}")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "")//
				.routeParam("customerId", customerId)//
				.routeParam("sourceId", sourceId)//
				.go();

		checkStripeError(response);
		return response.asJsonObject();

	}

	public ObjectNode charge(PaymentRequest request) {
		return charge(request.toStripParameters());
	}

	public ObjectNode charge(Credentials credentials, Map<String, String> params) {

		params = Maps.newHashMap(params);

		if (!Strings.isNullOrEmpty(params.get(STRIPE_CUSTOMER_PARAM)))
			throw Exceptions.illegalArgument(//
					"overriding of my stripe customer is forbidden");

		params.put(STRIPE_CUSTOMER_PARAM, getStripeCustomerId(credentials));

		return charge(params);
	}

	public ObjectNode charge(Map<String, String> parameters) {
		StripeSettings settings = settings();

		SpaceRequest request = SpaceRequest.post("/v1/charges")//
				.backend(STRIPE_BASE_URL)//
				.basicAuth(settings.secretKey, "");

		for (Entry<String, String> entry : parameters.entrySet())
			request.formField(entry.getKey(), entry.getValue());

		SpaceResponse response = request.go();
		checkStripeError(response);
		return response.asJsonObject();
	}

	public StripeSettings settings() {
		return Services.settings().getOrThrow(StripeSettings.class);
	}

	//
	// Implemetation
	//

	private static final String STRIPE_BASE_URL = "https://api.stripe.com";
	private static final String STRIPE_CUSTOMER_PARAM = "customer";
	private static final String CREDENTIALS_TAG_STRIPE_CUSTOMER_ID = "stripeCustomerId";

	private void checkStripeError(SpaceResponse response) {
		int status = response.status();
		if (status != 200)
			throw new SpaceException("stripe-error", status, //
					"Stripe error, type=[%s], message=[%s]", //
					response.getString("error.type"), //
					response.getString("error.message"));
	}

	private boolean hasStripeCustomerId(Credentials credentials) {
		return credentials.getTagValues(CREDENTIALS_TAG_STRIPE_CUSTOMER_ID)//
				.isEmpty() == false;
	}

	private String getStripeCustomerId(Credentials credentials) {

		Set<String> values = credentials.getTagValues(//
				CREDENTIALS_TAG_STRIPE_CUSTOMER_ID);

		if (values.isEmpty())
			throw Exceptions.invalidState("no-stripe-customer-id", //
					"[%s][%s] has no stripe customer id", //
					credentials.type(), credentials.username());

		if (values.size() > 1)
			throw Exceptions.runtime("credentials [%s][%s] has [%s] stripe customer ids", //
					credentials.type(), values.size(), credentials.username());

		return values.iterator().next();
	}

	private void updateStripeCustomerId(Credentials credentials, String stripeCustomerId) {
		credentials.addTag(CREDENTIALS_TAG_STRIPE_CUSTOMER_ID, stripeCustomerId);
		Services.credentials().update(credentials);
	}

	private void removeStripeCustomerId(Credentials credentials) {
		credentials.removeTags(CREDENTIALS_TAG_STRIPE_CUSTOMER_ID);
		Services.credentials().update(credentials);
	}

}
