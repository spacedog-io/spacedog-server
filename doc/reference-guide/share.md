---
layout: doc
title: Share
rank: 5
---

#### /1/share

The share endpoint provides a way to share files with other users. Endpoint ACL are managed via `share` settings.

##### {backendId}.spacedog.io/1/share

`GET` lists all the specified backend shared files. Only authorized to administrators.

`DELETE` deletes all the specified backend shared files. Only authorized to administrators.

##### {backendId}.spacedog.io/1/share/{filename}

`PUT` uploads a file for sharing. Users are authorized depending on ACL settings.

If file size is over 10 MB or `delay` is true, returns an `uploadTo` location for direct upload to S3. In this case, the client code is responsible for upload via PUT of the file on this location with the right `Content-Length` header.

Parameters | Param type | Description
-----------|------------|------------
`filename` | Route | String. Required. The file name. Used to derive the share identifier and the content type. Also used to provide a name when downloading the file from a web browser.
`delay` | Query | Boolean. Defaults to `false`. If true, force a response containing an `uploadTo` location to directly upload file to S3.
`Content-Length` | Header | Long. Required. The file size in bytes.
`Content-Type` | Header | String. Defaults to `application/octet-stream`. The format of file. If not set, the file name extension if present is used to derive the content type. 
Body | Payload | The file byte array.

##### {backendId}.spacedog.io/1/share/{id}/{filename}

`GET` retuns the specified shared file binary.

Parameters | Description
-------|------------
id | The share identifier.
filename | The file name.
withContentDisposition | Boolean. Defaults to false. Returns the `Content-Disposition` header to automaticaly save the file to disk upon download.

The following headers are returned:

Headers | Description
-------|------------
Etag | The file MD5 hash.
X-spacedog-owner | The file owner's username.
Content-disposition | Optional. The name of use to save the file to disk upon download.

`DELETE` deletes the specified shared file. Only authorized to administrators or the owner of the file.

Parameters | Description
-------|------------
id | The share identifier.
filename | The file name.
