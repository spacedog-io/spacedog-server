### Schema field types

Data object fields are defined in schemas. Find below all the permitted field types.

##### String

```json
{
  "car" : {
    "brand" : {
      "_type" : "string"
    }
  }
}
```

`string` fields are suitable for all string data that do not contain real text that should be analyzed for latter full text search. Names, identifiers, codes, zip codes, urls, path are good examples. `string` fields are better search with equal type queries.

##### Text

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

`_language` contains the language setting for better text analysis. Defaults to `english`. Here is a list of available [languages](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/analysis-lang-analyzer.html).

##### Boolean

```json
{
  "car" : {
    "convertible" : {
      "_type" : "boolean"
    }
  }
}
```

##### Integer, long, float and double

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

##### Object

```json
{
  "car" : {
    "lastToll" : {
      "_type" : "object",
      "paid" : {
        "_type" : "float",
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

##### Date, time and timestamp

```json
{
  "car" : {
    "..." : "...",
    "firstRegistration" : {
      "_type" : "date",
    },
    "avgMorningDepartureTime" : {
      "_type" : "time",
    },
    "lastToll" : {
      "_type" : "object",
      "paid" : {
        "_type" : "float",
      },
      "when" : {
        "_type" : "timestamp",
      },
    }
  }
}
```

`date` are year, month and day formatted strings. Exact format is `yyyy-MM-dd`.

`time` are hours, minutes and seconds formatted strings. Exact format is `HH:mm:ss.SSS`. The milliseconds part is optional.

`timestamp` are ISO 8601 strings with format like `yyyy-MM-ddTHH:mm:ss.SSSZ`.

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

##### Geopoint

```json
{
  "car" : {
    "location" : {
      "_type" : "geopoint"
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

##### Enum

```json
{
  "car" : {
    "color" : {
      "_type" : "enum",
      "_values" : ["electric-blue", "burgundy-red", "silver-metal", "tropical-green"]
    }
  }
}
```

[Not yet implemented] The directive `_values` is not yet implemented.

Car example:

```json
{
  "brand" : "Jeep",
  "color" : "tropical-green"
}
```

##### Stash

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

⋮

Next: [Authenticate users](authenticate-users.md) ⋙