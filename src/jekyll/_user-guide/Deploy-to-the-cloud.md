---
layout: doc
title: Deploy to the cloud
rank: 4
---

Use the `/v1/file` endpoint to upload and publish files and file sets to the cloud.

Why would you do that?

- to publish and manage static resources,
- to publish a single page js app and all its resources,
- to upload and store user files,
- to store app generated reports and files,
- ...

##### Publish a tgz file

First create tgz archive with the right file tree, call it `myapp.tgz`:

```
/myapp
|
|---- .settings.json
|---- main.js
|---- /images
|     |
|     |---- logo.png
|     |---- ...
|---- ...
```

Then send a `POST v1/file` with the tgz archive as multi part body and a `Content-type` header set to `???/???`. It returns a classic status JSON:

```json
{
  "success" : true,
  "id" : "lkjhgk876lklvfc67445hvbbljcdcgjm",
  "location" : "https://files.magicapps.com/{x-magic-app-id}/lkjhgk876lklvfc67445hvbbljcdcgjm/myapp"
}
```

The returned location is the base location of the archive file tree. To access a specific file of the file tree, append the file relative path to the base location:

```http
https://files.magicapps.com/{x-magic-app-id}/lkjhgk876lklvfc67445hvbbljcdcgjm/myapp/main.js
https://files.magicapps.com/{x-magic-app-id}/lkjhgk876lklvfc67445hvbbljcdcgjm/myapp/images/logo.png
```

##### Add a settings to your tgz file

To change the way uploaded file sets are served, add a `.settings.json` file at the root of the tgz archive. Example:

```json
{
     "_default" : {
          "_cdn" : true,
          "_purge" : 786765,
          "_headers" : {
               "ttl" : 5645
          },
          "_types" : {
               "js" : "application/js",
               "png" : "image/png"
          },
          "_acl" : {
               "public" : {
                    "read" : true
               },
               "admin" : {
                    "read" : true, "write" : true
               }
          }
     },
     "images" : {
          "_headers" : {
               "ttl" : 98778
          }
     }
}
```

- `_default` contains the default file settings for this archive.
- `_cdn` If true, files are accessed through a CDN for better performance. Default to false.
- `_purge` Duration in milliseconds before this archive is purged since upload. O means no purge. Default is 0.
- `_headers` List of headers to set when serving this archive files.
- `_types` maps file extensions to content types. This allow the `Content-type` header to be automatically set when serving this archive files.
- `_acl` contains access control settings. Equivalent to data objects access control settings in schema.
- `images` contains the specific file settings for the `images` file tree. Do the same for all archive file or file tree that need specific settings. Specific and default settings are merged with specific overriding default fields.


##### Default file settings

The system provides a default file settings JSON. When serving files, the system merges default and specific settings (if any) an archive might provided. The specific settings have precedence.

To get the default file settings, send a `GET /v1/file/settings` request. To update it, send a `PUT /v1/file/settings` request with a body set to the updated settings JSON.


##### Publish a single file

To upload and publish a single file, send a `POST /v1/file` with

- a file as multi part body,
- an optional `filename` query param,
- an optional `Content-type` header with the right value Mandatory if there is no filename or no known file extension at the end of the filename.

Filenames are not use to uniquely identify uploaded files. A unique generated identifier is attached to all uploaded files or archives. Use filenames to help humans debug or manage since filenames are visible in file locations.


##### Attach a specific URL to a file or file tree

You can use your own domain names to serve uploaded files and file trees. The domain names must first be attached to your app with the help of the Magic Console.

Then to attach your file or file tree to a specific domain and URI, send a `PUT /v1/url/{hostname}/{port}/{uri}` request with a `fileId` query param set the the uploaded file/archive identifier. `port` and `uri` can be empty. It returns the following JSON:

```json
{
     "success" : true,
     "id" : "lkjhhg98776jkhkgvhgcftyc9078T",
     "location" : "https://{hostname}:{port}/{uri}"
}
```
