---
layout: doc
title:  "Push - String Extraction"
categories: guides
permalink: /docs/guides/string-extraction/
---

In this guide, we use `mojito-cli` to extract strings from source resource file.  Translatable strings are extracted from source resource file and stored in the repository.  This process is called `push` in {{ site.mojito_green }} because extracted strings are "pushed" to {{ site.mojito_green }}.  


For {{ site.mojito_green }} supported source resource files, see [Supported File Formats]({{ site.github.url }}/docs/refs/mojito-file-formats/).


### Push

Let's say we have the following source resource file `strings.properties` in the current working directory.

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye.  Have a nice day!
```


    mojito push -r MyRepo


This extracts the two strings from the source resource file and stores them in `MyRepo` repository.  The two strings are now visible in {{ site.mojito_green }} and ready to be translated in locales configured in the repository.


![Repository Statistics](./images/repository-statistics.png)


![Workbench](./images/workbench.png)


### Overriding Source Directory

    mojito push -r MyRepo -s relativePath/ProjectA

    mojito push -r MyRepo -s /home/explicitPath/ProjectA


By default, {{ site.mojito_green }} searches source resource files from current working directory and its sub-directories.  If you have your source resource file in a specific directory, you can use `-s` parameter to telll {{ site.mojito_green }} where to find source resource files.  The above example extracts strings `strings.properties` from `ProjectA` directory using relative path and explicit path.


### Specific Source File Type

    mojito push -r MyRepo -ft PROPERTIES


By default, {{ site.mojito_green }} processes all supported source resource files in the working directory.  If your working directory has many types of source resource files and if you want to only process specific type, you can use `-ft` parameter.  The above example only extracts strings from Java Properties file.

Available file types are `XLIFF`, `MAC_STRING`, `ANDROID_STRINGS`, `PROPERTIES`, `PROPERTIES_NOBASENAME`, `RESW`, `RESX`, `PO`.  The difference between `PROPERTIES` and `PROPERTIES_NOBASENAME` is that the source resource file of `PROPERTIES_NOBASENAME` has source locale name as the file name. For example, `strings.properties` vs. `en.properties`.


### Overriding Source Locale

    mojito push -r MyRepo -sl en-US -ft PROPERTIES_NOBASENAME


{{ site.mojito_green }} uses `en` as source locale by default.  {{ site.mojito_green }} uses soure locale to identity source resource files from localized resource files.  For example, if you have `en.properties` and `en-US.properties` in your working directory, `en.properties` is used as source resource file by default and `en-US.properties` is considered as localized resource file. The above example overrides the default source locale and use `en-US` as source locale using `-sl` parameter.  You must use `-sl` parameter with `-ft` parameter.


### Specific Source File Regex

Let's say you have the following source resource files in working directory.

    release-1.1.xliff
    release-1.2.xliff
    release-2.1.xliff

You can use regular expression to filter source resource files to push.  The following example only processes release-1 related files using `-sr` parameter for regular expression.

    mojito push -r MyRepo -sr "^(release-1).*$"
