# Schema field types

Data object fields are defined in schemas using [*ElasticSearch* mapping DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html). Find below property examples.

---
#### Keyword

```json
{
  "car" : {
    "properties": {
      "brand" : {
        "type" : "keyword"
      }
    }
  }
}
```

`keyword` fields are suitable for all string data that do not contain real text that should be analyzed for latter full text search. Names, identifiers, codes, zip codes, urls, path are good examples. `string` fields are better search with equal type queries.

---
#### Text

```json
{
  "car" : {
    "properties": {
      "description" : {
        "type" : "text",
        "analyzer" : "english"
      }
    }
  }
}
```

`text` fields are suitable for all string data that do contain real text and should be analyzed for latter full text search. Descriptions, forum posts, messages, emails are good examples. `text` fields are better search with full text type queries.

`analyzer` contains the analyzer or language setting for better text analysis. Defaults to `english`. Here is a list of available [languages](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/analysis-lang-analyzer.html).

---
#### Boolean

```json
{
  "car" : {
    "properties": {
      "convertible" : {
        "type" : "boolean"
      }
    }
  }
}
```

---
#### Integer, long, float and double

```json
{
  "car" : {
    "properties" {
      "taxHorsePower" : {
        "type" : "integer",
      }
    }
  }
}
```

Car example:

```json
{
  "brand" : "Jeep",
  "taxHorsePower" : 12
}
```

---
#### Object

```json
{
  "car" : {
    "properties": {
      "lastToll" : {
        "type" : "object",
        "properties": {
          "paid" : {
            "type" : "float"
          }
      }
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

---
#### Date, time and timestamp

```json
{
  "car" : {
    "properties": {
      "firstRegistration" : {
        "type" : "date",
        "format": "date"
      },
      "avgMorningDepartureTime" : {
        "type": "date",
        "format": "hour_minute_second"
      },
      "lastToll" : {
        "type" : "object",
        "properties": {
          "when" : {
            "type" : "date",
            "format": "date_time"
          }
        }
      }
    }
  }
}
```

*ElasticSearch* default date formats:

- `date` are year, month and day formatted strings. Exact format is `yyyy-MM-dd`.

- `hour_minute_second` are hours, minutes and seconds formatted strings. Exact format is `HH:mm:ss.SSS`. The milliseconds part is optional.

- `date_time` are ISO 8601 strings with format like `yyyy-MM-ddTHH:mm:ss.SSSZ`.

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

---
#### Geo point

```json
{
  "car" : {
    "location" : {
      "type" : "geo_point"
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

A `geo_point` field represent a precise (1 meter) point on planet earth. They are automatically geo hashed and indexed for geographical search.

---
#### Stash

```json
{
  "car" : {
    "properties": {
      "extra" : {
        "type" : "object",
        "enabled": false
      }
    }
  }
}
```

Use a `object` field with `enabled` false to store a JSON object that does not need any validation nor indexing. Fields and values inside a *stashed* field do not need to be defined in the schema.

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