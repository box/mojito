---
layout: doc
title:  "Open Source Contributors"
date:   2016-02-17 15:25:25 -0800
categories: guides
permalink: /docs/guides/open-source-contributors/
---
## Prerequisites

1. [JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
2. Install [Maven](https://maven.apache.org/download.cgi)
3. Git clone or download Mojito source code to ${PROJECT_DIR}

## Build
    cd ${PROJECT_DIR}/l10n
    mvn clean install -DskipTests=true

## Run Mojito
    cd ${PROJECT_DIR}/l10n/webapp
    npm run start-dev

Mojito should be running on [http://localhost:8080/login](http://localhost:8080/login).  You can login with admin/admin.

Mojito is running with in-memory HSQL DB.  When you restart Mojito, all data is reset.  You can configure Mojito to run with MySQL.  See [Mojito Database Configuration](https://gitenterprise.inside-box.net/Box/l10n/wiki/2.-Configurations#database-configuration) for more information.

## Add Demo Data in Mojito
Make sure Mojito is up and running.

    cd ${PROJECT_DIR}/l10n/webapp
    npm run create-demo-data

This creates Demo repository in Mojito with 21 languages.  17 languages are fully translated.  A Demo directory is created in ${Project_DIR} with source file.

## Run Unit Tests
    cd ${PROJECT_DIR}/l10n
    mvn test





