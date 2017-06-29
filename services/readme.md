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

- The web services `./services/target/spacedog-services-x.y.z-bundle.tar.gz`.
- The CLI `./cli/target/spacedog-cli-0.33.8-bundle.tar.gz`.

## Install the server

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

- copier `spacedog.server.properties dans `/home/adminsuez/spacedog`

- configurer la propriété `spacedog.elastic.networl.host` de spacedog.server.properties avec l’adresse IP du serveur

- In `~/spacedog` create a `data` directory to contain the server data :

  ```sh
  $ mkdir ~/spacedog/data
  ```

  ​

## Configure the server



## Run the server

### Start

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

### Stop

- Go into `~/spacedog/binXYZ`.
- Stop the server with `./stop.sh`. It reads the `pid` file created by the start script and containing the Java process id.

### Create a SuperDog

Create SuperDog credentials to administrate the server. Only use these credentials with care and only for technical administration of the server backends since SuperDogs have all permissions.

- Go into `~/spacedog/binXYZ`.
- Create a SuperDog with `./superdog.sh`. 
- The SpaceDog Java Server must be running for this script to work.
- The script will ask the adminsitrator all necessary information to create these credentials (username, password, email).

