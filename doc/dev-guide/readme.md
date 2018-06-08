# Getting started

The **Developer's Guide** is a step by step guide to learn the basics of the SpaceDog backend services using code examples extensively.

The **Reference Guide** is the complete reference documentation of all the SpaceDog backend services, parameters, object structures, ...

---
#### Use a SpaceDog backend

There are 3 ways to provision a SpaceDog backend:

- Run your own SpaceDog backend on your own server. Check out server build/install/run [readme](../../server/readme.md) in the `server` sub project.

- Contact [SpaceDog](https://spacedog.io) by email to get a free fully managed SpaceDog backend in the AWS cloud.

- Use our REST API to provision a free fully managed SpaceDog backend in the AWS cloud. We will get to it in the following...

---
#### Security

SpaceDog services are only accessible over HTTPS (to forbid any forgery). Admin services are accessible from `https://api.spacedog.io`. Backend services from `https://{backendId}.spacedog.io`.

For example, if the GetMePizza iOS application want to get data from its `getmepizza` SpaceDog backend, it will send an HTTPS request to `https://getmepizza.spacedog.io/1/data`:

```http
GET /1/data HTTP/1.1
Host: getmepizza.spacedog.io
```

To access services for authorized users, the request must contain a regular `Authorization` header using `Basic` scheme and the BASE64 encoded username and password of the user.

For example, delete the `pizza` schema (a.k.a. type of data object) is an admin request and need this HTTPS request:

```http
DELETE /1/schema/pizza HTTP/1.1
Host: getmepizza.spacedog.io
Authorization: Basic ZG9jOmhpIGRvYw==
```

---
#### Provision a backend

First you need to sign in with SpaceDog and get credentials. Send this request:

```http
POST /1/credentials
Host: api.spacedog.io

{
	"username": "mario",
	"password": "********",
	"email": "mario@getmepizza.it"
}
```

With your new SpaceDog credentials, your are able to provision a new backend with the request bellow. You need to use your SpaceDog credentials to authenticate this request.

```http
POST /1/backends
Host: api.spacedog.io
Authorization: Basic base64(mario:********)

{
	"backendId": "getmepizza",
	"superadmin": {
		"username": "superadmin",
		"password": "********",
		"email": "mario@getmepizza.it"
	}
}
```

It provisions the `getmepizza` backend and creates superadmin credentials for this backend. To access this backend, send requests to `https://getmepizza.spacedog.io`.

SpaceDog credentials are use to access SpaceDog admin API (https://api.spacedog.io). Backend `getmepizza` credentials are used to access the backend API (https://getmepizza.spacedog.io).


⋮

Next: [Storing and searching data](storing-and-searching-data.md) ⋙