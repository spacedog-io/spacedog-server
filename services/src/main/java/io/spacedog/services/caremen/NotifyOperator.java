package io.spacedog.services.caremen;

import java.util.Locale;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.spacedog.services.SettingsResource;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.DateTimeZones;

public class NotifyOperator extends Notificator {

	void newScheduled(Course course) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course programmée pour le ")//
				.append(pickupFormatter.print(course.requestedPickupTimestamp)) //
				.append(". Départ : ").append(course.from.address)//
				.append(". Catégorie : ").append(course.requestedVehiculeType)//
				.append(".");

		sendSms(builder);
	}

	void newImmediate(Course course, int notifications) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course immédiate. Départ : ")//
				.append(course.from.address).append(". Catégorie : ")//
				.append(course.requestedVehiculeType).append(". Chauffeurs notifiés : ")//
				.append(notifications);

		sendSms(builder);
	}

	void driverHasGivenUp(Course course, Credentials credentials) {

		StringBuilder builder = new StringBuilder("Le chauffeur [")//
				.append(credentials.name())//
				.append("] a renoncé à la course du client [")//
				.append(course.customer.firstname).append(" ")//
				.append(course.customer.lastname)//
				.append("]. La course a été proposée à d'autres chauffeurs.");

		sendSms(builder);
	}

	//
	// Implementation
	//

	private static final DateTimeFormatter pickupFormatter = DateTimeFormat//
			.forPattern("dd/MM' à 'HH'h'mm")//
			.withZone(DateTimeZones.PARIS)//
			.withLocale(Locale.FRENCH);

	private void sendSms(StringBuilder builder) {
		sendSms(operatorPhoneNumber(), builder.toString());
	}

	private String operatorPhoneNumber() {
		return SettingsResource.get().load(AppConfigurationSettings.class)//
				.operatorPhoneNumber;
	}

}
