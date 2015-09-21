package com.magiclabs.restapi;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class MappyImport extends AbstractTest {

	public static void main1(String[] args) {
		int i = 0;
		for (double lat = 48; lat <= 49.5; lat += 0.1) {
			for (double lon = 1.8; lon <= 2.9; lon += 0.1) {

				System.out.println("i=" + (i++) + " : " + lat + ',' + lon + ','
						+ (lat + 0.1) + ',' + (lon + 0.1));
			}
		}
	}

	public static void main(String[] args) {
		try {
			HttpRequestWithBody req2 = Unirest
					.delete("http://localhost:8080/v1/schema/resto")
					.basicAuth("dave", "hi_dave")
					.header("x-magic-app-id", "test");

			delete(req2, 200, 404);

			RequestBodyEntity req3 = Unirest
					.post("http://localhost:8080/v1/schema/resto")
					.basicAuth("dave", "hi_dave")
					.header("x-magic-app-id", "test")
					.body(buildRestoSchema().toString());

			post(req3, 201);

			for (double lat = 48; lat <= 49.5; lat += 0.1) {
				for (double lon = 1.8; lon <= 2.9; lon += 0.1) {

					HttpRequest req1 = Unirest
							.get("http://search.mappy.net/search/1.0/find")
							.queryString("max_results", "100")
							.queryString("q", "restaurant")
							.queryString(
									"bbox",
									"" + lat + ',' + lon + ',' + (lat + 0.1)
											+ ',' + (lon + 0.1));

					// "48.671228,1.854415,49.034931,2.843185");

					JsonObject res1 = get(req1, 200).json();

					res1.get("pois").asArray().forEach(MappyImport::copyPoi);
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static void copyPoi(JsonValue jsonValue) {
		JsonObject src = jsonValue.asObject();
		JsonBuilder target = Json.builder()
				.add("name", src.get("name").asString()) //
				.add("town", src.get("town").asString()) //
				.add("zipcode", src.get("pCode").asString()) //
				.add("way", src.get("way").asString()) //
				.stObj("where") //
				.add("lat", src.get("lat").asDouble()) //
				.add("lon", src.get("lng").asDouble()) //
				.end();

		if (src.get("rubricId") != null)
			target = target.add("mainRubricId", src.get("rubricId").asString());

		if (src.get("phone") != null)
			target = target.add("phone", src.get("phone").asString());

		if (src.get("url") != null)
			target = target.add("url", src.get("url").asString());

		if (src.get("illustration") != null)
			target = target.add("illustration", src.get("illustration")
					.asObject().get("url").asString());

		JsonValue allRubrics = src.get("allRubrics");
		if (allRubrics != null && allRubrics.asArray().size() > 0) {
			final JsonBuilder target2 = target.stArr("rubrics");
			allRubrics.asArray().forEach(
					rubric -> {
						target2.addObj()
								.add("rubricId",
										rubric.asObject().get("id").asString()) //
								.add("rubricLabel",
										rubric.asObject().get("label")
												.asString());
					});
		}

		RequestBodyEntity req = Unirest
				.post("http://localhost:8080/v1/data/resto")
				.basicAuth("dave", "hi_dave").header("x-magic-app-id", "test")
				.body(target.build().toString());

		try {
			post(req, 201);
		} catch (UnirestException e) {
			throw new RuntimeException(e);
		}
	}

	public static JsonObject buildRestoSchema() {
		return SchemaBuilder.builder("resto") //
				.add("name", "text").language("french").required() //
				.add("where", "geopoint").required() //
				.add("way", "text").language("french").required() //
				.add("town", "text").language("french").required() //
				.add("zipcode", "string").required() //
				.add("mainRubricId", "string").required() //
				.add("url", "string") //
				.add("illustration", "string") //
				.add("phone", "string") //
				.startObject("rubrics").required() //
				.array() //
				.add("rubricId", "string").required() //
				.add("rubricLabel", "text").language("french").required() //
				.end() //
				.build();
	}

}
