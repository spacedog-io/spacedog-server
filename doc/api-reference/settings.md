# Settings

Settings are JSON documents used to configure your backend. Each settings doc has a name.

Predefined settings are used to configure common SpaceDog services. Predefined settings are `credentials`, `email`, `sms`, `settingsacl`, `schema`, `file`, `web`, `push` and `stripe`. Checkout documentation of these different services for details on their specific settings.

Custom settings are not predefined and are specific to your backend.

Example of `sms` settings:

```json
{
    "rolesAllowedToSendSms": null,
    "twilio": {
      "defaultFrom": "+336446...",
      "accountSid": "ACb2a1678e6...",
      "authToken": "8c15546b7e4..."
    },
    "templates": {
      "phone-validation": {
        "from": null,
        "to": "{{to}}",
        "body": "Your validation code is {{code}}",
        "model": {
          "code": "string",
          "to": "string"
        },
        "roles": [
          "validator"
        ]
      }
    }
}
```

---
#### /1/settings

**GET** returns all backend settings. Only authorized to superadmins.

Query Parameters | Description
-----------------|--------------
`from` | Integer. Defaults to 0. Row of the first settings object to return.
`size` | Integer. Defaults to 10. Maximum number of settings objects to return.

Returns:

```json
{
     "email": {
          ...
     },
     "sms": {
          ...
     },
     "credentials": {
          ...
     }
}
```

**DELETE** deletes all backend settings.

---
#### /1/settings/{name}

**GET** returns the specified settings doc. By default authorized to superadmins. Can be configured via `settingsacl` settings with `read` permission. See below.

If doc not found:
- returns 404 for custom settings,
- returns default values for predifined settings.

**PUT** creates/updates the specified settings doc. By default authorized to superadmins. Can be configured via `settingsacl` settings with `update` permission. See below. The request body id required and contains a JSON object.

**DELETE** deletes the specified settings document. By default authorized to superadmins. Can be configured via `settingsacl` settings with `update` permission. See below.

---
#### /1/settings/{name}/{field}


**GET** returns the specified settings field. By default authorized to superadmins. Can be configured via `settingsacl` settings with `read` permission. See below.

If doc not found:
- returns 404 for custom settings,
- returns default field value for predifined settings.

**PUT** updates the specified settings field. Creates settings document if necessary.

By default authorized to superadmins. Can be configured via `settingsacl` settings with `update` permission. See below. The request body id required and contains a JSON object.

**DELETE** null the specified settings field. By default authorized to superadmins. Can be configured via `settingsacl` settings with `update` permission. See below.

---
#### /1/settings/settingsacl

**GET/PUT** get/set all settings documents ACL. By default, settings documents can only be GET/PUT by superadmins. 

Example:

```json
{
  "email" :{
    "user": ["read"],
    "admin": ["read", "update"]
  },
  "appconf": {
    "all": ["read"]
  }
}
```
