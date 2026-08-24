---
layout: page
title: Roadmap
permalink: /docs/roadmap/
---

Planned areas of future development in the Mojito open-source platform, grouped by topic. Existence of a feature on this list should be viewed as a proposal, not as a commitment to implement. If you would like to see particular features or lend support for particular features below, leave us a note in the [Mojito Discussions](https://github.com/box/mojito/discussions) forum.

Items are ordered roughly by dependency where that applies; ordering within a topic is not a firm commitment.

## AI Translations

Core Mojito orchestration for AI-assisted translation.

1. Add Repo Types
2. Prompt Layer per Repo Type
3. Prompt Layer per Repo
4. Integrity checks per Repo Type
5. Glossary awareness in AI translation flows (approved terms in prompts)
6. Derived-locale AI adaptation (parent → regional variant)
7. Derived-locale prompt layers
8. Fuzzy match support using Lucene search
9. Translation memory support using fuzzy match (TM hits as AI reference context)
10. Workbench free-text / TM search (filters by repo, locale, status)
11. MQM-style quality measurement (segment-level scoring, batch scorecards, rollups)
12. Automatic translation within Mojito (triggered on push / status changes), instead of kicking off translations externally via the CLI

### A Few Definitions

"Repo Types" are a classification of repos that allows you to create an AI prompt or a list of integrity checkers just once and then assign this type to new repos you create. The new repos then inherit the AI prompts and the integrity checks. It also allows you to modify the AI prompt or integrity checks for all repos of the same type at once.

"Layered Prompts" refers to dynamic construction of AI prompts. The final prompt is constructed by taking the system prompt, adding the repo type prompt, then the repo prompt, and finally the per-request prompt from the CLI command-line. This offers more flexibiity to describe the strings being translated in a better way without putting all the possibly irrelevant info into every single prompt.

"Derived Locales" are locales where the translations are mostly inherited from another parent locale. For example, French for Canada is derived from French for France. French for Canada only has small changes needed from France French, so it makes sense to translate once for France French and the derived the Canadian French from it. That increases the consistency and at the same time makes French for Canada be adapted properly for Canada. In the mojito CLI, this is usually denoted with the parentheses in locale lists:  `mojito-cli repo-create -n MyNewRepo -l fr-FR,(fr-CA)->fr-FR`

"MQM means "Multidimensional Quality Metrics" which is an industry standard way of measuring translation quality. There is a rubric of 20 or so questions that you ask of each translation to score it, and then you combine the scores of individual translations together to create the overall score for a whole batch. See [The MQM](https://www.themqm.org/) for more details.

## Glossary Management

First-class glossary product inside Mojito.

1. Global glossary (installation-scoped; not a fake translation repository)
2. Term workflow aligned with string statuses (New → Needs translation → Needs review → Accepted)
3. CRUD UI and API without coupling to translation assets
4. CSV import / export
5. Roles (view / edit / approve)
6. Glossary tab in the Mojito UI
7. Glossary term management API (including Lucene-backed similar-term search)
8. Do-not-translate (DNT) support

## Screenshots & in-context review hooks

1. Deduplicated screenshot upload / refresh for text units
2. Screenshots as context in AI translation (and re-translate when a screenshot changes)
3. Export screenshots with vendor drops for human/vendor locales
4. APIs for in-context review to set status (Accepted vs Needs translation) and attach notes
5. "Tagging" pseudo-locale: copy source strings and embed project name plus string id in hidden Unicode characters

## UI Updates

1. Update to modern React
2. Update to modern Node.js
