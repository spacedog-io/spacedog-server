---
layout: doc
title: Schema
---

#### Data object schemas

A schema object defines a type of data objects. A schema must be first defined before objects can be stored.

```json
{
    "_type" : "...",
    "name" : {
        "_type" : "text",
        "_required" : true
    },
    "email" : {
    	"..." : "..."
	},
	"_acl" : {
		"admin" : {
			"_read" : true, "_write" : true
		},
		"_creator" : {
			"_read" : true, "_write" : true
		},
		"_public" : {
			"_read" : true
		},
		"myFirstAppApiKey" : {
			"_read" : true, "_write" : true
		}		
	},
	"_triggers" : {
		"_beforeSave" : "f1",
		"_beforeDelete" : "f2",
		"_afterSave" : "f3",
		"_afterDelete" : "f4"
	}
}
```

This table references all the schema fields:

Field | Type | Description
------|------|------------
_name | string | The unique identifier of the schema. Required.
_id | string | The path to the field containing the identifier of this type of object.
_type | string | The type of schema. Required. Valid values = object, stash.
  |  |  
{fieldname} | object | The schema settings for the specified field.
{fieldname}._type | string | The type of field. See below the valid types.
{fieldname}._required | boolean | Optional. Default to false. If true, the field is mandatory.
{fieldname}._language | string | Optional. ISO blablabla representing a language code. Valid values are [ en, fr, es, de, ...]. To be used with a text field to indicate the language of the text and improve full text search accuracy.
{fieldname}._reftype | string | Optional for field of type `ref`. The type of the referenced object. If not set, the referenced object can be of any type.
{fieldname}._array | boolean | Optional. Default is false. If true, the field is an array of values of the specified type. If no type specified, the values are of any type and this is equivalent to a stash field since the values will not be analysed, validated nor indexed.
{fieldname}._index | [] of strings | Optional for a field of type `ref`. List all the referenced object fields to index with this object for relational queries. The system will automaticaly manage consistency when the referenced object is updated or deleted. These fields are not part of this object schema and can not be read or write. Their only use is indexing and queries. Use dot notation to duplicate fields of sub objects.
{fieldname}._cascade | boolean | Optional. Default to false. If true, cascade delete a referenced file when the referencing object is deleted of if the reference field is updated to another reference.
{fieldname}._gt | number or date or time or timestamp | The value must be greater than the specified number. For string values, applies to the string length. 
{fieldname}._gte | number | The value must be greater than or equal to the specified number. For string values, applies to the string length. 
{fieldname}._lt | number | The value must be lesser than the specified number. For string values, applies to the string length. 
{fieldname}._lte | number | The value must be lesser than or equal to the specified number. For string values, applies to the string length. 
{fieldname}._values | [] of strings | Optional for `enum`fields. The list of enum values.
{fieldname}._regex | string | The value must comply to the specified regular expression. Validates `text`, `code` and `enum` fields.
{fieldname}._currency | string | Optional for `amount` fields. Indicates the awaited currency. Currencies are represented by ISO norm blablabla. Examples EUR, USD, CAD, ...
_acl | object | Required. Security settings for objects of this type.
_acl.{id} | object | At least one is required. The specified id can be the id of a user, an api key or a group of users. It can also be the following reserved words: creator, admin, public. This objet will defines security settings for the specified type of user or client.
_acl.{id}.read | boolean | Optional. Default to false. If true, gives read permission on this type of objects.
_acl.{id}.read | boolean | Optional. Default to false. If true, gives write permission on this type of objects.
_triggers | object | Set the services to call when this type of object is saved or deleted.
_triggers._beforeSave | string | The id of the service to call before this type of object is saved.
_triggers._beforeDelete | string | The id of the service to call before this type of object is deleted.
_triggers._afterSave | string | The id of the service to call after this type of object is saved.
_triggers._afterDelete | string | The id of the service to call after this type of object is deleted.

This table references all the data field types:

Type | Format | Description
-----|--------|------------
text | any string | Text fields are indexed for full text search. Use the type `code` if you need regular equal search.
code | any string | A code field is meant to store identifiers, codes, ... i.e. strings that don't have real text significance. A code field is not analysed and is better search through equal function.
boolean | true/false | A regular JSON boolean.
geopoint | object with lat and long fields of type double | A geopoint field represents a precise point on planet earth. It's an object with a lat and long fields of type double. Geopoints are automatically indexed for geographical search.
number | int, long, float or double | A regular JSON number.
date | YYYY-MM-DD string| A year, month and day type of date.
time | HH:MM:SS.sss string | A time. The milliseconds part is optional.
timestamp | YYYY-MM-DDTHH:MM:SS.sssZ string | A timestamp.
enum | string | A fixed number of string values. Enum are not analysed for full text search.
stash | object | Use a stash field to store a JSON document that does not need any validation and indexing. Fields and values inside a stashed document do not need to be defined in the schema. Two objects of the same schema type can contain in their stash field two structuraly different JSON documents. Fields and values inside a stash field are not validated, analysed nor indexed.
ref | valid objet string id | A reference to another object of the same data store. If necessary, use `_reftype` to indicate the type of the referenced object.
file | file id as string | Reference an uploaded file.
amount | string EUR-67.765 | An amount. The three first characters encodes the currency with ISO norm blablabla. The decimal separator must be a dot.

##### /v1/schema

`GET` returns all schemas as a set.

`POST` create schemas.

The requets body is An array of schemas. It returns an array of status objects.

##### /v1/schema/{type},...

`GET` returns the specified schemas.

`PUT`updates the specified schemas.

`DELETE` deletes the specified schemas and all data objects of their types.

