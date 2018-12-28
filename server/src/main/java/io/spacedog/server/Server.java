/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.analysis.common.CommonAnalysisPlugin;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.logging.LogConfigurator;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.repositories.s3.S3RepositoryPlugin;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.transport.Netty4Plugin;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.services.CrossOriginFilter;
import io.spacedog.services.admin.AdminResty;
import io.spacedog.services.admin.HealthCheckResty;
import io.spacedog.services.bulk.BulkResty;
import io.spacedog.services.credentials.CredentialsResty;
import io.spacedog.services.credentials.LinkedinResty;
import io.spacedog.services.data.AggregationSerializer;
import io.spacedog.services.data.DataResty;
import io.spacedog.services.data.SchemaResty;
import io.spacedog.services.email.EmailResty;
import io.spacedog.services.file.FileResty;
import io.spacedog.services.file.WebResty;
import io.spacedog.services.job.JobResty;
import io.spacedog.services.log.LogFilter;
import io.spacedog.services.log.LogResty;
import io.spacedog.services.push.ApplicationResty;
import io.spacedog.services.push.PushResty;
import io.spacedog.services.settings.SettingsResty;
import io.spacedog.services.sms.SmsResty;
import io.spacedog.services.snapshot.SnapshotResty;
import io.spacedog.services.stripe.StripeResty;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.DateTimes;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.WebServer;
import net.codestory.http.extensions.Extensions;
import net.codestory.http.misc.Env;
import net.codestory.http.payload.Payload;
import net.codestory.http.routes.Routes;

@SuppressWarnings("serial")
public class Server implements Extensions {

	public static final String CLUSTER_NAME = "spacedog-elastic-cluster";

	private long startTime;
	private ElasticNode elasticNode;
	private ElasticClient elasticClient;
	private FluentServer fluent;
	private Info info;

	public ElasticClient elasticClient() {
		return elasticClient;
	}

	public ElasticNode elasticNode() {
		return elasticNode;
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}

	public void start() {

		try {
			init();
			startElastic();
			elasticIsStarted();
			startFluent();
			fluentIsStarted();
			logStartupDuration();

		} catch (Throwable t) {
			t.printStackTrace();
			if (fluent != null)
				fluent.stop();
			if (elasticClient != null)
				elasticClient.close();
			if (elasticNode != null)
				elasticClient.close();
			System.exit(-1);
		}
	}

	protected void init() {
		startTime = System.currentTimeMillis();
		DateTimeZone.setDefault(DateTimes.PARIS);
		initJsonMapper();
		ServerConfig.log();
		System.setProperty("http.agent", ServerConfig.userAgent());
		String string = ClassResources.loadAsString(Server.class, "info.json");
		info = Json.toPojo(string, Info.class);
	}

	protected void initJsonMapper() {
		SimpleModule module = new SimpleModule()//
				.addSerializer(Aggregation.class, new AggregationSerializer());
		Json.mapper().registerModule(module);
	}

	protected void startElastic()
			throws InterruptedException, ExecutionException, IOException, NodeValidationException {

		Settings settings = Settings.builder()//
				.put(Environment.PATH_HOME_SETTING.getKey(), //
						ServerConfig.homePath().toAbsolutePath().toString())//
				.build();

		// .put(Node.NODE_MASTER_SETTING.getKey(), true)//
		// .put(Node.NODE_DATA_SETTING.getKey(), true)//
		// .put(DiscoveryModule.DISCOVERY_TYPE_SETTING.getKey(), "single-node")//
		// // .put(NetworkModule.TRANSPORT_TYPE_SETTING.getKey(), "netty4") //
		// .put(ClusterName.CLUSTER_NAME_SETTING.getKey(), CLUSTER_NAME)//
		// // disable automatic index creation
		// .put(AutoCreateIndex.AUTO_CREATE_INDEX_SETTING.getKey(), false)//
		// // disable dynamic indexing
		// // .put("index.mapper.dynamic", false)//
		// // .put("index.max_result_window", 5000)//
		// // disable rebalance to avoid automatic rebalance
		// // when a temporary second node appears
		// .put("cluster.routing.rebalance.enable", "none")//
		// .put(NetworkModule.HTTP_ENABLED.getKey(), //
		// config.elasticIsHttpEnabled())//
		// .put(NetworkService.GLOBAL_NETWORK_HOST_SETTING.getKey(), //
		// config.elasticNetworkHost())//
		// .put(Environment.PATH_LOGS_SETTING.getKey(), //
		// config.elasticLogsPath().toAbsolutePath().toString())
		// .put(Environment.PATH_DATA_SETTING.getKey(), //
		// config.elasticDataPath().toAbsolutePath().toString());
		//
		// if (config.elasticSnapshotsPath().isPresent())
		// builder.put(Environment.PATH_REPO_SETTING.getKey(), //
		// config.elasticSnapshotsPath().get().toAbsolutePath().toString());

		Environment environment = InternalSettingsPreparer.prepareEnvironment(//
				settings, Terminal.DEFAULT, Collections.emptyMap(), //
				ServerConfig.elasticConfigPath());

		try {
			LogConfigurator.configure(environment);
		} catch (Exception e) {
			throw Exceptions.runtime(e, "error configuring elastic log");
		}

		elasticNode = new ElasticNode(environment, //
				Netty4Plugin.class, //
				CommonAnalysisPlugin.class, //
				ReindexPlugin.class, //
				S3RepositoryPlugin.class);

		elasticNode.start();
		this.elasticClient = new ElasticClient(elasticNode.client());

		// wait for cluster to fully initialize and turn asynchronously from
		// RED status to GREEN before to initialize anything else
		// wait for 60 seconds maximum by default
		// configurable with spacedog.server.green.check and timeout properties
		elasticClient.ensureAllIndicesAreAtLeastYellow();
	}

	protected void elasticIsStarted() {
		Services.data().init();
		initBackendIndices();
	}

	public void initBackendIndices() {
		Services.credentials().initIndex();
		Services.logs().initIndex();
	}

	public void clear() {
		Services.files().deleteAllBuckets();
		elasticClient().deleteAbsolutelyAllIndices();
		initBackendIndices();
	}

	protected void startFluent() {
		if (ServerConfig.isProduction())
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MODE", "true");

		fluent = new FluentServer();
		fluent.configure(routes -> configure(routes));
		fluent.start(ServerConfig.port());
	}

	protected void fluentIsStarted() {
	}

	protected void configure(Routes routes) {
		routes.add(HealthCheckResty.class)//
				.add(AdminResty.class)//
				.add(SnapshotResty.class)//
				.add(DataResty.class)//
				.add(JobResty.class)//
				.add(SchemaResty.class)//
				.add(CredentialsResty.class)//
				.add(LinkedinResty.class)//
				.add(BulkResty.class)//
				.add(EmailResty.class)//
				.add(SmsResty.class)//
				.add(LogResty.class)//
				.add(PushResty.class)//
				.add(ApplicationResty.class)//
				.add(StripeResty.class)//
				.add(SettingsResty.class);

		routes.filter(SpaceContext.checkBackendFilter())//
				.filter(new CrossOriginFilter())//
				.filter(new LogFilter())//
				.filter(new DebugFilter())//
				.filter(ErrorFilters.global())//
				.filter(SpaceContext.checkAuthorizationFilter())//
				.filter(new WebResty())//
				.filter(ErrorFilters.specific())//
				.filter(new FileResty());

		routes.setExtensions(this);
	}

	/**
	 * Replace fluent http default mapper by Json mapper
	 */
	@Override
	public ObjectMapper configureOrReplaceObjectMapper(ObjectMapper defaultObjectMapper, Env env) {
		return Json.mapper();
	}

	public static class Info {
		public String version;
		public String baseline;
	}

	public Info info() {
		return info;
	}

	public Payload executeRequest(Request request, Response response) throws Exception {
		return fluent.executeRequest(request, response);
	}

	//
	// Implementation
	//

	private void logStartupDuration() {
		System.err.println("Started in " + (System.currentTimeMillis() - startTime) + " ms");
	}

	private static class FluentServer extends WebServer {

		public FluentServer() {
			withThreadCount(12);
			withSelectThreads(1);
			withWebSocketThreads(1);
		}

		protected Payload executeRequest(Request request, Response response) throws Exception {
			return routesProvider.get().apply(request, response);
		}

		@Override
		protected void handleHttp(Request request, Response response) {

			if (threadLocalSpaceContext.get() == null) {
				try {

					threadLocalSpaceContext.set(//
							new SpaceContext(request, response));

					super.handleHttp(request, response);

				} finally {
					doCloseAfterAll();
					threadLocalSpaceContext.set(null);
				}
			} else
				// means space context is managed higher in the stack
				super.handleHttp(request, response);
		}
	}

	//
	// Thread local space context
	//

	private final static ThreadLocal<SpaceContext> threadLocalSpaceContext = new ThreadLocal<>();

	public static SpaceContext context() {
		SpaceContext context = threadLocalSpaceContext.get();
		if (context == null)
			throw Exceptions.runtime("no space context set");
		return context;
	}

	public static SpaceBackend backend() {
		SpaceContext context = threadLocalSpaceContext.get();
		return context == null //
				? ServerConfig.apiBackend()
				: context.backend();
	}

	public static void runWithContext(String backendId, //
			Credentials credentials, Runnable action) {

		SpaceContext oldContext = threadLocalSpaceContext.get();

		try {
			threadLocalSpaceContext.set(//
					new SpaceContext(backendId, credentials));
			action.run();

		} finally {
			threadLocalSpaceContext.set(oldContext);
		}
	}

	//
	// Thread local closeables
	//

	private final static ThreadLocal<List<Closeable>> threadLocalCloseables = new ThreadLocal<>();

	public static void closeAfterAll(Closeable closeable) {
		List<Closeable> list = threadLocalCloseables.get();
		if (list == null) {
			list = Lists.newArrayList();
			threadLocalCloseables.set(list);
		}
		list.add(closeable);
	}

	private static void doCloseAfterAll() {
		List<Closeable> list = threadLocalCloseables.get();
		if (!Utils.isNullOrEmpty(list)) {
			for (Closeable closeable : list)
				Utils.closeSilently(closeable);
			list.clear();
		}
	}

	//
	// Singleton
	//

	private static Server singleton;

	public static Server get() {
		return singleton;
	}

	protected Server() {
		if (singleton != null)
			throw Exceptions.runtime("server is already running");
		singleton = this;
	}

}