---
layout: doc
title:  "User Roles & Permissions"
date:   2025-02-04 10:00:00 -0800
categories: guides
permalink: /docs/guides/user-roles-overview/
---

{{ site.mojito_green }} uses roles to control what you can see and do. This guide explains each role and what applies to you.

## Roles at a glance

| Role | Who you are | What you can do |
|------|-------------|-----------------|
| **User** | Basic access | View repositories, workbench, branches, screenshots. Search and browse. No edits. |
| **Translator** | Translation team member | Everything User can do, plus edit translations in Workbench for your assigned locales. Export, import, share searches. |
| **Project Manager (PM)** | Coordinates translation work | Everything Translator can do, plus create/import/cancel project requests, manage screenshots, manage users. |
| **Admin** | System administrator | Everything PM can do, plus AI Translate, database monitoring, Box integration. |

---

## For Translators

**You can:**
- Search and browse text units in the [Workbench]({{ site.url }}/docs/guides/workbench/)
- Add and edit translations for locales assigned to you
- Export search results (CSV/JSON) for offline work or reporting
- Import translations from CSV or JSON files
- Share search links with colleagues
- View [project requests]({{ site.url }}/docs/guides/project-request/) (read-only)
- View [branches]({{ site.url }}/docs/guides/branching/) and branch statistics
- View screenshots (legacy page) and upload screenshots (new dropzone)

**You cannot:**
- Create or import project requests (PM or Admin only)
- Add or edit screenshots in the legacy screenshot dashboard (PM or Admin only)
- Access AI Translate (Admin only)
- Access database monitoring (Admin only)
- Manage users (PM or Admin only)
- Configure Box integration (Admin only)

If you can't edit a translation, or if you get an error when importing, your role or locale assignment may not include that permission. Ask your project manager or admin.

---

## For Project Managers

**You can do everything Translators can, plus:**
- Create translation and review [project requests]({{ site.url }}/docs/guides/project-request/)
- Import and re-import completed projects
- Cancel project requests
- Add and manage screenshots in the [legacy screenshot dashboard]({{ site.url }}/docs/guides/branching/#collecting-screenshots)
- Manage users (add, update roles) via **Settings → User Management**

**You cannot:**
- Access [AI Translate]({{ site.url }}/docs/guides/ai-translate/) (Admin only)
- Access [database monitoring]({{ site.url }}/docs/refs/monitoring/) (Admin only)
- Configure Box integration (Admin only)

---

## For Admins

**You can do everything Project Managers can, plus:**
- Use [AI Translate]({{ site.url }}/docs/guides/ai-translate/) to batch-translate repositories with AI
- Access [database latency monitoring]({{ site.url }}/docs/refs/monitoring/) (User menu → Monitoring)
- Configure [Box integration]({{ site.url }}/docs/guides/integrating-with-box/) for project requests (Settings → Box Integration)

The **AI Translate** link appears in the main navigation only for Admins. The **Monitoring** link appears in your user menu (top right) only for Admins.

---

## Feature quick reference

| Feature | User | Translator | PM | Admin |
|---------|------|------------|-----|-------|
| Workbench: search, view | ✓ | ✓ | ✓ | ✓ |
| Workbench: edit translations | — | ✓ (assigned locales) | ✓ | ✓ |
| Workbench: export, import, share | — | ✓ | ✓ | ✓ |
| Project requests: view | ✓ | ✓ | ✓ | ✓ |
| Project requests: create, import, cancel | — | — | ✓ | ✓ |
| Screenshots: view, upload (dropzone) | ✓ | ✓ | ✓ | ✓ |
| Screenshots: add/edit (legacy) | — | — | ✓ | ✓ |
| Branches: view | ✓ | ✓ | ✓ | ✓ |
| User management | — | — | ✓ | ✓ |
| AI Translate | — | — | — | ✓ |
| Monitoring | — | — | — | ✓ |
| Box integration (Settings) | — | — | — | ✓ |

---

## Locale assignment (Translators)

Translators can only edit translations for locales they are assigned to. If you have "translate all locales" permission, you can edit any locale. Otherwise, you'll only see edit controls for your assigned locales. Ask your admin or PM to update your locale assignment if you need access to more languages.
