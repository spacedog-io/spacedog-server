# Files

The file endpoint allows users to upload/download files to/from the cloud. Uploaded files will be available to download to authorized users. Uploaded files are identified by a path and a name. Examples: `a/b/c/index.html` or `a/b/c/z`.

---
#### /1/files/{bucket}/{path}

Path Parameters | Description
----------------|------------
bucket          | String. Required. The file bucket. Examples: `www`, `shared`.
path            | String. Optional. The path. Examples: `assets/images`.

**GET** lists all files in the specified bucket and with the specified path prefix. Authorized to users with `search`permission on the specified bucket.

Returns:

```json
{
  "success" : true,
  "status" : 200,
  "next" : "...",
  "results" : [
    {
      "path" : "/www/a/b/index.html",
      "location" : "https://mybackend.spacedog.io/1/files/www/a/b/index.html",
      "size" : 32,
      "lastModified" : "2016-05-25T11:59:51.000+02:00",
      "etag" : "876c3c6474fe1654f31edc0d3e841d82"
    },
    ...
  ]
}
```

Query Parameters | Description
-----------------|------------
next             | String. Optional. Next page token retrieved from last page response hash `next` field.

**DELETE** deletes all files in the specified bucket and with the specified path prefix. Authorized to users with `delete` permission. Returns the list of deleted files. Example:

```http
DELETE /1/file/www HTTP/1.1
Authorization: Basic ZG9jOmhpIGRvYw==
```

returns

```json
{
  "success" : true,
  "status" : 200,
  "deleted" : [
    "/www/app.html",
    "/www/app.js",
    "/www/images/fifi.jpg",
    "/www/images/riri.png"
  ]
}
```

---
#### /1/files/{bucket}/{path}/{name}

Path Parameters | Description
----------------|------------
bucket          | String. Required. The file bucket. Examples: `www`, `shared`.
path            | String. Optional. The path. Examples: `assets/images`.
name            | String. Required. The file name. Examples: `index.html`, `logo.png`.

**GET** retuns the specified file. The response body is a byte array.

Authorized to users with:

- `readMine` permission if owned by user,
- `readGroup` permission if part of user's group,
- `read` permission without any restriction.

Parameters | Description
-------|------------
withContentDisposition | Boolean. Defaults to false. Returns the `Content-Disposition` header to automaticaly save the file to disk upon download.

Returns the following headers:

Headers | Description
-------|------------
Etag | The file MD5 hash.
X-Spacedog-Owner | The file owner's credentials id.
X-Spacedog-Group | The file owner's group id.
Content-Disposition | The name of use to save the file to disk upon download. Only if `withContentDisposition` param is true.
Content-Length | The response body length in bytes only if response is not gziped.

**PUT** uploads a file. The request body is the file byte array. The `Content-Type` of the file is derived from file name extension if any. Otherwise it comes from request header.

Errors | Status | Description
-------|--------|------------
Content-Length is required | 400 | `Content-Length` request header not found.
File too big | 400 | if file size is greater than file settings `sizeLimitInKB` field.

Authorized to users with:

- `updateMine` permission if owned by user,
- `updateGroup` permission if part of user's group,
- `update` permission without any restriction.

Returns:

```json
{
  "success" : true,
  "status" : 200,
  "path" : "/www/a/b/index.html",
  "location" : "https://mybackend.spacedog.io/1/files/www/a/b/index.html",
  "contentType" : "text/html",
  "contentLength" : 132,
  "expirationTime" : "2016-08-25T11:59:51.000+02:00",
  "lastModified" : "2016-05-25T11:59:51.000+02:00",
  "etag" : "876c3c6474fe1654f31edc0d3e841d82"
  "contentMd5" : "876c3c6474fe1654f31edc0d3e841d82"
}
```

**DELETE** deletes the specified file.

Authorized to users with:

- `deleteMine` permission if owned by user,
- `deleteGroup` permission if part of user's group,
- `delete` permission.

---
#### /1/files/{bucket}/_download

**POST** downloads a zip of the specified bucket files. Request body hash looks like this:

```json
{
  "fileName": "download.zip",
  "paths": [
    "/shares/7879-97Y8-986R/file.pdf",
    "/shares/8730-9845-7634/logo.png"
  ]
}
```

Authorized to users with:

- `readMine` permission for files owned by user,
- `readGroup` permission for files part of user's group,
- `read` permission.

---
#### /1/settings/file

**GET** returns file settings.
**PUT** updates file settings.
**DELETE** reset file settings to defaults.

File settings example:

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

Settings | Description
---------|------------
sizeLimitInKB | Long. Defaults to 20,000. File size limit for all buckets.
permissions   | Hash. Defaults to empty hash. Map of bucket/roles/permissions.
permissions.{bucket} | Hash. Map of role/permissions for the specified bucket.
permissions.{bucket}.{role} | List of strings. List of permissions for the specified bucket and role.

- Special role `all` means: any user.
- Bucket permissions are: `readMine`, `readGroup`, `read`, `updateMine`, `updateGroup`, `update`, `deleteMine`, `deleteGroup`, `delete`.
