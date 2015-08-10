---
layout: doc
title: Storing and searching data
rank: 2
---

Before to store any data with the MagicApps platform, you first need to create a data object type by posting a new schema. A schema defines data objects. It is used:

- to validate that data is well formed
- to know how to index data objects and, evidently, how to search for it,
- to defines who can read or write these objects,
- to set which custom functions to trigger when objects are saved or deleted.

Here is a very simple schema:

```json
{
  "car" : {
    "_type" : "object",
    "brand" : {
      "_type" : "string",
      "_required" : true
    },
    "model" : {
      "_type" : "string",
      "_required" : true
    },
    "color" : {
      "_type" : "enum",
      "_required" : true,
      "_values" : ["blue", "red", "green"]
    },
    "description" : {
      "_type" : "text",
      "_language" : "en"
    }
  }
}
```

- `car` is the schema identifier and the name of the object type this schema defines.
- `_type` is the field or object type. Read the [field types](field types) page for more details.
- `_required` is optional and indicates if the field is mandatory.
- `_language` is useful for `text` fields. It indicates the text language and allow a better full text indexing.
- `_values` is useful for `enum` fields by listing authorized values. The system will reject any undefined value in this field.

The `car`schema defines objects like this one:

```json
{
  "brand" : "Citroën",
  "model" : "2CV",
  "color" : "blue",
  "description" : "The Citroën 2CV (French: \"deux chevaux\" i.e. \"deux chevaux-vapeur\" (lit. \"two steam horses\"), \"two tax horsepower\") is a front-engine, front wheel drive, air-cooled economy car introduced at the 1948 Paris Mondial de l'Automobile and manufactured by Citroën for model years 1948–1990."
}
```


##### Create a schema

To create a schema, `POST /v1/schema` a body set to the schema JSON. It will return a status object, either a success:

```json
{
  "success" : true,
  "id" : "car",
  "location" : "https://api.magicapps.com/v1/schema/car"
}
```

or a failure:

```json
{
  "success" : false,
  "error" : {
    "code" : 123,
    "message" : "not authorized, missing x-magic-app-id http header"
  }
}
```

This is generally true for `POST`, `PUT` and `DELETE` requests on the other API endpoints.

##### Create a data object

To store a new car, send a `POST /v1/data/car` with a body set to a car JSON. You'll get the new car id from the status object.

##### Fetch a data object

To check the new car is correctly stored, send a `GET /v1/data/car/76867kjvkgcjg8764jhgkhc`and check the car JSON returned.

##### Full text search data objects

Send a `GET /v1/data` to search into all types of objects or `GET /v1/data/car` to search only cars. Add a `text` query param set to `deux chevaux` to full text search this text and you'll get the response:

```json
{
  "took" : 145,
  "total" : 1,
  "results" : [
    {
      "meta" : {
        "id" : "76867kjvkgcjg8764jhgkhc",
        "type" : "car",
        "version" : 1,
        "createdBy" : "ptidenis",
        "updatedBy" : "ptidenis",
        "createdAt" : "2015-06-01T15:12:56.123Z",
        "updatedAt" : "2015-06-01T15:17:06.123Z"
      },
      "brand" : "Citroën",
      "model" : "2CV",
      "color" : "blue",
      "description" : "The Citroën 2CV (French: \"deux chevaux\" i.e. \"deux chevaux-vapeur\" (lit. \"two steam horses\"), \"two tax horsepower\") is a front-engine, front wheel drive, air-cooled economy car introduced at the 1948 Paris Mondial de l'Automobile and manufactured by Citroën for model years 1948–1990."
    }
  ]
  }
}
```

For full text search, only `text` fields are analyzed:

- polished: some characters are removed or replaced,
- tokenized: broken down into small terms,
- filtered: some terms are added (synonyms), others are removed (stopwords like `a`, `and`, `the`, ...).

Only the analyzed field value is indexed for later full text search. The value raw data is stored for retrieval.

##### Update or delete a data object

To update the car object, send a `PUT /v1/data/car/76867kjvkgcjg8764jhgkhc/1` where `76867kjvkgcjg8764jhgkhc` is the car identifier and where `1` is the object version. Add a body set to the updated car JSON. You'll get a regular  status response.

When updating a data object, the version is mandatory to alloy optimistic consistency check. More than one person or program might update the same object at the same time. This might end up with a data inconsistency. To avoid such problem, all data objects have a system managed version. Every time an object is updated, the provided version is compared to the one in store. Versions are different means that the object has been updated in the meantime by someone else. It results an error for the second to try an update.

To delete the car object, send a `DELETE /v1/data/car/76867kjvkgcjg8764jhgkhc` and get a status response. No need to provide a version in this case since a delete will always win over an update.

##### More advanced search

For an advanced search, send a `POST /v1/data/search` or a `POST /v1/data/car/search` with a body set to a query JSON. Since MagicApps uses ElasticSearch for storing and searching data objects, the query JSON must be compliant with the ElasticSearch query DSL. Please read the [query DSL documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html) on the Elastic company web site.

For example, this query JSON will full text search for car objects containing `air-cooled` but only when it is a `Citroën`:

```json
{
  "query" : {
    "filtered" : {
      "query" : {
        "match" : { "message" : "air-cooled" }
      },
      "filter" : {
        "term" : { "brand" : "Citroën" }
      }
    }
  }
}
```

##### Update or delete a schema

To update the `car` schema, send a `PUT /v1/schema/car` with a body set with the updated schema JSON.

The system will reject all changes that might endanger retro compatibility. It means that all changes that might end up with a failure in old released versions of your app are forbidden. For example, these changes are rejected:

- delete a field
- change a field's name or type
- move a field to another sub object
- remove a defined value from an `enum`
- ...

When developing a new release of your app, you usually need to make changes in your objet schemas. You are only authorized with adding fields or changing validation to less restrictive rules.

To delete the `car` schema, send a `DELETE /v1/schema/car`. It will also delete all the `car` objects.