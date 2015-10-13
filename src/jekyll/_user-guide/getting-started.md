---
layout: doc
title: Getting started
rank: 1
---

#### Getting started

Documentation is divided into 

- The User's Guide: a step by step guide to learn the basics of the SpaceDog backend services using code examples extensively.
- The Reference Guide: the complete reference documentation of all the SpaceDog backend services, parameters, object structures, ...

#### Sign up

First, [sign up](/console.html) to the SpaceDog platform. Provide

- the account administrator username, password and email address,
- the backend id wich will identify all API requests to this backend.

Once signed up, the administrator is granted access to administration console and can manage account settings and backend data. The console also provides the backend API keys used to access the SpaceDog API. See below.

#### Security

SpaceDog **application** services are only accessible over HTTPS (to forbid any forgery) to applications providing a `x-spacedog-backend-key` header with the key an account administrator can get from the SpaceDog [administration console](/console.html).

For example, if the GetMePizza iOS application want to get data from its `getmepizza` SpaceDog backend, it will send an HTTPS request like this one:

```http
GET /v1/data/
X-spacedog-backend-key: getmepizza:default:d738739b-f130-49d0-9d01-ba84b4910ddb
```

SpaceDog **admin** services are also accessible over HTTPS but does not need any `x-spacedog-backend-key` header. It needs a regular `Authorization` header with a `Basic` scheme and an administrator base64 encoded login and password pair.

For example, delete the `pizza` schema (a.k.a. type of data object) is an admin type of request and need this HTTPS request:

```http
DELETE /v1/schema/pizza
Authorization Basic ZG9jOmhpIGRvYw==
```

Be careful not to mix backend key and admin authorization.

⋮

Next: [Storing and searching data](storing-and-searching-data.html) ⋙