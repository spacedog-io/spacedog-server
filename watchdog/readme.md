# SpaceDog Admin Jobs

This module builds a bundle containing 3 admin jos :

- The snaphot job snapshot every hour the server data into a specific S3 bucket.
- The purge job deletes every day obsolete server logs.
- The watchdog job checks the server services every hour to verify everything is working normaly.



## Package

Use `mvn clean package` to build and package all jobs in the same bundle.



## Snapshot Job

The snaphot job snapshots the server data into a specific S3 bucket. This job usually runs every hour.

This job is an AWS lambda job. To install :

- Create a lambda function with the bundle `spacedog-watchdog-x.y.z-bundle.tar.gz` and this configuration

| Field         | Value                                    |
| ------------- | ---------------------------------------- |
| Function type | Java 8                                   |
| Manager       | io.spacedog.watchdog.Snapshot::run       |
| Role          | the role specifically created for these admin jobs |
| Memory        | 192 MB                                   |
| Expiration    | 5 minutes                                |

- Use these environment variables

| Key                                  | Value                                    |
| ------------------------------------ | ---------------------------------------- |
| spacedog_jobs_snapshotall_password   | `snapshotall` job user password          |
| spacedog_superdog_notification_topic | SNS notification topic to whom all errors and messages should be sent |

- Trigger this job every hour.




## Purge Job

The purge job deletes all obsolete logs from the server to avoid disk outage. This job sends an HTTP DELETE request to the SpaceDog Server route https://api.spacedog.io/1/log with a `before` date parameter before then all logs should be deleted.

This job is an AWS lambda job. To install :

- Create a lambda function with the bundle `spacedog-watchdog-x.y.z-bundle.tar.gz` and this configuration

| Field         | Value                                    |
| ------------- | ---------------------------------------- |
| Function type | Java 8                                   |
| Manager       | io.spacedog.watchdog.Purge::run          |
| Role          | role specifically created for these admin jobs |
| Memory        | 192 MB                                   |
| Expiration    | 5 minutes                                |

- Use these environment variables

| Key                                  | Value                                    |
| ------------------------------------ | ---------------------------------------- |
| spacedog_jobs_purgeall_password      | `purgeall` job user password             |
| spacedog_superdog_notification_topic | SNS notification topic to whom all errors and messages should be sent |

- Trigger this job every day.



## Watchdog Job

This job is an AWS lambda job. To install :

- Create a lambda function with the bundle `spacedog-watchdog-x.y.z-bundle.tar.gz` and this configuration

| Field         | Value                                    |
| ------------- | ---------------------------------------- |
| Function type | Java 8                                   |
| Manager       | io.spacedog.watchdog.Watchdog::run       |
| Role          | role specifically created for these admin jobs |
| Memory        | 192 MB                                   |
| Expiration    | 5 minutes                                |

- Use these environment variables

| Key                                  | Value                                    |
| ------------------------------------ | ---------------------------------------- |
| spacedog_superdog_notification_topic | SNS notification topic to whom all errors and messages should be sent |

- Trigger this job every hour.