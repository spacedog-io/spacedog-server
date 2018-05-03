# Credentials

Users must have credentials to access a backend, its data and services. Users have standard roles (`user`, `admin`, `superadmin`) and custom app roles. Users must log in with a valid username and password pair provided as regular http `Basic` authorization.

Credentials object public fields:

| Fields                 | Description                              |
| ---------------------- | ---------------------------------------- |
| username               | String. Required. The username of this credentials used to login. Username must be unique among all credentials. |
| email                  | String. Required. The credentials user email. |
| enabled                | Boolean. Default to true. If false, credentials can no longer be used to access any resource of the backend. |
| enableAfter            | Timestamp. Optional. The timestamp after which this credentials is enabled. |
| disableAfter           | Timestamp. Optional. The timestamp after which this credentials is disabled. |
| roles                  | String list. Defaults to `[user]`. The credentials list of roles. Credentials access to backend services depends on roles. |
| passwordMustChange      | Boolean. Defaults to false. If true, user must update his password. |
| invalidChallenges      | Integer. Defaults to 0. Count of invalid password challenges. The sytem counts invalid challenges only if enabled with credentials settings `maximumInvalidChallenges`. |
| lastInvalidChallengeAt | Timestamp. Optional. Timestamp of the last invalid password challenge. The sytem counts invalid challenges only if enabled with credentials settings `maximumInvalidChallenges`. |
| createdAt              | Timestamp. Read only. The credentials creation timestamp. |
| updatedAt              | Timestamp. Read only. The credentials last update timestamp. |

---
#### /1/login

**POST** checks if the credentials specified by the `Authorization` header is authorized to access this backend. Credentials's username and password must be provided through regular http `Basic` authorization scheme. It also creates a new user session and returns its access token.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| lifetime   | Long. Optional. Number of seconds the user wants this session to live. Defaults to the credentials settings `sessionMaximumLifetime`. Throw an error if greater than the `sessionMaximumLifetime`. |

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
    "...": "...",
    "roles" : [
      "user"
    ],
    "createdAt" : "2017-01-04T11:30:01.712+01:00",
    "updatedAt" : "2017-01-04T11:30:01.822+01:00"
  }
}
```

---
#### /1/logout

**POST** closes a valid user session. Session access token must be provided through regular http `Bearer` authorization scheme.

---
#### /1/settings/credentials

**GET** returns the credentials service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.

**PUT** sets the credentials service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.

Credentials settings example:

```json
{
  "guestSignUpEnabled": false,
  "usernameRegex": "[a-zA-Z0-9_%@+\\-\\.]{3,}",
  "passwordRegex": ".{6,}",
  "linkedin": {
    "backendUrl": "...",
    "clientId": "...",
    "clientSecret": "...",
    "useExpiresIn": true,
    "finalRedirectUri": "..."
  },
  "sessionMaximumLifetime": 86400,
  "maximumInvalidChallenges": 0,
  "resetInvalidChallengesAfterMinutes": 60
}
```

| Settings                           | Description                              |
| ---------------------------------- | ---------------------------------------- |
| guestSignUpEnabled                 | Boolean. Defaults to false. Wether guests are allowed to create credentials. |
| usernameRegex                      | String. Defaults to "[a-zA-Z0-9_%@+\\-\\.]{3,}". The regular expression validating usernames. |
| passwordRegex                      | String. Defaults to ".{6,}". The regular expression validating passwords. |
| sessionMaximumLifetime             | Long. Defaults to 86400 (24 hours). The maximum lifetime of session access tokens. In seconds. |
| linkedin.backendUrl                | String. Optional. The linkedin oauth backend url. |
| linkedin.clientId                  | String. Optional. The linkedin oauth account id. |
| linkedin.clientSecret              | String. Optional. The linkedin oauth account secret. |
| linkedin.useExpiresIn               | Boolean. Defaults to true. Wether to use token lifetime provided by linkedin when using linkedin oauth authentication. |
| linkedin.finalRedirectUri          | String. Optional. The URI linkedin finaly redirects to after linkedin oauth handcheck. |
| maximumInvalidChallenges           | Integer. Defaults to 0. The number of invalid password challenges before credentials are automatically disabled. Set this to 0 to disable this features. |
| resetInvalidChallengesAfterMinutes | Integer (in minutes). Defaults to 60. The number of minutes after the last invalid password challenge before this credentials invalid challenge count is reset to 0. |

---
#### /1/credentials

**POST** creates user credentials. Guests are authorized to create credentials if credentials settings `guestSignUpEnabled` is true.

Request payload example:

```json
{
	"username" : "roberta",
	"password" : "MyNameIsRoberta",
	"email" : "roberta@me.com"
}
```

Returns:

```json
{
  "id" : "AVlpMtRpQleq3kcG93ij",
  "type" : "credentials",
  "location" : "http://mybackend.spacedog.io/1/credentials/AVlpMtRpQleq3kcG93ij"
}
```

If no password is provided, it returns a password reset code. This is commonly used when credentials are created by administrators. The reset code must be used to set user's password. In this case, the response is:

```json
{
  "id" : "AVlpMtTTQleq3kcG93im",
  "type" : "credentials",
  "location" : "http://mybackend.spacedog.io/1/credentials/AVlpMtRpQleq3kcG93ij"
  "passwordResetCode" : "00cee6d8-a718-4ae3-8410-09255d00bed2"
}
```


**GET** returns all credentials. Authorized to admins.

| Parameters | Description                              |
| ---------- | ---------------------------------------- |
| q   | String. Optional. Filter credentials with this query phrase using *elasticsearch* [Simple Query String](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-simple-query-string-query.html) search. |
| from       | Integer. Defaults to 0. Row of the first credentials to return. |
| size       | Integer. Defaults to 10. Number of credentials to return. |

Example:

```http
GET /1/credentials?q=dave
```

**DELETE** deletes all credentials but superadmins. Only authorized to superadmins.

---
#### /1/credentials/{id}

**GET** returns the specified credentials.

**PUT** updates the specified credentials. Only the following credentials fields can be updated this way:

| Fields        | Update rules                             |
| ------------- | ---------------------------------------- |
| username      | Is unique among all credentials. Password must be challenged (i.e. authentication with `Basic` scheme). |
| email         | Password must be challenged (i.e. authentication with `Basic` scheme). |
| enabled       | Only updatable by administrators. |
| enableAfter   | Only updatable by administrators. |
| disableAfter  | Only updatable by administrators. |

Request payload exemple :

```json
{
	"username" : "dave",
	"email" : "dave@me.com"
}
```

Authorized to credentials owner. Administrators can update users. Super administrators can update regular and super administrators.

**DELETE** deletes the specified credentials. Authorized to credentials owner. Administrators can delete users. Super administrators can delete regular and super administrators. Superadmin can't be deleted if last superadmin of the backend.

---
#### /1/credentials/me

`me` is a valid `id` alias to credentials id of the authenticated user of the request.

---
#### /1/credentials/_send_password_reset_email

**POST** sends an email to the specified credentials email address with a reset password link. Credentials password and tokens are still functional at the end of the request invocation. Request body hash contains at least the username of credentials for which password is forgotten. Email is generated via email template named `password_reset_email_template`. See [email templates](/mail.md). No default template is defined.

Hash might contain other email template parameters. The following 4 string parameters are required: 

| Parameters    | Description                              |
| ------------- | ---------------------------------------- |
| username      | String. Username of credentials for which password has been forgotten. |
| credentialsId | String. Id of credentials for which password has been forgotten. |
| to            | String. Email address to send reset password email to. |
| passwordResetCode | String. Password reset code to be used to generate a password reset link. |

Example of `password_reset_email_template` template:

```json
{
  "name": "password_reset_email_template",
  "from": "no-reply@...",
  "to": ["{{to}}"],
  "subject": "Please reset your password ...",
  "text": "https://myapp.mydomain.com/reset_password?credentialsId={{credentialsId}}&reset_code={{passwordResetCode}}",
  "model": {
    "username": "string",
    "credentialsId": "string",
    "to": "string",
    "passwordResetCode": "string"
  },
  "authorizedRoles": ["all"]
}

```

---
#### /1/credentials/{id}/_reset_password

**POST** deletes the specified credentials password. Returns a password reset code to be used when setting the new password. The user can not log in anymore until he sets a new password. Only authorized to administrators. Reset code is returned in the response hash `passwordResetCode` field. 

---
#### /1/credentials/{id}/_set_password

**POST** sets the password of the specified credentials. Clears any sessions and access tokens. Request hash contains the following fields:

| Fields        | Description                              |
| ----------------- | ---------------------------------------- |
| password          | String. Required. User's new password. |
| passwordResetCode | String. Optional. The password reset code necessary to set this user's password. Reset code is generated after `_reset_password` or `_send_password_reset_email` invocation. |

Authorized to credentials owner. Administrators can update users. Super administrators can update regular and super administrators. If reset code is provided, request doesn't need authentication.

---
#### /1/credentials/{id}/_password_must_change

**POST** flag the specified credentials password to be expired. User can not access his backend anymore with his password but to update his expired password with a new one. See `_set_password` route. Authorized to administrators. No body required.

---
#### /1/credentials/{id}/_enable

**POST** enables the specified credentials. Only authorized to administrators.

---
#### /1/credentials/{id}/_disable

**POST** disables the specified credentials. Only authorized to administrators.

---
#### /1/credentials/{id}/roles

**GET** returns the roles of the specified credentials. Authorized to credentials owner. Administrators can update users. Super administrators can update regular and super administrators.

**DELETE** removes all custom roles from the specified credentials. Administrators can delete user's roles. Super administrators can delete regular and super administrators roles.

---
#### /1/credentials/{id}/roles/{role}

**PUT** adds the specified role to the specified credentials. Administrators can add roles to users and other administrators. Super administrators can add roles to users and other regular and super administrators.

**DELETE** removes the specified role from the specified credentials. Administrators can add roles to users and other administrators. Super administrators can add roles to users and other regular and super administrators.
