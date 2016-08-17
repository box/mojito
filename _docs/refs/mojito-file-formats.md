---
layout: doc
title:  "Supported File Formats"
categories: refs
permalink: /docs/refs/mojito-file-formats/
---


| Format                             | Source Resource File                   |
|:-----------------------------------|:---------------------------------------|
| Android Strings &nbsp;&nbsp;&nbsp; | res/values/strings.xml                 |
| iOS/Mac Strings                    | Localizable.strings, InfoPList.strings |
| Java Properties                    | *.properties                           |
| RESW                               | *.resw                                 |
| RESX                               | *.resx                                 |
| XLIFF                              | *.xlf, *.xliff, *.sdlxliff, *.mxliff   |


### Android Strings Example
Source Resource File (English): res/values/strings.xml


```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello" description="Greeting from Main UI">Hello!</string>
</resources>
```

Localized Resource File (Spanish): res/values-es/strings.xml


```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello" description="Greeting from Main UI">¡Hola!</string>
</resources>
```

### iOS/Mac Strings Example
Source Resource File (English): en.lproj/Localizable.strings


```c++
/* Greeting from Main UI */
"Hello!" = "Hello!";
```

Localized Resource File (Spanish): es.lproj/Localizable.strings


```c++
/* Greeting from Main UI */
"Hello!" = "¡Hola!";
```


### Java Properties Example
Source Resource File (English): en.properties


```properties
# Greeting from Main UI
hello = Hello!
```

Localized Resource File (Spanish): es.properties


```properties
# Greeting from Main UI
hello = ¡Hola!
```


### RESW Example
Source Resource File (English): en/Resources.resw


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>Hello!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```

Localized Resource File (Spanish): es/Resources.resw


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
Source Resource File (English): Resources.resx


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>Hello!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```

Localized Resource File (Spanish): Resources.es-ES.resx


```xml
<?xml version="1.0" encoding="utf-8"?>
<root>
  <data name="hello" xml:space="preserve">
    <value>¡Hola!</value>
    <comment>Greeting from Main UI</comment>
  </data>
</root>
```


### XLIFF Example
Source Resource File (English): resource.xliff    


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

Localized Resource File (Spanish): resource_es-ES.xliff


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


