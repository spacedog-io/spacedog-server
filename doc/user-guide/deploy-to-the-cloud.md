---
layout: doc
title: Deploy to the cloud
rank: 5
---

#### Publish files to the cloud

Use the `/1/file` endpoint to upload and publish files to the cloud.

Why would you do that?

- to publish a static web site,
- to publish static resources (images, html files, css files, js files, ...)
- to publish a JavaScript app and all its resources,
- ...

To upload and publish files, send `PUT /1/file` requests for all files. Each file must have a path and a file name. The path can not be empty. Example:

```http
PUT /1/file/www/images/pizza.png HTTP/1.1
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
  "location" : "https://getmepizza.spacedog.io/1/file/www/images/pizza.png",
  "s3" : "https://spacedog-files.s3.amazonaws.com/getmepizza/www/images/pizza.png"
}
```

The content type is usually derived from the file name extension. If none, provide a `Content-Type` header with the right value.

#### Attach a domain name to your cloud resources

You can use your own domain names to serve your files and create a real web site. Contact us by email to help us do so.

#### Share a file

Use the `/1/share` endpoint when the system or a user need to share a single file with others.

Example: send a `PUT` request to https://getmepizza.spacedog.io/1/share/your-invoice.pdf. The request body if the file byte array. It returns the JSON response:

```json
{
  "success" : true,
  "status" : 200,
  "path" : "/3795c4ad-d6b6-48f9-9276-6e3659805b4f/your-invoice.pdf",
  "location" : "https://getmepizza.spacedog.io/1/share/3795c4ad-d6b6-48f9-9276-6e3659805b4f/your-invoice.pdf",
  "s3" : "https://spacedog-shared.s3.amazonaws.com/getmepizza/3795c4ad-d6b6-48f9-9276-6e3659805b4f/your-invoice.pdf"
}
```

Share the location or s3 urls you find in the response to the people you like.

File names can not use to uniquely identify shared files. The identifier is the path you find in the response.