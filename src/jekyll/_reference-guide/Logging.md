---
layout: doc
title: Logging
---

#### Application and backend logging

Use the log API endpoints to monitor your backend. We log:

- all the requests to the API,
- all job runs,
- all queue messages processing.

Example of log JSON object:

```json
{
	"id" : "AZERTY123456789",
	"type" : "api-request",
	"started" : "2016-03-12T12:23:34.675Z",
	"terminated" : "2016-03-12T12:23:35.123Z",
	"took" : 13413,
	"sucess" : true,
	"api-request" : {
		"..." : "..."
	}
}
```

An API request log contains:

```json
{
	"api-request" : {
		"method" : "GET",
		"uri" : "/v1/user/ptidenis",
		"headers" : [],
		"params" : [],
		"status" : 200
	}
}
```

A custom service request log contains:

```json
{
	"service-call" : {
		"id" : "myService",
		"body" : {
			"..." : "..."
		},
	}
}
```

A job run contains:

```json
{
	"job-run" : {
		"jobId" : "mySuperJob",
		"serviceId" : "mySuperService",
		"triggeringPattern" : "monday:22:34",
		"params" : {
			"..." : "..."
		}
	}
}
```

A queue message processing log contains:

```json
{
	"message-processed" : {
		"queueId" : "mySuperQueue",
		"messageId" : "131213251318",
		"message" : {
			"..." : "..."
		}
	}
}
```

In case of error, the log contains:

```json
{
	"..." : "...",
	"sucess" : false,
	"error" : {
		"..." : "..."
	}
}
```


##### /v1/log

`GET` returns the last 100 logs.

- `before` –– Optional timestamp. Returns logs older than this timestamp. 
- `after` –– Optional timestamp. Returns logs younger than this timestamp. 
- `first` = 1 to œ –– The first log to return from the latest to the oldest.
- `size` = 1 to 1000 –– number to logs to return. Default is 100. Maximum is 1000.


`POST` upload client defined logs.

- request body –– An array of log objects. Client can define specific log types. The value of the type field must be used as the key of log specific data part.
- returns a created status object.

