---
layout: doc
title:  "Managing Locales"
categories: guides
permalink: /docs/guides/managing-locales/
---

In this guide, let's go over locales in Mojito in detail.  Every repository is configured with set of locales.  These locales are used to create translation requests as well as to generate localized files.


We use `mojito-cli` to configure locales in a repository.  Locales can be configured when you create and update repository in Mojito with `-l` parameter.

    mojito repo-create -n MyRepo -l de-DE es-ES fr-FR ja-JP

    mojito repo-update -n MyRepo -l de-DE es-ES fr-FR ja-JP


Mojito requires that the locale name follows BCP 47 syntax with region subtag, in the form of language-region.  See [Mojito Locales]({{ site.github.url }}/docs/refs/mojito-locales/) for the complete list of available locales in Mojito.



### Partially Translated Locales

By default, all locales configured in the repository are required to be fully translated.  These locales get automatically included in the translation requests.

You can configure the locales to be partially translated.  English in United Kingdom (en-GB) is a good example because most of the strings do not need to be "translated".  Source strings can be used as-is in most cases and only some strings that are specific to English in United Kingdom need to be overriden.

```bash
    mojito repo-update -n MyRepo -l "(en-GB)" es-ES fr-FR ja-JP zh-CN zh-TW
```

The above example adds English in United Kingdome (en-GB) to `MyRepo` repository.  Having parenthesis around the locale excludes the locale from being fully translated.



### Locale Inheritance

If the language is the same but the region is different, majority of the translations can be shared.  For example, French in Switzerland (fr-CH) can share translations of French in France (fr-FR).  In such case, you can set up locale inheritance so that translations of parent locale can be re-used as translations of child locale.  The translation requests only include parent locales and the children locales are also considered as partially translated locales.

```bash
    mojito repo-update -n MyRepo -l "(fr-CH)->fr-FR" fr-FR ja-JP zh-CN zh-TW
```

The above example makes French in Switzerland (fr-CH) the child locale of French in France (fr-FR).


Note that the partially translated locales are displayed in grey color in locales list.


![Partially Translated Locales](./images/partially-translated-locales.png)
