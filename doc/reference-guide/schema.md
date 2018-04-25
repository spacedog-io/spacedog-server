---
layout: doc
title: Schema
rank: 1
---

#### /1/schema

A schema object defines a type of data objects. A schema must be defined first before objects can be stored.

##### {backendId}.spacedog.io/1/schema

`GET` returns all schemas as a map.

##### {backendId}.spacedog.io/1/schema/{type}

`GET` returns the specified schema.

`POST`, `PUT` creates or updates the specified schema. Only authorized to adminsitrators.

`DELETE` deletes the specified schema and all its data objects. Only authorized to adminsitrators.

##### Schema JSON format

```json
{
	"myNewType" : {
	    "_type" : "object",
	    "name" : {
	        "_type" : "text",
	        "language": "english"
	    },
	    "email" : {
	        "_type" : "string"
		}
		...
	}
}
```

This table references all the schema fields:

| Field                 | Type    | Description                              |
| --------------------- | ------- | ---------------------------------------- |
| _name                 | string  | The unique identifier of the schema. Required. |
| _id                   | string  | The path to the field containing the identifier of this type of object. |
| _type                 | string  | The type of schema. Required. Valid values = object, stash. |
|                       |         |                                          |
| {fieldname}           | object  | The schema settings for the specified field. |
| {fieldname}._type     | string  | The type of field. See below the valid types. |
| {fieldname}._language | string  | Optional. The name of the language of a text field. This improves full text search accuracy. Valid values are [english, french, espagnol, german, ...]. |
| {fieldname}._array    | boolean | Optional. Default is false. If true, the field is an array of values of the specified type. If no type specified, the values are of any type and this is equivalent to a stash field since the values will not be analysed, validated nor indexed. |

This table references all the data field types:

| Type      | Format                                   | Description                              |
| --------- | ---------------------------------------- | ---------------------------------------- |
| text      | any string                               | Text fields are indexed for full text search. Use the type `string` if you need regular equal search. |
| string    | any string                               | A string field is meant to store identifiers, codes, ... i.e. strings that don't have real text significance. A string field is not analysed and is better search through equal function. |
| boolean   | true/false                               | A regular JSON boolean.                  |
| geopoint  | object with lat and long fields of type double | A geopoint field represents a precise point on planet earth. It's an object with a lat and long fields of type double. Geopoints are automatically indexed for geographical search. |
| integer   | integer                                  | A signed 32-bit integer (minimum value of -2^31 and a maximum value of 2^31-1). |
| long      | long                                     | A signed 64-bit integer with a minimum value of -2^63 and a maximum value of 2^63-1. |
| float     | float                                    | A single-precision 32-bit IEEE 754 floating point. |
| double    | double                                   | A double-precision 64-bit IEEE 754 floating point. |
| date      | YYYY-MM-DD string                        | A year, month and day type of date.      |
| time      | HH:MM:SS.sss string                      | A time. The milliseconds part is optional. |
| timestamp | YYYY-MM-DDTHH:MM:SS.sssZ string          | A timestamp, in ISO form.                |
| enum      | string                                   | A fixed number of string values. Enum are not analysed for full text search. |
| stash     | object                                   | Use a stash field to store a JSON document that does not need any validation and indexing. Fields and values inside a stashed document do not need to be defined in the schema. Two objects of the same schema type can contain in their stash field two structuraly different JSON documents. Fields and values inside a stash field are not validated, analysed nor indexed. |
| object    | object                                   | Use an object field to store a regular JSON sub object. |
