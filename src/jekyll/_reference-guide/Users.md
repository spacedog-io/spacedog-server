---
layout: doc
title: Users
---

#### Application users

Account's users represent all the people that can log in and access its apps. A user can sign up to a specific app if a valid pair of app key and secret are provided as basic authorization.

##### /v1/user

`POST` signs up a new user to the specified app.

- `X-spacedog-backend-key` header contains the app key the specified user wants to sign up to.
- `Authorization` header with a valid app key and secret in `Basic` scheme.
- `Body` a user sign-up JSON object:

```json
{
	"username" : "roberta",
	"password" : "MyNameIsRoberta",
	"email" : "roberta@me.com"
}
```

`GET` returns all users.

##### /v1/user/{id},...

`GET` returns the specified users.

- `id, ...` –– One or more of user identifiers.

`PUT` updates the specified users.

- `id, ...` –– One or more of user identifiers.
- request body –– One or an array of user JSON objects.

`DELETE` deletes the specified users.

- `id, ...` –– One or more of user identifiers.

##### /v1/login

`GET` checks if the header specified user is authorized to access the header specified app.
