/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Optional;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.lucene.uid.Versions;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.MetadataBase;
import io.spacedog.model.Permission;
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
public class DataService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		return SearchService.get().getSearchAllTypes(context);
	}

	@Get("/:type")
	@Get("/:type/")
	public Payload getByType(String type, Context context) {
		return SearchService.get().getSearchForType(type, context);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload post(String type, String body, Context context) {
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).source(Json.readObject(body));
		return doPost(object);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return SearchService.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public Payload getById(String type, String id, Context context) {
		return JsonPayload.ok().withObject(doGet(type, id)).build();
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).id(id).source(Json.readObject(body));
		boolean patch = !context.query().getBoolean(STRICT_PARAM, false);
		return doPut(object, patch, context);
	}

	@Delete("/:type/:id")
	@Delete("/:type/:id/")
	public Payload deleteById(String type, String id, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, type, Permission.delete))
			return doDeleteById(type, id);

		if (DataAccessControl.check(credentials, type, Permission.deleteMine)) {
			Optional<DataObject<MetadataBase>> metadata = DataStore.get().getMetadata(type, id);

			if (!metadata.isPresent())
				throw Exceptions.notFound(type, id);

			if (credentials.id().equals(metadata.get().owner()))
				return doDeleteById(type, id);

			throw Exceptions.forbidden("not the owner of object [%s][%s]", type, id);
		}
		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	@Get("/:type/:id/:field")
	@Get("/:type/:id/:field/")
	public Payload getField(String type, String id, String fieldPath, Context context) {
		return JsonPayload.ok().withObject(Json.get(doGet(type, id).source(), fieldPath)).build();
	}

	@Put("/:type/:id/:field")
	@Put("/:type/:id/:field/")
	public Payload putField(String type, String id, String fieldPath, String body, Context context) {
		ObjectNode source = Json.object();
		Json.with(source, fieldPath, Json.readNode(body));
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).id(id).source(source);
		return doPut(object, true, context);
	}

	@Delete("/:type/:id/:field")
	@Delete("/:type/:id/:field/")
	public Payload deleteField(String type, String id, String fieldPath, Context context) {
		DataObject<ObjectNode> object = doGet(type, id);
		Json.remove(object.source(), fieldPath);
		return doPut(object, false, context);
	}

	//
	// Implementation
	//

	protected DataObject<ObjectNode> doGet(String type, String id) {

		Credentials credentials = SpaceContext.credentials();
		if (DataAccessControl.check(credentials, type, Permission.read, Permission.search))
			return DataStore.get().getObject(new JsonDataObject().type(type).id(id));

		if (DataAccessControl.check(credentials, type, Permission.readMine)) {
			DataObject<ObjectNode> object = DataStore.get().getObject(//
					new JsonDataObject().type(type).id(id));

			if (!credentials.id().equals(object.owner()))
				throw Exceptions.forbidden("not owner of [%s][%s]", type, id);

			return object;
		}

		if (DataAccessControl.check(credentials, type, Permission.readMyGroup)) {
			DataObject<ObjectNode> object = DataStore.get().getObject(//
					new JsonDataObject().type(type).id(id));

			if (Strings.isNullOrEmpty(credentials.group()) //
					|| !credentials.group().equals(object.group()))
				throw Exceptions.forbidden("not in the same group than [%s][%s]", type, id);

			return object;
		}

		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	public <K> Payload doPut(DataObject<K> object, boolean patch, Context context) {
		Optional<DataObject<MetadataBase>> metaOpt = DataStore.get()//
				.getMetadata(object.type(), object.id());

		if (metaOpt.isPresent()) {
			DataObject<MetadataBase> metadata = metaOpt.get();
			Credentials credentials = SpaceContext.credentials();
			checkPutPermissions(metadata, credentials);

			// avoid any update on createdAt field
			object.createdAt(metadata.createdAt());

			// TODO return better exception-message in case of invalid format
			object.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY));

			object = patch ? DataStore.get().patchObject(object)//
					: DataStore.get().updateObject(object);

			return ElasticPayload.saved("/1/data", object).build();

		} else
			return doPost(object);
	}

	protected <K> Payload doPost(DataObject<K> object) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.check(credentials, object.type(), Permission.create)) {
			object.owner(credentials.id());
			object.group(credentials.group());
			object = DataStore.get().createObject(object);
			return ElasticPayload.saved("/1/data", object).build();
		}

		throw Exceptions.forbidden("forbidden to create [%s] objects", object.type());
	}

	private Payload doDeleteById(String type, String id) {
		elastic().delete(DataStore.toDataIndex(type), id, false, true);
		return JsonPayload.ok().build();
	}

	public void checkPutPermissions(DataObject<MetadataBase> metadata, Credentials credentials) {

		if (DataAccessControl.check(credentials, metadata.type(), Permission.update))
			return;

		if (DataAccessControl.check(credentials, metadata.type(), Permission.updateMine)) {

			if (credentials.id().equals(metadata.owner()))
				return;

			throw Exceptions.forbidden("not the owner of [%s][%s] object", //
					metadata.type(), metadata.id());
		}
		throw Exceptions.forbidden("forbidden to update [%s] objects", metadata.type());
	}

	//
	// singleton
	//

	private static DataService singleton = new DataService();

	public static DataService get() {
		return singleton;
	}

	private DataService() {
	}

}
