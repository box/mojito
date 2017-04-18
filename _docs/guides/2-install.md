---
layout: doc
title:  "Installation and Setup"
categories: guides
permalink: /docs/guides/install/
---

## Installation

### Using Brew

Install [Brew](http://brew.sh/) if needed and run following commands:

    brew tap box/mojito
    brew install mojito-cli
    brew install mojito-webapp

Run the Webapp with:

    mojito-webapp

Run the CLI with:

    mojito
    
Default configuration location:
    
    usr/local/etc/mojito/cli/application.properties
    usr/local/etc/mojito/webapp/application.properties
            
### Using Executable Jars

Executable Jars can be downloaded in the [release section](https://github.com/box/mojito/releases/).

Run the Webapp with:

    java -jar mojito-webapp-*.jar 

Run the CLI with:

    java -jar mojito-cli-*.jar

As {{ site.mojito_green }} is based on Spring Boot, it can be [configured](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config) in many ways.
       
One simple solution is to add an `application.properties` next to the `jar`. To use a different location use `--spring.config.location=/path/to/your/application.properties`.

The following JVM options might be useful to run the Webapp: `-XX:MaxPermSize=128m -Xmx1024m`. 

## Setup

The default setup comes with `HSQL` in-memory database, database authentication and runs on port `8080`.
For production, `MySQL` should be setup. It is also possible to use [LDAP](/docs/guides/ldap-authentication) for authentication.

On the first Webapp startup, a user: `admin/ChangeMe` is created. This can be customized with configuration, see [Manage Users]({{ site.github.url }}/docs/guides/manage-users/#bootstraping). 

### Server port

The port can be changed with the `server.port` property.

### MySQL

[Install MySQL](http://dev.mysql.com/doc/refman/5.7/en/installing.html) and then create a database for {{ site.mojito_green }}.

Connect to MySQL DB as root user

    mysql -u root

Create user `${DB_USERNAME}` with `${DB_PASSWORD}`

    mysql> CREATE USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';

Create database `${DB_NAME}` and give `${DB_USERNAME}` full access to the database

    mysql> CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_bin';
    mysql> GRANT ALL ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    mysql> FLUSH PRIVILEGES;

Configure {{ site.mojito_green }} to use MySQL. When using MySQL, Flyway must be turned on.

    flyway.enabled=true
    l10n.flyway.clean=false
    spring.jpa.database=MYSQL
    spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
    spring.jpa.hibernate.ddl-auto=none
    spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?characterEncoding=UTF-8&useUnicode=true
    spring.datasource.username=${DB_USERNAME}
    spring.datasource.password=${DB_PASSWORD}
    spring.datasource.driverClassName=com.mysql.jdbc.Driver


Note that `utf8mb4` setup has been tested on MySQL `5.7`. The server will probably needs some configuration too, for
example by editing `my.cnf` (if installed with brew: `/usr/local/etc/my.cnf`) with something like:

    [client]
    default-character-set = utf8mb4

    [mysqld]
    character-set-server = utf8mb4

If using a older version of MySQL, there is a [known issue](https://github.com/box/mojito/issues/120) when creating the schema. One workaround is to use `utf8`
instead `utf8mb4` but it has its limitation in term of character support.
    
### CLI

The default CLI configuration maps to the server default configuration and allows to access the server without
having to enter credential. 

To access a production instance, the server url and port should be configured and it is also common to use the console to enter credential.

    l10n.resttemplate.host=${HOSTNAME}
    l10n.resttemplate.port=${PORT}
    l10n.resttemplate.authentication.credentialProvider=CONSOLE





