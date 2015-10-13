---
layout: doc
title: Authenticate users
rank: 4
---

#### Create a new user

If your application has a sign up form to allow users to create an account, your app can create users by sending `POST /v1/user` with a user sign up JSON:

```json
{
	"username" : "zabou",
	"password" : "hi zabou",
	"email" : "zabou@dog.io"
}
```

Once a user is created, you can authenticate his backend requests with an `Authorization` header with a `Basic` scheme using his username and password base 64 encoded. Even if requests are authenticated this way, you need to provide the backend key header.

Example:

```http
GET /v1/data/
X-spacedog-backend-key: getmepizza:default:d738739b-f130-49d0-9d01-ba84b4910ddb
Authorization Basic ZG9jOmhpIGRvYw==
```

#### Login a user

To login a user, send a `GET /v1/login` with the backend key header and the `Authorization` header. Login is not mandatory since, all requests are authenticated the same way. It is a convenient method to check whether the user credentials are valid or not before going further.

⋮

Next: [Deploy to the cloud](deploy-to-the-cloud.html) ⋙