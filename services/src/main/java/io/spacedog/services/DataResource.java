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

import io.spacedog.core.Json8;
import io.spacedog.model.DataPermission;
import io.spacedog.services.DataStore.Meta;
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
		ObjectNode object = Json8.readObject(body);
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
		return JsonPayload.json(load(type, id));
	}

	protected ObjectNode load(String type, String id) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.read_all, DataPermission.search))
			return DataStore.get().getObject(type, id);

		if (DataAccessControl.check(credentials, type, DataPermission.read)) {
			ObjectNode object = DataStore.get().getObject(type, id);

			if (credentials.name().equals(Json8.get(object, "meta.createdBy").asText())) {
				return object;
			} else
				throw Exceptions.forbidden("not the owner of [%s][%s] object", type, id);
		}
		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		ObjectNode object = Json8.readObject(body);
		return put(type, id, object, context);
	}

	public Payload put(String type, String id, ObjectNode object, Context context) {
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

			if (context.query().getBoolean(STRICT_PARAM, false)) {

				IndexResponse response = DataStore.get()//
						.updateObject(type, id, object, meta);
				return JsonPayload.saved(false, //
						"/1/data", type, id, response.getVersion());

			} else {

				UpdateResponse response = DataStore.get()//
						.patchObject(type, id, meta.version, object, meta.updatedBy);
				return JsonPayload.saved(false, //
						"/1/data", type, id, response.getVersion());
			}
		} else
			return post(type, Optional.of(id), object);
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

	@Get("/:type/:id/:field")
	@Get("/:type/:id/:field/")
	public Payload getField(String type, String id, String field, Context context) {
		return JsonPayload.json(load(type, id).get(field));
	}

	@Put("/:type/:id/:field")
	@Put("/:type/:id/:field/")
	public Payload putField(String type, String id, String field, String body, Context context) {
		return put(type, id, Json8.object(field, Json8.readNode(body)), context);
	}

	@Delete("/:type/:id/:field")
	@Delete("/:type/:id/:field/")
	public Payload deleteField(String type, String id, String field, Context context) {
		return put(type, id, Json8.object(field, null), context);
	}

	//
	// Implementation
	//

	protected Payload post(String type, Optional<String> id, ObjectNode object) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.create)) {

			IndexResponse response = DataStore.get().createObject(//
					type, id, object, credentials.name());

			return JsonPayload.saved(true, "/1/data", response.getType(), //
					response.getId(), response.getVersion());
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
