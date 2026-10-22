---
layout: doc
title:  "Repository Types"
categories: guides
permalink: /docs/guides/repository-types/
---

A repository type is shared configuration for repositories of the same kind, such as React or Android.  Create the type once in {{ site.mojito_green }}, then assign it to each repository that should use those settings.


You change integrity checkers in one place instead of repeating them on every repository.  A repository can stay untyped and keep using only its own settings.


### Creating a Repository Type

    mojito repo-type-create -n React -d "Repo type for React applications"
    

This creates a repository type named `React`.  The name is required and must be unique.  The description is optional.


Add integrity checkers with `-it` / `--integrity-check`, using the same `FILE_EXTENSION:CHECKER_TYPE` pairs as on a repository.  Separate pairs with commas.  See [Integrity Checkers]({{ site.url }}/docs/guides/integrity-checkers/) for the checker types.


    mojito repo-type-create -n React -it "properties:MESSAGE_FORMAT,properties:TRAILING_WHITESPACE"
    

Leave `-it` off to create the type with no checkers.


A type can also store one AI prompt.  Pass the text with `--ai-prompt`, or a UTF-8 file with `--ai-prompt-file`.  The two flags cannot be used together.  Mojito saves the prompt on the type.  AI translation and review do not use it yet.


    mojito repo-type-create -n React --ai-prompt-file react-prompt.txt
    

### Listing Repository Types

    mojito repo-type-list
    

This prints every repository type: id, name, and description.  When an AI prompt is set, the line shows `contains a value` and not the prompt text.  Integrity checkers are a count, such as `contains 2 values`, or blank when the type has none.


    mojito repo-type-list --verbose
    

`--verbose` (short `-vb`) prints the AI prompt and each integrity checker as `extension:TYPE`.


### Viewing a Repository Type

    mojito repo-type-view -n React
    

This prints the id, name, description, and AI prompt for `React` repo type.  Integrity checkers are printed as comma-separated `extension:TYPE` pairs.  That line is omitted when the type has no checkers.


### Updating a Repository Type

Pass at least one option besides the name.  `-n` is the current name.


    mojito repo-type-update -n React --new-name ReactWeb
    

This renames `React` to `ReactWeb`.  `--new-name` (short `-nn`) sets the new name.


    mojito repo-type-update -n React -d "Web React applications"
    

This replaces the description.


`-it` on update replaces the whole set of integrity checkers.  Leave the flag off to keep the current checkers.  An empty value clears them.


    mojito repo-type-update -n React -it "properties:MESSAGE_FORMAT"
    
    mojito repo-type-update -n React -it ""
    

An empty `--ai-prompt` clears the prompt.  To clear it with `--ai-prompt-file`, pass a UTF-8 file that is empty after Mojito strips a leading BOM and one trailing newline.  Leaving both flags off keeps the current prompt.


    mojito repo-type-update -n React --ai-prompt ""
    

### Deleting a Repository Type

    mojito repo-type-delete -n React
    

This deletes the repository type `React`.  The command fails while any repository is still assigned to that type.  Clear those assignments first, then delete the type.


### Assigning a Repository Type

The type must already exist.  Assign it when you create a repository, or later.  See [Creating Repository]({{ site.url }}/docs/guides/creating-repository/).


    mojito repo-create -n MyRepo --repo-type React -l fr-FR
    
    mojito repo-update -n MyRepo --repo-type React
    

Omitting `--repo-type`, or passing an empty value, creates an untyped repository.  An empty value on update clears the assignment.


    mojito repo-update -n MyRepo --repo-type ""
    

Assigning a type does not replace the repository's own `-it` checkers.  At check time {{ site.mojito_green }} runs the union of the type's checkers and the repository's checkers.  `repo-view` prints `Repository type --> React` when a type is assigned, and lists that type's checkers on `Repository type checkers`.  See [Integrity Checkers]({{ site.url }}/docs/guides/integrity-checkers/).
