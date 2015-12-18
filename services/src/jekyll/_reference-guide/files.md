---
layout: doc
title: Files
---

#### Files

The file API allows developers and apps to upload files and tgz to the cloud.
Uploaded files will be available to download to authorized users.

##### /v1/file/settings

`GET` returns the default file settings.

```json
{
     "default" : {
          "_cdn" : true,
          "_purge" : 786765,
          "_header" : {
               "ttl" : 5645
          },
          "_types" : {
               "html" : "text/html"
               "htm" : "text/html"
               "png" : "image/png"
          }
          "_acl" : {
               "public" : {
                    "read" : true
               },
               "admin" : {
                    "read" : true, "write" : true
               }
          }
     }
}
```

`PUT` updates the default file settings.


##### /v1/file

`GET` lists all uploaded files.

It returns this type of JSON object:

```json
{
     "appId" : "…",
     "fileCount" : 1,
     "files" : [ {
          "id" : "...",
          "name" : "...",
          "location" : "...",
          "content-type" : "..."
     } ]    
}
```

`POST` uploads a file (or an archive) for publication in the cloud.

- `name` –– Optional. The name of the file (or archive), used as a description not an identifier.
- `content-type` –– Optional header defining the type of file. Mandatory if there is no name or no known file extension at the end of the name.
- `request body`
     - a file (with a json setings in multipart?),
     - an archive (a tgz file) containing a tree of files to be published in the cloud. The archive can contain a .settings.json file containing specific settings for this archive.
- returns a JSON object like this one:

```json
{
     "success" : true,
     "id" : "kjnjknkjnjknkjbkjbkjbkjbkjb",
     "location" : "http://files.mgapps.com/myapp/kjnjknkjnjknkjbkjbkjbkjbkjb/me.png"
}
```

If the file uploaded is an archive, the returned location is the base location of the archice file tree. To access a specific file of the file tree, append the file relative path to the base location.


##### /v1/file/{id}

`PUT` updates the specified file or archive.

- `id` –– The file identifier returned by the API when the file was first uploaded. This is not the file name.

`GET` returns the specified file or archive meta informations.

- `content` = [false]/true –– returns the file content instead of file meta informations.

`DELETE` deletes the specified file or archive.


##### /v1/file/{id}/settings

`GET` returns the specified file or archive specific settings.

`PUT` updates the specified file specific settings. Fails if this file is an archive since archive settings must be set in the file .settings.json at the root of the archive tree.

`DELETE` deletes the specified file specific settings. Fails if this file is an archive since archive settings must be set in the file .settings.json at the root of the archive tree.

A file specific settings is equivalent to the default file settings. For archives, you can override settings for specific entries in the archive:

```json
{
     "images.ios" : {
          "_header" : {
               "_ttl" : 98778
          }
     }
}
```


#### URL

You can use your own domain names to serve uploaded files and file trees. The domain names must first be attached to your app. Read this tutorial to learn how to proceed: http://… Use the following API endpoints to attach your files or file trees to a specific domain and URI.

##### /v1/url/{hostname}/{port}/{uri}

`POST/PUT` attach the specified URL to a file or file tree.

- `fileId` –– The identifier of the file or file tree to be served through this URL.

`DELETE` deletes the specified URL attachement to a file or file tree.

`GET` returns the file identifier attached to the specified URL.

```json
{
     "fileId" : "KJHKBFVFDVIYH778TV6RV5CEVK"
}
