# Publish files to the cloud

Use the `/1/files` endpoint to upload and publish files to the cloud.

Why would you do that?

- to publish a static web site,
- to publish static resources (images, html files, css files, js files, ...)
- to publish a JavaScript app and all its resources,
- to share user contents (photos, pdf, ...) with other users,
- ...

First, set file endpoint settings to set which buckets are accessible to whom. and to grant permissions to user's roles. Use `PUT /1/settings/files` with a body like:

```json
{
  "sizeLimitInKB": 20000,
  "permissions": {
    "www": {
      "all": ["read"],
      "user": ["updateMine", "deleteMine"]  
    }
  }
}
```

Then, to upload and publish files, send `PUT /1/files/{bucket}/{path}/{fileName}` requests for all files to upload. Each file must have a bucket and a file name. The path is optional. 

Example:

```http
PUT /1/files/www/images/pizza.png HTTP/1.1
Host: getmepizza.spacedog.io
Authorization: Basic ZG9jOmhpIGRvYw==
...
```
returns

```json
{
  "success" : true,
  "status" : 200,
  "path" : "/www/images/pizza.png",
  "location" : "https://getmepizza.spacedog.io/1/files/www/images/pizza.png"
}
```

The content type is usually derived from the file name extension. If none, provide a `Content-Type` header with the right value.