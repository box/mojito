---
layout: doc
title:  "Push - String Extraction"
categories: guides
permalink: /docs/guides/string-extraction/
---

In this guide, we use `mojito-cli` to extract strings from source resource file.  Translatable strings are extracted from source resource file and stored in the repository.  This process is called `push` in Mojito because extracted strings are "pushed" to Mojito.  


For Mojito supported source resource files, see [Mojito Supported File Formats]({{ site.github.url }}/docs/refs/mojito-file-formats/).


## Push

Let's say we have the following source resource file `en.properties`

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye.  Have a nice day!
```


    mojito push -r MyRepo
    

This extracts the two strings from the source resource file assuming that `en.properties` exists in the current working directory.  The two strings are now visible in Mojito and ready to be translated in locales configured in the repository.


![Repository Statistics](./images/repository-statistics.png)


![Workbench](./images/workbench.png)


## Overriding Source Directory

    mojito push -r MyRepo -s relativePath/ProjectA
    
    mojito push -r MyRepo -s /home/explicitPath/ProjectA
    

By default, Mojito searches source resource files from current working directory and its sub-directories.  If you have your source resource file in a specific directory, you can use `-s` parameter to telll Mojito where to find source resource files.  The above example extracts strings `en.properties` from `ProjectA` directory using relative path and explicit path.


## Overriding Source Locale

    mojito push -r MyRepo -sl fr-FR
    

By default, Mojito uses `en` as source locale.  Mojito uses soure locale to identity source resource files from localized resource files.  For example, if you have `en.properties` and `fr-FR.properties` in your working directory, `en.properties` is used as source resource file by default and `fr-FR.properties` is considered as localized resource file. The above example overrides the default source locale and use `fr-FR` as source locale using `-sl` parameter.


## Specific Source File Type

    mojito push -r MyRepo -ft Properties
    

Mojito processes all supported source resource files in the working directory by default.  If your working directory has many types of source resource files and if you want to only process specific type, you can use `-ft` parameter.  The above example only extracts strings from Java Properties file. 


## Specific Source File Regex

    mojito push -r MyRepo -sr "^(release-1).*$"
    

You can also use regex to filter source resource files to push.  For example, if you have `release-1.1.xliff`, `release-1.2.xliff` and `release-2.1.xliff` in your working directory and want to only process release-1 related files, use `-sr` parameter to specify regular expression accordingly.




