package io.spacedog.services.caremen;

import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.spacedog.services.SettingsResource;
import io.spacedog.services.SmsResource;
import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.utils.Credentials;

public class NotifyOperator implements CourseStatus {

	//
	// text messages
	//

	private static DateTimeZone parisZone = DateTimeZone.forID("Europe/Paris");

	private static DateTimeFormatter pickupFormatter = DateTimeFormat//
			.forPattern("dd/MM' à 'HH'h'mm").withZone(parisZone).withLocale(Locale.FRENCH);

	void newScheduled(Course course) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course programmée pour le ")//
				.append(pickupFormatter.print(course.requestedPickupTimestamp)) //
				.append(". Départ : ").append(course.from.address)//
				.append(". Catégorie : ").append(course.requestedVehiculeType)//
				.append(".");

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	void newImmediate(Course course, int notifications) {

		StringBuilder builder = new StringBuilder()//
				.append("Nouvelle demande de course immédiate. Départ : ")//
				.append(course.from.address).append(". Catégorie : ")//
				.append(course.requestedVehiculeType).append(". Chauffeurs notifiés : ")//
				.append(notifications);

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	void driverHasGivenUp(Course course, Credentials credentials) {

		StringBuilder builder = new StringBuilder("Le chauffeur [")//
				.append(credentials.name())//
				.append("] a renoncé à la course du client [")//
				.append(course.customer.firstname).append(" ")//
				.append(course.customer.lastname)//
				.append("]. La course a été proposée à d'autres chauffeurs.");

		SmsMessage message = new SmsMessage()//
				.to(operatorPhoneNumber()).body(builder.toString());
		SmsResource.get().send(message);
	}

	private String operatorPhoneNumber() {
		return SettingsResource.get().load(AppConfigurationSettings.class)//
				.operatorPhoneNumber;
	}

}
