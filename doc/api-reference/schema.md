# Schema

A schema object defines a type of data objects. A schema must be defined first before objects can be stored.

SpaceDog uses [*ElasticSearch* data mapping format](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html).

Example of a log schema:

```json
{
    "log" : {
        "properties" : {
            "method" : {"type" : "keyword"},
            "path" : {"type" : "keyword"},
            "receivedAt" : {"type" : "date", "format" : "date_time"},
            "processedIn" : {"type" : "long", "coerce" : false},
            "credentials" : {
                "type" : "object",
                "properties" : {
                    "id" : {"type" : "keyword"},
                    "username" : {"type" : "keyword"},
                    "roles" : {"type" : "keyword"}
                }
            },
            "parameters" : {"type" : "keyword"},
            "headers" : {"type" : "keyword"},
            "payload" : {"type" : "object", "enabled" : false},
            "status" : {"type" : "integer", "coerce" : false},
            "response" : {"type" : "object", "enabled" : false}
        }
    }
}
```

SpaceDog enhances user's provided mappings with the following settings and properties:


```json
{
    "log" : {
    	"dynamic": "strict",
    	"date_detection": false,
        "properties" : {
            "owner" : {"type" : "keyword"},
            "group" : {"type" : "keyword"},
            "createdAt" : {"type" : "date", "format" : "date_time"},
            "createdAt" : {"type" : "date", "format" : "date_time"},
            "..." : "..."
        }
    }
}
```

Added fields | Description
-------------|--------------
`owner`		 | String. Credentials id of the data object owner. Usualy the user who did create the object. You can update `owner` field if you have `update` permission on the object.
`group`		 | String. Group id of the data object owner. You can update `group` field if you have `update` permission on the object.
`createdAt`		 | Timestamp of the object creation. Read-only.
`updatedAt`		 | Timestamp of the last object update. Read-only.

---
#### /1/schemas

**GET** returns all schemas as a map.

___
#### /1/schema/{type}

**GET** returns the specified schema.

**PUT** creates/updates the specified schema. Only authorized to adminsitrators.

**DELETE** deletes the specified schema and all its data objects. Only authorized to adminsitrators.