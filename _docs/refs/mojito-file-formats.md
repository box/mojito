---
layout: doc
title:  "Supported File Formats"
categories: refs
permalink: /docs/refs/mojito-file-formats/
---


| Format                             | Source Resource File                   |
|:-----------------------------------|:---------------------------------------|
| Android Strings &nbsp;&nbsp;&nbsp; | res/values/strings.xml                 |
| CSV File                           | *.csv                                  |
| iOS/Mac Strings                    | Localizable.strings, InfoPList.strings |
| iOS/Mac Stringsdict                | Localizable.stringsdict                |
| Java Properties                    | *.properties                           |
| JS File                            | *.js                                   |
| JSON File                          | *.json                                 |
| RESW                               | *.resw                                 |
| RESX                               | *.resx                                 |
| PO File                            | *.pot                                  |
| TS File                            | *.ts                                   |
| XLIFF                              | *.xlf, *.xliff, *.sdlxliff, *.mxliff   |
| XTB File                           | *.xtb                                  |



### Android Strings Example
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

### iOS/Mac Strings Example
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


### CVS Example

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


### iOS/Mac Stringsdict Example
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


### Java Properties Example
Source Resource File (English): `en.properties`


```properties
# Greeting from Main UI
hello = Hello!
```

Localized Resource File (Spanish): `es.properties`


```properties
# Greeting from Main UI
hello = ¡Hola!
```


### JS Example
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


### JSON Example
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


### RESW Example
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


### RESX Example
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


### TS Example
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


### XLIFF Example
Source Resource File (English): `resource.xliff`  


```xml
<?xml version="1.0" encoding="UTF-8"?>
<xliff xmlns="urn:oasis:names:tc:xliff:document:2.0" version="2.0">
  <file original="" source-language="en" datatype="x-undefined">
    <body>
      <trans-unit id="1" resname="hello" datatype="php">
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
<xliff xmlns="urn:oasis:names:tc:xliff:document:2.0" version="2.0">
  <file original="" source-language="en" datatype="x-undefined" target-language="es-es">
    <body>
      <trans-unit id="1" resname="hello" datatype="php">
        <source>Hello!</source>
        <target xml:lang="es-es">¡Hola!</target>
        <note>Greeting from Main UI</note>
      </trans-unit>
    </body>
  </file>
</xliff>   
```


### PO File Example
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


### XTB Example
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