---
layout: doc
title: Configurations and contents
rank: 6
---

#### Use configurations [Not yet implemented]

Mobile apps need remote configuration to avoid having to release new versions just to change a small setting. Configuration usually contains base URLs to remote services, name of data stores, tips, messages, ... The SpaceDog platform provides a configuration service. A configuration can be any valid JSON document. There is no schema to set and comply to. New version of an application might fetch a different configuration set to get old and new settings.

To create a configuration, send a `PUT /v1/config/{id}` with a body set to a JSON document and a client provided identifier.

To get a configuration set, send a `GET /v1/config/{id1},{id2},...` with a list of configuration identifiers. It returns an array of JSON documents, one for each configuration requested.

#### Manage contents [Not yet implemented]

...

#### Stash changes [Not yet implemented]

With an app in production, you might want to be careful with changing configurations and contents. To avoid immediate publication of changes, use the `stash` query param when creating/updating configurations and contents. If set to `true`, all changes are not published but stashed to wait for the next release.

If we take the configuration API for example,

- use `stash` query param with `PUT /v1/config/{id}` or `DELETE /v1/config/{id}` to stash these changes,
- send a `GET /v1/config/stash` request to get all stashed configurations,
- send a `DELETE /v1/config/stash` request to delete all stashed configurations.

#### Release stashed changes [Not yet implemented]

To release all the stashed changes in configurations and contents all at the same time, send a `POST /v1/stash/release` request.

#### Rollback to previous state [Not yet implemented]

Sometime the configuration and content changes you just released happen to break your app in production. In this case, you can rollback the changes to the previous state by sending a `POST /v1/rollback` request. All rollback changes are lost. This does not affect what has been stashed but not yet released.

if the last change to your configurations or contents was an immediate change (the change was not stashed then released but immediately published with `stash=false`), then a rollback request only rollbacks this immediate change.