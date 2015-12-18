---
layout: doc
title: Queues
---

#### Queues

Use a queue when you want to process a task later. This is very usefull to speed up some user requests since you don’t need to wait for all the tasks to complete before returning a result to the user. Some of the tasks might be processed later in queues.

First you need to choose or create a service that will be responsible for the processing to the queue messages (or tasks). Then create a queue. Then add messages to the queue. They will be processed when a CPU is available.

The queue object is of the type:

```json
{
     "id" : "myqueue",
     "service" : "processTransfers",
     "serialize" : true,
     "timeout" : 98798,
     "active" : true,
     "archive": true,
     "errorPolicy" : "manual"
}
```

Field | Type | Description
-----|-----|-----
id | string | the queue id
service | string | the service id
serialize | boolean | Optional. If true, queue messages are processed one at the time. If false, they can be processed in parallel. Default to false.
timeout | int | The timeout in milliseconds of the process of a message. Precedes the service timeout property.
active | boolean | If false, the queue does not process its messages. But messages can be added to the queue.
archive | boolean | Optional. Default is false. If true, store all processed messages.
errorPolicy | string | Optional. Valid values are: delete, manual, automatic. Default is manual.

##### /v1/queue

`GET` returns all queues.

`POST` creates a new queue.

- request body: a queue JSON object.
- returns a created status object.

##### /v1/queue/{qid}

`GET` returns the specified queue.

`PUT` update the specified queue.

- request body: a queue JSON objet.
- returns an updated status object.

A queue object:

```json
{
  "name" : "sms",
  "purge" : 12562,
  "serviceId" : "sendSMS",
  "retry" : 3,
  "deletePolicy": "processRemaining"
}
```

`DELETE` deletes the specified queue.

- `deletePolicy` = `processRemaining` : the queue is deleted when all messages are processed. No new message can be added to the queue.
- `deletePolicy` = `immediately` : the queue is deleted immediately. No message can be added. Remaining messages are discarded and the queue is deleted.
- returns a deleted status object.

##### /v1/queue/{qid}/process

`POST` adds an object at the end of the specified queue.
 
- `shortcut` = [false]/true : If true, adds the object at the top of the queue.
- request boby: a JSON object or array the queue service is able to process
- returns a created status object.

`GET` returns the list of the remaining objects to be processed.

- `first` = 1 to œ —  The first objet of the list to return. Default is 1.
- `size` = 1 to 1000 — The number of objets to return. Default is 10. Maximum is 1000.
- returns an array of JSON objects.

##### /v1/queue/{qid}/processed

`GET` returns the list of already processed objects from the youngest to the oldest. Processed messages are stored if the queue `archive` field is est to `true`.
 
- `first` = 1 to œ —  The first objet of the list to return. Default is 1.
- `size` = 1 to 1000 — The number of objets to return. Default is 10. Maximum is 1000.
- returns an array of JSON objects of this type:

```json
{
  "id" : "1234567585757",
  "status" : "processed",
  "processed" : "2016-04-13T23:45:07.454Z",
  "took" : 12562,
  "message" : {
    "..." : "..."
  }
}
```

##### /v1/queue/{qid}/message/{mid}

`GET` returns the specified message if it has not been processed or if it has been archived.

- `qid` the queue id.
- `mid` the message id.

`DELETE` deletes the specified message if it has not been processed already.

- `qid` the queue id.
- `mid` the message id.
- returns a deleted status object.