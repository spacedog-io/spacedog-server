---
layout: doc
title: Credentials
rank: 0
---

#### Credentials

Users must have credentials to access a backend, its data and services. Users have standard roles (`user`, `admin`, `super_admin`) and custom app roles. Users must log in with a valid username and password pair provided as regular http basic authorization.

A credentials object has the following public fields:

| Fields                 | Description                              |
| ---------------------- | ---------------------------------------- |
| backendId              | String. Required. The backend of this credentials. |
| username               | String. Required. The username of this credentials used to login. |
| email                  | String. Required. The credentials user email. |
| enabled                | Boolean. Default to true. If false, credentials can no longer be used to access any resource of the backend. |
| enableAfter            | Timestamp. Optional. The timestamp after which this credentials is enabled. |
| disableAfter           | Timestamp. Optional. The timestamp after which this credentials is disabled. |
| roles                  | String list. Defaults to `[user]`. The credentials list of roles. Credentials access to backend services depends on the its roles. |
| invalidChallenges      | Integer. Defaults to 0. The credentials count of invalid password challenges. The sytem counts invalid challenges only if enabled with credentials settings `maximumInvalidChallenges`. |
| lastInvalidChallengeAt | Timestamp. Optional. The timestamp of the last invalid password challenge. The sytem counts invalid challenges only if enabled with credentials settings `maximumInvalidChallenges`. |
| createdAt              | Timestamp. Read only. The credentials creation timestamp. |
| updatedAt              | Timestamp. Read only. The credentials las update timestamp. |


##### /1/login

*POST* checks if the credentials specified by the `Authorization` header is authorized to access the specified backend. Credentials's username and password must be provided through regular http `Basic authorization scheme. It also creates a new user session and returns its access token.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| lifetime   | Long. Optional. The number of seconds the user wants this session to live. Defaults to the credentials settings `sessionMaximumLifetime`. Throw an error if greter than the `sessionMaximumLifetime`. |

Returns:

```json
{
  "success" : true,
  "status" : 200,
  "accessToken" : "NDNlMmEwMjgtNmNmNy00MTQ3LWJlMmMtYzU0YTRiNWFiMDg2",
  "expiresIn" : 86400,
  "credentials" : {
    "id" : "AVlpB0bxQleq3kcG93hv",
    "backendId" : "...",
    "username" : "...",
    "email" : "...",
    "enabled" : true,
    "..." : "..."
    "roles" : [
      "user"
    ],
    "createdAt" : "2017-01-04T11:30:01.712+01:00",
    "updatedAt" : "2017-01-04T11:30:01.822+01:00"
  }
}
```

##### /1/logout

*POST* closes a valid user session. Session access token must be provided through regular http `Bearer` authorization scheme.

##### /1/settings/credentials

*GET* returns the credentials service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.

*PUT* sets the credentials service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.

Default credentials settings:

```json
{
  "disableGuestSignUp": false,
  "usernameRegex": "[a-zA-Z0-9_%@+\\-\\.]{3,}",
  "passwordRegex": ".{6,}",
  "linkedinId": null,
  "linkedinSecret": null,
  "useLinkedinExpiresIn": true,
  "linkedinRedirectUri": null,
  "linkedinFinalRedirectUri": null,
  "sessionMaximumLifetime": 86400,
  "maximumInvalidChallenges": 0,
  "resetInvalidChallengesAfterMinutes": 60
}
```

| Property                           | Description                              |
| ---------------------------------- | ---------------------------------------- |
| disableGuestSignUp                 | Boolean. Default to false. Wether guests are allowed to create credentials |
| usernameRegex                      | String. Defaults to "[a-zA-Z0-9_%@+\\-\\.]{3,}". The regular expression validating usernames. |
| passwordRegex                      | String. Defaults to ".{6,}". The regular expression validating passwords. |
| sessionMaximumLifetime             | Long. Defaults to 86400 (24 hours). The maximum lifetime of session access tokens. In seconds. |
| linkedinId                         | String. Optional. The linkedin oauth account id. |
| linkedinSecret                     | String. Optional. The linkedin oauth account secret. |
| useLinkedinExpiresIn               | Boolean. Defaults to true. Wether to use token lifetime provided by linkedin when using linkedin oauth authentication. |
| linkedinRedirectUri                | String. Optional. The URI linkedin redirects to after linkedin oauth handcheck. |
| linkedinFinalRedirectUri           | String. Optional. The URI linkedin finaly redirects to after linkedin oauth handcheck. |
| maximumInvalidChallenges           | Integer. Defaults to 0. The number of invalid password challenges before credentials are automatically disabled. Set this to 0 to disable this features. |
| resetInvalidChallengesAfterMinutes | Integer (in minutes). Defaults to 60. The number of minutes after the last invalid password challenge before this credentials invalid challenge count is reset to 0. |


##### /1/credentials

*POST* creates the credentials of a new user of the specified backend. Guests are authorized to create credentials if credentials settings `disableGuestSignUp` is false.

The request payload is a sign-up JSON object:

```json
{
	"username" : "roberta",
	"password" : "MyNameIsRoberta",
	"email" : "roberta@me.com"
}
```

The response:

```json
{
  "success" : true,
  "status" : 201,
  "id" : "AVlpMtRpQleq3kcG93ij",
  "type" : "credentials",
  "location" : "http://test.lvh.me:8443/1/credentials/AVlpMtRpQleq3kcG93ij"
}
```

If no password is provided, it returns a password reset code. This is commonly used when credentials are created by administrators. The reset code must be used the credentials's user to set his password. In this case, the response is :

```json
{
  "success" : true,
  "status" : 201,
  "id" : "AVlpMtTTQleq3kcG93im",
  "type" : "credentials",
  "location" : "http://test.lvh.me:8443/1/credentials/AVlpMtTTQleq3kcG93im",
  "passwordResetCode" : "00cee6d8-a718-4ae3-8410-09255d00bed2"
}
```


*GET* returns all credentials.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| username   | String. Optional. Only return credentials with the specified username. |
| email      | String. Optional. Only return credentials with the specified email. |
| from       | Integer. Defaults to 0. Row of the first credentials to return. |
| size       | Integer. Defaults to 10. Maximum Number of credentials to return. |

*DELETE* deletes all backend credentials but superadmins. Only authorized to superadmins.


##### /1/credentials/{id}

*GET* returns the specified credentials.

*PUT* updates the specified credentials. Regular users can update their username, email and password. Only authorized if the password of the user has been challenged i.e. authentication with `Basic` scheme. Administrators can also update the following fields: enabled, enableAfter, disableAfter. 

Request payload exemple :

```json
{
	"username" : "dave",
	"email" : "dave@me.com"
}
```

*DELETE* deletes the specified credentials. A superadmin can't be deleted if he is the last superadmin of the backend.


##### /1/credentials/{id}/password

*DELETE* deletes the specified credentials password. Returns a password reset code to be used when setting the new password. The user can not log in anymore until he sets his password. Only authorized to administrators.

*POST* sets the password of the specified user. The user must not have any password or his previous password must have been deleted. A `passwordResetCode` must be provided.

| Parameters        | Description                              |
| ----------------- | ---------------------------------------- |
| passwordResetCode | The code received when the credentials password has been deleted and allowing you to set a new password. |
| password          | The user's new password.                 |

*PUT* resets the password of the specified credentials with a new password. Only authorized to the user of the specified credentials. The new password is passed in the payload as a JSON string. Example of payload :

```json
"mynewpassword"
```


##### /1/credentials/{id}/enabled

*PUT* enables/disables the specified credentials. Only authorized to administrators.

The request payload must be a JSON boolean and decides if the specified credentials are enabled or disabled. Example of payload :

```json
true
```

##### /1/credentials/{id}/roles

*GET* returns the roles of the specified credentials.

*DELETE* removes all custom roles from the specified credentials. Standard default roles (`user`, `admin` and `super_admin`) can't be removed.

##### /1/credentials/{id}/roles/{role}

*PUT* adds the specified custom role to the specified credentials.

*DELETE* removes the specified custom role from the specified credentials. Standard default roles (`user`, `admin` and `super_admin`) can't be removed.
