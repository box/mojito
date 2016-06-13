---
layout: doc
title:  "Creating Repository"
categories: guides
permalink: /docs/guides/creating-repository/
---

In this guide, we use `mojito-cli` to create a repository in Mojito.  Repository is a container for strings and their translations.  It also stores localization configurations such as locales and integrity checkers.

## Creating Repository

    mojito repo-create -n MyRepo -d "Project A" -l de-DE es-ES fr-FR ja-JP
    
This creates a repository with name `MyRepo` with description `Project A`.  It is configured with locales `de-DE es-ES fr-FR ja-JP`.  You can now see the repository `MyRepo` in the Webapp.

![Creating Repository](./images/creating-repository.png)


## Updating Locales in Repository

    mojito repo-update -n MyRepo -l es-ES fr-FR ja-JP zh-CN zh-TW
    
This updates locales for the repository to `es-ES fr-FR ja-JP zh-CN zh-TW`.


## Renaming Repository

    mojito repo-update -n MyRepo -nn ProjectA
    
This updates the name of the repository from `MyRepo` to `ProjectA`.


## Deleting Repository

    mojito repo-delete -n MyRepo
    
This deletes the repository `MyRepo` and it is no longer visible from the Webapp.
