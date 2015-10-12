---
layout: doc
title: Configuration
---

#### Application configuration

Use the config API endpoints to configure your app. A configuration is a plain JSON object containing the first properties an app needs to initialize. It can contant for example the base URL of the backend API, the name of a data store, ... New version of an application might fetch a different
configuration set. There is no schema for configurations.


##### /v1/config

`GET` returns all configurations.

`POST` creates one or many configurations.

- `stash` = [true]/false –– Not published until stash is released (see Stash).
- `body` –– A JSON object or an array of JSON objects.
- `result` –– A created status object or an array of it.


##### /v1/config/{id1},{id2},...

`GET` returns the specified configurations.

- `stash` = [false]/true –– Returns stashed in place of the published configurations if available.

`PUT` updates the specified configurations.

- `ttl` –– Optional header. TTL in milliseconds. Default is ?.
- `stash` = [true]/false –– Not published until stash is released (see Stash).
- `body` –– A JSON object or an array of JSON objects.
- `result` –– A created status object or an array of it.


##### /v1/config/stash

`GET` returns the stashed configurations.

`DELETE` deletes the stashed configurations.


##### Questions:
1. do we merge a list of configurations to a single merged configuration containing all the fields to ease the developer work?
