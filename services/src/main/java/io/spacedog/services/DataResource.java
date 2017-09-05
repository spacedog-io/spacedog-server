/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.lucene.uid.Versions;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataPermission;
import io.spacedog.services.DataStore.Meta;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
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
		ObjectNode object = Json.readObject(body);
		return post(type, Optional.empty(), object);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return SearchResource.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public Payload getById(String type, String id, Context context) {
		return JsonPayload.ok().with(get(type, id)).build();
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		ObjectNode object = Json.readObject(body);
		boolean patch = !context.query().getBoolean(STRICT_PARAM, false);
		return put(type, id, object, patch, context);
	}

	@Delete("/:type/:id")
	@Delete("/:type/:id/")
	public Payload deleteById(String type, String id, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, type, DataPermission.delete_all)) {
			elastic().delete(DataStore.toDataIndex(type), id, false, true);
			return JsonPayload.ok().build();

		} else if (DataAccessControl.check(credentials, type, DataPermission.delete)) {
			ObjectNode object = DataStore.get().getObject(type, id);

			if (credentials.name().equals(Json.get(object, "meta.createdBy").asText())) {
				elastic().delete(DataStore.toDataIndex(type), id, false, true);
				return JsonPayload.ok().build();
			} else
				throw Exceptions.forbidden(//
						"not the owner of object of type [%s] and id [%s]", type, id);
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	@Get("/:type/:id/:field")
	@Get("/:type/:id/:field/")
	public Payload getField(String type, String id, String fieldPath, Context context) {
		return JsonPayload.ok().with(Json.get(get(type, id), fieldPath)).build();
	}

	@Put("/:type/:id/:field")
	@Put("/:type/:id/:field/")
	public Payload putField(String type, String id, String fieldPath, String body, Context context) {
		ObjectNode object = Json.object();
		Json.with(object, fieldPath, Json.readNode(body));
		return put(type, id, object, true, context);
	}

	@Delete("/:type/:id/:field")
	@Delete("/:type/:id/:field/")
	public Payload deleteField(String type, String id, String fieldPath, Context context) {
		ObjectNode node = get(type, id);
		Json.remove(node, fieldPath);
		return put(type, id, node, false, context);
	}

	//
	// Implementation
	//

	protected ObjectNode get(String type, String id) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.read_all, DataPermission.search))
			return DataStore.get().getObject(type, id);

		if (DataAccessControl.check(credentials, type, DataPermission.read)) {
			ObjectNode object = DataStore.get().getObject(type, id);

			if (credentials.name().equals(Json.get(object, "meta.createdBy").asText())) {
				return object;
			} else
				throw Exceptions.forbidden("not the owner of [%s][%s] object", type, id);
		}
		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	public Payload put(String type, String id, ObjectNode object, boolean patch, Context context) {
		Optional<Meta> metaOpt = DataStore.get().getMeta(type, id);

		if (metaOpt.isPresent()) {
			Meta meta = metaOpt.get();
			Credentials credentials = SpaceContext.credentials();
			checkPutPermissions(meta, credentials);

			// TODO return better exception-message in case of invalid version
			// format
			meta.version = context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY);
			meta.updatedBy = credentials.name();
			meta.updatedAt = DateTime.now();

			if (patch) {
				UpdateResponse response = DataStore.get()//
						.patchObject(type, id, meta.version, object, meta.updatedBy);
				return ElasticPayload.saved("/1/data", response).build();

			} else {
				IndexResponse response = DataStore.get()//
						.updateObject(type, id, object, meta);
				return ElasticPayload.saved("/1/data", response).build();
			}
		} else
			return post(type, Optional.of(id), object);
	}

	protected Payload post(String type, Optional<String> id, ObjectNode object) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.create)) {

			IndexResponse response = DataStore.get().createObject(//
					type, id, object, credentials.name());

			return ElasticPayload.saved("/1/data", response).build();
		}
		throw Exceptions.forbidden("forbidden to create [%s] objects", type);
	}

	public void checkPutPermissions(Meta meta, Credentials credentials) {

		if (DataAccessControl.check(credentials, meta.type, DataPermission.update_all))
			return;

		if (DataAccessControl.check(credentials, meta.type, DataPermission.update)) {

			if (credentials.name().equals(meta.createdBy))
				return;

			throw Exceptions.forbidden("not the owner of [%s][%s] object", meta.type, meta.id);
		}
		throw Exceptions.forbidden("forbidden to update [%s] objects", meta.type);
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
