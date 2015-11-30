/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

	//
	// singleton
	//

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
	public Payload getAllTypes(Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);
		refreshIfNecessary(credentials.getBackendId(), context, false);
		ObjectNode result = SearchResource.get()//
				.searchInternal(credentials, null, null, context);
		return PayloadHelper.json(result);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAllTypes(Context context) throws JsonParseException, JsonMappingException, IOException {
		return SearchResource.get().deleteAllTypes(null, context);
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getAllForType(String type, Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);
		refreshIfNecessary(credentials.getBackendId(), context, false);
		ObjectNode result = SearchResource.get()//
				.searchInternal(credentials, type, null, context);
		return PayloadHelper.json(result);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload create(String type, String body, Context context)
			throws NotFoundException, JsonProcessingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);

		// check if type is well defined
		// object should be validated before saved
		SchemaResource.getSchema(credentials.getBackendId(), type);

		IndexResponse response = ElasticHelper.get().createObject(credentials.getBackendId(), type,
				Json.readObjectNode(body), credentials.getName());

		return PayloadHelper.saved(true, "/v1/data", response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteForType(String type, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		return SearchResource.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	public Payload get(String type, String id, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);

		// check if type is well defined
		// throws a NotFoundException if not
		// TODO useful for security?
		SchemaResource.getSchema(credentials.getBackendId(), type);

		Optional<ObjectNode> object = ElasticHelper.get().getObject(credentials.getBackendId(), type, id);

		return object.isPresent() ? PayloadHelper.json(object.get()) : PayloadHelper.error(HttpStatus.NOT_FOUND);
	}

	@Put("/:type/:id")
	public Payload update(String type, String id, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException {
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
			return PayloadHelper.saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());

		} else {

			UpdateResponse response = ElasticHelper.get().patchObject(credentials.getBackendId(), type, id, version,
					object, credentials.getName());
			return PayloadHelper.saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());
		}
	}

	@Delete("/:type/:id")
	public Payload delete(String type, String id, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);

		// check if type is well defined
		// throws a NotFoundException if not
		// TODO useful for security?
		SchemaResource.getSchema(credentials.getBackendId(), type);

		DeleteResponse response = Start.getElasticClient().prepareDelete(credentials.getBackendId(), type, id).get();
		return response.isFound() ? PayloadHelper.success()
				: PayloadHelper.error(HttpStatus.NOT_FOUND, "object of type [%s] and id [%s] not found", type, id);
	}

	@Post("/search")
	@Post("/search/")
	@Deprecated
	public Payload searchAllTypes(String body, Context context) throws JsonParseException, JsonMappingException,
			NotFoundException, IOException, InterruptedException, ExecutionException {
		return SearchResource.get().postSearchAllTypes(body, context);
	}

	@Post("/:type/search")
	@Post("/:type/search/")
	@Deprecated
	public Payload searchForType(String type, String body, Context context) throws JsonParseException,
			JsonMappingException, NotFoundException, IOException, InterruptedException, ExecutionException {
		return SearchResource.get().postSearchForType(type, body, context);
	}

	@Post("/:type/filter")
	@Post("/:type/filter/")
	@Deprecated
	public Payload filter(String type, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException, InterruptedException, ExecutionException {
		return SearchResource.get().postFilterForType(type, body, context);
	}

}
