/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.core.Json8;
import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/data")
public class DataResource extends Resource {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		return SearchResource.get().getSearchAllTypes(context);
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getByType(String type, Context context) {
		return SearchResource.get().getSearchForType(type, context);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload post(String type, String body, Context context) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.create)) {

			Check.notNullOrEmpty(body, "JSON body");
			ObjectNode object = Json8.readObject(body);

			/*
			 * 3 cases: (1) id is not provided and generated by ES when object is indexed,
			 * (2) id is a property of the object and the _id schema field contains the
			 * property path, (3) id is provided with the 'id' query parameter
			 */
			Schema schema = DataStore.get().getSchema(type);

			Optional<String> id = Optional.empty();

			if (schema.hasIdPath()) {
				JsonNode idPropertyValue = Json8.get(object, schema.idPath());

				if (idPropertyValue == null)
					throw Exceptions.illegalArgument(//
							"id path [%s] of type [%s] is null or missing", //
							schema.idPath(), type);

				id = Optional.of(idPropertyValue.asText());

			} else if (!Strings.isNullOrEmpty(context.get("id")))
				id = Optional.of(context.get("id"));

			IndexResponse response = DataStore.get().createObject(//
					type, id, object, credentials.name());

			return JsonPayload.saved(true, "/1/data", response.getType(), response.getId(), response.getVersion());
		}
		throw Exceptions.forbidden("forbidden to create [%s] objects", type);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return SearchResource.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public Payload getById(String type, String id, Context context) {
		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.read_all, DataPermission.search)) {
			return JsonPayload.json(DataStore.get()//
					.getObject(type, id));
		}
		if (DataAccessControl.check(credentials, type, DataPermission.read)) {
			ObjectNode object = DataStore.get()//
					.getObject(type, id);

			if (credentials.name().equals(Json8.get(object, "meta.createdBy").asText())) {
				return JsonPayload.json(object);
			} else
				throw Exceptions.forbidden("not the owner of [%s][%s] object", type, id);
		}
		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		Credentials credentials = SpaceContext.credentials();

		checkPutPermissions(type, id, credentials);

		ObjectNode object = Json8.readObject(body);
		Schema schema = DataStore.get().getSchema(type);

		// TODO add a test on this idPath feature
		if (schema.hasIdPath()) {
			JsonNode idValue = Json8.get(object, schema.idPath());
			if (!Json8.isNull(idValue) && !id.equals(idValue.asText()))
				throw Exceptions.illegalArgument(//
						"field [%s][%s][%s] is the object id and can not be updated to [%s]", //
						type, id, schema.idPath(), idValue.asText());
		}

		boolean strict = context.query().getBoolean(PARAM_STRICT, false);
		// TODO return better exception-message in case of invalid version
		// format
		long version = context.query().getLong(PARAM_VERSION, 0l);

		if (strict) {

			IndexResponse response = DataStore.get().updateObject(type, //
					id, version, object, credentials.name());
			return JsonPayload.saved(false, "/1/data", response.getType(), response.getId(), //
					response.getVersion());

		} else {

			UpdateResponse response = DataStore.get().patchObject(type, //
					id, version, object, credentials.name());
			return JsonPayload.saved(false, "/1/data", response.getType(), response.getId(), //
					response.getVersion());
		}
	}

	@Delete("/:type/:id")
	@Delete("/:type/:id/")
	public Payload deleteById(String type, String id, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, type, DataPermission.delete_all)) {
			elastic().delete(DataStore.toDataIndex(type), id, false, true);
			return JsonPayload.success();

		} else if (DataAccessControl.check(credentials, type, DataPermission.delete)) {
			ObjectNode object = DataStore.get().getObject(type, id);

			if (credentials.name().equals(Json8.get(object, "meta.createdBy").asText())) {
				elastic().delete(DataStore.toDataIndex(type), id, false, true);
				return JsonPayload.success();
			} else
				throw Exceptions.forbidden(//
						"not the owner of object of type [%s] and id [%s]", type, id);
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	//
	// Implementation
	//

	public void checkPutPermissions(String type, String id, Credentials credentials) {

		if (DataAccessControl.check(credentials, type, DataPermission.update_all))
			return;

		if (DataAccessControl.check(credentials, type, DataPermission.update)) {

			ObjectNode object = DataStore.get()//
					.getObject(type, id);

			if (credentials.name().equals(Json8.get(object, "meta.createdBy").asText()))
				return;

			throw Exceptions.forbidden("not the owner of [%s][%s] object", type, id);
		}
		throw Exceptions.forbidden("forbidden to update [%s] objects", type);
	}

	//
	// singleton
	//

	private static DataResource singleton = new DataResource();

	public static DataResource get() {
		return singleton;
	}

	private DataResource() {
	}

}
