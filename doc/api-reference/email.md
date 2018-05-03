# EMail

Use this endpoint to send emails.

---
#### /1/settings/mail

**GET** returns email service settings. Only authorized to superadmins by default. See [Settings](settings.md) for more information on settings ACL.

**PUT** sets email service settings. Only authorized to superadmins by default. See [Settings](settings.md) for more information on settings ACL.

Default mail settings:

```json
{
  "authorizedRoles": [],
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

Property   | Description                              
----------- | ----------------------------------------
authorizedRoles | List of roles (string). Default to empty list. Roles authorized to send to emails.
mailgun | Object. Optional. The mailgun account settings.
mailgun.domain | String. Required. The mailgun account verified DNS domain.
mailgun.key | String. Required. The mailgun account api key.
smtp | Object. Optional. The SMTP settings.
smtp.host | String. Required. The SMTP server host.
smtp.login | String. Optional. The SMTP server account login.
smtp.password | String. Optional. The SMTP server account password.
smtp.startTlsRequired | Boolean. Defaults to false. Wether to start a TLS negociation or not.
smtp.sslOnConnect | Boolean. Defaults to false. Wether to connect to server with SSL security.

---
#### /1/email/templates/{name}

**PUT** creates/updates the specified email template. Request body is required and represent the template to save. Template is a JSON object with the following fields:

Parameters   | Description                            
----------- | ----------------------------------------
`name` | String. Required. Name of the template.
`from` | String. Required. Email address of the sender.
`to` | List of strings (email address). Required. List of recipients.
`cc` | List of strings (email address). Optional. List of carbon copy recipients.
`bcc` | List of strings (email address). Optional. List of blind carbon copy recipients.
`subject` | String. Optional. Email subject line.
`text` | String. Optional. Email body in text format.
`html` | String. Optional. Email body in HTML format.
`model` | Hash. Optional. The model used to render this templated email. The model must contain all fields that will be used to render this templated email.
`authorizedRoles` | List of strings (role). Required. List of roles authorized to send emails via this template.

Template example:

```json
{
	"name": "company-associates"
	"from": "CAREMEN <no-reply@caremen.fr>",
	"to": ["{{to}}"],
	"subject": "You've been associated to {{company.name}}",
	"text": "Hello {{firstname}} {{lastname}}, ...",
	"model": {
		"to": "string",
		"firstname": "string",
		"lastname": "string",
		"company": "company"
	},
	"authorizedRoles": ["operator"]

}
```

**DELETE** deletes the specified email template.

---
#### /1/emails

**POST** sends an email. Request body is required. 2 different type request bodies: basic and templated.

**Basic email request** fields:

Parameters   | Description                            
----------- | ----------------------------------------
`from` | String. Required. Email address of the sender. If platform default email service provider is used, `from` is forced with `no-reply@api.spacedog.io`. Use your own email service provider account to be able to provide your own `from` value.
`to` | List of strings (email address). Required. List of recipients.
`cc` | List of strings (email address). Optional. List of carbon copy recipients.
`bcc` | List of strings (email address). Optional. List of blind carbon copy recipients.
`subject` | String. Optional. Email subject line.
`text` | String. Optional. Email body in text format.
`html` | String. Optional. Email body in HTML format.

The `from` email property sent through this endpoint is not customizable and is set to `no-reply@api.spacedog.io`.

Only users whose role are authorized via email settings are authorized to send emails via basic requests.

**Templated email request** fields

Parameters   | Description                            
----------- | ----------------------------------------
`templateName` | String. Required. Name of the template to use to send this email.
`parameters` | Hash. Optional. Hash of parameters. Parameter is of the form `key: value`. Only parameters predefined in the specified template `model` are authorized. Parameters must have the type predefined in the template `model`. Available types are `string`, `integer`, `long`, `float`, `double`, `boolean`, `object`, `array` or any data type. In the case, parameter is of a data type, say `company`, parameter should contain a company id. The matching company is retrieved from storage and put in template context before rendering.

Only users whose role is authorized by the specified template `authorizedRoles` field are authorized to send emails via this template.

Example of templated email request:

```json
{
	"templateName": "company-associates",
	"parameters": {
		"to": "vince@dog.com"
		"firstname": "Vince",
		"lastname": "Miramond",
		"company": "AWI_RDNJSB9FQmeqhzkZ"
	}
}
```
