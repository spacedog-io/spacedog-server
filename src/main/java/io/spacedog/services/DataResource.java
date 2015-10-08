package io.spacedog.services;

import io.spacedog.services.ElasticHelper.FilteredSearchBuilder;

import java.util.concurrent.ExecutionException;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.eclipsesource.json.JsonObject;

@Prefix("/v1/data")
public class DataResource extends AbstractResource {

	private static DataResource singleton = new DataResource();

	static DataResource get() {
		return singleton;
	}

	private DataResource() {
	}

	@Get("")
	@Get("/")
	public Payload search(Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);
			return doSearch(credentials.getAccountId(), null, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload search(String type, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);
			return doSearch(credentials.getAccountId(), type, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload create(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getAccountId(), type);

			String id = createInternal(credentials.getAccountId(), type,
					JsonObject.readFrom(jsonBody), credentials.getId());

			return created("/v1/data", type, id);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	String createInternal(String index, String type, JsonObject object,
			String createdBy) {

		String now = DateTime.now().toString();

		// remove then add meta to avoid developers to
		// set any meta fields directly
		object.remove("meta").add(
				"meta",
				new JsonObject().add("createdBy", createdBy)
						.add("updatedBy", createdBy).add("createdAt", now)
						.add("updatedAt", now));

		IndexResponse response = Start.getElasticClient()
				.prepareIndex(index, type).setSource(object.toString()).get();

		return response.getId();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		return SchemaResource.get().deleteSchema(type, context);
	}

	@Get("/:type/:id")
	public Payload get(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getAccountId(), type);

			GetResponse response = Start.getElasticClient()
					.prepareGet(credentials.getAccountId(), type, objectId)
					.get();

			if (!response.isExists())
				return error(HttpStatus.NOT_FOUND,
						"object of type [%s] for id [%s] not found", type,
						objectId);

			JsonObject object = JsonObject.readFrom(response
					.getSourceAsString());
			object.get("meta").asObject().add("id", response.getId())
					.add("type", response.getType())
					.add("version", response.getVersion());

			return new Payload(JSON_CONTENT, object.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	public Payload search(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);
			return doSearch(credentials.getAccountId(), type, jsonBody, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/filter")
	@Post("/:type/filter/")
	public Payload filter(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);

			FilteredSearchBuilder builder = ElasticHelper
					.searchBuilder(credentials.getAccountId(), type)
					.applyContext(context)
					.applyFilters(JsonObject.readFrom(jsonBody));

			return extractResults(builder.get());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/:type/:id")
	public Payload update(String type, String objectId, String jsonBody,
			Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getAccountId(), type);

			JsonObject object = JsonObject.readFrom(jsonBody)
					// removed to forbid developers the update of meta fields
					.remove("meta")
					.add("meta",
							new JsonObject().add("updatedBy",
									credentials.getId()).add("updatedAt",
									DateTime.now().toString()));

			Start.getElasticClient()
					.prepareUpdate(credentials.getAccountId(), type, objectId)
					.setDoc(object.toString()).get();

			return success();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource
					.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getAccountId(), type);

			DeleteResponse response = Start.getElasticClient()
					.prepareDelete(credentials.getAccountId(), type, objectId)
					.get();
			return response.isFound() ? success()
					: error(HttpStatus.NOT_FOUND,
							"object of type [%s] and id [%s] not found", type,
							objectId);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	private Payload doSearch(String index, String type, String json,
			Context context) throws InterruptedException, ExecutionException {

		SearchRequest request = new SearchRequest(index);

		if (!Strings.isNullOrEmpty(type)) {
			// check if type is well defined
			// throws a NotFoundException if not
			SchemaResource.getSchema(index, type);
			request.types(type);
		}

		if (Strings.isNullOrEmpty(json)) {
			SearchSourceBuilder builder = SearchSourceBuilder
					.searchSource()
					.from(context.request().query().getInteger("from", 0))
					.size(context.request().query().getInteger("size", 10))
					.fetchSource(
							context.request().query()
									.getBoolean("fetch-contents", true))
					.query(QueryBuilders.matchAllQuery());

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				builder.query(QueryBuilders.simpleQueryStringQuery(queryText));
			}

			request.extraSource(builder);

		} else {
			request.source(json);
		}

		return extractResults(Start.getElasticClient().search(request).get());
	}
}
