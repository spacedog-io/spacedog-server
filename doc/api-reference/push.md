# Push

Use this endpoint to push notifications to mobile apps.

---
#### /1/settings/push

**GET** returns push service settings. Only authorized to superadmins by default. See [Settings](settings.md) for more information on settings ACL.

**PUT** sets push service settings. Only authorized to superadmins by default. See [Settings](settings.md) for more information on settings ACL.

Default push settings:

```json
{
  "authorizedRoles": []
}
```

Property   | Description                              
----------- | ----------------------------------------
authorizedRoles | List of roles (string). Default to empty list. Roles authorized to push notifications.

---
#### /1/push/applications/{appId}/{protocol}

**PUT** sets the credentials of the specified application and push protocol. Available push protocols are APNS, APNS_SANDBOX and GCM. The request body JSON object must contain the `principal` and `credentials` fields specific to the specified push protocol. Authorized to administrators.

For example:

```http
PUT https://caremen.spacedog.io/1/push/applications/driver/APNS
```

with body:

```json
{
  "principal" : "-----BEGIN CERTIFICATE-----\nMIIGVzC...JkjhGHGF\n-----END CERTIFICATE-----",
  "credentials" : "-----BEGIN PRIVATE KEY-----\nMIIEvAIBA...VIqy6fJA==\n-----END PRIVATE KEY-----"
}
```

sets the APNS push credentials of an application with id `caremen-driver` (`caremen` is the backend id and `driver` is the app name).

**DELETE** deletes the credentials for the specified application and push protocol.

---
#### /1/push/installations

To play with installation objects, first init `installation` schema. Default schema is created with the following request and an empty body:

```http
PUT https://caremen.spacedog.io/1/schema/installation

```

To use a custom `installation` schema, init default, retrieve and update schema with your changes.

**POST** creates a new installation. Request body must be:

```json
{
  "token" : "...",
  "appId" : "...",
  "protocol" : "...",
  "badge": 0,
  "tags" : [
    "tag1",
    "tag2",
    "..."
  ]
}
```

Field       | Description                             
----------- | ----------------------------------------
token       | The token provided by the device to push notifications to this app.
appId       | The id of the app to push to.
protocol    | The protocol to use to push to this device. Valid values are APNS, APNS_SANDBOX, GCM.
tags        | An array of tags used to select matching installations to push to. By convention, tags are strings of the form "key:value".

If an installation is posted with credentials of a valid user, the new installation is owned by this user like any data objects.

---
#### /1/push/installations/{id}

*PUT* updates the specified installation.

---
#### /1/data/installation/...

Installation objects are *data* objects. All data service routes are valid:

- *GET* /1/data/installation to list installation objects.
- *GET* /1/data/installation/{id} to get the specified installation object.
- *DELETE* /1/data/installation/{id} to delete the specified installation object.
- *POST* /1/data/installation/_search to search for installation objects.
- *GET* /1/data/installation/{id}/tags to get the tags of the specified installation object.
- *DELETE* /1/data/installation/{id}/tags to delete the tags of the specified installation object.
- *POST* /1/data/installation/{id}/tags to add tags of the specified installation object.
- *PUT* /1/data/installation/{id}/tags to set the tags of the specified installation object.

---
#### /1/push

**POST** pushes a notification to app installations matching the request body. Only authorized to users whose role is authorized in push settings. Request body is required. Example:

```json
{
  "appId" : "caremen-driver",
  "tags" : [
      "status:available",
      "type:premium"
  ],
  "protocol" : "APNS",
  "message" : "Hello there!"
}
```

Request fields:

Fields       | Description
----------- | ----------------------------------------
`refresh` | Boolean. Defaults to false. If true, forces installation index refresh to make sure the latests created objects are there.
`appId` | String. Optional. The id of the app to push to. If none specified, push to any applications.
`credentialsIds` | List of strings. Optional. If set, filters installations to those owned by one of these users.
`installationIds` | List of strings. Optional. If set, push only to these installations.
`tags` | List of strings. Optional. If set, filters installations havings all these tags.
`protocol` | String. Optional. The push protocol to push to. If none specified, pushes to all protocols.
`usersOnly` | Boolean. Defaults to false. If true, pushes only to installations owned by a registered user.
`badgeStrategy` | String. Defaults to `manual`. The badge strategy to use. Available strategies: `manual`, `semi`, `auto`. See below.
`data` | Hash. Optional. The data used to generare the push message. Take a look at the exemple below and the [AWS documentation](http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage.html).
`text` | String. Optional. A simple string alert. Only used if `data` field is not set. 

Example of JSON formatted push data :

```json
{
  "default": "This is the default message which must be present when publishing a message to a topic. The default message will only be used if a message is not present for one of the notification platforms.",
  "APNS": {
    "aps": {
      "alert": "Check out these awesome deals!",
      "url": "www.amazon.com"
    }
  },
	"GCM": {
    "data": {
      "message": "Check out these awesome deals!",
      "url": "www.amazon.com"
    }
  },
	"ADM": {
    "data": {
      "message": "Check out these awesome deals!",
      "url": "www.amazon.com"
    }
  } 
}
```

Badge strategies:

- `manual`: any badge information must be manually set in push request `data` field. `badge` fields of installation objects are not used.
- `semi`: `badge` field of the installation object (to push to) is automatically inserted in the push request `data` message. For APNS and APNS_SANDBOX protocols only.
- `auto`: `semi` behavior + installation `badge` field is automatically incremented when installation is pushed to. For APNS and APNS_SANDBOX protocols only.

A push request returns a list of installations pushed to. Example:

```json
{
  "success" : true,
  "status" : 200,
  "failures" : true,
  "pushedTo" : [
    {
      "id" : "AVURQaeykYZydi-1LnY2"
    },
    {
      "id" : "AVURQaf1kYZydi-1LnY5",
      "userId" : "vince"
    },
    {
      "id" : "AVURQagxkYZydi-1LnY8",
      "userId" : "fred"
    },
    {
      "id" : "AVURQahnkYZydi-1LnY_",
      "userId" : "dave",
      "error" : {
        "type" : "java.lang.IllegalArgumentException",
        "message" : "property [endpoint] is missing"
      }
    }
  ]
}
```