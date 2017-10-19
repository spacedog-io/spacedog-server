package io.spacedog.services.toolee;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.spacedog.core.Json8;
import io.spacedog.services.ElasticClient;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Resource;
import io.spacedog.services.SpaceContext;
import io.spacedog.services.Start;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class TooleeResource extends Resource {

	private static final String COMPANY_TYPE = "company";
	private static final String NOTIFICATION_TYPE = "notification";
	private static final String DOCUMENT_TYPE = "document";
	private static final String ANALYZED_STATUS = "analyzed";
	private static final String CLASSIFIED_STATUS = "classified";
	private static final String CONTROLLER_ROLE = "controller";
	private static final String OPERATOR_ROLE = "operator";
	private static final String ACCOUNTANT_ROLE = "accountant";
	private static final String ACCOUNTANT_CREDENTIALS_IDS = "accountantCredentialsIds";
	private static final String CONTROLLER_CREDENTIALS_IDS = "controllerCredentialsIds";
	private static final String STATUS = "status";
	private static final String COMPANY_ID = "companyId";
	private static final String TAGS = "tags";

	//
	// Routes
	//

	@Get("/1/service/companies/_my_toolee_customers")
	@Get("/1/service/companies/_my_toolee_customers/")
	public Payload getMyTooleeCustomers(Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		credentials.checkRoles(OPERATOR_ROLE, ACCOUNTANT_ROLE);

		Company[] companies = findMyTooleeCutomers(credentials);

		String[] companyIds = Arrays.stream(companies)//
				.map(company -> company.id).toArray(String[]::new);

		computeDocuments(credentials, companies, companyIds);
		computeNotifications(credentials, companies, companyIds);

		ObjectNode object = Json7.object("total", companies.length, //
				"results", Json8.toNode(companies));

		return JsonPayload.json(object);
	}

	@Get("/1/service/companies/:id/_tags")
	@Get("/1/service/companies/:id/_tags/")
	public Payload getCompanyTags(String id, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		ElasticClient elastic = Start.get().getElasticClient();
		String alias = elastic.toAlias(credentials.backendId(), DOCUMENT_TYPE);

		int size = context.query().getInteger(PARAM_SIZE, 10);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(COMPANY_ID, id))//
				.must(QueryBuilders.existsQuery(TAGS));

		String prefix = context.get("q");
		if (!Strings.isNullOrEmpty(prefix))
			query.must(QueryBuilders.prefixQuery(TAGS, prefix));

		SearchHits searchHits = elastic.prepareSearch()//
				.setIndices(alias).setTypes(DOCUMENT_TYPE)//
				.setQuery(query)//
				.setFrom(0)//
				.setSize(size)//
				.setVersion(false)//
				.addFields(TAGS)//
				.setFetchSource(false)//
				.get()//
				.getHits();

		Set<String> tags = Sets.newHashSet();
		for (SearchHit hit : searchHits) {
			List<Object> docTags = hit.field(TAGS).getValues();
			for (Object object : docTags) {
				String tag = object.toString();
				if (Strings.isNullOrEmpty(prefix) || tag.startsWith(prefix))
					tags.add(tag);
			}
		}

		return JsonPayload.json(Json7.toNode(tags));
	}

	//
	// Implementation
	//

	private static String[] statuses = { ANALYZED_STATUS, CLASSIFIED_STATUS };

	private void computeDocuments(Credentials credentials, //
			Company[] companies, String[] companyIds) {

		ElasticClient elastic = Start.get().getElasticClient();
		String alias = elastic.toAlias(credentials.backendId(), DOCUMENT_TYPE);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termsQuery(COMPANY_ID, companyIds));

		TermsBuilder aggBuilder = AggregationBuilders.terms("documents")//
				.field(COMPANY_ID).include(companyIds)//
				.subAggregation(AggregationBuilders.terms("waiting")//
						.field(STATUS).include(statuses));

		Terms agg = (Terms) elastic.prepareSearch()//
				.setIndices(alias).setTypes(DOCUMENT_TYPE)//
				.setQuery(query)//
				.addAggregation(aggBuilder)//
				.setSize(0)//
				.get()//
				.getAggregations()//
				.get("documents");

		for (Company company : companies) {
			Bucket bucket = agg.getBucketByKey(company.id);
			if (bucket != null) {
				Terms agg2 = (Terms) bucket.getAggregations().get("waiting");
				bucket = agg2.getBucketByKey(CLASSIFIED_STATUS);
				company.docsToAnalyze = bucket == null ? 0 : bucket.getDocCount();
				bucket = agg2.getBucketByKey(ANALYZED_STATUS);
				company.docsToProcess = bucket == null ? 0 : bucket.getDocCount();
			}
		}

	}

	private void computeNotifications(Credentials credentials, //
			Company[] companies, String[] companyIds) {

		ElasticClient elastic = Start.get().getElasticClient();
		String alias = elastic.toAlias(credentials.backendId(), NOTIFICATION_TYPE);
		String mainRole = getMainRole(credentials);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termsQuery(COMPANY_ID, companyIds));

		BoolQueryBuilder subAggQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery(mainRole + ".show", true))//
				.mustNot(QueryBuilders.existsQuery(mainRole + ".readAt"));

		TermsBuilder aggBuilder = AggregationBuilders.terms("notifications")//
				.field(COMPANY_ID).include(companyIds)//
				.subAggregation(AggregationBuilders.filter("notread")//
						.filter(subAggQuery));

		Terms agg = (Terms) elastic.prepareSearch()//
				.setIndices(alias).setTypes(NOTIFICATION_TYPE)//
				.setQuery(query)//
				.addAggregation(aggBuilder)//
				.setSize(0)//
				.get()//
				.getAggregations()//
				.get("notifications");

		for (Company company : companies) {
			Bucket bucket = agg.getBucketByKey(company.id);
			if (bucket != null) {
				Filter agg2 = (Filter) bucket.getAggregations().get("notread");
				company.notifications = agg2 == null ? 0 : agg2.getDocCount();
			}
		}
	}

	private Company[] findMyTooleeCutomers(Credentials credentials) {
		ElasticClient elastic = Start.get().getElasticClient();
		String alias = elastic.toAlias(credentials.backendId(), COMPANY_TYPE);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.must(QueryBuilders.existsQuery(CONTROLLER_CREDENTIALS_IDS));

		if (credentials.roles().contains(ACCOUNTANT_ROLE))
			query.must(QueryBuilders.termQuery(ACCOUNTANT_CREDENTIALS_IDS, credentials.id()));

		SearchHits searchHits = elastic.prepareSearch()//
				.setIndices(alias).setTypes(COMPANY_TYPE)//
				.setQuery(query)//
				.addSort("name", SortOrder.ASC)//
				.setFrom(0)//
				.setSize(5000)//
				.setVersion(false)//
				.get()//
				.getHits();

		if (searchHits.totalHits() > 5000)
			throw Exceptions.space(HttpStatus.NOT_IMPLEMENTED, //
					"Toolee manages no more than 5000 companies");

		SearchHit[] hits = searchHits.getHits();
		Company[] companies = new Company[hits.length];

		for (int i = 0; i < hits.length; i++) {
			companies[i] = Json7.toPojo(hits[i].sourceAsString(), Company.class);
			companies[i].id = hits[i].getId();
		}

		return companies;
	}

	private String getMainRole(Credentials credentials) {
		Set<String> roles = credentials.roles();
		if (roles.contains(CONTROLLER_ROLE))
			return CONTROLLER_ROLE;
		if (roles.contains(OPERATOR_ROLE))
			return OPERATOR_ROLE;
		if (roles.contains(ACCOUNTANT_ROLE))
			return ACCOUNTANT_ROLE;
		throw Exceptions.illegalState("user [%s][%s] not a controller, operator or accountant", //
				credentials.level(), credentials.name());
	}

	//
	// singleton
	//

	private static TooleeResource singleton = new TooleeResource();

	public static TooleeResource get() {
		return singleton;
	}

	private TooleeResource() {
	}
}
