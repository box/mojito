---
layout: doc
title: Getting Started
categories: guides
permalink: /docs/guides/getting-started/
---

For this guide, we use `brew` to install Mojito Webapp and CLI.

## Setup and install


    brew tap box/mojito
    brew install mojito-cli
    brew install mojito-webapp



## Start the Webapp

    mojito-webapp


To start the server on [http://localhost:8080](http://localhost:8080), log in with `admin/admin`.

![start webapp](./images/start-webapp.gif)


## Create a demo repository


    mojito demo-create -n Demo1

Create a new repository called `Demo1` in the server initialized
with some translation. A resource bundle `demo.properties` is copied on the
local directory `Demo1`.

![create demo repository](./images/create-demo.gif)



## Generate the localized files

    cd Demo1
    mojito pull -r Demo1

Move into `Demo1` directory and generate the localized fies. Check generated
file with `cat demo_fr-FR.properties`.

![create demo repository](./images/generate-localized.gif)



## Add a new string

    printf "\nFOR_DEMO=Add a string for the demo" >> demo.properties
    mojito push -r Demo1


Add a new string with ID: `FOR_DEMO` and english: `Add a string for the demo`
into the resource bundle. `push` the modified bundle to the server.

![add new string](./images/add-string.gif)


## Translate

Check in the Webapp that the string was added and is untranslated. Add a translation.

![translate](./images/translate.gif)


## Generate new localized files

    mojito pull -r Demo1


Finally, generate the updated localized files and check that the new
translation is present.

![update localized filwa](./images/update-localized.gif)


