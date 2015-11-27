/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

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

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = SearchResource.get()//
					.searchInternal(credentials, null, null, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getAllForType(String type, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ObjectNode result = SearchResource.get()//
					.searchInternal(credentials, type, null, context);
			return new Payload(JSON_CONTENT, result.toString(), HttpStatus.OK);
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

			IndexResponse response = ElasticHelper.get().createObject(credentials.getBackendId(), type,
					Json.readObjectNode(jsonBody), credentials.getName());

			return saved(true, "/v1/data", response.getType(), response.getId(), response.getVersion());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Get("/:type/:id")
	public Payload get(String type, String id, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			Optional<ObjectNode> object = ElasticHelper.get().getObject(credentials.getBackendId(), type, id);

			return object.isPresent() ? new Payload(JSON_CONTENT, object.get().toString(), HttpStatus.OK)
					: error(HttpStatus.NOT_FOUND);

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Put("/:type/:id")
	public Payload update(String type, String id, String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// object should be validated before saved
			SchemaResource.getSchema(credentials.getBackendId(), type);

			ObjectNode object = Json.readObjectNode(body);
			boolean strict = context.query().getBoolean("strict", false);
			// TODO return better exception-message in case of invalid version
			// format
			long version = context.query().getLong("version", 0l);

			if (strict) {

				IndexResponse response = ElasticHelper.get().updateObject(credentials.getBackendId(), type, id, version,
						object, credentials.getName());
				return saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());

			} else {

				UpdateResponse response = ElasticHelper.get().patchObject(credentials.getBackendId(), type, id, version,
						object, credentials.getName());
				return saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());
			}

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteAll(String type, Context context) {
		try {
			Account adminAccount = AdminResource.checkAdminCredentialsOnly(context);

			DeleteByQueryResponse response = ElasticHelper.get().delete(adminAccount.backendId, null, type);

			return toPayload(response.status(), response.getIndex(adminAccount.backendId).getFailures());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("")
	@Delete("/")
	public Payload deleteQuery(String query, Context context) {
		try {
			Account adminAccount = AdminResource.checkAdminCredentialsOnly(context);

			DeleteByQueryResponse response = ElasticHelper.get().delete(adminAccount.backendId, query, new String[0]);

			return toPayload(response.status(), response.getIndex(adminAccount.backendId).getFailures());

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String id, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);

			// check if type is well defined
			// throws a NotFoundException if not
			// TODO useful for security?
			SchemaResource.getSchema(credentials.getBackendId(), type);

			DeleteResponse response = SpaceDogServices.getElasticClient()
					.prepareDelete(credentials.getBackendId(), type, id).get();
			return response.isFound() ? success()
					: error(HttpStatus.NOT_FOUND, "object of type [%s] and id [%s] not found", type, id);
		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	@Post("/search")
	@Post("/search/")
	@Deprecated
	public Payload searchAllTypes(String body, Context context) {
		return SearchResource.get().searchAllTypes(body, context);
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	@Deprecated
	public Payload searchForType(String type, String body, Context context) {
		return SearchResource.get().searchForType(type, body, context);
	}

	@Post("/:type/filter")
	@Post("/:type/filter/")
	@Deprecated
	public Payload filter(String type, String body, Context context) {
		return SearchResource.get().filter(type, body, context);
	}

}
