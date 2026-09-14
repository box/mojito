---
layout: doc
title:  "Generating Pseudolocalized Files"
categories: guides
permalink: /docs/guides/pseudolocalization/
---

Pseudolocalization replaces source characters with accented alternatives and writes localized files for the synthetic locale `en-x-pseudo`. Use it to find hard-coded text, layout problems, and code that incorrectly assumes ASCII content before translations are available.

Push the source assets before generating pseudolocalized files:

```bash
mojito push -r MyRepo
mojito pseudo -r MyRepo
```

The `pseudo` command supports the same source and target directory, file type, source locale, source regex, filter, and directory include/exclude options used by other file-processing commands:

```bash
mojito pseudo -r MyRepo \
    --source-directory src \
    --target-directory build/pseudo \
    --file-type PROPERTIES JSON
```

## Character substitution

Set the substitution strategy with `--substitute`:

| Value | Behavior |
|-------|----------|
| `RANDOM` | Select a random accented replacement for each character. This is the default. Repeated runs, and repeated occurrences within a string, may differ. |
| `CONSISTENT` | Map a character to the same replacement within a string. Use this when reproducible-looking output is easier to test or review. |

```bash
mojito pseudo -r MyRepo --substitute CONSISTENT
```

Both strategies preserve the original text structure while making unlocalized content visually obvious.
