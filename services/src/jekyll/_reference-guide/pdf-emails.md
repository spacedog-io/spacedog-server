---
layout: doc
title: PDF & Emails
---

#### PDF

Use the PDF API endpoints to generate PDF files. First upload a PDF template script. Then process the template with one or more data objects as input.

##### /v1/pdf/{id}

`GET` returns the specified pdf template.

`POST/PUT` creates/updates the specified pdf template.

- body: a pdf template script


##### /v1/pdf/{id}/process/{oid},…

`POST` processes the specified pdf template with the provided object identifiers.

The generated pdf is uploaded to the file API. The service returns the file status object.


#### Emails

Use the email API endpoints to generate emails from data objects and send them. First upload en email template script, then process the template with one or more data objects as input.

##### /v1/email/{id}

`GET` returns the specified email template.

`POST/PUT` creates/updates the specified email template.

- body = an email template script


##### /v1/email/{id}/process/{oid},…

`POST` processes the specified email template with the provided object ids. The email is sent. Returns an email status object.