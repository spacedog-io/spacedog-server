package io.spacedog.utils;

public interface SpaceParams {

	String PARAM_FROM = "from";
	String PARAM_SIZE = "size";
	String PARAM_Q = "q";
	String PARAM_SHARDS = "shards";
	String PARAM_REPLICAS = "replicas";
	String PARAM_LOG_TYPE = "logType";
	String PARAM_REFRESH = "refresh";
	String PARAM_STRICT = "strict";
	String PARAM_VERSION = "version";
	String PARAM_MIN_STATUS = "minStatus";
	String PARAM_ASYNC = "async";
	String PARAM_LIFETIME = "lifetime";
	String PARAM_USERNAME = "username";
	String PARAM_EMAIL = "email";
	String PARAM_BEFORE = "before";
	String PARAM_NOTIF = "notif";
	boolean PARAM_ASYNC_DEFAULT = false;
	int PARAM_SHARDS_DEFAULT = 1;
	int PARAM_REPLICAS_DEFAULT = 0;
	String PARAM_WAIT_FOR_COMPLETION = "waitForCompletion";
	String PARAM_ACCESS_TOKEN = "accessToken";
	String PARAM_DELAY = "delay";
	String PARAM_WITH_CONTENT_DISPOSITION = "withContentDisposition";
}
