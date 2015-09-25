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
	"email" : "zabou@zz.com"
}
```


Once a user is created, you can use the user's username and password to authenticate his api requests.