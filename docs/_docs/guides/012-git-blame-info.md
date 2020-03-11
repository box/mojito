---
layout: doc
title:  "Text unit information"
categories: guides
permalink: /docs/guides/git-blame-info/
---

When you use `Mojito`, you may need additional information about the text units related to who created them and what
commit introduced them. 

This information about the text unit can be extracted using the `git-blame` command from the `mojito-cli`. This command
uses the [git-blame](https://git-scm.com/docs/git-blame) command to extract the the information about the text unit.

Depending on the file type, the `blame` command can be run on the lines of the source resource files or read in the usage locations from the resource files to use to get the `blame` information from 
the code base.

### Blame source files

The simplest call is

    mojito git-blame -r MyRepo
    
This will look for all supported file types and get the `blame` information for the strings in these files.

For example, if you have a Android strings file (`res/values/strings.xml`)

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="hello" description="Greeting from Main UI">Hello!</string>
    </resources> 
    
Then run 

    mojito git-blame -r MyRepo
    
The command will run `git blame` on the file `res/values/strings.xml` and save the information with the `hello` text unit.

The command can also be run on specified file types using the `-ft` parameter

    mojito git-blame -r MyRepo -ft FileType
    
### Blame with usages
    
Certain file types that include the usage of the text unit in the codebase have a `GitBlameType` defined as 
`TEXT_UNIT_USAGES`. Such file types include PO, MacStrings, and MacStringsdict. This enables extracting the 
information from string location in the code base and not from the source file directly.

For example, if you have a PO file (`messages.pot`) with an entry

    #. Greeting from Main UI
    #: file.js:2
    msgctxt "hello"
    msgid "Hello!"
    msgstr ""
    
Then run

    mojito git-blame -r MyRepo -ft PO
    
The command will extract the location from the file and run `git blame` on the file `file.js` on line 2. 
