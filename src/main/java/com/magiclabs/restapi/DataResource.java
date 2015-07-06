package com.magiclabs.restapi;

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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

@Prefix("/v1/data")
public class DataResource extends AbstractResource {

	@Get("")
	@Get("/")
	public Payload search(Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			return doSearch(credentials.getAccountId(), null, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload search(String type, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			return doSearch(credentials.getAccountId(), type, null, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload create(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getAccountId(), type);

			IndexResponse response = Start.getElasticClient()
					.prepareIndex(credentials.getAccountId(), type)
					.setSource(jsonBody).get();
			return created("/v1/data", type, response.getId());
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		return new SchemaResource().deleteSchema(type, context);
	}

	@Get("/:type/:id")
	public Payload get(String type, String objectId, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

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

			return new Payload(JSON_CONTENT, response.getSourceAsBytes(),
					HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	public Payload search(String type, String jsonBody, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);
			return doSearch(credentials.getAccountId(), type, jsonBody, context);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/:type/:id")
	public Payload update(String type, String objectId, byte[] bytes,
			Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getAccountId(), type);

			Start.getElasticClient()
					.prepareUpdate(credentials.getAccountId(), type, objectId)
					.setDoc(bytes).get();
			return success();
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String objectId, Context context) {
		try {
			Credentials credentials = AccountResource.checkCredentials(context);

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
			Context context) {

		SearchRequestBuilder builder = Start.getElasticClient().prepareSearch(
				index);

		if (!Strings.isNullOrEmpty(type)) {
			// check if type is well defined
			// throws a NotFoundException if not
			SchemaResource.getSchema(index, type);
			builder.setTypes(type);
		}

		if (Strings.isNullOrEmpty(json)) {
			int from = context.request().query().getInteger("from", 0);
			int size = context.request().query().getInteger("size", 10);
			boolean returnContents = context.request().query()
					.getBoolean("fetch-contents", true);

			builder.setFrom(from).setSize(size).setFetchSource(returnContents);

			QueryBuilder query = QueryBuilders.matchAllQuery();
			String searchString = context.get("s");
			if (!Strings.isNullOrEmpty(searchString)) {
				query = QueryBuilders.simpleQueryStringQuery(searchString);
			}
			builder.setQuery(query);

		} else {
			builder.setSource(json);
		}

		return extractResults(builder.get());
	}
}
