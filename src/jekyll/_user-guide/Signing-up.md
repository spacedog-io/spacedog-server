---
layout: doc
title: Signing up
rank: 1
---

All API endpoints are only accessible through HTTPS to forbid any forgery.

#### A customer is signing up

A new customer must sign up to create his account on the MagicApps platform. He can do so with the Magic Console sign up page. An account, an app and a user are created. The default app is created with the same id than the account. A customer can also sign up with a `POST /v1/account/` and a customer signing up JSON body:

```json
{
	"id" : "PtiLabs",
	"username" : "ptidenis",
	"password" : "MyNameIsPetitDenis",
	"email" : "ptidenis@ptilabs.com"
}
```

Access any other API endpoints necessitate 2 security headers:

- `X-magic-app-id` with the key of the app you are accessing,
- `Authorization` with a `Basic` scheme using any api key:secret or user username:password pair associated with this app.

To check that the `ptidenis` user has been correctly created when signing up:

```http
GET /v1/user/ptidenis
Authorization: Basic cHRpZGVuaXM6TXlOYW1lSXNQZXRpdERlbmlz
X-magic-app-id: PtiLabs
```

#### An app is signing up a new user

To sign up a new user, an app must `POST /v1/user` with a user sign up JSON:

```json
{
	"username" : "zabou",
	"password" : "MyNameIsZabou",
	"email" : "zabou@z.com"
}
```
