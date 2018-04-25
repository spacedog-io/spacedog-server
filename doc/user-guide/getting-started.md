---
layout: doc
title: Getting started
rank: 1
---

#### Getting started


The **User's Guide** is a step by step guide to learn the basics of the SpaceDog backend services using code examples extensively.

The **Reference Guide** is the complete reference documentation of all the SpaceDog backend services, parameters, object structures, ...

#### Sign up

First, [sign up](https://cockpit.spacedog.io/sign-up.html) for free to the SpaceDog platform. Provide

- the backend administrator username, password and email address,
- the backend id to identify your backend. A valid id is at least 4 characters long, only composed of a-z and 0-9 characters, lowercase and does not start with 'api' nor 'spacedog'.

A backend id must be unique over all the SpaceDog registered backends. If you get the error `backend id not available`, try another one.

Once signed up, the administrator is granted access to administration cockpit and can manage backend settings and data.

#### Security

SpaceDog services are only accessible over HTTPS (to forbid any forgery). Admin services are accessible from `api.spacedog.io`. Backend services from `{backendId}.spacedog.io`.

For example, if the GetMePizza iOS application want to get data from its `getmepizza` SpaceDog backend, it will send an HTTPS request to `https://getmepizza.spacedog.io/1/data`:

```http
GET /1/data HTTP/1.1
host: getmepizza.spacedog.io
```

To access services for authorized users, the request must contain a regular `Authorization` header using `Basic` scheme and the BASE64 encoded username and password of the user.

For example, delete the `pizza` schema (a.k.a. type of data object) is an admin request and need this HTTPS request:

```http
DELETE /1/schema/pizza HTTP/1.1
host: getmepizza.spacedog.io
Authorization: Basic ZG9jOmhpIGRvYw==
```

⋮

Next: [Storing and searching data](storing-and-searching-data.html) ⋙