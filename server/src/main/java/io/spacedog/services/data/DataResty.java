/**
 * © David Attias 2015
 */
package io.spacedog.services.data;

import java.io.IOException;
import java.util.Locale;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.data.CsvRequest;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.server.Server;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import io.spacedog.services.elastic.ElasticUtils;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.Request;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

@Prefix("/2/data")
public class DataResty extends SpaceResty {

	@Get("")
	@Get("/")
	public DataResults<ObjectNode> getAll(Context context) {
		Credentials credentials = Server.context().credentials();
		String[] types = DataAccessControl.types(credentials, Permission.search);
		return doGet(context, types);
	}

	@Post("/_search")
	@Post("/_search/")
	public DataResults<ObjectNode> postSearchAll(String body, Context context) {
		Credentials credentials = Server.context().credentials();
		String[] types = DataAccessControl.types(credentials, Permission.search);
		return doSearch(body, context, types);
	}

	@Delete("/_search")
	@Delete("/_search/")
	public Payload deleteSearchAll(String query, Context context) {
		Credentials credentials = Server.context().credentials().checkAtLeastAdmin();
		String[] types = DataAccessControl.types(credentials, Permission.delete);

		if (Utils.isNullOrEmpty(types))
			return JsonPayload.ok().build();

		return doDelete(query, context, types);
	}

	@Get("/:type")
	@Get("/:type/")
	public DataResults<ObjectNode> getType(String type, Context context) {
		DataAccessControl.checkPermission(type, Permission.search);
		return doGet(context, type);
	}

	@Post("/:type")
	@Post("/:type/")
	public Payload postType(String type, String body, Context context) {
		DataWrap<ObjectNode> object = DataWrap.wrap(Json.readObject(body)).type(type);
		boolean forceMeta = context.query().getBoolean(FORCE_META_PARAM, false);
		object = Services.data().saveIfAuthorized(object, forceMeta);
		return JsonPayload.saved(object).build();
	}

	@Delete("/:type")
	@Delete("/:type/")
	public Payload deleteByType(String type, Context context) {
		return deleteSearchType(type, null, context);
	}

	@Post("/:type/_search")
	@Post("/:type/_search/")
	public DataResults<ObjectNode> postSearchType(String type, String body, Context context) {
		DataAccessControl.checkPermission(type, Permission.search);
		return doSearch(body, context, type);
	}

	@Delete("/:type/_search")
	@Delete("/:type/_search/")
	public Payload deleteSearchType(String type, String query, Context context) {
		Server.context().credentials().checkAtLeastAdmin();
		DataAccessControl.checkPermission(type, Permission.delete);
		return doDelete(query, context, type);
	}

	@Get("/:type/:id")
	@Get("/:type/:id/")
	public DataWrap<ObjectNode> getById(String type, String id, Context context) {
		return Services.data().getIfAuthorized(type, id);
	}

	@Put("/:type/:id")
	@Put("/:type/:id/")
	public Payload put(String type, String id, String body, Context context) {
		DataWrap<ObjectNode> object = DataWrap.wrap(Json.readObject(body))//
				.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY))//
				.type(type).id(id);

		boolean forceMeta = context.query().getBoolean(FORCE_META_PARAM, false);
		boolean patch = context.query().getBoolean(PATCH_PARAM, false);

		object = patch //
				? Services.data().patchIfAuthorized(object) //
				: Services.data().saveIfAuthorized(object, forceMeta);

		return JsonPayload.saved(object).build();
	}

	@Delete("/:type/:id")
	@Delete("/:type/:id/")
	public Payload deleteById(String type, String id, Context context) {
		boolean deleted = Services.data().deleteIfAuthorized(type, id);
		return JsonPayload.ok().withFields("deleted", deleted).build();
	}

	@Get("/:type/:id/:field")
	@Get("/:type/:id/:field/")
	public JsonNode getField(String type, String id, String field, Context context) {
		DataWrap<ObjectNode> wrap = Services.data().getIfAuthorized(type, id);
		JsonNode value = Json.get(wrap.source(), field);
		if (value == null)
			value = NullNode.getInstance();
		return value;
	}

	@Put("/:type/:id/:field")
	@Put("/:type/:id/:field/")
	public Payload putField(String type, String id, String field, String body, Context context) {
		ObjectNode source = Json.object();
		Json.with(source, field, Json.readNode(body));
		DataWrap<ObjectNode> object = DataWrap.wrap(source)//
				.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY))//
				.type(type).id(id);
		object = Services.data().patchIfAuthorized(object);
		return JsonPayload.saved(object).build();
	}

	@Delete("/:type/:id/:field")
	@Delete("/:type/:id/:field/")
	public Payload deleteField(String type, String id, String field, String body, Context context) {
		DataWrap<ObjectNode> object = Services.data().getWrapped(type, id);
		Json.remove(object.source(), field);
		object = Services.data().saveIfAuthorized(object, false);
		return JsonPayload.saved(object).build();
	}

	@Post("/:type/_export")
	@Post("/:type/_export/")
	public Payload postExport(String type, String body, Context context) {
		DataAccessControl.checkPermission(type, Permission.search);

		if (context.query().getBoolean(REFRESH_PARAM, false))
			Services.data().refresh(type);

		QueryBuilder query = Strings.isNullOrEmpty(body) //
				? QueryBuilders.matchAllQuery()
				: ElasticUtils.toQueryBuilder(body);

		StreamingOutput output = Services.data().exportNow(type, query);
		return new Payload(ContentTypes.TEXT_PLAIN_UTF8, output);
	}

	@Post("/:type/_import")
	@Post("/:type/_import/")
	public void postImport(String type, Request request) throws IOException {
		DataAccessControl.checkPermission(type, Permission.importAll);
		boolean preserveIds = request.query().getBoolean(PRESERVE_IDS_PARAM, false);

		Services.data().prepareImport(type)//
				.withPreserveIds(preserveIds)//
				.go(request.inputStream());
	}

	@Post("/:type/_csv")
	@Post("/:type/_csv/")
	public Payload postSearchForTypeToCsv(String type, CsvRequest request, Context context) {
		DataAccessControl.checkPermission(type, Permission.search);
		Locale locale = getRequestLocale(context);
		StreamingOutput csv = Services.data().csv(type, request, locale);
		return new Payload("text/plain;charset=utf-8;", csv);
	}

	//
	// Implementation
	//

	private DataResults<ObjectNode> doGet(Context context, String... types) {

		DataResults<ObjectNode> results = DataResults.of(ObjectNode.class);
		if (!Utils.isNullOrEmpty(types)) {

			refreshIfRequested(context, types);

			String q = context.get(Q_PARAM);
			int from = context.query().getInteger(FROM_PARAM, 0);
			int size = context.query().getInteger(SIZE_PARAM, 10);
			boolean fetchSource = context.query().getBoolean("fetch-contents", true);

			QueryBuilder query = Strings.isNullOrEmpty(q) //
					? QueryBuilders.matchAllQuery() //
					: QueryBuilders.simpleQueryStringQuery(q);

			SearchSourceBuilder search = SearchSourceBuilder.searchSource()//
					.query(query).from(from).size(size).fetchSource(fetchSource);

			results = Services.data().search(ObjectNode.class, search, types);
		}
		return results;
	}

	private DataResults<ObjectNode> doSearch(String body, Context context, String... types) {

		DataResults<ObjectNode> results = DataResults.of(ObjectNode.class);
		if (!Utils.isNullOrEmpty(types)) {
			refreshIfRequested(context, types);
			SearchSourceBuilder builder = ElasticUtils.toSearchSourceBuilder(body).version(true);
			results = Services.data().search(ObjectNode.class, builder, types);
		}
		return results;
	}

	private Payload doDelete(String query, Context context, String... types) {

		long deleted = 0;
		if (!Utils.isNullOrEmpty(types)) {
			refreshIfRequested(context, types);

			QueryBuilder builder = Strings.isNullOrEmpty(query) //
					? QueryBuilders.matchAllQuery()
					: QueryBuilders.wrapperQuery(query);

			deleted = Services.data().deleteAll(builder, types);
		}
		return JsonPayload.ok().withFields("deleted", deleted).build();
	}

	private void refreshIfRequested(Context context, String... types) {
		Services.data().refresh(isRefreshRequested(context), types);
	}
}
