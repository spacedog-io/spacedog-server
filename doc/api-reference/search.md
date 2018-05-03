# Search

---
#### /1/data/_search


**POST** searches for data objects of types for which user has `search` permission. Body is required containing an *ElasticSearch* JSON search request [search DSL](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/search-request-body.html). `search` permission is required.

Parameters:

- `refresh` - Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.

**DELETE** searches and deletes data objects of types for which user has `delete` permission. Body is required containing an *ElasticSearch* JSON query request [query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl.html).

Parameters:

- `refresh` - Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are deleted.

---
#### /1/data/{type}/_search

The same but for objects of the specified type. `search` or `delete` permission is required depending on the context.

---
#### /1/data/{type}/_csv

**POST** searches for data objects of the specified type and returns them in CSV format. `search` permission is required. Body is required and contains a request like:

```json
{
	"refresh": false,
	"query": {
		"range" : {
			"fare" : {
				"gt" : 0
      		}
    	}
    },
	"settings" : {
	    "delimiter" : ";",
	    "firstRowOfHeaders" : true
	},
	"columns" : [
		{
			"field" : "createdAt",
			"header" : "Created At",
			"type" : "timestamp",
			"pattern" : "dd/MM/yy"
		},
		{
			"field" : "status",
			"header" : "Status",
			"type" : "string",
		},
		{
			"field" : "fare",
			"header": "Fare",
			"type" : "floating",
			"pattern" : "#.##"
		}
	]
}
```

Request fields:

- `refresh` - Boolean. Defaults to false. If true, forces index refresh to make sure the latests created objects are there.

- `query` - Object. Defaults to a match all query. *ElasticSearch* query to filter the collection of all data objects of the specified type.

- `setting.delimiter` - String. Defaults to `,`. The character to delimiter CSV data fields.

- `setting.firstRowOfHeaders` - Boolean. Defaults to false. If true, add all columns headers as the first row of the CSV file.

- `columns` - Array of column objects. Required. The list of fields/columns to export and how to export them.

- `columns.field` - String. Required. The path (in dot notation) to the JSON field. Examples: `firstname` or `customer.credentialsId`.

- `columns.header` - String. Defaults to field name. The name of the column to display in first row of headers.

- `columns.type` - String. Required. The type of the JSON field. Available types are: `bool`, `integral`, `floating`, `string`, `timestamp`, `date`, `time`, `other`.

- `columns.pattern` - String. Optional. Used to format field of type `floating` with [Java DecimalFormat patterns](https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html) and `timestamp` with [Joda DateTimeFormat patterns](http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html).
