package io.spacedog.services.caremen;

import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.core.Json8;
import io.spacedog.jobs.Internals;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.PushService;
import io.spacedog.services.PushResource;
import io.spacedog.services.PushResource.PushLog;
import io.spacedog.services.SmsResource;
import io.spacedog.services.SmsResource.SmsMessage;
import io.spacedog.services.Start;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.JsonBuilder;

public class Notificator implements CourseStatus {

	//
	// Sms Notification
	//

	protected void sendSms(String to, String body) {
		SmsMessage message = new SmsMessage().to(to).body(body);
		SmsResource.get().send(message);
	}

	//
	// Push Notification
	//

	protected static class Alert {
		String title;
		String body;
		String sound;

		static Optional<Alert> of(String title, String body, String sound) {
			Alert alert = new Alert();
			alert.title = title;
			alert.body = body;
			alert.sound = sound;
			return Optional.of(alert);
		}

		static Optional<Alert> empty() {
			return Optional.empty();
		}

	}

	protected SearchResponse searchInstallations(String backendId, String appIdSuffix, List<String> credentialsIds) {

		// appId is caremendriver or caremenpassenger for prod and dev env
		// appId is carerec-driver or carerec-passenger for recette env
		String appId = (backendId.equals("carerec") ? "carerec-" : "caremen") //
				+ appIdSuffix;

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termsQuery("tags.value", credentialsIds))//
				.must(QueryBuilders.termQuery("appId", appId));

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, "installation")//
				.setQuery(query)//
				// user might have more than one installation
				// when app is reinstalled or if installed on more
				// than one device
				.setSize(100)//
				.get();

		return response;
	}

	protected ObjectNode toPushMessage(String courseId, //
			String type, Optional<Alert> alert) {

		ObjectNode apsMessage = apsMessage(courseId, type, alert);
		ObjectNode gcmMessage = gcmMessage(courseId, type, alert);

		return Json8.objectBuilder()//
				.node(PushService.APNS_SANDBOX.name(), apsMessage)//
				.node(PushService.APNS.name(), apsMessage)//
				.node(PushService.GCM.name(), gcmMessage)//
				.build();
	}

	protected ObjectNode apsMessage(String courseId, String type, Optional<Alert> alert) {
		JsonBuilder<ObjectNode> builder = Json8.objectBuilder()//
				.put("id", courseId)//
				.put("type", type)//
				.object("aps")//
				.put("content-available", 1);

		if (alert.isPresent())
			builder.put("sound", alert.get().sound)//
					.object("alert")//
					.put("title", alert.get().title)//
					.put("body", alert.get().body);

		return builder.build();
	}

	protected ObjectNode gcmMessage(String courseId, String type, Optional<Alert> alert) {
		JsonBuilder<ObjectNode> builder = Json8.objectBuilder()//
				.object("data")//
				.put("id", courseId)//
				.put("type", type);

		if (alert.isPresent())
			builder.put("title", alert.get().title)//
					.put("body", alert.get().body);

		return builder.build();
	}

	protected PushLog pushTo(String credentialsId, String type, //
			SearchResponse response, ObjectNode message, Credentials credentials) {

		PushLog pushLog = new PushLog();

		try {

			if (response.getHits().getTotalHits() == 0) {

				String title = String.format(//
						"no installation found for %s with credentials id [%s] in backend [%s]", //
						type, credentialsId, credentials.backendId());

				Internals.get().notify(//
						Start.get().configuration()//
								.superdogAwsNotificationTopic().orElse(null), //
						title, title);
			}

			for (SearchHit hit : response.getHits().hits()) {
				ObjectNode installation = Json8.readObject(hit.sourceAsString());
				PushResource.get().pushToInstallation(pushLog, hit.id(), //
						installation, message, credentials, BadgeStrategy.manual);
			}

			if (pushLog.successes == 0) {
				String title = String.format(//
						"failed to push to %s in backend [%s]", //
						type, credentials.backendId());

				Internals.get().notify(//
						Start.get().configuration()//
								.superdogAwsNotificationTopic().orElse(null), //
						title, pushLog.toNode().toString());

			}

		} catch (Throwable t) {

			Internals.get().notify(//
					Start.get().configuration()//
							.superdogAwsNotificationTopic().orElse(null), //
					String.format("Error pushing to [%s]", type), //
					Throwables.getStackTraceAsString(t));
		}

		return pushLog;
	}

}
