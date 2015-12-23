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

import io.spacedog.utils.Json;
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
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, false);
		ElasticHelper.get().refresh(refresh, credentials.backendId());
		ObjectNode result = SearchResource.get()//
				.searchInternal(credentials, null, null, context);
		return PayloadHelper.json(result);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll(Context context) throws JsonParseException, JsonMappingException, IOException {
		return SearchResource.get().deleteAllTypes(null, context);
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getByType(String type, Context context)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, false);
		ElasticHelper.get().refresh(refresh, credentials.backendId());
		ObjectNode result = SearchResource.get()//
				.searchInternal(credentials, type, null, context);
		return PayloadHelper.json(result);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload post(String type, String body, Context context)
			throws NotFoundException, JsonProcessingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();

		// check if type is well defined
		// object should be validated before saved
		ElasticHelper.get().getSchema(credentials.backendId(), type);

		IndexResponse response = ElasticHelper.get().createObject(credentials.backendId(), type,
				Json.readObjectNode(body), credentials.name());

		return PayloadHelper.saved(true, "/v1/data", response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		return SearchResource.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	public Payload getById(String type, String id, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();

		// check if type is well defined
		// throws a NotFoundException if not
		// TODO useful for security?
		ElasticHelper.get().getSchema(credentials.backendId(), type);

		Optional<ObjectNode> object = ElasticHelper.get().getObject(credentials.backendId(), type, id);

		if (object.isPresent()) {

			// TODO remove this when hashed passwords have moved to dedicated
			// indices
			object.get().remove(UserResource.HASHED_PASSWORD);

			return PayloadHelper.json(object.get());
		}

		return PayloadHelper.error(HttpStatus.NOT_FOUND);
	}

	@Put("/:type/:id")
	public Payload put(String type, String id, String body, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();

		// check if type is well defined
		// object should be validated before saved
		ElasticHelper.get().getSchema(credentials.backendId(), type);

		ObjectNode object = Json.readObjectNode(body);
		boolean strict = context.query().getBoolean("strict", false);
		// TODO return better exception-message in case of invalid version
		// format
		long version = context.query().getLong("version", 0l);

		if (strict) {

			IndexResponse response = ElasticHelper.get().updateObject(credentials.backendId(), type, id, version,
					object, credentials.name());
			return PayloadHelper.saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());

		} else {

			UpdateResponse response = ElasticHelper.get().patchObject(credentials.backendId(), type, id, version,
					object, credentials.name());
			return PayloadHelper.saved(false, "/v1/data", response.getType(), response.getId(), response.getVersion());
		}
	}

	@Delete("/:type/:id")
	public Payload deleteById(String type, String id, Context context)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = SpaceContext.checkCredentials();

		// check if type is well defined
		// throws a NotFoundException if not
		// TODO useful for security?
		ElasticHelper.get().getSchema(credentials.backendId(), type);

		DeleteResponse response = Start.get().getElasticClient().prepareDelete(credentials.backendId(), type, id).get();

		if (response.isFound())
			return PayloadHelper.success();

		return PayloadHelper.error(HttpStatus.NOT_FOUND, "object of type [%s] and id [%s] not found", type, id);
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

	//
	// singleton
	//

	private static DataResource singleton = new DataResource();

	static DataResource get() {
		return singleton;
	}

	private DataResource() {
	}

}
