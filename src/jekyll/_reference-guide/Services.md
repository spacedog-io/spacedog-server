---
layout: doc
title: Services
---

#### Custom backend services

Use this API endpoints when you need to deploy some business logic in your backend. This is usually the case when you need to interact a lot with the data store without the performance constraint of getting all in the client.

You also need services if you want to:
- create scheduled jobs,
- create queues for message async processing,
- add business logic when data objets are saved or deleted.


##### /v1/service/{id}

`POST/PUT` creates/updates the specified service.

- `id` –– the service unique identifier.
- request body –– The service script.
- `content-type` header –– the script content type.

`GET`returns the script of the specified service.

`DELETE` deletes the specified script. If the service is curently in use, deletion occurs when the service terminates. In the meantime, requests of this service are denied with a `NOT FOUND` HTTP status.


##### /v1/service/{id}/run

`POST` runs the specified service.

- `id` –– the service identifier.
- request body –– a JSON object containing the params passed to the service.
