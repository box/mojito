---
layout: doc
title:  "Configurations"
date:   2016-02-17 15:25:25 -0800
categories: refs
permalink: /docs/refs/configurations/
---
## Configuration Location

{{ site.mojito_green }} configuration loaded from the following files in order:

    classpath:/application.properties                           # default config
    /usr/local/etc/mojito/cli/application.properties            # override config

To override default configurations of {{ site.mojito_green }}, add them in

    /usr/local/etc/mojito/cli/application.properties

If you want to use different path to store the override configuration, you can specify the following extra parameter when you start {{ site.mojito_green }} server and when you run {{ site.mojito_green }} CLI.  For example,

    -Dspring.config.location=file:/${YOUR_PATH}/application.properties


## Database Configuration

The default database configuration of {{ site.mojito_green }} is in-memory HSQL database.

    flyway.enabled=false
    spring.jpa.database=HSQL
    spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect
    spring.jpa.hibernate.ddl-auto=update
    spring.datasource.initialize=true
    spring.datasource.data=classpath:/db/hsql/data.sql


You can override the database configuration with MySQL.
First, install MySQL from http://dev.mysql.com/doc/refman/5.7/en/installing.html.  Then Configure MySQL.
Connect to MySQL DB as root user

    mysql -uroot

Create user `${DB_USERNAME}` with `${DB_PASSWORD}`

    mysql> CREATE USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';

Create database `${DB_NAME}` and give `${DB_USERNAME}` full access to the database

    mysql> CREATE DATABASE IF NOT EXISTS ${DB_NAME};
    mysql> GRANT ALL ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    mysql> FLUSH PRIVILEGES;

Configure {{ site.mojito_green }} to use MySQL.  When using MySQL, Flyway must be turned on.

    flyway.enabled=true
    l10n.flyway.clean=false
    spring.jpa.database=MYSQL
    spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
    spring.jpa.hibernate.ddl-auto=none
    spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?characterEncoding=UTF-8&useUnicode=true
    spring.datasource.username=${DB_USERNAME}
    spring.datasource.password=${DB_PASSWORD}
    spring.datasource.driverClassName=com.mysql.jdbc.Driver


## Server Configuration

The default server configuration of {{ site.mojito_green }} to run on port 8080.

	server.port=8080

### Database Authentication

The default user authentication setting in {{ site.mojito_green }} is to use database.  User information is stored in database.  {{ site.mojito_green }} initially is set up with one default user `admin/admin`.  You can override the default user settings.  These values are only respected on initial bootstrapping.

    l10n.security.authenticationType=DATABASE
    l10n.bootstrap.defaultUser.username=admin
    l10n.bootstrap.defaultUser.password=admin

With database authentication, {{ site.mojito_green }} users can be added, updated (with new password) and deleted using {{ site.mojito_green }} CLI.

    # add user - enter password when promted
    mojito user-create  --username ${USERNAME} --password --surname ${SURNAME} --given-name ${GIVEN_NAME} --common-name ${COMMON_NAME}

    # update password - enter password when promted
    mojito user-update --username ${USERNAME} --password

    # delete user
    mojito user-delete --username ${USERNAME}   

### LDAP Authentication

You can override the user authentication setting to use LDAP.  Here are the settings required to use LDAP.

    l10n.security.authenticationType=LDAP
    l10n.security.ldap.url=${URL}
    l10n.security.ldap.port=${PORT}
    l10n.security.ldap.root=${ROOT}
    l10n.security.ldap.userSearchBase=${USER_SEARCH_BASE}
    l10n.security.ldap.userSearchFilter=${USER_SEARCH_FILTER}
    l10n.security.ldap.groupSearchBase=${GROUP_SEARCH_BASE}
    l10n.security.ldap.groupSearchFilter=${GROUP_SEARCH_FILTER}
    l10n.security.ldap.groupRoleAttribute=${GROUP_ROLE_ATTR}
    l10n.security.ldap.managerDn=${MANAGER_DN}
    l10n.security.ldap.managerPassword=${MANAGER_PASSWORD}
    l10n.security.ldap.ldif=${LDIF_FILE}


## CLI Configuration

The default CLI configuration of {{ site.mojito_green }} is to connect to [http://localhost:8080](http://localhost:8080) with admin user.

    l10n.resttemplate.host=localhost
    l10n.resttemplate.port=8080
    l10n.resttemplate.scheme=http
    l10n.resttemplate.authentication.credentialProvider=CONFIG
    l10n.resttemplate.authentication.username=admin
    l10n.resttemplate.authentication.password=admin

If you want to authenticate the user running CLI on command-line, update the following.  Enter password for ${USER} when promtped.

    l10n.resttemplate.authentication.credentialProvider=CONSOLE
    #l10n.resttemplate.authentication.username=admin   # set to ${USER} at run-time
    #l10n.resttemplate.authentication.password=admin   # obtained at run-time


## Box Platform Integration

### Custom Configurations
All `l10n.boxclient.*` configurations are required for `BoxSDK` Tests to run.

    l10n.boxclient.clientId={REQUIRED_FOR_TEST}
    l10n.boxclient.clientSecret={REQUIRED_FOR_TEST}
    l10n.boxclient.publicKeyId={REQUIRED_FOR_TEST}
    l10n.boxclient.privateKey={REQUIRED_FOR_TEST}
    l10n.boxclient.privateKeyPassword={REQUIRED_FOR_TEST}
    l10n.boxclient.enterpriseId={REQUIRED_FOR_TEST}
    l10n.boxclient.appUserId={REQUIRED_FOR_TEST}

    l10n.boxclient.rootFolderId={REQUIRED_FOR_TEST}
    l10n.boxclient.dropsFolderId={REQUIRED_FOR_TEST}


- `l10n.boxclient.rootFolderId` corresponds to the `mojito` folder
- `l10n.boxclient.dropsFolderId` corresponds to the `Project Requests` folder.

You can also optionally use it in production by setting the following configuration:

    l10n.boxclient.useConfigsFromProperties=true


### Box App User
1. A default Box App User is created when the Box Client Configurations are set.
2. {{ site.mojito_green }} will create the following folder structure under the App User's root folder.

   The ID of the newly created `mojito` folder will be stored and used as the rootFolderId

        <UserRootRolder>
        |-> mojito
          |-> Project Requests

   Most of the time, this folder will be used to exchange files with others.
   Follow instructions [here](https://community.box.com/t5/For-Admins/How-Do-I-Share-Files-And-Folders-From-The-Admin-Console/ta-p/211) to share with collaborators.

   Tips: Access the Box Account as the Admin of the enterprise using the credentials for the Box Developer Account you created.





