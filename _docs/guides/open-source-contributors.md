---
layout: doc
title:  "Open Source Contributors"
date:   2016-02-17 15:25:25 -0800
categories: guides
permalink: /docs/guides/open-source-contributors/
---
## Prerequisites

1. [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
           (9 & 10 are not supported)
2. Install [Maven](https://maven.apache.org/download.cgi) (or use the project Maven wrapper `mvnw`)
3. Git clone or download {{ site.mojito_green }} source code to ${PROJECT_DIR}

You can also [use docker](#docker-image) to setup the build environment.

## Build
    cd ${PROJECT_DIR}
    mvn clean install -DskipTests=true

## Setup `npm`

It is advised to reuse the `npm` version that was downloaded during {{ site.mojito_green }}'s build.

    source webapp/use_local_npm.sh

Else, make sure the global `npm` is compatible.
 

## Run {{ site.mojito_green }}
    cd ${PROJECT_DIR}/webapp
    npm run start-dev

{{ site.mojito_green }} should be running on [http://localhost:8080/login](http://localhost:8080/login).  You can login with admin/ChangeMe.

{{ site.mojito_green }} is running with in-memory HSQL DB.  When you restart {{ site.mojito_green }}, all data is reset.  You can configure {{ site.mojito_green }} to run with MySQL.  See [Database Configuration]({{ site.github.url }}/docs/refs/configurations/#database-configuration) for more information.

## Add Demo Data in {{ site.mojito_green }}
Make sure {{ site.mojito_green }} is up and running.

    cd ${PROJECT_DIR}/webapp
    npm run create-demo-data

This creates Demo repository in {{ site.mojito_green }} with 21 languages.  17 languages are fully translated.  A Demo directory is created in ${Project_DIR} with source file.

## Run Unit Tests
    cd ${PROJECT_DIR}
    mvn test

## DB migration
DB migration is done with Flyway, and Default DB setup with MySql.  The default app configuration uses HSQL hence Flyway is disabled.

When using MySql Flyway must be turned on with `flyway.enabled=true` (for dev or in production).

### Clean the DB
Set the properties `l10n.flyway.clean=true` to have Flyway clean the schema first and then recreate it from the migration files. This will allow during development to do have a similar behavior as `spring.jpa.hibernate.ddl-auto=create`.

### When working on Jpa entities
- Set `spring.jpa.hibernate.ddl-auto=update` so that Hibernate will update the DB schema as you keep doing changes
- Flyway is be used to create or just update the schema depending if the flag was set to clean the DB (`l10n.flyway.clean`)

### To Generate the final migration script before committing code
- Set `l10n.flyway.clean=true`, Flyway will drop the database and recreate the previous version
- Set `spring.jpa.hibernate.ddl-auto=none` so that Hibernate won't perform any change on the DB schema
- Run DDLGenerator to generate the baseline schema to update the database.
- The generated code in: `target/db/migration/Vx__yy_zz.sql` must be adapted (eg. if renaming a column, hibernate will add one, so the statements must be changed)
- Move the update file to: `src/main/resources/db/migration`, rename the file (`x` is the version, `yy_zz` to provide a description: `yy zz`)
- Test the migration script

## Docker image

The `aurambaj/mojito-dev` docker image comes with Java, Maven and some other build dependencies required to build the project easily.

Build reusing host Maven repository (Recommanded):
    
    cd ${PROJECT_DIR}
    docker run -v $(pwd):/mnt/mojito -v ~/.m2:/root/.m2 -it aurambaj/mojito-dev mvn install -DskipTests

Or start the container and run build commands in it

    docker run -v $(pwd):/mnt/mojito -v ~/.m2:/root/.m2 -p 8080:8080 -it aurambaj/mojito-dev bash
    
    # and then build commands
    mvn install -DskipTests
    cd webapp/
    npm run start-dev
    
    
To build from a clean slate:

    docker run -v $(pwd):/mnt/mojito -it aurambaj/mojito-dev mvn install -DskipTests
    
    
## Troubleshooting

### Check Java version 

Make sure you have Java 8 installed:

```sh
[11:05:13] ~/code/mojito (fix_add_text_unit_plural) $ java -version
java version "1.8.0_121"
Java(TM) SE Runtime Environment (build 1.8.0_121-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.121-b13, mixed mode)
```

### Check Maven version

Maven 3.5+ should work fine. Consider trying the Maven wrapper `mvnw` from the project if you have any issue with Maven or don't want to install it.
    
If you have multiple version of Java installed, make sure Maven uses the right version (forth line):

```sh
[01:21:31] ~ $ mvn -version
Apache Maven 3.5.2 (138edd61fd100ec658bfa2d307c43b76940a5d7d; 2017-10-18T00:58:13-07:00)
Maven home: /usr/local/Cellar/maven/3.5.2/libexec
Java version: 1.8.0_121, vendor: Oracle Corporation
Java home: /Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "mac os x", version: "10.13.5", arch: "x86_64", family: "mac"
```

### Caused by: java.lang.NoClassDefFoundError: javax/xml/bind/ValidationException

If you see an error like: 

`Caused by: java.lang.NoClassDefFoundError: javax/xml/bind/ValidationException`

you most likely have a wrong version of Java. Java 8 is required. See [Check Java version](#check-java-version)


