/**
 * © David Attias 2015
 */
package io.spacedog.test.stripe;

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

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.test.SpaceTest;

public class StripeRestyTest extends SpaceTest {

	@Test
	public void testStripeServices() throws AuthenticationException, InvalidRequestException, APIConnectionException,
			CardException, APIException {

		// prepare
		prepareTest();
		SpaceDog superdamin = clearServer();
		SpaceDog david = createTempDog(superdamin, "david");

		// set stripe settings
		StripeSettings settings = new StripeSettings();
		settings.secretKey = SpaceEnv.env().getOrElseThrow("spacedog.stripe.test.secret.key");
		settings.rolesAllowedToCharge = Sets.newHashSet("superadmin");
		settings.rolesAllowedToPay = Sets.newHashSet("user");
		Stripe.apiKey = settings.secretKey;
		superdamin.stripe().settings(settings);

		// david's stripe customer is not yet created
		david.get("/2/stripe/customers/me").go(404).asVoid();

		// david creates his stripe customer with his card token
		ObjectNode stripeCustomer = david.post("/2/stripe/customers").go(201)//
				.assertEquals("customer", "object")//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(0, "sources.data")//
				.asJsonObject();

		// david fails to create another stripe customer
		// since he's got one already
		david.post("/2/stripe/customers").go(400).asVoid();

		// david gets his stripe customer
		david.get("/2/stripe/customers/me").go(200)//
				.assertEquals(stripeCustomer);

		// david registers a first card
		String bnpCardId = david.post("/2/stripe/customers/me/sources")//
				.bodyJson("source", createCardToken().getId(), "description", "bnp")//
				.go(201)//
				.assertEquals("bnp", "metadata.description")//
				.getString("id");

		// check david has a bnp card
		david.get("/2/stripe/customers/me").go(200)//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(1, "sources.data")//
				.assertEquals(bnpCardId, "sources.data.0.id")//
				.assertEquals("bnp", "sources.data.0.metadata.description");

		// superadmin is not allowed to pay
		// because of settings.rolesAllowedToPay
		superdamin.post("/2/stripe/charges/me").go(403).asVoid();

		// david fails to pays because 'customer' field is forbidden
		// because the stripe customer must be the one stored in credentials
		david.post("/2/stripe/charges/me")//
				.formField("customer", "XXX").go(400).asVoid();

		// david fails to pays because no fields
		david.post("/2/stripe/charges/me").go(400).asVoid();

		// david pays with his bnp card
		david.post("/2/stripe/charges/me")//
				.formField("amount", "800")//
				.formField("currency", "eur")//
				.formField("source", bnpCardId)//
				.go(200).asVoid();

		// david deletes his card from his stripe customer account
		david.delete("/2/stripe/customers/me/sources/" + bnpCardId).go(200).asVoid();

		// check david has no card anymore
		david.get("/2/stripe/customers/me").go(200)//
				.assertSizeEquals(0, "sources.data");

		// david deletes again his card to gets 404
		david.delete("/2/stripe/customers/me/sources/" + bnpCardId).go(404).asVoid();

		// david creates another card
		String lclCardId = david.post("/2/stripe/customers/me/sources")//
				.bodyJson("source", createCardToken().getId(), "description", "lcl").go(201)//
				.assertEquals("lcl", "metadata.description")//
				.getString("id");

		// check david has an lcl card
		david.get("/2/stripe/customers/me").go(200)//
				.assertEquals("platform@spacedog.io", "email")//
				.assertSizeEquals(1, "sources.data")//
				.assertEquals(lclCardId, "sources.data.0.id")//
				.assertEquals("lcl", "sources.data.0.metadata.description");

		// david is not allowed to charge a customer
		// because of settings.rolesAllowedToCharge
		david.post("/2/stripe/charges").go(403).asVoid();

		// superadmin fails to make charge a customer if no parameter
		superdamin.post("/2/stripe/charges").go(400).asVoid();

		// superadmin charges david's lcl card
		superdamin.post("/2/stripe/charges")//
				.formField("amount", "1200")//
				.formField("currency", "eur")//
				.formField("customer", stripeCustomer.get("id").asText())//
				.formField("source", lclCardId)//
				.go(200).asVoid();

		// david deletes his stripe customer account
		david.delete("/2/stripe/customers/me").go(200).asVoid();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// check david's stripe customer account is gone
		david.get("/2/stripe/customers/me").go(404).asVoid();
	}

	Token createCardToken() throws AuthenticationException, InvalidRequestException, APIConnectionException,
			CardException, APIException {
		Map<String, Object> tokenParams = Maps.newHashMap();
		Map<String, Object> cardParams = Maps.newHashMap();
		cardParams.put("number", "4242424242424242");
		cardParams.put("exp_month", 11);
		cardParams.put("exp_year", 2019);
		cardParams.put("cvc", "314");
		tokenParams.put("card", cardParams);
		Token token = Token.create(tokenParams);
		return token;
	}

}
