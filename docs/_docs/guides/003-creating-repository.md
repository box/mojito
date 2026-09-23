---
layout: doc
title:  "Creating Repository"
categories: guides
permalink: /docs/guides/creating-repository/
---

In this guide, we use `mojito-cli` to create a repository in {{ site.mojito_green }}.  Repository is a container for strings and their translations.  It also stores localization configurations such as locales, an optional repository type, and integrity checkers.


### Creating Repository

    mojito repo-create -n MyRepo -d "Project A" -l de-DE es-ES fr-FR ja-JP
    

This creates a repository with name `MyRepo` with description `Project A`.  Description is optional.


It is configured with locales `de-DE es-ES fr-FR ja-JP`.  These locales are used to create translation requests to translators and to generate localized resource files.  For more information about locales, see [Managing Locales]({{ site.url }}/docs/guides/managing-locales/).


You can now see the repository `MyRepo` in the Webapp.


![Creating Repository](./images/creating-repository.png)



### Creating Repository with Integrity Checker

    mojito repo-create -n MyRepo -it resw:COMPOSITE_FORMAT -l fr-FR
    

This creates a repository with name `MyRepo` with locales `fr-FR`.


The repository is configured to use `COMPOSITE_FORMAT` integrity checker for files with `resw` extension.  Integrity checkers are used to validate translations.  Integrity checker configuration is optional but it is highly recommended to catch translations with errors and reject them.  See [Integrity Checkers]({{ site.url }}/docs/guides/integrity-checkers/) for more information.  If the repository is also assigned to a repository type, Mojito also runs that type's checkers; see [Creating Repository with a Repository Type](#creating-repository-with-a-repository-type).


### Creating Repository with a Repository Type

    mojito repo-create -n MyRepo --repo-type React -l fr-FR
    

This creates a repository named `MyRepo` assigned to the existing repository type `React`.  Omitting `--repo-type`, or passing an empty value, creates an untyped repository.


Assigning a type does not replace repository-level `-it` checkers.  At check time Mojito runs the union of the type's integrity checkers and the repository's own checkers for each file extension.  You can also assign or change the type later, or clear it:

    mojito repo-update -n MyRepo --repo-type React
    
    mojito repo-update -n MyRepo --repo-type ""
    

`repo-view` prints `Repository type --> React` when a type is assigned.  Type-owned integrity checkers appear on a separate `Repository type checkers` line; repository `-it` checkers stay on `Integrity checkers`.  See [Integrity Checkers]({{ site.url }}/docs/guides/integrity-checkers/).



### Updating Locales in Repository

    mojito repo-update -n MyRepo -l es-ES fr-FR ja-JP zh-CN zh-TW
    

This updates locales for the repository to `es-ES fr-FR ja-JP zh-CN zh-TW`.


### Renaming Repository

    mojito repo-update -n MyRepo -nn ProjectA
    

This updates the name of the repository from `MyRepo` to `ProjectA`.


### Deleting Repository

    mojito repo-delete -n MyRepo
    

This deletes the repository `MyRepo` and it is no longer visible from the Webapp.
