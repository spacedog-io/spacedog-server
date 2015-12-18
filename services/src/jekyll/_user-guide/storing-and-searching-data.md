---
layout: doc
title: Storing and searching data
rank: 2
---

#### Data objects

A SpaceDog data object is a typed JSON document. To get some of your backend data objects, send a `GET /v1/data` and don't forget to set the `x-spacedog-backend-key`. You will get the following answer since you did not create any object yet:

```json
{
  "took": 3,
  "total": 0,
  "results": []
}
```

#### Create a schema

To create objects, you first need to create a schema. Schemas are used to:

- validate data is well formed,
- define how data is indexed and, evidently, how to search for it,
- set who can read or write these objects,
- set which custom functions to trigger when objects are saved or deleted.

Here is a very simple schema for a car object:

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
      "_required" : true
    },
    "description" : {
      "_type" : "text",
      "_language" : "english"
    }
  }
}
```

- `car` is the schema identifier and the name of the object type this schema defines.
- `_type` is the field or object type. Read the [defining schema](defining-schema.html) page for more details.
- `_required` is optional and indicates if the field is mandatory.
- `_language` is useful for `text` fields. It indicates the text language and allow a better full text indexing.

The `car` schema defines objects like:

```json
{
  "brand" : "Citroën",
  "model" : "2CV",
  "color" : "blue",
  "description" : "The Citroën 2CV (French: \"deux chevaux\" i.e. \"deux chevaux-vapeur\" (lit. \"two steam horses\"), \"two tax horsepower\") is a front-engine, front wheel drive, air-cooled economy car introduced at the 1948 Paris Mondial de l'Automobile and manufactured by Citroën for model years 1948–1990."
}
```

To create a schema, `POST /v1/schema/car` with a body set to the `car` JSON schema and an administrator `Authorization` header (see Security section in [Getting started](getting-started.html) page). It will return a status object, either a success:

```json
{
  "success": true,
  "id": "car",
  "type": "schema",
  "location": "https://spacedog.io/v1/schema/car"
}
```

or a failure:

```json
{
  "success" : false,
  "error" : {
    "type" : "io.spacedog.services.AuthenticationExcpetion",
    "message" : "invalid authorization header",
    "trace" : [
      "..."
    ]
  }
}
```

This is generally true for `POST`, `PUT` and `DELETE` requests on the other API endpoints.

#### Create a data object

To store a new car, send a `POST /v1/data/car` with a body set to a JSON car. You'll get the new car id from the status object or from the `x-spacedog-object-id` HTTPS response header.

#### Fetch a data object

To check the new car is correctly stored, send a `GET /v1/data/car/<<mynewcarid>>` and check the car JSON returned.

#### Full text search data objects

Send a `GET /v1/data` to search into all types of objects or `GET /v1/data/car` to search only cars. Add a `q` query param set to `deux chevaux` to full text search this text and you'll get the response:

```json
{
  "took" : 145,
  "total" : 1,
  "results" : [
    {
      "meta" : {
        "id" : "AVBNO3a-QyG1NXhw6uuH",
        "type" : "car",
        "version" : 1,
        "createdBy" : "default",
        "updatedBy" : "default",
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
```

For full text search, only `text` fields are analyzed:

- polished: some characters are removed or replaced, case is lowered,
- tokenized: broken down into small terms,
- filtered: some terms are added (synonyms), others are removed (stopwords like `a`, `and`, `the`, ...).

Only the analyzed field value is indexed for later full text search. The value raw data is stored for retrieval.

Non `text` fields are also indexed for search but only the raw value will match a full text search query.

#### Update or delete a data object

To update the car object, send a `PUT /v1/data/car/{id}?version={current}` where `id` is the car identifier and `current` is the current object version. Add a body set to the updated car JSON. You'll get a regular status response.

When updating a data object, the current object version can be used to enforce optimistic consistency check. More than one person or program might update the same object at the same time. This might end up with a data inconsistency. To avoid such problem, all data objects have a system managed version. Every time an object is updated, the provided version is compared to the one in store. Versions are different means that the object has been updated in the meantime by someone else. It results an error for the second to try an update.

To delete the car object, send a `DELETE /v1/data/car/AVBNO3a-QyG1NXhw6uuH` and get a status response. No need to provide a version in this case since a delete will always win over an update.

#### More advanced search

For an advanced search, send a `POST /v1/data/search` or a `POST /v1/data/car/search` with a body set to a query JSON. Since SpaceDog uses ElasticSearch for storing and searching data objects, the query JSON must be compliant with the ElasticSearch query DSL. Please read the [query DSL documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html) on the Elastic web site.

For example, this query JSON will full text search for car objects containing `air-cooled` but only when it is a `Citroën`:

```json
{
  "query" : {
    "filtered" : {
      "query" : {
        "match" : { "description" : "air cooled" }
      },
      "filter" : {
        "term" : { "brand" : "Citroën" }
      }
    }
  }
}
```

#### Update or delete a schema

To update the `car` schema, send a `PUT /v1/schema/car` with a body set with the updated JSON schema.

The system will reject all changes that might endanger retro compatibility. It means that all changes that might end up with a failure in old released versions of your app are forbidden. For example, these changes are rejected:

- delete a field
- change a field's name or type
- move a field to another sub object
- remove a defined value from an `enum`
- ...

When developing a new release of your app, you usually need to make changes in your object schemas. You are only authorized with adding fields or changing validation to less restrictive rules.

To delete the `car` schema, send a `DELETE /v1/schema/car`. It will also delete all the `car` objects.

⋮

Next: [Defining schema](defining-schema.html) ⋙
