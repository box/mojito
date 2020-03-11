---
layout: doc
title:  "Plural Support"
date:   2018-02-22 15:25:25 -0800
categories: guides
permalink: /docs/guides/plural-support/
---

Some platforms and file formats provide support for "plurals". For example, Android
has [Quantity Strings](https://developer.android.com/guide/topics/resources/string-resource.html#Plurals)
and [gettext](https://www.gnu.org/software/gettext/manual/html_node/Plural-forms.html#Plural-forms)
allows to store plural translations in `PO` files that are accessed using `ngettext`. iOS has the
[stringsdict](https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPInternational/StringsdictFileFormat/StringsdictFileFormat.html) 
 file type which is dedicated to plural strings.

`mojito` follows [CLDR](http://cldr.unicode.org/index/cldr-spec/plural-rules)
standard to manage plurals.

The mapping is straight forward for platforms that also use CLDR like Android.
For `gettext`, a mapping is required between CLDR forms and gettext indices.

### Mojito's Workbench
In `mojito` workbench, plural text units are easily identified with the `CLDR`
form displayed as a label next the locale label. Clicking on the plural label
opens the workbench with all different plural forms.

Depending on the locale, the forms shown are customized. For example, while English
has 2 forms (singular and plural), Japanese has a single form and Russian 4 forms.

The 2 forms shown for English in the the workbench:

![create demo repository](./images/wb-english.png)

Single form shown for Japanese:

![create demo repository](./images/wb-japanese.png)

Four forms shown for Russian:

![create demo repository](./images/wb-russian.png)


### Android

Corresponding Android resource file (`res/values/strings.xml`) looks like:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <plurals name="pin_recipe_ingredients_with_count">
        <item quantity="one">%1$d ingredient</item>
        <item quantity="other">%1$d ingredient</item>
    </plurals>
</resources>
```

The Japanese resource file (`res/values-ja/strings.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <plurals name="pin_recipe_ingredients_with_count">
        <item quantity="other">材料：%1$d 種類</item>
    </plurals>
</resources>
```

And the Russian resource file (`res/values-ru/strings.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <plurals name="pin_recipe_ingredients_with_count">
        <item quantity="one">%1$d ингредиент</item>
        <item quantity="few">%1$d ингредиента</item>
        <item quantity="many">%1$d ингредиентов</item>
        <item quantity="other">%1$d ингредиента</item>
    </plurals>
</resources>
```

### Gettext

A corresponding Gettext `PO` file (`messages.pot`) would look like:

```c
msgid "%1$d ingredient"
msgid_plural "%1$d ingredient"
msgstr[0] ""
msgstr[1] ""
```

The Japanese `PO` file (`ja/LC_MESSAGES/messages.po`):

```c
msgid "%1$d ingredient"
msgid_plural "%1$d ingredient"
msgstr[0] "材料：%1$d 種類"
```

And the Russian `PO` file (`ru/LC_MESSAGES/messages.po`):

```c
msgid "%1$d ingredient"
msgid_plural "%1$d ingredient"
msgstr[0] "%1$d ингредиент"
msgstr[1] "%1$d ингредиента"
msgstr[2] "%1$d ингредиентов"
```

### Stringsdict
The corresponding `stringsdict` file (`Localization.stringsdict`) looks like:
```xml
<plist version="1.0">
<dict>
<key>%d ingredient(s)</key>
<dict>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@ingredient@</string>
    <key>ingredient</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>one</key>
        <string>%d ingredient</string>
        <key>other</key>
        <string>%d ingredient</string>
    </dict>
</dict>
</dict>
</plist>
```

The Japanese `stringsdict` file (`ja.lproj/Localization.stringsdict`):
```xml
<plist version="1.0">
<dict>
<key>%d ingredient(s)</key>
<dict>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@ingredient@</string>
    <key>ingredient</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>other</key>
        <string>材料：%d 種類</string>
    </dict>
</dict>
</dict>
</plist>
```

And the Russian `stringsdict` file (`ru.lproj/Localization.stringsdict`):
```xml
<plist version="1.0">
<dict>
<key>%d ingredient(s)</key>
<dict>
    <!-- Header for the recipe ingredients with the ingredients count section -->
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@ingredient@</string>
    <key>ingredient</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>one</key>
        <string>%d ингредиент</string>
        <key>few</key>
        <string>%d ингредиента</string>
        <key>many</key>
        <string>%d ингредиентов</string>
        <key>other</key>
        <string>%d ингредиента</string>
    </dict>
</dict>
</dict>
</plist>
```
