# Getting started

The **Developer's Guide** is a step by step guide to learn the basics of the SpaceDog backend services using code examples extensively.

The **Reference Guide** is the complete reference documentation of all the SpaceDog backend services, parameters, object structures, ...

---
#### Provision your backend

There are 2 ways to provision a SpaceDog backend:

- Run your own SpaceDog backend on your own server. Check out server build/install/run [readme](../../server/readme.md) in the `server` sub project.

- Contact [SpaceDog](https://spacedog.io) by email to get a free fully managed SpaceDog backend in the AWS cloud.

In the following, we consider that your backend is up and running in the SpaceDog cloud. In this case, you get

- administrator username, password and email address,
- backend id to identify your backend.

---
#### Security

SpaceDog services are only accessible over HTTPS (to forbid any forgery). Admin services are accessible from `https://api.spacedog.io`. Backend services from `https://{backendId}.spacedog.io`.

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

Next: [Storing and searching data](storing-and-searching-data.md) ⋙