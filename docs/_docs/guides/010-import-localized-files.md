---
layout: doc
title:  "Import Localized Files"
date:   2018-05-11 15:25:25 -0800
categories: guides
permalink: /docs/guides/import-localized-files/
---

When starting to use `Mojito`, you may already have localized applications. In
that case you can import your translations and start working from there instead
of starting from scratch.

Using the `mojito-cli`, importing translations with the `import` command is really
simple and helps bootstrap quickly a repository. It can also be used to recreate
repository from scratch following a bad manipulation.

The main options of the `import` command are the same as the `push` and `pull`
command. The simplest call being:

```bash
mojito import -r MyRepo
```

The major addition to the `push`/`pull` options is `--status-equal-target`
that allows you to define the behavior of the import when the "translation" is
the same the source string.

It lets you skip the import or mark the string with special status (approved,
translation needed, review needed)

```bash
mojito import -r MyRepo --status-equal-target SKIPPED
```

This option can be useful when the imported localized files were generated using
"inheritance". In that case the translation might not be coming from the locale
itself but from a parent locale and in that case you may want not to import it,
or maybe review it.
