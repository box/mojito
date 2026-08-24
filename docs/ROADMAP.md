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
3. Prompt Layer per Project
4. Integrity checks per Repo Type
5. Glossary awareness in AI translation flows (approved terms in prompts)
6. Derived-locale AI adaptation (parent → regional variant)
7. Derived-locale prompt layers
8. Fuzzy match support using Lucene search
9. Translation memory support using fuzzy match (TM hits as AI reference context)
10. Workbench free-text / TM search (filters by repo, locale, status)
11. MQM-style quality measurement (segment-level scoring, batch scorecards, rollups)
12. Automatic translation within Mojito (triggered on push / status changes), instead of kicking off translations externally via the CLI

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
