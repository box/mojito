---
layout: doc
title:  "Calling the REST API from the CLI"
categories: guides
permalink: /docs/guides/cli-api/
---

The `mojito api` command makes authenticated requests to the Mojito REST API. It uses the CLI's configured server and credentials, prints response bodies as JSON, and is useful for scripting and automation.

## Inspect the API

Print the server's OpenAPI specification:

```bash
mojito api --spec --pretty
```

Make a GET request by passing either a resource name or an `/api/` path:

```bash
mojito api repositories --pretty
mojito api /api/repositories --pretty
```

Only paths are accepted; full URLs are rejected.

## Fields and request bodies

Use `-F` for typed values. Booleans, `null`, and integers are converted to their JSON types. Use `-f` to keep a value as a string:

```bash
mojito api repositories -F page=0 -F size=20
mojito api repositories -f name=123
```

For methods that send a body, fields become a JSON object. The command always defaults to `GET`, even when fields are provided, to avoid accidental mutations. Set the method explicitly for POST requests:

```bash
mojito api textunits/search -X POST \
    -F "repositoryIds[]=1" -F "localeTags[]=fr-FR"
```

Use `key[]=value` more than once to construct an array. For a pre-built request body, use `--input` with an explicit method:

```bash
mojito api textunits/search -X POST --input search-request.json
cat search-request.json | mojito api textunits/search -X POST --input -
```

Use `--binary --input image.png` for binary uploads. Add headers with `-H`, for example `-H "Content-Type:image/png"`.

## Waiting for asynchronous work

Add `--wait` when an endpoint returns a pollable task or a polling token:

```bash
mojito api proto-ai-translate -X POST --input request.json --wait --pretty
```

Progress is written to standard error. The final JSON response remains on standard output so scripts can parse it.

## Pagination

Use `--paginate` to fetch multiple pages. Mojito detects Spring `page`/`size` responses and `offset`/`limit` array responses:

```bash
mojito api repositories --paginate --page-size 100 --max-pages 0
```

By default, each page is printed separately. Add `--slurp` to merge all items into one JSON array. Use `--start-page` to resume after a capped request, or `--paginate-style page|offset` when automatic detection is not appropriate.

## Output controls

- `--pretty` formats JSON.
- `--include` includes the HTTP status and response headers.
- `--silent` suppresses the response body.
- HTTP error bodies are still written to standard output; the summary is written to standard error and the command exits unsuccessfully.
