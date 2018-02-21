/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Optional;
import java.util.function.Supplier;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.lucene.uid.Versions;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Credentials;
import io.spacedog.model.DataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Metadata;
import io.spacedog.model.Permission;
import io.spacedog.model.RolePermissions;
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
		return doPost(object, context);
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return SearchService.get().deleteSearchForType(type, null, context);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public Payload getById(String type, String id, Context context) {
		return JsonPayload.ok().withContent(doGet(type, id)).build();
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
		RolePermissions roles = DataAccessControl.roles(type);

		if (roles.containsOne(credentials, Permission.delete))
			return doDeleteById(type, id);

		if (roles.containsOne(credentials, Permission.deleteMine)) {
			DataObject<Metadata> metadata = getMetadataOrThrow(type, id);
			checkOwner(credentials, metadata);
			return doDeleteById(type, id);
		}

		if (roles.containsOne(credentials, Permission.deleteGroup)) {
			DataObject<Metadata> metadata = getMetadataOrThrow(type, id);
			checkGroup(credentials, metadata);
			return doDeleteById(type, id);
		}

		throw Exceptions.forbidden("forbidden to delete [%s] objects", type);
	}

	@Get("/:type/:id/:path")
	@Get("/:type/:id/:path/")
	public Payload getField(String type, String id, String path, Context context) {
		return JsonPayload.ok().withContent(//
				Json.get(doGet(type, id).source(), path)).build();
	}

	@Put("/:type/:id/:path")
	@Put("/:type/:id/:path/")
	public Payload putField(String type, String id, String path, String body, Context context) {
		ObjectNode source = Json.object();
		Json.with(source, path, Json.readNode(body));
		DataObject<ObjectNode> object = new JsonDataObject()//
				.type(type).id(id).source(source);
		return doPut(object, true, context);
	}

	@Post("/:type/:id/:path")
	@Post("/:type/:id/:path/")
	public Payload postField(String type, String id, String path, String body, Context context) {
		DataObject<ObjectNode> object = doGet(type, id);
		Object[] toAdd = Json.toPojo(body, JsonNode[].class);
		ArrayNode fieldNode = Json.withArray(object.source(), path);
		Json.addToSet(fieldNode, toAdd);
		return doPut(object, false, context);
	}

	@Delete("/:type/:id/:path")
	@Delete("/:type/:id/:path/")
	public Payload deleteField(String type, String id, String path, String body, Context context) {
		DataObject<ObjectNode> object = doGet(type, id);

		if (Strings.isNullOrEmpty(body))
			Json.remove(object.source(), path);
		else {
			Object[] toDelete = Json.toPojo(body, JsonNode[].class);
			ArrayNode fieldNode = Json.withArray(object.source(), path);
			Json.removeFromSet(fieldNode, toDelete);
		}

		return doPut(object, false, context);
	}

	//
	// Implementation
	//

	protected DataObject<ObjectNode> doGet(String type, String id) {
		Credentials credentials = SpaceContext.credentials();
		RolePermissions roles = DataAccessControl.roles(type);
		Supplier<DataObject<ObjectNode>> supplier = () -> DataStore.get().getObject(//
				new JsonDataObject().type(type).id(id));

		if (roles.containsOne(credentials, Permission.read, Permission.search))
			return supplier.get();

		else if (roles.containsOne(credentials, Permission.readMine)) {
			DataObject<ObjectNode> object = supplier.get();
			checkOwner(credentials, object);
			return object;
		}

		else if (roles.containsOne(credentials, Permission.readGroup)) {
			DataObject<ObjectNode> object = supplier.get();
			checkGroup(credentials, object);
			return object;
		}

		throw Exceptions.forbidden("forbidden to read [%s] objects", type);
	}

	public <K> Payload doPut(DataObject<K> object, boolean patch, Context context) {
		Optional<DataObject<Metadata>> metaOpt = DataStore.get()//
				.getMetadata(object.type(), object.id());

		if (metaOpt.isPresent()) {
			DataObject<Metadata> metadata = metaOpt.get();
			Credentials credentials = SpaceContext.credentials();
			checkPutPermissions(credentials, metadata);

			boolean forceMeta = checkForceMeta(object.type(), credentials, context);
			updateMetadata(object, metadata.source(), forceMeta);

			// TODO return better exception-message in case of invalid format
			object.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY));

			object = patch ? DataStore.get().patchObject(object)//
					: DataStore.get().updateObject(object);

			return ElasticPayload.saved("/1/data", object).build();

		} else
			return doPost(object, context);
	}

	protected <K> Payload doPost(DataObject<K> object, Context context) {
		Credentials credentials = SpaceContext.credentials();

		if (DataAccessControl.roles(object.type())//
				.containsOne(credentials, Permission.create)) {

			boolean forceMeta = checkForceMeta(object.type(), credentials, context);
			createMetadata(object, credentials, forceMeta);

			object = DataStore.get().createObject(object);

			return ElasticPayload.saved("/1/data", object).build();
		}

		throw Exceptions.forbidden("forbidden to create [%s] objects", object.type());
	}

	private <K> void createMetadata(DataObject<K> object, //
			Credentials credentials, boolean forceMeta) {

		if (forceMeta == false) {
			object.owner(credentials.id());
			object.group(credentials.group());
		}

		DateTime now = DateTime.now();

		if (forceMeta == false || object.createdAt() == null)
			object.createdAt(now);

		if (forceMeta == false || object.updatedAt() == null)
			object.updatedAt(now);
	}

	private <K> void updateMetadata(DataObject<K> object, //
			Metadata metadata, boolean forceMeta) {

		if (forceMeta == false) {
			object.owner(metadata.owner());
			object.group(metadata.group());
		}

		if (forceMeta == false || object.createdAt() == null)
			object.createdAt(metadata.createdAt());

		if (forceMeta == false || object.updatedAt() == null)
			object.updatedAt(DateTime.now());
	}

	private <K> boolean checkForceMeta(String type, //
			Credentials credentials, Context context) {

		boolean forceMeta = context.query()//
				.getBoolean(FORCE_META_PARAM, false);

		if (forceMeta)
			DataAccessControl.roles(type)//
					.check(credentials, Permission.updateMeta);

		return forceMeta;
	}

	private Payload doDeleteById(String type, String id) {
		elastic().delete(DataStore.toDataIndex(type), id, false, true);
		return JsonPayload.ok().build();
	}

	public void checkPutPermissions(Credentials credentials, DataObject<Metadata> metadata) {

		RolePermissions roles = DataAccessControl.roles(metadata.type());

		if (roles.containsOne(credentials, Permission.update))
			return;

		if (roles.containsOne(credentials, Permission.updateMine)) {
			checkOwner(credentials, metadata);
			return;
		}

		if (roles.containsOne(credentials, Permission.updateGroup)) {
			checkGroup(credentials, metadata);
			return;
		}

		throw Exceptions.forbidden("forbidden to update [%s] objects", metadata.type());
	}

	private void checkGroup(Credentials credentials, DataObject<?> object) {
		if (Strings.isNullOrEmpty(credentials.group()) //
				|| !credentials.group().equals(object.group()))
			throw Exceptions.forbidden("not in the same group than [%s][%s] object", //
					object.type(), object.id());
	}

	private void checkOwner(Credentials credentials, DataObject<?> object) {
		if (!credentials.id().equals(object.owner()))
			throw Exceptions.forbidden("not the owner of [%s][%s] object", //
					object.type(), object.id());
	}

	private DataObject<Metadata> getMetadataOrThrow(String type, String id) {
		return DataStore.get().getMetadata(type, id)//
				.orElseThrow(() -> Exceptions.notFound(type, id));
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