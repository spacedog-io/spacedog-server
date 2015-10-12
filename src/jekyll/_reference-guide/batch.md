---
layout: doc
title: Batch
---

#### Batch requests

Use a batch request when you want to send to your backend a batch of requests. It speeds up operations since there is only one round trip to the server.

##### /v1/batch

`POST` process a batch of requests.

- `serialize` = [false]/true –– If true, all requests are processed one at the time in the requested order. If false, some requests might be processed in parallel dependding on CPU availability.
- request body –– An array of request JSON objects.
- returns an array containing all the results of each batch request in the same order.

Example of batch request:

```json
[
     {
          "GET" : "/v1/data/product/15113431",
     },
     {
          "POST" : "/v1/data/product",
          "body" : {
               "name" : "Teddy Bear",
               "price" : "$23.45"
          }
     },
     {
          "POST" : "/v1/data/product",
          "body" : {
               "name" : "Mobby Dick",
               "price" : "$29.99"
          }
     },
     {
          "DELETE" : "/v1/data/product/1234567890"
     }
]
```

It would return:

```json
[
     {
          "name" : "Thunder Buzz",
          "price" : "$13.45"
     },
     {
          "success" : true,
          "id" : "88787T76R5R",
          "location" : "https://api.mglabs.com/v1/data/product/88787T76R5R"
     },
     {
          "success" : true,
          "id" : "7Y78T7T86R",
          "location" : "https://api.mglabs.com/v1/data/product/7Y78T7T86R"
     },
     {
          "success" : false,
          "error" : {
               "httpCode" : 404,
               "appCode" : 987,
               "message" : "product not found",
               "trace" : []
          }
     }
]
```
