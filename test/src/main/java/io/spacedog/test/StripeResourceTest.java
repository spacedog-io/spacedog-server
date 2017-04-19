/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Token;

import io.spacedog.model.StripeSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class StripeResourceTest extends SpaceTest {

	@Test
	public void testStripeServices() throws AuthenticationException, InvalidRequestException, APIConnectionException,
			CardException, APIException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog david = signUp(test, "david", "hi david");

		// set stripe settings
		StripeSettings settings = new StripeSettings();
		settings.secretKey = SpaceEnv.defaultEnv().get("spacedog.stripe.test.secret.key");
		settings.rolesAllowedToCharge = Sets.newHashSet("super_admin");
		settings.rolesAllowedToPay = Sets.newHashSet("user");
		Stripe.apiKey = settings.secretKey;
		test.settings().save(settings);

		// david's stripe customer is not yet created
		SpaceRequest.get("/1/stripe/customers/me").auth(david)//
				.go(404);

		// david creates his stripe customer with his card token
		ObjectNode stripeCustomer = SpaceRequest.post("/1/stripe/customers").auth(david)//
				.go(201)//
				.assertEquals("customer", "object")//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(0, "sources.data")//
				.asJsonObject();

		// david fails to create another stripe customer
		// since he's got one already
		SpaceRequest.post("/1/stripe/customers").auth(david)//
				.go(400);

		// david gets his stripe customer
		SpaceRequest.get("/1/stripe/customers/me").auth(david)//
				.go(200)//
				.assertEquals(stripeCustomer);

		// david registers a first card
		String bnpCardId = SpaceRequest.post("/1/stripe/customers/me/sources")//
				.bodyJson("source", createCardToken().getId(), "description", "bnp").auth(david).go(201)//
				.assertEquals("bnp", "metadata.description")//
				.getString("id");

		// check david has a bnp card
		SpaceRequest.get("/1/stripe/customers/me").auth(david).go(200)//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(1, "sources.data")//
				.assertEquals(bnpCardId, "sources.data.0.id")//
				.assertEquals("bnp", "sources.data.0.metadata.description");

		// superadmin is not allowed to pay
		// because of settings.rolesAllowedToPay
		SpaceRequest.post("/1/stripe/charges/me").auth(test).go(403);

		// david fails to pays because 'customer' field is forbidden
		// because the stripe customer must be the one stored in credentials
		SpaceRequest.post("/1/stripe/charges/me")//
				.formField("customer", "XXX").auth(david).go(400);

		// david fails to pays because no fields
		SpaceRequest.post("/1/stripe/charges/me").auth(david).go(400);

		// david pays with his bnp card
		SpaceRequest.post("/1/stripe/charges/me")//
				.formField("amount", "800")//
				.formField("currency", "eur")//
				.formField("source", bnpCardId).auth(david).go(200);

		// david deletes his card from his stripe customer account
		SpaceRequest.delete("/1/stripe/customers/me/sources/" + bnpCardId).auth(david).go(200);

		// check david has no card anymore
		SpaceRequest.get("/1/stripe/customers/me").auth(david).go(200)//
				.assertSizeEquals(0, "sources.data");

		// david deletes again his card to gets 404
		SpaceRequest.delete("/1/stripe/customers/me/sources/" + bnpCardId).auth(david).go(404);

		// david creates another card
		String lclCardId = SpaceRequest.post("/1/stripe/customers/me/sources")//
				.bodyJson("source", createCardToken().getId(), "description", "lcl").auth(david).go(201)//
				.assertEquals("lcl", "metadata.description")//
				.getString("id");

		// check david has an lcl card
		SpaceRequest.get("/1/stripe/customers/me").auth(david).go(200)//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(1, "sources.data")//
				.assertEquals(lclCardId, "sources.data.0.id")//
				.assertEquals("lcl", "sources.data.0.metadata.description");

		// david is not allowed to charge a customer
		// because of settings.rolesAllowedToCharge
		SpaceRequest.post("/1/stripe/charges").auth(david).go(403);

		// superadmin fails to make charge a customer if no parameter
		SpaceRequest.post("/1/stripe/charges").auth(test).go(400);

		// superadmin charges david's lcl card
		SpaceRequest.post("/1/stripe/charges")//
				.formField("amount", "1200")//
				.formField("currency", "eur")//
				.formField("customer", stripeCustomer.get("id").asText())//
				.formField("source", lclCardId).auth(test).go(200);

		// david deletes his stripe customer account
		SpaceRequest.delete("/1/stripe/customers/me").auth(david).go(200);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// check david's stripe customer account is gone
		SpaceRequest.get("/1/stripe/customers/me").auth(david).go(404);
	}

	Token createCardToken() throws AuthenticationException, InvalidRequestException, APIConnectionException,
			CardException, APIException {
		Map<String, Object> tokenParams = Maps.newHashMap();
		Map<String, Object> cardParams = Maps.newHashMap();
		cardParams.put("number", "4242424242424242");
		cardParams.put("exp_month", 11);
		cardParams.put("exp_year", 2017);
		cardParams.put("cvc", "314");
		tokenParams.put("card", cardParams);
		Token token = Token.create(tokenParams);
		return token;
	}

}
