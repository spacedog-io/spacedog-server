package io.spacedog.client.http;

public interface SpaceParams {

	String FROM_PARAM = "from";
	String SIZE_PARAM = "size";
	String Q_PARAM = "q";
	String SHARDS_PARAM = "shards";
	String REPLICAS_PARAM = "replicas";
	String REFRESH_PARAM = "refresh";
	String STRICT_PARAM = "strict";
	String VERSION_PARAM = "version";
	String ASYNC_PARAM = "async";
	String LIFETIME_PARAM = "lifetime";
	String USERNAME_PARAM = "username";
	String EMAIL_PARAM = "email";
	String BEFORE_PARAM = "before";
	String NOTIF_PARAM = "notif";
	boolean ASYNC_DEFAULT_PARAM = false;
	int SHARDS_DEFAULT_PARAM = 1;
	int REPLICAS_DEFAULT_PARAM = 0;
	String WAIT_FOR_COMPLETION_PARAM = "waitForCompletion";
	String WITH_CONTENT_DISPOSITION = "withContentDisposition";
	String ACCESS_TOKEN_PARAM = "accessToken";
	String FORCE_META_PARAM = "forceMeta";

}