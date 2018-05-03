# Log

Use the log endpoint to monitor your backend. SpaceDog logs all service requests sent to your backend.

---
#### /1/log

**GET** returns the last requests logged. Only authorized to administrators.

Query Parameters | Description
-----------------|-------------
`from` | Integer. Defaults to 0. The index of the first log to return from the latest to the oldest.
`size` | Integer. Defaults to 10. Number to logs to return. Maximum is 1000.
`q` | String. Optional. The query text to search for in requests logged.

Response body example:

```json
{
  "took" : 15,
  "total" : 75,
  "results" : [
    {
      "method" : "GET",
      "path" : "/1/login",
      "receivedAt" : "2018-04-26T19:49:42.969+02:00",
      "processedIn" : 91,
      "status" : 200,
      "credentials" : {
        "id" : "P-gTA2MBOYooo0jzPtGc",
        "username" : "vince",
        "roles" : [
          "user"
        ]
      },
      "headers" : [
        "User-Agent: okhttp/3.9.1",
        "Connection: Keep-Alive",
        "Host: api.lvh.me:8443",
        "Accept-Encoding: gzip",
        "X-Spacedog-Test: true"
      ],
      "response" : {
        "accessToken" : "MTkyZjM4ZmQtM2ZlOC00MTRkLTk0YTctNWMyZDYwYmY3N2Mw",
        "expiresIn" : 86400,
        "credentials" : {
          "id" : "P-gTA2MBOYooo0jzPtGc",
          "username" : "vince",
          "email" : "platform@spacedog.io",
          ...
        }
      }
    },
    ...
  ]
}
```

---
#### /1/log/_search

**POST** searches for logged requests. Only authorized to administrators. Body is required and must contain an *ElasticSearch* search request. See [search DSL](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/search-request-body.html).

Example:

```json
{
  "size": 100,
  "query": {
    "term" : {
        "path" : "/1/data/message"
    }
  }
}
```