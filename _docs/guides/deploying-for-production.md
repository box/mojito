---
layout: doc
title:  "Deploying for Production"
date:   2016-02-17 15:25:25 -0800
categories: guides
permalink: /docs/guides/deploying-for-production/
---
## Installation
#### Brew

TODO: Can we reference the getting started section? or need to duplicate?

Run the CLI with:
```
mojito
```

Run the Webapp with:
```
mojito-webapp
```

#### Download Jars

Executable Jars can be downloaded in the [release section](https://gitenterprise.inside-box.net/Box/l10n/releases/).

Run the CLI with:
```
java -jar mojito-cli-*.jar
```

Run the Webapp with:
```
java -jar mojito-webapp-*.jar
```

## Database Configuration
    spring.jpa.database=MYSQL
    spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
    spring.jpa.hibernate.ddl-auto=none
    flyway.enabled=true

    spring.datasource.url=
    spring.datasource.username=
    spring.datasource.password=
    spring.datasource.driverClassName=com.mysql.jdbc.Driver
    spring.datasource.validation-query=SELECT 1
    spring.datasource.test-on-borrow=true

## Default admin user
If you're using database authentication, you can choose a unique password.

    l10n.security.authenticationType=DATABASE
    l10n.bootstrap.defaultUser.username=admin
    l10n.bootstrap.defaultUser.password=admin

## Increase PermSize and Mem size
As the number of strings you have increases, jvm will need to have access to more memory.  The following options might be useful.

    -Xms512m
    -Xmx1024m
    -XX:MaxPermSize=128m





