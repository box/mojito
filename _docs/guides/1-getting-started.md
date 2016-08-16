---
layout: doc
title: Getting Started
categories: guides
permalink: /docs/guides/getting-started/
---

For this guide, we use [Brew](http://brew.sh/) to install {{ site.mojito_green }} Webapp and CLI.

### Setup and install

    brew tap box/mojito
    brew install mojito-cli
    brew install mojito-webapp

### Start the Webapp

    mojito-webapp

This starts the server on [http://localhost:8080](http://localhost:8080).  You can log in with `admin/ChangeMe`.

![start webapp](./images/start-webapp.gif)

### Create a demo repository

    mojito demo-create -n Demo1

It creates a new repository called `Demo1` in the server with some translations.  A resource bundle `demo.properties` is copied on the local directory `Demo1`.

![create demo repository](./images/create-demo.gif)

### Generate the localized files

    cd Demo1
    mojito pull -r Demo1

It goes into `Demo1` directory and generates the localized files. You can see the generated file with `cat demo_fr-FR.properties`.

![create demo repository](./images/generate-localized.gif)

### Add a new string

    printf "\nFOR_DEMO=Add a string for the demo" >> demo.properties
    mojito push -r Demo1

It adds a new string with ID `FOR_DEMO` and English value `Add a string for the demo` in the resource bundle and sends the modified bundle to the server.

![add new string](./images/add-string.gif)

### Translate

Check in the Workbench that the string was added and is untranslated. Try adding a translation.

![translate](./images/translate.gif)

### Generate new localized files

    mojito pull -r Demo1

Finally, this generates the updated localized files with the new translation.  

![update localized filwa](./images/update-localized.gif)


