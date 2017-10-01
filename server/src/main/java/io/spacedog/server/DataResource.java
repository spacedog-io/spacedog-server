/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Optional;

import org.elasticsearch.common.lucene.uid.Versions;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataObject;
import io.spacedog.model.DataPermission;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.MetaWrapper;
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
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).source(Json.readObject(body));
		return post(object);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return SearchResource.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public Payload getById(String type, String id, Context context) {
		return JsonPayload.ok().withObject(get(type, id)).build();
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).id(id).source(Json.readObject(body));
		boolean patch = !context.query().getBoolean(STRICT_PARAM, false);
		return put(object, patch, context);
	}

	@Delete("/:type/:id")
	@Delete("/:type/:id/")
	public Payload deleteById(String type, String id, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, type, DataPermission.delete_all)) {
			elastic().delete(DataStore.toDataIndex(type), id, false, true);
			return JsonPayload.ok().build();

		} else if (DataAccessControl.check(credentials, type, DataPermission.delete)) {
			DataObject<ObjectNode> object = DataStore.get().getObject(//
					new JsonDataObject().type(type).id(id));

			if (credentials.name().equals(Json.get(object.source(), "meta.createdBy").asText())) {
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
		return JsonPayload.ok().withObject(Json.get(get(type, id).source(), fieldPath)).build();
	}

	@Put("/:type/:id/:field")
	@Put("/:type/:id/:field/")
	public Payload putField(String type, String id, String fieldPath, String body, Context context) {
		ObjectNode source = Json.object();
		Json.with(source, fieldPath, Json.readNode(body));
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).id(id).source(source);
		return put(object, true, context);
	}

	@Delete("/:type/:id/:field")
	@Delete("/:type/:id/:field/")
	public Payload deleteField(String type, String id, String fieldPath, Context context) {
		DataObject<ObjectNode> object = get(type, id);
		Json.remove(object.source(), fieldPath);
		return put(object, false, context);
	}

	//
	// Implementation
	//

	protected DataObject<ObjectNode> get(String type, String id) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, DataPermission.read_all, DataPermission.search))
			return DataStore.get().getObject(new JsonDataObject().type(type).id(id));

		if (DataAccessControl.check(credentials, type, DataPermission.read)) {
			DataObject<ObjectNode> object = DataStore.get().getObject(//
					new JsonDataObject().type(type).id(id));

			if (credentials.name().equals(Json.get(object.source(), "meta.createdBy").asText())) {
				return object;
			} else
				throw Exceptions.forbidden("not the owner of [%s][%s] object", type, id);
		}
		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	public <K> Payload put(DataObject<K> object, boolean patch, Context context) {
		Optional<DataObject<MetaWrapper>> metaOpt = DataStore.get()//
				.getMeta(object.type(), object.id());

		if (metaOpt.isPresent()) {
			DataObject<MetaWrapper> metadata = metaOpt.get();
			Credentials credentials = SpaceContext.credentials();
			checkPutPermissions(metadata, credentials);

			// reset object meta with meta from database
			object.meta(metadata.meta());

			// TODO return better exception-message in case of invalid version
			// format
			object.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY));

			object = patch //
					? DataStore.get().patchObject(object, credentials.name())//
					: DataStore.get().updateObject(object, credentials.name());

			return ElasticPayload.saved("/1/data", object).build();

		} else
			return post(object);
	}

	protected <K> Payload post(DataObject<K> object) {

		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, object.type(), DataPermission.create)) {

			object = DataStore.get().createObject(object, credentials.name());
			return ElasticPayload.saved("/1/data", object).build();
		}

		throw Exceptions.forbidden("forbidden to create [%s] objects", object.type());
	}

	public void checkPutPermissions(DataObject<MetaWrapper> metadata, Credentials credentials) {

		if (DataAccessControl.check(credentials, metadata.type(), DataPermission.update_all))
			return;

		if (DataAccessControl.check(credentials, metadata.type(), DataPermission.update)) {

			if (credentials.name().equals(metadata.source().meta().createdBy()))
				return;

			throw Exceptions.forbidden("not the owner of [%s][%s] object", //
					metadata.type(), metadata.id());
		}
		throw Exceptions.forbidden("forbidden to update [%s] objects", metadata.type());
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
