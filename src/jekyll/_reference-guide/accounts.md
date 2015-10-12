---
layout: doc
title: Accounts
---

#### SpaceDog Accounts

A new customer must sign up to create his account on the SpaceDog platform. An app is automatically created with an id equal to the account id. This can be edited in the admin console.

##### /v1/account/

`POST` signs up a new customer. It creates account, user and app for the specified customer.

- `body` An customer sign up JSON object:

```json
{
	"accountId" : "PtiLabs",
	"username" : "ptidenis",
	"password" : "MyNameIsPetitDenis",
	"email" : "ptidenis@ptilabs.com"
}
```

`GET` returns all customer accounts.


##### /v1/account/{id}

`GET` returns the specified account.

`PUT` updates the specified account.

- `body` An account JSON object.

`DELETE` deletes the account and all the objects associated to this account.
