---
layout: doc
title:  "Springboot 2 Migration"
categories: refs
permalink: /docs/refs/springboot2migration/
---

This document is meant to help migrate {{ site.mojito_green }} from versions `<= 0.110` to newer versions which actually 
corresponds to upgrading `Spring Boot` from version `1.x` to `2.x`.

Upgrading `Spring boot` was a major change and it was not possible to keep full backward compatibility.

The migration requires to update `build/deploy` scripts and as well as `some configurations`.

## Non backward compatible changes

This is the summary of required changes plus some optional depending on the usage. Details are [following](#migration-details).

Must have:
* `jar` file names have changed --> `build/deploy` scripts must be updated accordingly
* Configuration properties for Database/Datasource, Flyway and Additional configuration location 

Optional:
* OAuth2 major changes in Spring Boot 2x + Mojito's improvment
* Actuator health check path has changed
* Spring session jdbc
* Any non Mojito specific `Spring Boot 1` features that may have been used also need to be migrated
* Logging file
* Tomcat/server configuration

## Noteworthy additions

Everything that `Spring boot 2.x` brings can potentially be re-used in Mojito. A few things to call-out though:

* Improved OAuth2 support: multiple registrations, improved login page (see [details](/docs/guides/authentication/#oauth-2))
* Monitoring with `micrometer` (statds dependency was added)

## Known issue

* `spring-session` with JDBC has some transient failures, fix not identified yet

## Not tested yet

* `BoxSDKServiceTest` - needs account setup, I don't have that handy anymore. Seems like the documentation to create a Box account is outdated too

## Migration details

### Jar file names and build/deploy scripts

**Build/Deploy scripts must be updated to use the new `-exec.jar` files**

In `Spring Boot 1x` the jar files: `mojito-cli-{version}.jar` and `mojito-webapp-{version}.jar` used to contain the whole
 spring boot application and to be executable `jars`. To run the application you could just do `java -jar mojito-cli-{version}.jar`.
Those are the files available in `Github` for version `<= 0.110`.

With `Spring boot 2x` the `jar` files from the `Maven` build are standard `jar` files. They are not re-packaged by spring boot
 to be executable. 

The new executable `jar` files are named with postfix: `-exec.jar`: 
* `mojito-cli-{version}-exec.jar` 
* `mojito-webapp-{version}-exec.jar`

Build script and deploy script must be updated to use those new file names.

## Configuration changes

### Providing additional spring boot configuration

**If `spring.config.location` property was used in any form** (system property, environment variable, etc) to pass extra 
configuration to the spring application, you need to be aware that for `Spring boot 2`, the "additional" property should be used instead.

If you forget to update it, it may not lead to a start failure which will be confusing as the system won't work as expected either.

This is a standard `Spring boot 2` change: 

`spring.config.location` --> `spring.config.additional-location`

### Database/Datasource + Flyway configuration

In `Spring boot 1x` the configuration used to look like:

```
flyway.enabled=true
l10n.flyway.clean=false
spring.jpa.database=MYSQL
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=none
spring.datasource.initialize=false
spring.datasource.url=jdbc:mysql://localhost:3306/mojito?characterEncoding=UTF-8&useUnicode=true&useSSL=false
spring.datasource.username=mojito
spring.datasource.password=
spring.datasource.driverClassName=com.mysql.jdbc.Driver
spring.datasource.testOnBorrow=true
spring.datasource.validationQuery=SELECT 1
```

in `Spring boot 2x` becomes:

```
l10n.flyway.clean=false
spring.flyway.enabled=true
# new for extra security (optional)
spring.flyway.clean-disabled=true
spring.datasource.url=jdbc:mysql://localhost:3306/mojito?characterEncoding=UTF-8&useUnicode=true&useSSL=false
spring.datasource.username=mojito
spring.datasource.password=
```

### OAuth2

See the new configuration for details,

In `Spring boot 1x` the configuration used to look like:

```
l10n.security.oauth2.enabled=true
l10n.security.oauth2.client.clientId={ACTUAL_VALUE}
l10n.security.oauth2.client.clientSecret={ACTUAL_VALUE}
l10n.security.oauth2.client.accessTokenUri=https://github.com/login/oauth/access_token
l10n.security.oauth2.client.userAuthorizationUri=https://github.com/login/oauth/authorize
l10n.security.oauth2.client.useCurrentUri=false
l10n.security.oauth2.client.preEstablishedRedirectUri=http://localhost:8080/login/oauth
l10n.security.oauth2.resource.userInfoUri=https://api.github.com/user
```

in `Spring boot 2x` becomes:

```
l10n.security.authenticationType=DATABASE,OAUTH2
l10n.security.unauth-redirect-to=/login/oauth2/authorization/github

spring.security.oauth2.client.registration.github.client-id={ACTUAL_VALUE}
spring.security.oauth2.client.registration.github.client-secret={ACTUAL_VALUE}
spring.security.oauth2.client.registration.github.provider=github

l10n.security.oauth2.github.ui-label-text=Github
l10n.security.oauth2.github.common-name-attribute=name
```

### Spring session JDBC

This used to be a Mojito specific configuration but is now available in Spring boot:

`l10n.spring.session.store-type=jdbc` --> `spring.session.store-type=jdbc` 

### Actuator health check path has changed

Enable forwarding with: `l10n.actuator.health.legacy.forwarding=true` or update the health check. 

### Some useful Spring Boot 1 configuration mapping

* `logging.file` --> `logging.file.name`
* `server.connection-timeout` --> `server.tomcat.connection-timeout`
* `server.use-forward-headers=true` --> `server.forward-headers-strategy=native`