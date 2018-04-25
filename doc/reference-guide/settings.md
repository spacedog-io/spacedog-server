---
layout: doc
title: Settings
rank: 8
---

#### Settings

Use `settings` service to configure SpaceDog internal services but also custom backend specific settings. Each SpaceDog service has the corresponding settings like `mail`, `credentials`, ... Checkout the pages of these different services for details on their specific settings.

A settings resource is a JSON document and has a name. Common SpaceDog insternal settings are `credentials`, `mail`, `sms`, `settings`, `schema`, `share`, `web`, and `stripe`. Here an example of the `sms` settings:

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


##### /1/settings

*GET* returns all the backend settings. Only authorized to superadmins.

Response example:

```json
{
     "mail": {
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

##### /1/settings/{name}

*GET* returns the specified settings. Only authorized to superadmins by default. Can be configured with `settings` settings. See below. For SpaceDog internal settings, you get the defaults even if you didn't update them before.

*PUT* updates the specified settings. Only authorized to superadmins by default. Can be configured with `settings` settings. See below. The request payload should contain the settings JSON document.