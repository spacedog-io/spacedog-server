---
layout: doc
title: Push
rank: 11
---

#### /1/applications

Use this endpoint to manage your applications and their push credentials.

##### /1/applications/{name}/{pushService}

*PUT* sets the credentials of the specified `name` application with the specified push service. Available push services are APNS, APNS_SANDBOX and GCM. The request body must contain principal and credentials specific to the specified push service.

Example for APNS:

```http
PUT https://caremen.spacedog.io/1/applications/driver/APNS
```

with body:

```json
{
  "principal" : "-----BEGIN CERTIFICATE-----\nMIIGVzC...JkjhGHGF\n-----END CERTIFICATE-----",
  "credentials" : "-----BEGIN PRIVATE KEY-----\nMIIEvAIBA...VIqy6fJA==\n-----END PRIVATE KEY-----"
}
```

sets the APNS push credentials of an application named `caremen-driver`.

#### /1/installation

Use this endpoint to manage your mobile app installations on user devices and to push mobile notifications to them.

##### /1/installation

*GET* returns all the specified backend installations. Only authorized to administrators.

*DELETE* deletes all the specified backend installations. Only authorized to administrators.

*POST* creates a new installation. Request body example:

```json
{
  "token" : "...",
  "appId" : "...",
  "pushService" : "...",
  "tags" : [
    {"key" : "...", "value" : "..."},
    {"key" : "...", "value" : "..."}
  ]
}
```

| Field       | Description                              |
| ----------- | ---------------------------------------- |
| token       | The token provided by the device to push notifications to this app. |
| appId       | The id of the app to push to.            |
| pushService | The push service to use to push to this device. Valid values are APNS, APNS_SANDBOX, ADM, GCM, BAIDU, WNS. |
| tags        | An array of tags used to select the installations to push to. |
| tags.key    | the tag's key                            |
| tags.value  | the tag's value                          |

If an installation is posted with credentials of a valid user, the new installation is associated with this user. Otherwise the new installation is not associated with any user.

##### /1/installation/{id}

*GET* returns the specified installation.

*PUT* updates the specified installation.

*DELETE* deletes the specified installation. Only authorized to administrators or the owner of this object.

##### /1/installation/{id}/tags

*GET* returns the tags of the specified installation.

*DELETE* deletes the tags specified in the body from the tags of the specified installation. Request body example:

```json
[
  {"key" : "...",  "value" : "..."},
  {"key" : "...",  "value" : "..."}
]
```

*POST* adds a tag to the tags of the specified installation. Request body example:

```json
{
  "key" : "...",
  "value" : "..."
}
```

*PUT* replaces all the tags of the specified installation with the tags specified in the body. Request body example:


```json
[
  {"key" : "...",  "value" : "..."},
  {"key" : "...",  "value" : "..."}
]
```

##### /1/installation/push

*POST* pushes a notification to all installations of the specified app. Only authorized to users and administrators. Request body example:

```json
{
  "appId" : "...",
  "message" : "Hello there!",
  "usersOnly" : false,
  "pushService" : "APNS",
  "tags" : [
    {
      "key" : "...",
      "value" : "..."
    }
  ]
}
```

| Field       | Description                              |
| ----------- | ---------------------------------------- |
| appId       | The id of the app to push to.            |
| message     | A simple string or a JSON formatted message data. Take a look at the exemple below or the [AWS documentation](http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage.html). |
| pushService | Optional. The service to push to. If none specified, pushes to all services. |
| usersOnly   | Optional. Defaults to false. If true, pushes only to installations associated with a backend user. |
| tags        | Optional. Pushes only to installations with these tags. If none specified, pushes to all installations |

Example of JSON formatted message data :

```json
{
	"default": "This is the default message which must be present when publishing a message to a topic. The default message will only be used if a message is not present for one of the notification platforms.",
	"APNS": "{\"aps\":{\"alert\": \"Check out these awesome deals!\",\"url\":\"www.amazon.com\"} }",
	"GCM":"{\"data\":{\"message\":\"Check out these awesome deals!\",\"url\":\"www.amazon.com\"}}",
	"ADM": "{ \"data\": { \"message\": \"Check out these awesome deals!\",\"url\":\"www.amazon.com\" }}" 
}
```

It returns a list of installations pushed to. Example:

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