---
layout: doc
title: Jobs
---

#### Scheduled jobs

Use the batch API to define, schedule and run backend jobs. Only admin users can use the API.

A job JSON object is of type:

```json
{
     "id" : "mySuperJob",
     "description" : "...",
     "service" : "mySuperService",
     "timeout" : 12345,
     "params" : {
     	"..." : "..."
     },
     "when" : [ "monday", "friday" ]
}
```

Schedule patterns can be:
- an absolute date, ex. "2016-10-31T00:00:00Z" 
- a week day with a time, ex. "monday:22:33"
- ...


#####  /v1/jobs

`GET` returns all jobs objects.


##### /v1/job/{id}

`POST/PUT` create/update the specified job.

`GET` returns the specified job.

`DELETE` deletes the specified job.


##### /v1/job/run

`GET` returns info on the last 20 job runs.


##### /v1/job/{id}/run 

`POST` run the specified job immediately.

`GET` returns info on the last 20 runs of the specified job.

