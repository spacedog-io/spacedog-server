/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;

import com.eclipsesource.json.JsonObject;

import io.spacedog.services.ElasticHelper.FilteredSearchBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

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
			Credentials credentials = AdminResource.checkCredentials(context);
			return doSearch(credentials.getBackendId(), null, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload search(String type, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			return doSearch(credentials.getBackendId(), type, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload create(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			IndexResponse response = createInternal(credentials.getBackendId(), type, JsonObject.readFrom(jsonBody),
					credentials.getName());

			return saved(true, "/v1/data", response.getType(), response.getId(), response.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	IndexResponse createInternal(String index, String type, JsonObject object, String createdBy) {

		String now = DateTime.now().toString();

		// remove then add meta to avoid developers to
		// set any meta fields directly
		object.remove("meta").add("meta", new JsonObject().add("createdBy", createdBy).add("updatedBy", createdBy)
				.add("createdAt", now).add("updatedAt", now));

		return Start.getElasticClient().prepareIndex(index, type).setSource(object.toString()).get();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		/*
		 * TODO (1) this is not exactly right I might want to delete all objects
		 * but keep the schema for future use. (2) Admin credentials?
		 */
		return SchemaResource.get().deleteSchema(type, context);
	}

	@Get("/:type/:id")
	public Payload get(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			GetResponse response = Start.getElasticClient().prepareGet(credentials.getBackendId(), type, objectId)
					.get();

			if (!response.isExists())
				return error(HttpStatus.NOT_FOUND, "object of type [%s] for id [%s] not found", type, objectId);

			JsonObject object = JsonObject.readFrom(response.getSourceAsString());
			object.get("meta").asObject().add("id", response.getId()).add("type", response.getType()).add("version",
					response.getVersion());

			return new Payload(JSON_CONTENT, object.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/search")
	@Post("/search/")
	public Payload searchAllTypes(String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			return doSearch(credentials.getBackendId(), null, jsonBody, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	public Payload searchThisType(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			return doSearch(credentials.getBackendId(), type, jsonBody, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/filter")
	@Post("/:type/filter/")
	public Payload filter(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			FilteredSearchBuilder builder = ElasticHelper.searchBuilder(credentials.getBackendId(), type)
					.applyContext(context).applyFilters(JsonObject.readFrom(jsonBody));

			return extractResults(builder.get());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/:type/:id")
	public Payload update(String type, String objectId, String jsonBody, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			JsonObject object = JsonObject.readFrom(jsonBody)
					// removed to forbid developers the update of meta fields
					.remove("meta").add("meta", new JsonObject().add("updatedBy", credentials.getName())
							.add("updatedAt", DateTime.now().toString()));

			UpdateRequestBuilder update = Start.getElasticClient()
					.prepareUpdate(credentials.getBackendId(), type, objectId).setDoc(object.toString());

			String onVersion = context.get("version");

			if (!Strings.isNullOrEmpty(onVersion))
				try {
					update.setVersion(Long.parseLong(onVersion));
				} catch (NumberFormatException exc) {
					return invalidParameters("version", onVersion, "provided version is not a valid long");
				}

			UpdateResponse response = update.get();

			return saved(response.isCreated(), "/v1/data", response.getType(), response.getId(), response.getVersion());

		} catch (VersionConflictEngineException exc) {
			return error(HttpStatus.CONFLICT, exc);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String objectId, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			DeleteResponse response = Start.getElasticClient().prepareDelete(credentials.getBackendId(), type, objectId)
					.get();
			return response.isFound() ? success()
					: error(HttpStatus.NOT_FOUND, "object of type [%s] and id [%s] not found", type, objectId);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	private Payload doSearch(String index, String type, String json, Context context)
			throws InterruptedException, ExecutionException {

		SearchRequest request = new SearchRequest(index);

		if (!Strings.isNullOrEmpty(type)) {
			// check if type is well defined
			// throws a NotFoundException if not
			SchemaResource.getSchema(index, type);
			request.types(type);
		}

		SearchSourceBuilder builder = SearchSourceBuilder.searchSource()
				.from(context.request().query().getInteger("from", 0))
				.size(context.request().query().getInteger("size", 10)).version(true)
				.fetchSource(context.request().query().getBoolean("fetch-contents", true));

		if (Strings.isNullOrEmpty(json)) {
			builder.query(QueryBuilders.matchAllQuery());

			String queryText = context.get("q");
			if (!Strings.isNullOrEmpty(queryText)) {
				builder.query(QueryBuilders.simpleQueryStringQuery(queryText));
			}

		} else {
			request.source(json);
		}

		request.extraSource(builder);

		return extractResults(Start.getElasticClient().search(request).get());
	}
}
