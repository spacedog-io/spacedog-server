# SpaceDog Server

Clone the project from https://github.com/spacedog-io/spacedog-server.

The SpaceDog Server project is a `maven`multi module project :

- `services` module contains the server web services.
- `sdk` module contains the Java client SDK and core utils classes.
- `cli` module contains the SpaceDog Command Line Interface (CLI).
- `test` module contains all integration and non regression tests.
- `jobs` module contains util classes to develop SpaceDog jobs.
- `watchdog`module contains SpaceDog platform admin jobs (watchdog, purge and snapshot).
- `examples` module contains dev examples.




## Build

To build the whole multi module project, go into the parent project directory and build :

```sh
$ mvn clean install
```

You will find 2 bundles :

- Server bundle `./services/target/spacedog-services-x.y.z-bundle.tar.gz`.
- CLI bundle `./cli/target/spacedog-cli-0.33.8-bundle.tar.gz`.
- Admin jobs bundle  `./watchdog/target/spacedog-watchdog-0.33.8-bundle.tar.gz`.




## Install

- `ssh` your linux server with the account that will run the web services.

- install Java 1.8.0.

- Create a `spacedog` directory in the user home directory 

- In `~/spacedog` untar the services bundle `spacedog-services-x.y.z-bundle.tar.gz` :

  ```sh
  $ tar xvzf spacedog-services-x.y.z-bundle.tar.gz
  ```

- Renames the resulting `bin` directory by adding the bundle version :

  ```sh
  $ mv bin binXYZ
  ```

- Configure (see below) and copy your `spacedog.server.properties` into `~/spacedog`

- Make sure property `spacedog.elastic.networl.host` is set with the IP address of the server

- In `~/spacedog` create a `data` directory to contain the server data :

  ```sh
  $ mkdir ~/spacedog/data
  ```




## Configure

The server is configured by a the following properties :

| Configuration property                   | Description                              |
| ---------------------------------------- | ---------------------------------------- |
| spacedog.home.path                       | Defaults to `~/spacedog`. Path to SpaceDog home directory where file `spacedog.server.properties`should be found. |
| spacedog.production                      | Sets the server to production or debug mode. Values :`true` or `false`. Defaults to `false`. |
| spacedog.server.port                     | Mandatory. Port the server should bind to listen to incoming http requests. For example, `spacedog.io`servers are bound to `8443` port. |
| spacedog.api.url.base                    | Mandatory. Base of SpaceDog Server URLs. `spacedog.io`base URL is `.spacedog.io`. A debug server running locally with port `8443` should have `.lvh.me:8443` as base URL. The base URL is used to build all server URLs returned in request responses. |
| spacedog.api.url.scheme                  | Mandatory. Values `http`or `https`. Scheme of the SpaceDog Server URLs. `spacedog.io` sheme is `https`. A debug server running locally with port `8443` should have `http` as scheme. The scheme is used to build all server URLs returned in request responses. |
| spacedog.root.backend.id                 | Defaults to `api`. Name of the server root backend. |
| spacedog.elastic.data.path               | Path to the directory where the server stores all ElasticSearch data files. Defaults to `<spacedog.home.path>/data`. |
| spacedog.elastic.http.enabled            | `true` to enable ElasticSearch http REST API on port `9200`. Defaults to `false`. |
| spacedog.aws.bucket.prefix               | AWS S3 bucket name prefix of the buckets used by the SpaceDog Server. The server stores files in `<prefix>-files` bucket, shares in `<prefix>-shared` bucket, snapshots in `spacedog-snapshots` bucket. For example, `spacedog.io`platform S3 prefix is `spacedog-`. |
| spacedog.aws.region                      | AWS region of the AWS services used by the SpaceDog Server. `spacedog.io`platform AWS region is `eu-west-1`. |
| spacedog.aws.superdog.notification.topic | AWS SNS topic ARN, the SpaceDog Server uses to inform platform administrators (usually error or warning messages) . |
| spacedog.mail.domain                     | Server default email domain.  If a backend does not have any specific email settings. Emails are sent with MailGun email services using this default email domain. For example, `spacedog.io`platform email domain is `api.spacedog.io`. |
| spacedog.mail.mailgun.key                | Server default MailGun key. If a backend does not have any specific email settings. Emails are sent with MailGun email services using this default MailGun key. |
| spacedog.mail.smtp.debug                 | `true` to log debug information when server is sending emails with SMTP protocol. |



The server fetchs these configuration properties at the following places and with the following precedence

- system env variables,
- java system properties,
- in file `~/spacedog/spacedog.server.properties`.




## Run



#### Start

- Go into `~/spacedog/binXYZ`.

- Start the server with `./start.sh`.

- The script starts the SpaceDog Java server and then calls `tail -f log` to check if the server inits correctly. The server is fully started when log displays  `[main] INFO Fluent - Server started on port 8443`. Type  `ctrl-C` to cancel the `tail` command.

- All common `stdout` and `stderr` are appended to the `log` file in this directory. Take a look at this file to check if the server is throwing some errors.

- The script also creates a `pid` file containing the process id of the Java server process. The stop script will read this file to kill the process. If the process has crashed, remove this file to be able to start again the server.

- Test the server with

  ```sh
  $ curl http://<<adresse IP>>:8443
  ```

  It normaly returns this JSON

  ```json
  {
      "version": "X.Y.Z",
      "baseline": "We provide the back-ends of your apps",
      "success": true,
      "status": 200
  }
  ```




#### Stop

- Go into `~/spacedog/binXYZ`.
- Stop the server with `./stop.sh`. It reads the `pid` file created by the start script and containing the Java process id.




#### SuperDog

Create SuperDog credentials to administrate the server. Only use these credentials with care and only for technical administration of the server backends since SuperDogs have all permissions.

- Go into `~/spacedog/binXYZ`.
- Create a SuperDog with `./superdog.sh`. 
- The SpaceDog Java Server must be running for this script to work.
- The script will ask the adminsitrator all necessary information to create these credentials (username, password, email).

