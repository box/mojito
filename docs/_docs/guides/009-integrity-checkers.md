---
layout: doc
title:  "Integrity Checkers"
categories: guides
permalink: /docs/guides/integrity-checkers/
---

In this guide, let's go over the integrity checkers in {{ site.mojito_green }} in detail.  Integrity checkers perform checks on the translations against the source strings and reject the translations with errors.  This prevents translations with errors from being used in localized resource files which can lead to build faiilure or errors in application.


We use `mojito-cli` to configure integrity checkers in a repository.  Integrity checkers can be configured when you create and update repository in {{ site.mojito_green }} with `-it` parameter.  You can set integrity checker for each file extension of resource files.  For example, `-it resw:COMPOSITE_FORMAT,xlf:PRINTF_LIKE`.

```bash
    mojito repo-create -n MyRepo -it "properties:MESSAGE_FORMAT" -l de-DE es-ES
    
    mojito repo-update -n MyRepo -it "properties:MESSAGE_FORMAT" -l de-DE es-ES
```

### Available Integrity Checkers

| Integrity Checker                      | Recommended File Extensions &nbsp;&nbsp;&nbsp; | File Format                          |
|:---------------------------------------|:------------------------------- ---------------|:-------------------------------------|
| COMPOSITE_FORMAT                       | resw, resx                                     | RESW, RESX                           |
| MESSAGE_FORMAT                         | properties                                     | Java Properties                      |
| PRINTF_LIKE                            | xml, strings,                                  | Android Strings, iOS/Mac Strings,    |
| SIMPLE_PRINTF_LIKE                     |                                                |                                      |
| WHITESPACE                             |                                                |                                      |
| TRAILING_WHITESPACE &nbsp;&nbsp;&nbsp; |                                                |                                      |


### Composite Format Integrity Checker

Composite format integrity checker validates that the placeholders of format `{some-identifier}` in the source string exist in the translation.

The translation gets rejected if any placeholder in the source string is missing in the translation.  There can be multiple placeholders in the source string.  The order of the placeholders can change in the translation. 

| Source String &nbsp;&nbsp;&nbsp; | Translation &nbsp;&nbsp;&nbsp; | Checker                                  |
|:---------------------------------|:-------------------------------|:-----------------------------------------|
| <small>Hello {0}!</small>        | <small>¡Hola {0}!</small>      | <small>OK</small>                        |
| <small>Hello {0}!</small>        | <small>¡Hola!</small>          | <small>FAIL missing placeholder</small>  |
| <small>{0.00}% used</small>      | <small>{0.00} used</small>     | <small>OK</small>                        |
| <small>{0.00}% used</small>      | <small>{0} used</small>        | <small>FAIL modified placeholder</small> |
| <small>{0} with {1}</small>      | <small>{1} con {0}</small>     | <small>OK</small>                        |



### Message Format Integrity Checker

Message format integrity checker validates message format in the translation against [icu4j Message Format](http://icu-project.org/apiref/icu4j/com/ibm/icu/text/MessageFormat.html).

The translation gets rejected if any placeholder in the source string is missing in the translation.  There can be multiple placeholders in the source string.  The order of the placeholders can change in the translation.

Missing curly braces or translating elements within the curly braces also cause the translation to be rejected.

| Source String                                                   | Translation                                                          | Checker           |
|:----------------------------------------------------------------|:---------------------------------------------------------------------|:----------------- |
| <small>{numFiles, plural, one{one file} other{# files}}</small> | <small>{numFiles, plural, one{un fichier} other{# fichiers}}</small> | <small>OK</small> |
| <small>{numFiles, plural, one{one file} other{# files}}</small> | <small>{numFiles, plural, one{un fichier} other{# fichiers}</small>  | <small>FAIL missing closing curly braces</small>  |
| <small>{numFiles, plural, one{one file} other{# files}}</small> | <small>{numFiles, plural, un{un fichier} autre{# fichiers}}</small>  | <small>FAIL translating quantity elements</small> |




### Printf-Like Integrity Checker

Printf-like integrity checker validates that the placeholders in the source string exist in the translation.  The placeholders are in the form of printf specifiers.

The translation gets rejected if any placeholder in the source string is missing in the translation or the specifier is modified.  There can be multiple placeholders in the source string.  The order of the placeholders can change in the translation.

| Source String                                          | Translation                                               | Checker           |
|:-------------------------------------------------------|:----------------------------------------------------------|:------------------|
| <small>Hello %@!</small>                               | <small>¡Hola %@!</small>                                  | <small>OK</small> |
| <small>%1$s of %2$s</small>                            | <small>%2$s의 %1$s</small>                                 | <small>OK</small> |
| <small>%1$d files and %2$d folders</small>             | <small>%1$d fichiers et dossiers</small>                  | <small>FAIL missing placeholder</small> |
| <small>%1$d files and %2$d folders</small>&nbsp;&nbsp; | <small>%1$d fichiers et %2$s dossiers</small>&nbsp;&nbsp; | <small>FAIL modified placeholder</small> |




### Simple Printf-Like Integrity Checker

Simple Printf-like integrity checker validates that the placeholders in the source string exist in the translation.  The placeholders are in the form of `%{number}`, for example, %1, %2, %3, etc.

The translation gets rejected if any placeholder in the source string is missing in the translation.  There can be multiple placeholders in the source string.  The order of the placeholders can change in the translation.

| Source String                                      | Translation                                        | Checker           |
|:---------------------------------------------------|:---------------------------------------------------|:------------------|
| <small>Hello %1!</small>                           | <small>¡Hola %1!</small>                           | <small>OK</small> |
| <small>%1 of %2</small>                            | <small>%2의 %1</small>                              | <small>OK</small> |
| <small>%1 files and %2 folders</small>             | <small>%1 fichiers et dossiers</small>             | <small>FAIL missing placeholder</small> |
| <small>%1 files and %2 folders</small>&nbsp;&nbsp; | <small>fichiers et %2 dossiers</small>&nbsp;&nbsp; | <small>FAIL missing placeholder</small> |




### Whitespace Integrity Checker

Whitespace integrity checker validates that the leading and trailing whitespaces in the source string exist in the translation.

The translation gets rejected if any leading or traingling whitespace in the source string is missing in the translation.

| Source String                                          | Translation                                                           | Checker           |
|:-------------------------------------------------------|:----------------------------------------------------------------------|:------------------|
| <small>[space]Hello %@![newline]</small>               | <small>[space]¡Hola %@![newline]</small>                              | <small>OK</small> |
| <small>[space]%1$d files and %2$d folders</small>      | <small>%1$d fichiers et %2$s dossiers</small>                         | <small>FAIL missing leading space</small>                    |
| <small>%1$d files and %2$d folders[newline]</small>    | <small>%1$d fichiers et %2$s dossiers</small>                         | <small>FAIL missing trailing newline</small>                 |
| <small>[space]%1 files and %2 folders[newline]</small> | <small>[newline]%1 fichiers et %2 dossiers[space]</small>&nbsp;&nbsp; | <small>FAIL modified leading and trailing whitepsace</small> |




### Trailing Whitespace Integrity Checker

Trailing whitespace integrity checker validates that the trailing whitespaces in the source string exist in the translation.

The translation gets rejected if any traingling whitespace in the source string is missing in the translation.

| Source String                                                      | Translation                                                           | Checker           |
|:-------------------------------------------------------------------|:----------------------------------------------------------------------|:------------------|
| <small>Hello %@![space][newline]</small>                           | <small>¡Hola %@![space][newline]</small>                              | <small>OK</small> |
| <small>%1$d files and %2$d folders[space]</small>                  | <small>%1$d fichiers et %2$s dossiers</small>                         | <small>FAIL missing trailng space</small>         |
| <small>%1$d files and %2$d folders[newline]</small>                | <small>%1$d fichiers et %2$s dossiers</small>                         | <small>FAIL missing trailing newline</small>      |
| <small>%1 files and %2 folders[space][newline]</small>&nbsp;&nbsp; | <small>%1 fichiers et %2 dossiers[newline][space]</small>&nbsp;&nbsp; | <small>FAIL modified trailing whitespaces</small> |




### Handling Rejected Translations

While importing offline translations, the integrity checker catches errors and reject translations with errors.  The number of rejected translations show up in the Repository page.

![Repository with Rejected Translation](./images/repository-rejected-translation.png)

Clicking on the number of rejected translations loads the Workbench with rejected translation.  You can correct the translation and change its status to `Needs Review` or `Accepted`.

![Workbench with Rejected Translation](./images/workbench-warning.png)


