---
layout: doc
title: Files
rank: 4
---

#### /1/file

The file endpoint allows developers and apps to upload/download files to/from the cloud.
Uploaded files will be available to download to authorized users. Uploaded files are identified by a path and a file name. Examples: `a/b/c/index.html` or `a/b/c/z`.

##### {backendId}.spacedog.io/1/file/{path}

Path params | Description
-------|------------
path | The path prefix. Can be empty.

`GET` lists all files with the specified path prefix. Example of JSON response:

```json
{
  "success" : true,
  "status" : 200,
  "results" : [
    {
      "path" : "/www/a/b/index.html",
      "size" : 32,
      "lastModified" : "2016-05-25T11:59:51.000+02:00",
      "etag" : "876c3c6474fe1654f31edc0d3e841d82"
    },
    ...
  ]
}
```

`DELETE` deletes all the files with this path prefix. Only authorized to administrators. Returns the list of deleted file paths. Example:

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

##### {backendId}.spacedog.io/1/file/{path}/{filename}

Path Parameters | Description
-------|------------
path | The file path.
filename | The file name.

`GET` retuns the specified file. The response body is a byte array.

Parameters | Description
-------|------------
withContentDisposition | Boolean. Defaults to false. Returns the `Content-Disposition` header to automaticaly save the file to disk upon download.

Returns the following headers:

Headers | Description
-------|------------
Etag | The file MD5 hash.
X-spacedog-owner | The file owner's username.
Content-disposition | Optional. The name of use to save the file to disk upon download.

`PUT` uploads a file. The request body is the file byte array. Only authorized to administrators.

Parameters | Description
-------|------------
Content-type | Optional. Header defining the type of file. If this header is not set, the file extension if present is used to derive the content type. Defaults to `application/octet-stream`.

`DELETE` deletes the specified file. Only authorized to administrators.
