---
layout: doc
title: Stash
---

#### Stash

##### /v1/stash

`GET` returns the list of all changes stored in the stash for the next release.

`DELETE` deletes the stash and all stored changes. Use this to erase all changes and move the dev/stash version of the app back to the prod version.

##### /v1/stash/release

`POST` release all the stashed changes to production. The stash might contain changes on configurations, contents, files, ...

##### /v1/rollback

`POST` rollbacks from the latest release to the previous release. Use this service when you detect a problem in the stashed changes you've just released. It will cancel all the changes and move the prod version of the app to the previous state.