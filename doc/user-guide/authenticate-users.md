---
layout: doc
title: Authenticate users
rank: 4
---

#### Create a new user

If your application has a sign up form to allow users to create an account, your app can create users by sending `POST /1/user` with a user sign up JSON:

```json
{
	"username" : "bob",
	"password" : "*****",
	"email" : "bob@dog.com"
}
```

Once a user is created, you can authenticate requests with an `Authorization` header using `Basic` scheme and providing the user username and password, BASE64 encoded.

Example:

```http
GET /1/data HTTP/1.1
Authorization: Basic ZG9jOmhpIGRvYw==
```

#### Log in a user

To log in a user, send a `GET /1/login` with an `Authorization` header using `Basic` scheme. Log in is not mandatory since, all requests are authenticated the same way. It is a convenient method to check whether the user credentials are valid or not before going further.

⋮

Next: [Deploy to the cloud](deploy-to-the-cloud.html) ⋙