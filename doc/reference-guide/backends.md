---
layout: doc
title: Backends
rank: 7
---

#### /1/backend

A new customer must sign up to create his backend on the SpaceDog platform.

##### api.spacedog.io/1/backend/{id}

`POST` creates a new backend. The body is a create backend request JSON object:

```json
{
	"username" : "bob",
	"password" : "*******",
	"email" : "bob@me.com"
}
```

- `id` the backend identifier.

##### {backendId}.spacedog.io/1/backend

`GET` returns the specified backend list of super admins.

- `backendId` the backend identifier.

`DELETE` deletes this backend and all its users, data objects and files.

- `backendId` the backend identifier.