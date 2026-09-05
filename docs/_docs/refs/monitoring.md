---
layout: doc
title:  "Database Monitoring"
date:   2025-02-04 10:00:00 -0800
categories: refs
permalink: /docs/refs/monitoring/
---

The Database Latency page lets you measure database response times to diagnose performance issues.

> **Who can use Monitoring:** Admins only. The **Monitoring** link appears in your user menu (top right, under your username) only if you have the Admin role. Translators and Project Managers do not see this option. See [User Roles & Permissions]({{ site.url }}/docs/guides/user-roles-overview/).

## How to access

1. Click your username in the top-right corner.
2. Click **Monitoring** in the dropdown.
3. The Database Latency page opens at `/monitoring`.

## What it measures

The page runs database probes and reports latency (response time in milliseconds):

| Probe | Description |
|-------|-------------|
| **Direct JDBC (select 1)** | Raw JDBC connection executing a simple query |
| **Hibernate (select 1)** | Hibernate health check |
| **Hibernate repositories query** | A typical repositories query as used by the application |

For each probe, you get:
- **Min**, **Max**, **Average** latency (ms)
- Per-iteration measurements

## How to use it

1. Set **Iterations** (1–20, default 5). More iterations give a more stable average.
2. Click **Measure**.
3. Review the results. High or variable latency may indicate database or network issues.

Use this when troubleshooting slow page loads or when tuning database configuration.
