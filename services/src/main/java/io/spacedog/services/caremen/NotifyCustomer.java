package io.spacedog.services.caremen;

import java.util.Locale;
import java.util.Optional;

import org.elasticsearch.action.search.SearchResponse;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.services.PushResource.PushLog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.DateTimeZones;

public class NotifyCustomer extends Notificator {

	public static final String PASSENGER_APP_ID_SUFFIX = "passenger";

	private static final DateTimeFormatter pickupTimeFormatter = DateTimeFormat//
			.forPattern("HH'h'mm")//
			.withZone(DateTimeZones.PARIS)//
			.withLocale(Locale.FRENCH);

	PushLog driverHasGivenUp(Course course, Credentials credentials) {
		String message = "Votre chauffeur a rencontré un problème et ne peut pas "
				+ "vous rejoindre. Nous recherchons un autre chauffeur pour votre course.";

		return isSmsNotification(course)//
				? sendSms(course, message)
				: pushDriverHasGivenUp(course, message, credentials);
	}

	private PushLog pushDriverHasGivenUp(Course course, String body, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE, //
				Alert.of("Chauffeur indisponible", body, "default"));
		return pushToCustomer(course, message, credentials);
	}

	PushLog driverIsComing(Course course, Credentials credentials) {
		return isSmsNotification(course)//
				? smsDriverIsComing(course, credentials)
				: pushDriverIsComing(course, credentials);
	}

	private PushLog smsDriverIsComing(Course course, Credentials credentials) {

		StringBuilder builder = new StringBuilder()//
				.append("Votre chauffeur CAREMEN est en route.")//
				.append("\nHeure de départ : ").append(pickupTimeFormatter.print(course.requestedPickupTimestamp))//
				.append("\nLieu de départ : ").append(course.from.address)//
				.append("\nDestination : ").append(course.to.address)//
				.append("\nNuméro du chauffeur : ").append(course.driver.phone);

		return sendSms(course, builder.toString());
	}

	private PushLog pushDriverIsComing(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder("Votre chauffeur arrivera à ")//
				.append(course.from.address).append(" dans quelques instants");
		ObjectNode message = toPushMessage(course.meta.id, DRIVER_IS_COMING, //
				Alert.of("Votre chauffeur est en route", body.toString(), "default"));
		return pushToCustomer(course, message, credentials);
	}

	PushLog readyToLoad(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder(//
				"Votre chauffeur CAREMEN est arrivé et vous attend.");

		return isSmsNotification(course)//
				? sendSms(course, body.toString())
				: pushReadyToLoad(course, body, credentials);
	}

	private PushLog pushReadyToLoad(Course course, StringBuilder body, Credentials credentials) {
		Optional<Alert> alert = Alert.of("Votre chauffeur vous attend", //
				body.toString(), "readyToLoad.wav");
		ObjectNode message = toPushMessage(course.meta.id, READY_TO_LOAD, alert);
		return pushToCustomer(course, message, credentials);
	}

	PushLog inProgress(Course course, Credentials credentials) {
		if (isSmsNotification(course))
			return null;
		ObjectNode message = toPushMessage(course.meta.id, IN_PROGRESS, Alert.empty());
		return pushToCustomer(course, message, credentials);
	}

	PushLog completed(Course course, Credentials credentials) {
		if (isSmsNotification(course))
			return null;
		ObjectNode message = toPushMessage(course.meta.id, COMPLETED, Alert.empty());
		return pushToCustomer(course, message, credentials);
	}

	//
	// Implementation
	//

	private boolean isSmsNotification(Course course) {
		return course.customer.smsNotification //
				&& !Strings.isNullOrEmpty(course.customer.phone);
	}

	private PushLog pushToCustomer(Course course, ObjectNode message, Credentials credentials) {

		SearchResponse response = searchInstallations(credentials.backendId(), //
				PASSENGER_APP_ID_SUFFIX, Lists.newArrayList(course.customer.credentialsId));

		return pushTo(course.customer.credentialsId, "customer", //
				response, message, credentials);
	}

	private PushLog sendSms(Course course, String body) {
		sendSms(course.customer.phone, body);
		return null;
	}

}
