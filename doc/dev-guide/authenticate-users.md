### Create user credentials

If your application has a sign up form to allow users to create an account, you first need to authorized guest to sign up. Set credentials settings `guestSignUpEnabled` to true by sending `PUT /1/settings/credentials/guestSignUpEnabled` with a body = `true`.

Then your app can create credentials by sending `POST /1/credentials` with a new credentials request JSON:

```json
{
	"username" : "bob",
	"password" : "*****",
	"email" : "bob@dog.com"
}
```

Once credentials are created, you can authenticate requests with an `Authorization` header using `Basic` scheme and providing the credentials username and password, BASE64 encoded.

Example:

```http
GET /1/data HTTP/1.1
Authorization: Basic ZG9jOmhpIGRvYw==
```

### Login

For user to login, send a `POST /1/login` with an `Authorization` header using `Basic` scheme. Login is not mandatory since all requests can be authenticated with `Basic` scheme. But a login request returns an session access token that can be used to authenticate request with `Bearer` scheme. This way, client app can forget user password and only store the session access token in local storage. Access tokens lifetime depends on back-end credentials settings.

⋮

Next: [Deploy to the cloud](deploy-to-the-cloud.md) ⋙