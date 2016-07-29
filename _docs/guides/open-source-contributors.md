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

Mojito is running with in-memory HSQL DB.  When you restart Mojito, all data is reset.  You can configure Mojito to run with MySQL.  See [Database Configuration]({{ site.github.url }}/docs/refs/configurations/#database-configuration) for more information.

## Add Demo Data in Mojito
Make sure Mojito is up and running.

    cd ${PROJECT_DIR}/l10n/webapp
    npm run create-demo-data

This creates Demo repository in Mojito with 21 languages.  17 languages are fully translated.  A Demo directory is created in ${Project_DIR} with source file.

## Run Unit Tests
    cd ${PROJECT_DIR}/l10n
    mvn test

## DB migration
DB migration is done with Flyway, and Default DB setup with MySql.  The default app configuration uses HSQL hence Flyway is disabled.

When using MySql Flyway must be turned on with `flyway.enabled=true` (for dev or in production).

### Clean the DB
Set the properties l10n.flyway.clean=true to have Flyway clean the schema first and then recreate it from the migration files. This will allow during development to do have a similar behavior as `spring.jpa.hibernate.ddl-auto=create`.

### When working on Jpa entities
- Set spring.jpa.hibernate.ddl-auto=update so that Hibernate will update the DB schema as you keep doing changes
- Flyway is be used to create or just update the schema depending if the flag was set to clean the DB (`l10n.flyway.clean`)

### To Generate the final migration script before committing code
- Set `l10n.flyway.clean=true`, Flyway will drop the database and recreate the previous version
- Set `spring.jpa.hibernate.ddl-auto=none` so that Hibernate won't perform any change on the DB schema
- Run DDLGenerator to generate the baseline schema to update the database.
- The generated code in: `target/db/migration/Vx__yy_zz.sql` must be adapted (eg. if renaming a column, hibernate will add one, so the statements must be changed)
- Move the update file to: `src/main/resources/db/migration`, rename the file (`x` is the version, `yy_zz` to provide a description: `yy zz`)
- Test the migration script




