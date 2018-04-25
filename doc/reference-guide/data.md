---
layout: doc
title: Data
rank: 2
---

#### /1/data

##### {backendId}.spacedog.io/1/data

`GET` returns all the specified backend data objects of all types.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| refresh    | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there. |
| from       | Integer. Defaults to 0. Row of the first object to return. |
| size       | Integer. Defaults to 10. Maximum Number of objects to return. |

`DELETE` deletes all the specified backend data objects of all types.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| refresh    | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are deleted. |

##### {backendId}.spacedog.io/1/data/{type}

`GET` returns all the specified backend data objects of the specified type.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| refresh    | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there. |
| from       | Integer. Defaults to 0. Row of the first object to return. |
| size       | Integer. Defaults to 10. Maximum Number of objects to return. |

`DELETE` deletes all the specified backend data objects of the specified type.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| refresh    | Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are deleted. |

`POST` creates an new object of the specified type into the specified backend.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| id         | String. Optional. If set, forces SpaceDog to assign the specified `id` to the specified object unless the schema says otherwise with the `_id` directive. Otherwise SpaceDog assigns a generated id to this object. |
| saveCustomMeta | Boolean. Default to `false`. If true, save `meta` field provided in body along with the rest of the data. The `meta` field should contain a sub object with the following sub fields: `createdAt`, `createdBy`, `updatedAt`, `updatedBy` |
| body       | String. Required. The JSON repredentation of the data object. Must comply to the type schema. |

##### {backendId}.spacedog.io/1/data/{type}/{id}

`GET` returns the data object of the specified type and id.

`DELETE` deletes the data object of the specified type and id.

`PUT` updates the data object of the specified type and id with the specified body. Depending on the request body and the `strict`parameter, the object is fully updated or just patched.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| strict     | Boolean. Default to false. If true, stricly updates the whole object with the specified body. If false, updates only the fields present in the specified body. |
| version    | Long. Optional. The previous version of the data object to update. Used for optimistic locking. If the server side version of this object differs from the one specified, returns http status code `409 Conflict`, meaning, your data might be obsolete. |

*Warning:* An object can't be updated twice exactly at the same time. You get a `409 Conflict`error even if you didn't pass the `version`parameter to check for object obsolescence.