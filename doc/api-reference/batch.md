# Batch

Use a batch request when you need to send to your backend a batch of requests. It speeds up operations since there is only one round trip to the server.

---
#### /1/batch

**POST** processes a batch of maximum 10 requests. The body is an array of JSON objects. Example representings requests:

```json
[
     {
          "method" : "GET",
          "path" : "/1/data/product",
          "parameters": {
               "size": 20
          }
     },
     {
          "method" : "POST",
          "path" : "/1/data/product",
          "content" : {
               "name" : "Teddy Bear",
               "price" : 23.45
          }
     },
     {
          "method" : "DELETE",
          "path" : "/1/data/product/GVTkGHKsuJhkJMV-i2ke"
     }
]
```

And it returns an array of JSON objects representing responses. Example:

```json
[
     {
          "total" : 0,
          "results" : [
          ]
     },
     {
          "success" : true,
          "status" : 200,
          "id": "HJTkKLKsuJhkJMVoUIh6g",
          "type": "product",
          "location" : "https://mybackend.spacedog.io/1/data/product/HJTkKLKsuJhkJMVoUIh6g"
     },
     {
          "success" : false,
          "status" : 404,
          "error" : {
               "message" : "data object [product][GVTkGHKsuJhkJMV-i2ke] not found",
          }
     }
]
```

Parameters | Description
-----------|-------------------------------
`stopOnError` | Boolean. Defaults to false. If true, stops the batch processing when one request returns an http error (40X or 50X).

To pass specific parameters and headers to the batch requests, add a `parameters` and a `headers` sections to the request JSON object. Header values must be strings. Example:

```json
[
     {
          "method" : "GET",
          "path" : "/1/data/product",
          "parameters": {
               "size": 20
          },
          "headers": {
               "x-spacedog-debug": "true"
          }
     }
]
```