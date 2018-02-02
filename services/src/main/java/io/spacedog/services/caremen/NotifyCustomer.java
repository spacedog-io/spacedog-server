package io.spacedog.services.caremen;

import org.elasticsearch.action.search.SearchResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.services.PushResource.PushLog;
import io.spacedog.utils.Credentials;

public class NotifyCustomer extends Notificator {

	public static final String PASSENGER_APP_ID_SUFFIX = "passenger";

	PushLog driverHasGivenUp(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE, //
				Alert.of("Chauffeur indisponible", //
						"Votre chauffeur a rencontré un problème et ne peut pas vous rejoindre."
								+ " Nous recherchons un autre chauffeur.",
						"default"));
		return pushToCustomer(course, message, credentials);
	}

	PushLog driverIsComing(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder("Votre chauffeur arrivera à ")//
				.append(course.from.address).append(" dans quelques instants");
		ObjectNode message = toPushMessage(course.meta.id, DRIVER_IS_COMING, //
				Alert.of("Votre chauffeur est en route", body.toString(), "default"));
		return pushToCustomer(course, message, credentials);
	}

	PushLog readyToLoad(Course course, Credentials credentials) {
		StringBuilder body = new StringBuilder("Votre chauffeur est arrivé à ")//
				.append(course.from.address);
		ObjectNode message = toPushMessage(course.meta.id, READY_TO_LOAD, //
				Alert.of("Votre chauffeur vous attend", body.toString(), "readyToLoad.wav"));
		return pushToCustomer(course, message, credentials);
	}

	PushLog inProgress(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, IN_PROGRESS, Alert.empty());
		return pushToCustomer(course, message, credentials);
	}

	PushLog completed(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, COMPLETED, Alert.empty());
		return pushToCustomer(course, message, credentials);
	}

	//
	// Implementation
	//

	private PushLog pushToCustomer(Course course, ObjectNode message, Credentials credentials) {

		SearchResponse response = searchInstallations(credentials.backendId(), //
				PASSENGER_APP_ID_SUFFIX, Lists.newArrayList(course.customer.credentialsId));

		return pushTo(course.customer.credentialsId, "customer", //
				response, message, credentials);
	}

}
