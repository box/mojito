---
layout: doc
title:  "Copying Translation Memory Between Repositories"
categories: guides
permalink: /docs/guides/leveraging/
---

The `leveraging-copy-tm` command copies matching translations from a source repository into a target repository. Use its matching, status-preservation, and overwrite options together to control how much risk the copy can introduce.

## Basic usage

```bash
mojito leveraging-copy-tm \
    --source-repository SourceRepo \
    --target-repository TargetRepo
```

By default, Mojito uses `MD5` matching, preserves status only for unique high-precision matches, and allows existing translations to be overwritten.

You can restrict the copy with:

- `--name-regex` to select target text units whose names match a regular expression
- `--source-asset-path` to use translations from one source asset
- `--target-asset-path` to update one target asset
- `--target-branch-name` to update one target branch

## Matching modes

Set the mode with `--mode`:

| Mode | Match |
|------|-------|
| `MD5` | Resource name, source content, and comment must match. This is the default and highest-precision mode. |
| `EXACT` | Source content must match; name and comment are ignored. |
| `NAME` | Resource name must match; content and comment are ignored. |
| `TUIDS` | Copy using explicit source-to-target TM text unit ID mappings. |

`NAME` is useful when source text changed but identifiers stayed stable. Review the results carefully because the translated content may no longer correspond to the source.

For an explicit mapping:

```bash
mojito leveraging-copy-tm --mode TUIDS \
    --tuids-mapping "1001:2001;1002:2002"
```

Each pair is `sourceTmTextUnitId:targetTmTextUnitId`. A source ID must be unique within one call. Use another call to copy one source unit to multiple targets.

## Preserving translation status

`--preserve-status` controls whether the copied translation keeps its original status or is downgraded to `TRANSLATION_NEEDED`:

| Value | Behavior |
|-------|----------|
| `PRECISION` | Preserve status only when the match is unique and high-precision. Name-only and content-only matches are downgraded. This is the default and lowest-risk option. |
| `UNIQUE` | Preserve status for any unique match, regardless of matching precision. |
| `ALL` | Always preserve status, including ambiguous matches. One candidate may be selected arbitrarily. This is the highest-risk option. |

## Overwriting existing translations

`--overwrite-mode` controls which target translations may be replaced:

| Value | Behavior |
|-------|----------|
| `ALL` | Overwrite regardless of current status. This is the default. |
| `NONE` | Never overwrite; only fill locales with no translation. |
| `FOR_TRANSLATION` | Fill locales with no translation or with `TRANSLATION_NEEDED` status. |
| `HIGHER_STATUS` | Overwrite only when the candidate's original status is higher than the current status. |
| `HIGHER_OR_EQUAL_STATUS` | Overwrite when the candidate's original status is higher than or equal to the current status. |

For a conservative content-only copy:

```bash
mojito leveraging-copy-tm \
    --source-repository SourceRepo \
    --target-repository TargetRepo \
    --mode EXACT \
    --preserve-status PRECISION \
    --overwrite-mode FOR_TRANSLATION
```

Start with `PRECISION` and `FOR_TRANSLATION` or `NONE` unless you have verified that broader matches and overwrites are safe for the target repository.
