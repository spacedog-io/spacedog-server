package com.magiclabs.restapi;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;

public class MappyPoiImport extends AbstractTest {

	public static void main(String[] args) {
		try {
			HttpRequest req1 = Unirest
					.get("http://search.mappy.net/search/1.0/find")
					.queryString("max_results", "100")
					.queryString("q", "restaurant")
					.queryString("bbox",
							"48.671228,1.854415,49.034931,2.843185");

			JsonObject res1 = get(req1, 200).json();

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
			res1.get("pois").asArray().forEach(MappyPoiImport::copyPoi);

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
				.add("townCode", src.get("townCode").asString()) //
				.add("way", src.get("way").asString());

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
				.add("name", "text").required() //
				.add("where", "geopoint").required() //
				.add("way", "text").required() //
				.add("town", "text").required() //
				.add("townCode", "string").required() //
				.add("mainRubricId", "string").required() //
				.add("url", "string") //
				.add("illustration", "string") //
				.add("phone", "string") //
				.startObject("rubrics").required() //
				.array() //
				.add("rubricId", "string").required() //
				.add("rubricLabel", "text").required() //
				.end() //
				.build();
	}

}
