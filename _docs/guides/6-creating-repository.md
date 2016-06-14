---
layout: doc
title:  "Creating Repository"
categories: guides
permalink: /docs/guides/creating-repository/
---

In this guide, we use `mojito-cli` to create a repository in Mojito.  Repository is a container for strings and their translations.  It also stores localization configurations such as locales and integrity checkers.


## Creating Repository

    mojito repo-create -n MyRepo -d "Project A" -l de-DE es-ES fr-FR ja-JP
    

This creates a repository with name `MyRepo` with description `Project A`.


It is configured with locales `de-DE es-ES fr-FR ja-JP`.  These locales are used to create translation requests to translators and to generate localized resource files.  For more information about locales, see [Managing Locales]({{ site.github.url }}/docs/guides/managing-locales/).


You can now see the repository `MyRepo` in the Webapp.


![Creating Repository](./images/creating-repository.png)



## Creating Repository with Integrity Checker

    mojito repo-create -n MyRepo -it resw:COMPOSITE_FORMAT,resx:COMPOSITE_FORMAT -d "Project A" -l de-DE es-ES fr-FR ja-JP
    

This creates a repository with name `MyRepo` with description `Project A` and locales `de-DE es-ES fr-FR ja-JP`.


The repository is configured to use `COMPOSITE_FORMAT` integrity checker for files with `resw` or `resx` extensions.  Integrity checkers are used to validate translations.  Integrity checker configuration is optional but it is highly recommended to catch translations with errors and reject them.  See [Integrity Checkers]({{ site.github.url }}/docs/guides/integrity-checkers/) for more information.



## Updating Locales in Repository

    mojito repo-update -n MyRepo -l es-ES fr-FR ja-JP zh-CN zh-TW
    

This updates locales for the repository to `es-ES fr-FR ja-JP zh-CN zh-TW`.


## Renaming Repository

    mojito repo-update -n MyRepo -nn ProjectA
    

This updates the name of the repository from `MyRepo` to `ProjectA`.


## Deleting Repository

    mojito repo-delete -n MyRepo
    

This deletes the repository `MyRepo` and it is no longer visible from the Webapp.
