---
layout: doc
title: Data
---

#### Application data

##### /v1/data

`GET` searches for data objects of all types in full text mode.

- `text` The text to full text search.
- `fetch` One or more field names of type `ref` to automatically fetch. Use the keyword `all` to automatically fetch all referenced objects. This behavior is only available at the first level of the object graph.

##### /v1/data/{type}

`GET` searches for data objects of the specified type.

- `text` The text to full text search.
- `fetch` One or more field names of type `ref` to automatically fetch. Use the keyword `all` to automatically fetch all referenced objects. This behavior is only available at the first level of the object graph.

`POST` creates a new data object of this type.

`DELETE` deletes this type and all data objects of this type.


##### /v1/data/{type}/{id}

`GET` returns the specified data object.

`PUT` updates the specified data object.

- `body` The data JSON object of this type

`DELETE` deletes the specified data object.


##### /v1/data/search
##### /v1/data/{type}/search

`POST` searches for data objects of the specified type if provided.

- `body` An ElasticSearch JSON query.

