---
layout: doc
title: Mail
rank: 10
---

#### Mail

Use this endpoint to send emails.

##### /1/settings/mail

*GET* returns the mail service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.

*PUT* sets the mail service settings. Only authorized to superadmins by default. See [Settings](settings.html) for more information on settings ACL.
Mail settings can be updated with `/1/settings/mail`. Default mail settings:

Default mail settings:

```json
{
  "enableUserFullAccess": false,
  "mailgun": null,
  "smtp": null,
  "templates": null
}
```

MailGun settings example:

```json
{
	"domain": "spacedog.io",
	"key": "..."
}
```

SMTP settings example:

```json
{
	"host": "smtp.spacedog.io",
	"login": "vince@spacedog.io",
	"password": "...",
	"startTlsRequired": true,
	"sslOnConnect": true
}
```

|  Property   | Description                              |
| ----------- | ---------------------------------------- |
| enableUserFullAccess | Boolean. Default to false. Wether users other than admins are allowed to send emails. |
| mailgun | Object. Optional. The mailgun account settings. |
| mailgun.domain | String. Required. The mailgun account verified DNS domain. |
| mailgun.key | String. Required. The mailgun account api key. |
| smtp | Object. Optional. The SMTP settings. |
| smtp.host | String. Required. The SMTP server host. |
| smtp.login | String. Optional. The SMTP server account login. |
| smtp.password | String. Optional. The SMTP server account password. |
| smtp.startTlsRequired | Boolean. Defaults to false. Wether to start a TLS negociation or not. |
| smtp.sslOnConnect | Boolean. Defaults to false. Wether to connect to server with SSL security. |
| tempates | Object. Optional. The mail templates. |


##### /1/mail

*POST* sends an email with the specified parameters. Only authorized to administrators by default. 

|  Parameters   | Description                              |
| ----------- | ---------------------------------------- |
| to | String. Required. List of recipents. |
| cc | String. Optional. List of carbon copy recipents. |
| bcc | String. Optional. List of blind carbon copy recipents. |
| subject | String. Optional. The email subject. |
| text | String. Optional. The email body in text format. |
| html | String. Optional. The email body in HTML format. |

The `from` email property sent through this endpoint is not customizable and is set to `no-reply@api.spacedog.io`.

##### /1/mail/template/{name}

*POST* sends an email using the specified template. Templates are define in mail settings.

Mail template example

|  Parameters   | Description                              |
| ----------- | ---------------------------------------- |
| to | String. Required. List of recipents. |
| cc | String. Optional. List of carbon copy recipents. |
| bcc | String. Optional. List of blind carbon copy recipents. |

