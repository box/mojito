---
layout: doc
title:  "Supported File Formats"
categories: refs
permalink: /docs/refs/mojito-file-formats/
---


| Format                             | Source Resource File                   |
|:-----------------------------------|:---------------------------------------|
| Android Strings                    | res/values/strings.xml                 |
| CSV File                           | *.csv                                  |
| iOS/Mac Strings                    | Localizable.strings, InfoPList.strings |
| iOS/Mac Stringsdict                | Localizable.stringsdict                |
| Java Properties                    | *.properties                           |
| JS File                            | *.js                                   |
| JSON File                          | *.json                                 |
| Chrome extension JSON &nbsp;&nbsp; | _locales/{locale}/messages.json                                 |
| RESW                               | *.resw                                 |
| RESX                               | *.resx                                 |
| PO File                            | *.pot                                  |
| TS File                            | *.ts                                   |
| XLIFF                              | *.xlf, *.xliff, *.sdlxliff, *.mxliff   |
| XTB File                           | *.xtb                                  |



### Android Strings
Source Resource File (English): `res/values/strings.xml`


```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello" description="Greeting from Main UI">Hello!</string>
</resources>
```

Localized Resource File (Spanish): `res/values-es/strings.xml`


```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello" description="Greeting from Main UI">¡Hola!</string>
</resources>
```

### iOS/Mac Strings
Source Resource File (English): `en.lproj/Localizable.strings`


```c++
/* Greeting from Main UI */
"Hello!" = "Hello!";
```

Localized Resource File (Spanish): `es.lproj/Localizable.strings`


```c++
/* Greeting from Main UI */
"Hello!" = "¡Hola!";
```


### CSV

| Column Number | Description |
|:--------------|:------------|
| 1             | Source ID   |
| 2             | Source      |
| 3             | Target      |
| 4             | Comment     |
|||

Source Resource File (English): `example.csv`


```csv
hello,Hello!,Hello!,Greeting from Main UI
```

Localized Resource File (Spanish): `example_es-ES.csv`


```csv
hello,Hello!,¡Hola!,Greeting from Main UI
```


### iOS/Mac Stringsdict
Source Resource File (English): `en.lproj/Localizable.stringsdict`


```xml
<plist version="1.0">
<dict>
<key>Hello %d world(s)</key>
<dict>
    <!-- Greeting from Main UI -->
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@world@</string>
    <key>world</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>one</key>
        <string>Hello %d world</string>
        <key>other</key>
        <string>Hello %d worlds</string>
    </dict>
</dict>
</dict>
</plist>
```

Localized Resource File (Spanish): `es.lproj/Localizable.stringsdict`


```xml
<plist version="1.0">
<dict>
<key>Hello %d world(s)</key>
<dict>
    <!-- Greeting from Main UI -->
    <key>NSStringLocalizedFormatKey</key>
    <string>%#@world@</string>
    <key>world</key>
    <dict>
        <key>NSStringFormatSpecTypeKey</key>
        <string>NSStringPluralRuleType</string>
        <key>NSStringFormatValueTypeKey</key>
        <string>d</string>
        <key>one</key>
        <string>Hola %d mundo</string>
        <key>other</key>
        <string>Hola %d mundos</string>
    </dict>
</dict>
</dict>
</plist>
```


### Java Properties

3 flavors are supported: `PROPERTIES` (`UTF-8`), `PROPERTIES_JAVA` (`ISO-8891`), `PROPERTIES_NOBASENAME` (`UTF-8` + `ISO-8859`)

Source Resource File (English): `en.properties` (no basename) or `messages.properties` (with basename)

```properties
# Greeting from Main UI
hello = Hello!
```

Localized Resource File (Spanish): `es.properties` (no basename) or `messages_es.properties` (with basename)


```properties
# Greeting from Main UI
hello = ¡Hola!
```


### JS
Source Resource File (English): `en.js`


```jsx
export default {
  // Greeting from Main UI
  "hello": "Hello!"
}
```

Localized Resource File (Spanish): `es.js`


```jsx
export default {
  // Greeting from Main UI
  "hello": "¡Hola!"
}
```


### JSON
Source Resource File (English): `example.json`


```jsx
{
  // Greeting from Main UI
  "hello": "Hello!"
}
```

Localized Resource File (Spanish): `example_es-ES.json`


```jsx
{
  // Greeting from Main UI
  "hello": "¡Hola!"
}
```

### Chrome extension JSON

See [chrome.i18n documentation](https://developer.chrome.com/extensions/i18n) documentation for more details

Source Resource File (English): `_locales/en/messages.json`
```json
{
  "hello": {
    "message": "Hello!",
    "description": "Greeting from Main UI"
  }
}
```

Localized Resource File (Spanish): `_locales/es-ES/messages.json`
```json
{
  "hello": {
    "message": "¡Hola!",
    "description": "Greeting from Main UI"
  }
}
```

### RESW
Source Resource File (English): `en/Resources.resw`


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>Hello!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```

Localized Resource File (Spanish): `es/Resources.resw`


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>¡Hola!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```


### RESX
Source Resource File (English): `Resources.resx`


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>Hello!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```

Localized Resource File (Spanish): `Resources.es-ES.resx`


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>¡Hola!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```


### TS
Source Resource File (English): `en.ts`


```jsx
namespace Translations {
  // Greeting from Main UI
  "hello": "Hello!"
}

export default Translations;
```

Localized Resource File (Spanish): `es.ts`


```jsx
namespace Translations {
  // Greeting from Main UI
  "hello": "¡Hola!"
}

export default Translations;
```


### XLIFF
Source Resource File (English): `resource.xliff`  


```xml
<?xml version="1.0" encoding="UTF-8"?>
<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" xmlns:okp="okapi-framework:xliff-extensions" version="1.2">
  <file original="" source-language="en" datatype="x-undefined">
    <body>
      <trans-unit id="hi">
        <source>Hello!</source>
        <note>Greeting from Main UI</note>
      </trans-unit>
    </body>
  </file>
</xliff>   
```

Localized Resource File (Spanish): `resource_es-ES.xliff`


```xml
<?xml version="1.0" encoding="UTF-8"?>
<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" xmlns:okp="okapi-framework:xliff-extensions" version="1.2">
  <file original="" source-language="en" target-language="es-es" datatype="plaintext">
    <body>
      <trans-unit id="hi">
        <source>Hello!</source>
        <target xml:lang="es-es">¡Hola!</target>
        <note>Greeting from Main UI</note>
      </trans-unit>
    </body>
  </file>
</xliff>
```


### PO File
Source Resource File (English): `messages.pot`   


```c
msgid ""
msgstr ""
"Project-Id-Version: PACKAGE VERSION\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 2017-02-24 11:50-0800\n"
"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\n"
"Last-Translator: FULL NAME <EMAIL@ADDRESS>\n"
"Language-Team: LANGUAGE <LL@li.org>\n"
"Language: \n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=utf-8\n"
"Content-Transfer-Encoding: 8bit\n"

#. Greeting from Main UI
#: file.js:2
msgctxt "hello"
msgid "Hello!"
msgstr ""
```

Localized Resource File (Spanish): `es_ES/LC_MESSAGES/messages.po`


```c
msgid ""
msgstr ""
"Project-Id-Version: PACKAGE VERSION\n"
"Report-Msgid-Bugs-To: \n"
"POT-Creation-Date: 2017-02-24 11:50-0800\n"
"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\n"
"Last-Translator: FULL NAME <EMAIL@ADDRESS>\n"
"Language-Team: LANGUAGE <LL@li.org>\n"
"Language: \n"
"MIME-Version: 1.0\n"
"Content-Type: text/plain; charset=utf-8\n"
"Content-Transfer-Encoding: 8bit\n"

#. Greeting from Main UI
#: file.js:2
msgctxt "hello"
msgid "Hello!"
msgstr "¡Hola!"
```


### XTB
Source Resource File (English): `Example-en-US.xtb`


```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE translationbundle>
<translationbundle lang="en-US">
    <translation id="1" key="hello" source="example.js">Hello!</translation>
</translationbundle>
```

Localized Resource File (Spanish): `Example-es-ES.xtb`


```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE translationbundle>
<translationbundle lang="en-US">
    <translation id="1" key="hello" source="example.js">¡Hola!</translation>
</translationbundle>
```