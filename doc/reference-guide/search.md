---
layout: doc
title: Search
rank: 3
---

#### /1/search

##### {backendId}.spacedog.io/1/search

`GET` searches for data objects of all types.

- `q` String. Optional. The simple text query to search in all objects and fields. Example: `q=flower shops`.
- `refresh` Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.
- `from` Integer. Defaults to 0. Row of the first object to return.
- `size` Integer. Defaults to 10. Maximum Number of objects to return.

`POST` returns data objects of all types found by the specified query. The body must contain an ElasticSearch JSON query compliant with this [query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).

- `refresh` Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.

`DELETE` deletes data objects of all types found by the specified query. The body must contain an ElasticSearch JSON query compliant with this [query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).

- `refresh` Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are deleted.

##### {backendId}.spacedog.io/1/search/{type}

The same but only for objects of the specified type.