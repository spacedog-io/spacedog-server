---
layout: doc
title: Getting started
rank: 1
---

#### Sign up

First, sign up to the SpaceDog platform. Provide

- a username and password to be able to access the SpaceDog console and manage your SpaceDog account and application backends,
- an identifier for your application to identify all api requests to your application backend,
- an email for account notifications.

Once signed up, you can 

- access the console to manage your account and applicaton backends,
- use the SpaceDog web services described in this documentation to implement your applications.

#### Security

SpaceDog web services are only accessible through HTTPS to forbid any forgery.

Get your app secret in your [console](/console.html). Add the following security headers to all your service requests:

- `X-spacedog-app-id` with the id of the SpaceDog app you are accessing,
- `Authorization` with a `Basic` scheme using a base 64 encoded app id and secret pair.

For example, check if your app contains any data:

```http
GET /v1/data/
Authorization: Basic cHRpZGVuaXM6TXlOYW1lSXNQZXRpdERlbmlz 
X-spacedog-app-id: getmepizza
```
