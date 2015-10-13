---
layout: doc
title: Deploy to the cloud
rank: 5
---

#### The file endpoint [Not yet implemented]

Use the `/v1/file` endpoint to upload and publish files and file sets to the cloud.

Why would you do that?

- to publish and manage static resources (images, html files, css files, js files, ...)
- to publish a JavaScript app and all its resources,
- to upload and store user files,
- to store app generated reports and files,
- ...

#### Publish a tgz file [Not yet implemented]

First create tgz archive, call it `myapp.tgz`:

```
/
|---- .settings.json
|---- index.html
|---- main.css
|---- main.js
|---- /images
|     |
|     |---- logo.png
|     |---- ...
|---- ...
```

Then send a `PUT v1/file/getmepizza` where `getmepizza` is file tree identifier and with the tgz archive as multi part body and a `Content-type` header set to `???/???`. It returns a classic status JSON:

```json
{
  "success" : true,
  "id" : "getmepizza",
  "type" : "filetree",
  "location" : "https://static.spacedog.io/{backend-id}/getmepizza"
}
```

The returned location is the base location of the archive file tree. To access a specific file of the file tree, append the file relative path to the base location:

```http
https://static.spacedog.io/{backend-id}/getmepizza/index.png
https://static.spacedog.io/{backend-id}/getmepizza/images/logo.png
```

#### Add a settings to your tgz file [Not yet implemented]

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
- `_cdn` if true, files are accessed through a CDN for better performance. Default to false.
- `_purge` is the duration in milliseconds before this archive is purged since upload. O means no purge. Default is 0.
- `_headers` is the list of headers to set when serving this archive files.
- `_types` maps file extensions to content types. This allow the `Content-type` header to be automatically set when serving this archive files.
- `_acl` contains access control settings. Equivalent to data objects access control settings in schema.
- `images` contains the specific file settings for the `images` file tree. Do the same for all archive file or file tree that need specific settings. Specific and default settings are merged with specific overriding default fields.


#### Default file settings [Not yet implemented]

The system provides a default file settings JSON. When serving files, the system merges default and specific settings (if any) an archive might provided. The specific settings have precedence.

To get the default file settings, send a `GET /v1/file/settings` request. To update it, send a `PUT /v1/file/settings` request with a body set to the updated settings JSON.


#### Publish a single file [Not yet implemented]

To upload and publish a single file, send a `POST /v1/file` with

- a file as multi part body,
- an optional `filename` query param,
- an optional `Content-type` header with the right value Mandatory if there is no filename or no known file extension at the end of the filename.

Filenames are not use to uniquely identify uploaded files. A unique generated identifier is attached to all uploaded files or archives. Use filenames to help humans debug or manage since filenames are visible in file locations.

It should return:

```json
{
  "success" : true,
  "id" : "hjgfv1178678658khguf-{filename}",
  "type" : "file",
  "location" : "https://static.spacedog.io/{backend-id}/hjgfv1178678658khguf-{filename}"
}

```

#### Attach a specific URL to a file or file tree [Not yet implemented]

You can use your own domain names to serve uploaded files and file trees. The domain names must first be attached to your account with help of the SpaceDog administration console.

Then to attach your file or file tree to a specific domain and URI, send a `PUT /v1/url/{hostname}/{port}/{uri}` request with a `fileId` query param set the the uploaded file/archive identifier. `port` and `uri` can be empty. It returns the following JSON:

```json
{
     "success" : true,
     "id" : "lkjhhg98776jkhkgvhgcftyc9078T",
     "location" : "https://{hostname}:{port}/{uri}"
}
```

⋮

Next: [Configurations and contents](configurations-and-contents.html) ⋙