---
layout: doc
title: Defining schema
rank: 3
---

#### Schema field types

Data object fields are defined and typed in schemas. Find below all the permitted field types.

`string`

```json
{
  "car" : {
    "brand" : {
      "_type" : "string",
      "_required" : true
    }
  }
}
```

`string` fields are suitable for all string data that do not contain real text that should be analyzed for latter full text search. Names, identifiers, codes, zip codes are good examples. `string` fields are better search with equal type queries.

`text`

```json
{
  "car" : {
    "description" : {
      "_type" : "text",
      "_language" : "english"
    }
  }
}
```

`text` fields are suitable for all string data that do contain real text and should be analyzed for latter full text search. Descriptions, forum posts, messages, emails are good examples. `text` fields are better search with full text type queries.

`_language` contains the language setting for better text analysis. Defaults to `english`. Here is a list of available [languages](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html).

`boolean`

```json
{
  "car" : {
    "convertible" : {
      "_type" : "boolean"
    }
  }
}
```

`integer`, `long`, `float` and `double`

```json
{
  "car" : {
    "taxHorsePower" : {
      "_type" : "integer",
      "_gte" : 1,
      "_lte" : 13
    }
  }
}
```

[Not yet implemented] Number fields are validated before objects are saved if these rules are used:

- `_gt` greater than.
- `_gte` greater than or equal. 
- `_lt` lesser than.
- `_lte` lesser than or equal.
- `_values` array of valid values.

Car example:

```json
{
  "brand" : "Jeep",
  "taxHorsePower" : 12
}
```

`object`

```json
{
  "car" : {
    "lastToll" : {
      "_type" : "object",
      "paid" : {
        "_type" : "float",
        "_required" : true
    }
  }
}
```

Car example:

```json
{
  "brand" : "Jeep",
  "lastToll" : {
    "paid" : 3.56
  }
}
```

`date`, `time` and `timestamp`

```json
{
  "car" : {
    "..." : "...",
    "firstRegistration" : {
      "_type" : "date",
      "_required" : true
    },
    "avgMorningDepartureTime" : {
      "_type" : "time",
      "_required" : true
    },
    "lastToll" : {
      "_type" : "object",
      "paid" : {
        "_type" : "float",
        "_required" : true
      },
      "when" : {
        "_type" : "timestamp",
        "_required" : true
      },
    }
  }
}
```

- `date` are year, month and day formatted strings. Exact format is `yyyy-MM-dd`.
- `time` are hours, minutes and seconds formatted strings. Exact format is `HH:mm:ss.SSS`. The milliseconds part is optional.
- `timestamp` are ISO 8601 strings with format like `yyyy-MM-ddTHH:mm:ss.SSSZ`.

Car Example:

```json
{
  "brand" : "Jeep",
  "firstRegistration" : "1995-09-24",
  "avgMorningDepartureTime" : "08:22:17",
  "lastToll" : {
    "paid" : 4.76,
    "when" : "2015-03-04T19:34:56.123Z"
  }
}
```

`geopoint`

```json
{
  "car" : {
    "..." : "...",
    "location" : {
      "_type" : "geopoint",
      "_required" : true
    }
  }
}
```

Example of car object with a location field:

```json
{
  "brand" : "Jeep",
  "plate" : "FG-89776-XKE",
  "location" : {
    "lat" : 41.12,
    "lon" : -71.34
  }
}
```

A `geopoint` field represent a precise (1 meter) point on planet earth. They are automatically geo hashed and indexed for geographical search.

`enum`

```json
{
  "car" : {
    "color" : {
      "_type" : "enum",
      "_required" : true,
      "_values" : ["electric-blue", "burgundy-red", "silver-metal", "tropical-green"]
    }
  }
}
```

Car example:

```json
{
  "brand" : "Jeep",
  "color" : "tropical-green"
}
```

`stash`

```json
{
  "car" : {
    "extra" : {
      "_type" : "stash"
    }
  }
}
```

Use a `stash` field to store a JSON that does not need any validation nor indexing. Fields and values inside a `stash` field do not need to be defined in the schema.

You might need this when:

- you need less schema migration restriction,
- you are not interested in searching in this part of the object and you seek less index disk space.

Car example:

```json
{
  "brand" : "Jeep",
  "extra" : {
    "..." : "..."
  }
}
```

`ref` [Not yet implemented]

```json
{
  "car" : {
    "owner" : {
      "_type" : "ref",
      "_refType" : "user",
      "_required" : true
    }
  }
}
```

Use a `ref` field to store an identifier of another object of the same data store. If necessary, use `_refType` to indicate the type of the referenced object.

Car example:

```json
{
  "brand" : "Jeep",
  "owner" : "ptidenis"
}
```

To fetch a car object but also its owner referenced user object, send a `GET /v1/data/car/76867kjvkgcjg8764jhgkhc` request with the `fetch` param set to `owner`. You get the following response:

```json
{
  "brand" : "Jeep",
  "..." : "...",
  "owner" : {
    "username" : "ptidenis",
    "email" : "ptidenis@ptilabs.com",
    "..." : "..."
  }
}
```

The `fetch` param accepts a comma separated list of field names and the `all` keyword. It only fetches referenced fields from the root object of the graph.

`file` [Not yet implemented]

```json
{
  "car" : {
    "userManual" : {
      "_type" : "file",
      "_contentTypes" : ["application/pdf"],
      "_required" : true
    }
  }
}
```

Use a `file` field to reference an uploaded file. Reed the [deploy to the cloud](deploy-to-the-cloud.html) page. Use `_contentTypes` if you need to restrict this field to certain types of files.

Car example:

```json
{
  "brand" : "Jeep",
  "userManual" : "jhjhbjgvc9786Rjhchvghvk87954jhcJHVv"
}
```

`amount` [Not yet implemented]

```json
{
  "car" : {
    "lastToll" : {
      "_type" : "object",
      "paid" : {
        "_type" : "amount",
        "_required" : true
      },
      "when" : {
        "_type" : "timestamp",
        "_required" : true
      },
    }
  }
}
```

Use an `amount` field to store amounts. The three first characters encodes the currency in ISO 4217 format. The following characters represent the number. The decimal separator is a dot. A `+`or `-` can preceed the number. When dealing with amounts, prefer this type than `float` or `double` fields that might lead you to precision errors.

Car Example:

```json
{
  "brand" : "Jeep",
  "lastToll" : {
    "paid" : "USD4.76",
    "when" : "2015-03-04T19:34:56.123Z"
  }
}
```


#### Object access control [Not yet implemented]

Permissions can be set in a per type basis. The `_acl` schema field defines all the permissions of data objects defined by this schema. Car schema example:

```json
{
  "_id" : "car",
  "..." : "...",
  "_acl" : {
    "_creator" : {
      "_read" : true, "_write" : true
    },
    "admin" : {
      "_read" : true, "_write" : true
    },
    "myApiKey" : {
      "_read" : true, "_write" : true
    },
    "_public" : {
      "_read" : true
    }   
  },
}
```

This schema means that:

- the object creator - defined by the username or api key stored in document system field `_createdBy` - can read/write its own car objects.
- the members of the admin group car read/write all car objects.
- the api key `myApiKey` can read/write all car objects.
- the public - all the rest - can read all car objects.

Any user, api key and group can be listed in the access control list to set specific permissions. 

When a user is fetching or updating a specific data object for wich he does not have permission, he gets back an `NOT AUTHORIZED` response. When a user sends a search query, objects he does not have read permission will be automaticaly excluded from the response.

⋮

Next: [Authenticate users](authenticate-users.html) ⋙