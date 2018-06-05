# Data

---
#### /1/data

**GET** returns all data objects of types for which user has `search` permission.

Query Parameters | Description
-----------------|--------------
`refresh` | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.
`from` | Integer. Defaults to 0. Row of the first object to return.
`size` | Integer. Defaults to 10. Maximum Number of objects to return.
`q` | String. Optional. The query string to search objects with. See *elasticsearch* [Simple Query String](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-simple-query-string-query.html) search.

---
#### /1/data/{type}

**GET** returns all data objects of the specified type. `search` permission is required.

Query Parameters | Description
-----------------|--------------
`refresh` | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.
`from` | Integer. Defaults to 0. Row of the first object to return.
`size` | Integer. Defaults to 10. Maximum Number of objects to return.
`q` | String. Optional. The query string to search objects with. See *elasticsearch* [Simple Query String](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-simple-query-string-query.html) search.

**DELETE** deletes all data objects of the specified type. `delete` permission is required.

Query Parameters | Description
-----------------|--------------
`refresh` | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are deleted.

**POST** creates a new data object of the specified type. `create` permission is required. A payload is required with the JSON representation of the object. Object must comply its type schema.

Query Parameters | Description
-----------------|--------------
`forceMeta` | Boolean. Default to `false`. If true, initialize meta fields with those provided in body along with the rest of the data. Meta fields are: `createdAt`, `updatedAt`, `owner`, `group`. `updateMeta` permission is required in user's credentials to force meta fields.

---
#### /1/data/{type}/_export

**POST** returns all data objects of the specified type and matching the specified query as a multi lines json text. Only users with `search` permission are authorized.


Query Parameters | Description
-----------------|--------------
`refresh` | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.

---
#### /1/data/{type}/_import

**POST** index all data objects of the request body to the specified type. Only users with `importAll` permission are authorized. The body must be a multi line json text. Each line represents a data object of the following form:

```json
{"id": "...", "source": {"...": "..."}}
{"id": "...", "source": {"...": "..."}}
{"id": "...", "source": {"...": "..."}}
```

Metadata of imported objets (owner, group, createdAt, updatedAt) is imported as is and should be specified for each object in json body.

Query Parameters | Description
-----------------|--------------
`preserveIds` | Boolean. Defaults to false. If true, imports objects preserving the specified ids. If false, imported objects get a new id like if they were newly created.

---
#### /1/data/{type}/{id}

**GET** returns the data object of the specified type and id. One of these permissions is required depending on the context: `read`, `readMine` or `readGroup`.

**DELETE** deletes the data object of the specified type and id. One of these permissions is required depending on the context: `delete`, `deleteMine` or `deleteGroup`.

**PUT** creates or updates the data object of the specified type and id with the specified body. One of these permissions is required depending on the context: `create`, `update`, `updateMine` or `updateGroup`.

Query Parameters | Description
-----------------|--------------
`strict` | Boolean. Default to false. If true, stricly updates the whole object with the specified body. If false, updates only the fields present in the specified body.
`version` | Long. Optional. The version of the data object to update. Used for optimistic locking. If the server side version of this object differs from the one specified, returns http status code `409 Conflict`, meaning, your client data is obsolete and update request is cancelled.

*Warning:* An object can't be updated twice exactly at the same time. You get a `409 Conflict` error even if you didn't pass the `version` parameter to check for object obsolescence.

---
#### /1/data/{type}/{id}/{field}

In this case, the `field` route param represent the JSON path to the specified field in dot notation. Valid field path examples: `firstname`, `customer.firstname`.

**GET** returns the field of the specified data object. One of these permissions is required depending on the context: `read`, `readMine` or `readGroup`.

**DELETE** deletes the field of the specified data object. One of these permissions is required depending on the context: `delete`, `deleteMine` or `deleteGroup`. In case the specified field is an array, the request body contains the list of array values to remove. In case the body is null, the field array is cleared.

**POST** updates the field of the specified data object with the specified body. In the case field is an array, the body must contains values to be added to the array. One of these permissions is required depending on the context: `create`, `update`, `updateMine` or `updateGroup`. Data object is created if it didn't exist and if user has `create` permission.

**PUT** updates the field of the specified data object with the specified body. One of these permissions is required depending on the context: `create`, `update`, `updateMine` or `updateGroup`. Data object is created if it didn't exist and if user has `create` permission.