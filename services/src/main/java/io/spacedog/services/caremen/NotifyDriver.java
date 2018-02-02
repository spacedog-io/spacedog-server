package io.spacedog.services.caremen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.core.Json8;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.services.PushResource;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.SettingsResource;
import io.spacedog.services.Start;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceFields;

public class NotifyDriver extends Notificator implements SpaceFields {

	public static final String DRIVER_APP_ID_SUFFIX = "driver";

	PushLog newImmediate(Course course, Credentials credentials) {

		// search for drivers
		List<String> credentialsIds = searchDrivers(//
				credentials.backendId(), course);

		// if requester is a driver, he does not need the push
		if (credentials.roles().contains(Driver.TYPE))
			credentialsIds.remove(credentials.id());

		// search for installations
		SearchResponse response = searchInstallations(//
				credentials.backendId(), DRIVER_APP_ID_SUFFIX, credentialsIds);

		// push message to drivers
		Optional<Alert> alert = Alert.of("Demande de course immédiate", //
				"Un client vient de commander une course immédiate", //
				"newImmediate.wav");

		ObjectNode message = toPushMessage(course.meta.id, NEW_IMMEDIATE, alert);

		PushLog pushLog = new PushLog();
		for (SearchHit hit : response.getHits().hits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			PushResource.get().pushToInstallation(pushLog, hit.id(), //
					installation, message, credentials, BadgeStrategy.manual);
		}

		return pushLog;
	}

	PushLog cancelled(Course course, Credentials credentials) {
		ObjectNode message = toPushMessage(course.meta.id, CANCELLED, //
				Alert.of("Course annulée", "Le client a annulé la course", "cancelled.wav"));
		return pushToDriver(course, message, credentials);
	}

	//
	// Implementation
	//

	private List<String> searchDrivers(String backendId, Course course) {

		AppConfigurationSettings settings = SettingsResource.get()//
				.load(AppConfigurationSettings.class);

		int maxDistanceToCustomer = settings.maxDistanceToCustomerFromEligibleDriversInMeters;
		int maxDistanceBetweenDrivers = settings.maxDistanceBetweenEligibleDriversInMeters;
		int obsolescence = settings.driverLastLocationObsolescenceInMinutes;

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(FIELD_STATUS, "working"))//
				.must(QueryBuilders.termsQuery("vehicule.type", //
						compatibleVehiculeTypes(course.requestedVehiculeType)))//
				.must(QueryBuilders.rangeQuery("lastLocation.when")//
						.gt("now-" + obsolescence + "m"));

		GeoDistanceSortBuilder sort = SortBuilders.geoDistanceSort("lastLocation.where")//
				.point(course.from.geopoint.lat, course.from.geopoint.lon)//
				.order(SortOrder.ASC).unit(DistanceUnit.METERS).sortMode("min");

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, Driver.TYPE).setQuery(query).setSize(5)//
				.setFetchSource(false).addField(FIELD_CREDENTIALS_ID)//
				.addSort(sort).get();

		double closestDriverDistance = -1;
		List<String> credentialsIds = new ArrayList<>(5);

		for (SearchHit hit : response.getHits().hits()) {
			double driverDistance = distance(hit);
			if (closestDriverDistance < 0)
				closestDriverDistance = driverDistance;
			if (!isDriverEligible(driverDistance, closestDriverDistance, //
					maxDistanceToCustomer, maxDistanceBetweenDrivers))
				break;
			credentialsIds.add(hit.field(FIELD_CREDENTIALS_ID).getValue());
		}

		return credentialsIds;
	}

	private boolean isDriverEligible(double driverDistance, //
			double closestDriverDistance, int maxDistanceToCustomer, //
			int maxDistanceBetweenDrivers) {

		if (driverDistance > maxDistanceToCustomer)
			return false;

		if (driverDistance - closestDriverDistance > maxDistanceBetweenDrivers)
			return false;

		return true;
	}

	private double distance(SearchHit hit) {
		return (double) hit.sortValues()[0];
	}

	private String[] compatibleVehiculeTypes(String requestedVehiculeType) {
		if ("classic".equals(requestedVehiculeType))
			return new String[] { "classic", "premium", "green", "break", "van" };

		if ("premium".equals(requestedVehiculeType))
			return new String[] { "premium", "green", "van" };

		if ("green".equals(requestedVehiculeType))
			return new String[] { "green" };

		if ("break".equals(requestedVehiculeType))
			return new String[] { "break", "van" };

		if ("van".equals(requestedVehiculeType))
			return new String[] { "van" };

		throw Exceptions.illegalArgument("invalid vehicule type [%s]", requestedVehiculeType);
	}

	private PushLog pushToDriver(Course course, ObjectNode message, Credentials credentials) {

		if (course.driver == null || course.driver.credentialsId == null)
			return new PushLog();

		SearchResponse response = searchInstallations(credentials.backendId(), //
				DRIVER_APP_ID_SUFFIX, Lists.newArrayList(course.driver.credentialsId));

		return pushTo(course.driver.credentialsId, Driver.TYPE, //
				response, message, credentials);
	}
}
