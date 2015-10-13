---
layout: doc
title: Contents
---

#### Application contents

Contents are collection of JSON documents. Each collection is defined by a type and schema.

##### /v1/content

`GET` returns the list of all content types and schemas.

##### /v1/content/{type}

`GET` returns content objects of the specified type.

##### /v1/content/{type}/{id},...

`GET` returns the specified contents

- `id, ...` -– one or more content ids.
- `context` = tag1, tag2, tag3, … –– returns contents that match the specified context tags. Tags can represent countries, languages, device types, OS types, OS versions, screen size, ...
- `strict` = [false]/true : If true, only returns contents that match all the context constraints. If false, returns the best possible contents even if they don't match all the context constraints.
