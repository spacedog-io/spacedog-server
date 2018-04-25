---
layout: doc
title: Logging
rank: 6
---

#### /1/log

Use the log endpoint to monitor your backend. SpaceDog logs all service requests sent to your backend.

##### {id}.spacedog.io/1/log

*GET* returns the last requests logged. Only authorized to administrators.

- `from` –– Defaults to 0. The index of the first log to return from the latest to the oldest.
- `size` –– Defaults to 10. Number to logs to return. Maximum is 1000.

Response body example:

```json
{
  "took" : 78,
  "total" : 752,
  "results" : [
    {
      "method" : "GET",
      "path" : "/1/data/message",
      "receivedAt" : "2016-05-26T22:19:38.722+02:00",
      "processedIn" : 28,
      "credentials" : {
        "backendId" : "test",
        "name" : "vince",
        "type" : "USER"
      },
      "status" : 200,
      "response" : {
        "took" : 11,
        "total" : 0,
        "results" : [ ]
      }
    },
    ...
  ]
}
```

##### {id}.spacedog.io/1/log/search

*POST* search for logged requests. Only authorized to administrators.

- `from` –– Defaults to 0. The index of the first log to return from the latest to the oldest.
- `size` –– Defaults to 10. Number to logs to return. Maximum is 1000.

The request body is the inner part of an Elasticsearch type query. Example:

```json
{
  "term" : {
      "path" : "/1/data/message"
  }
}
```

The query is automatically sorted from the latest to the oldest logs.
