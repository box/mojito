---
layout: doc
title:  "AI Translate"
date:   2025-02-04 10:00:00 -0800
categories: guides
permalink: /docs/guides/ai-translate/
---

AI Translate lets you batch-translate a repository using an AI model. {{ site.mojito_green }} sends source strings to the configured AI service and imports the translations back.

> **Who can use AI Translate:** Admins only. The **AI Translate** link appears in the main navigation only if you have the Admin role. Translators and Project Managers do not see this page. See [User Roles & Permissions]({{ site.url }}/docs/guides/user-roles-overview/).

## When to use it

Use AI Translate when you want to:
- Get an initial pass of translations for a new locale
- Fill in missing translations at scale
- Experiment with different models or prompts (dry run first)

Translations are imported with the status you choose (e.g. Needs Review) so they can go through your normal review workflow.

## How it works

1. Go to **AI Translate** in the main navigation.
2. Select a **Repository**.
3. Optionally select **Target locales** (leave all unselected to translate every locale in the repository).
4. Configure options (see below).
5. Click **Start translation**.
6. The job runs in the background. You can wait for completion or leave the page.
7. If you enabled **Download JSON report**, a report per locale is available after completion.

## Options

| Option | Description | Default |
|--------|-------------|---------|
| **Source texts per locale** | Maximum number of source strings to translate per locale | 100 |
| **Text unit IDs** | Optional comma/whitespace-separated list of TM text unit IDs to limit the scope | — |
| **Model override** | AI model identifier (e.g. `gpt-4.1`) | gpt-4.1 |
| **Prompt suffix** | Optional text appended to the base prompt | — |
| **Related strings** | Extra context sent to the AI. See [Related strings](#related-strings) below. | NONE |
| **Translate type** | `TARGET_ONLY_NEW` = only untranslated; `TARGET_ONLY` = overwrite existing; `WITH_REVIEW` = translate all and mark for review | TARGET_ONLY_NEW |
| **Status filter** | `FOR_TRANSLATION` = only strings needing translation; `ALL` = every string | FOR_TRANSLATION |
| **Import status** | Status applied to imported translations: `REVIEW_NEEDED`, `ACCEPTED`, or `TRANSLATION_NEEDED` | REVIEW_NEEDED |
| **Request timeout (seconds)** | Per-request timeout; leave blank for server default | — |
| **Download JSON report** | Download a JSON report per locale after completion | Off |
| **Dry run** | Run without importing results (useful for testing) | Off |

## Related strings

**Related strings** adds extra context to each translation request so the AI can produce more natural, coherent translations. The AI is instructed to use this context (e.g. surrounding text) to match tone and improve accuracy.

| Value | What it does | When to use |
|-------|--------------|-------------|
| **NONE** | No related strings. Each string is translated in isolation. | Default. Use when strings are independent (e.g. UI labels, buttons). |
| **USAGES** | Sends strings that appear in the *same source file* at nearby positions (by line number). Uses usage metadata from extraction (e.g. `#: file.js:2` in PO files). | Best for strings that appear in sequence (emails, documents, templates). The AI sees preceding and following text for context. |
| **ID_PREFIX** | Groups strings by the part of the name before the first dot. Sends other strings from the same group. E.g. `email.subject`, `email.body`, `email.greeting` share prefix `email`. | Best for structured keys (e.g. `screen.login.title`, `screen.login.subtitle` or `button.save`, `button.cancel`). The AI sees related strings from the same logical group. |

**USAGES** requires that your file format provides usage/location info (e.g. PO, MacStrings). **ID_PREFIX** works with any string names that use dot-separated prefixes.

## Dry run

Enable **Dry run** to test your configuration without importing translations. The AI translation runs, but results are not saved. Use this to verify the model, prompt, and scope before a real run.

## Reports

If you enable **Download JSON report**, after the job finishes you can download a JSON report for each locale. The report contains details about the translation run for that locale.

## Command-line usage

Administrators and automated jobs can run the same workflow with the `repository-ai-translate` CLI command. This advanced command is hidden from the main CLI command list.

```bash
mojito repository-ai-translate -r MyRepo -l fr-FR de-DE \
    --source-text-max-count 100 \
    --translate-type TARGET_ONLY_NEW \
    --status-filter FOR_TRANSLATION \
    --import-status REVIEW_NEEDED \
    --download-report
```

The CLI supports the same model, prompt, related-string, timeout, dry-run, and text-unit ID options as the web page. It also supports:

- `--use-batch true` to use the OpenAI Batch API
- `--glossary-name` and the `--glossary-term-*` options to supply glossary context
- `--glossary-only-matched-text-units` to process only text units matched by the glossary
- `--attach-job-id` to monitor an existing batch job
- `--import-job-id` with `--resume` to retry or resume an import

The CLI defaults `--translate-type` to `WITH_REVIEW`, while the web page defaults to `TARGET_ONLY_NEW`. Set it explicitly in automation when the distinction matters. `--download-report` is available only in non-batch mode.

## Requirements

AI Translate requires an AI translation service to be configured on the server. Contact your administrator if the feature is not available or jobs fail.
