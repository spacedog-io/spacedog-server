# SpaceDog CLI

## Build

To build this module, use maven :

```sh
mvn clean package
```

## Install

- Download a CLI bundle fron the github release page : https://github.com/spacedog-io/spacedog-server/releases.
- Unzip the bundle on your linux or mac computer wherever you want. 
- Set the `DOG_HOME`variable to the CLI install directory.
- Type `java —version` to make sure a java version 8 is installed and accessible in the path.
- Create the soft symbolic link `/usr/local/bin/dog` to point to the CLI `dog.sh`script to have access to the SpaceDog command in the path.
- Type `dog` to see the command usage help.



## Usage

The `dog` command is used to access a SpaceDog backend.

### login

Use`dog login` to login to a specific SpaceDog backend. Exemple :

```sh
$ dog login -b https://mybackend.spacedog.io -u vince@gmail.com
```

| Parameters     | Description                              |
| -------------- | ---------------------------------------- |
| -b, --backend  | The backend to log in to. Example : https://mybackend.spacedog.io, https://connectapi.colibee.com |
| -u, --username | The username to log in with. Usualy an email address. |
| -v, --verbose  | Verbose mode will display debug traces of all REST requests to the backend. |

### sync

Use`dog sync` to sync a file folder on your computer to the file bucket of your backend. Very usefull to deploy a web app to the web. Exemple :

```sh
$ dog login -b https://myapp.spacedog.io -u vince@gmail.com
$ dog sync -p v2 -s /home/vince/dev/target
```

will synchronize all `home/vince/dev/target` files to the `myapp` backend file bucket with prefix `v2`. File like `home/vince/dev/target/images/logo.png` will be accessible at : https://myapp.spacedog.io/1/file/v2/images/logo.png and https://myapp.spacedog.io/1/web/v2/images/logo.png.

The `www` prefix has a special meaning for backends deployed on `spacedog.io`. It automatically maps files to a *root* URL : https://myapp.www.spacedog.io/...


| Parameters    | Description                              |
| ------------- | ---------------------------------------- |
| -p, --prefix  | The bucket prefix you want to deploy to. |
| -s, --source  | The source folder to synchronize to the backend bucket. |
| -v, --verbose | Verbose mode will display debug traces of all REST requests to the backend. |


### exportlog

Use`dog exportlog` to export all requests and responses processed by a backend. Exemple :

```sh
$ dog login -b https://myapp.spacedog.io -u vince@gmail.com
$ dog exportlog -d 2017-05-23 -f /home/vince/yesterday.log
```

will export all all requests and responses logs processed the 23th of may by the `myapp` backend to the `yesterday.log` file on your computer.

| Parameters    | Description                              |
| ------------- | ---------------------------------------- |
| -d, --day     | The day of wich request logs must be exported. |
| -f, --file    | The file to export to.                   |
| -v, --verbose | Verbose mode will display debug traces of all REST requests to the backend. |
