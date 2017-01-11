package io.spacedog.utils;

public interface SpaceParams {

	String SHARDS = "shards";
	String REPLICAS = "replicas";
	String LOG_TYPE = "logType";
	String REFRESH = "refresh";
	String STRICT = "strict";
	String VERSION = "version";
	String MIN_STATUS = "minStatus";
	String ASYNC = "async";
	String LIFETIME = "lifetime";
	String BEFORE = "before";
	String NOTIF = "notif";
	boolean ASYNC_DEFAULT = false;
	int SHARDS_DEFAULT = 1;
	int REPLICAS_DEFAULT = 0;
	String WAIT_FOR_COMPLETION = "waitForCompletion";

}
