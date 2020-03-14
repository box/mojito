---
layout: doc
title:  "Configurations"
date:   2016-02-17 15:25:25 -0800
categories: refs
permalink: /docs/refs/configurations/
---
## Configuration Location

{{ site.mojito_green }} configuration loaded from the following files in order:

    classpath:/application.properties                    # default config
    /usr/local/etc/mojito/cli/application.properties     # override config for mojito cli
    /usr/local/etc/mojito/webapp/application.properties  # override config for mojito webapp

To override default configurations of {{ site.mojito_green }}, add them in

    /usr/local/etc/mojito/cli/application.properties     # for mojito cli
    /usr/local/etc/mojito/webapp/application.properties  # for mojito webapp

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

[Install MySQL 5.7](http://dev.mysql.com/doc/refman/5.7/en/installing.html) and then create a database for {{ site.mojito_green }} 
(with Brew: `brew install mysql@5.7`). 

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
    spring.datasource.testOnBorrow=true
    spring.datasource.validationQuery=SELECT 1
    
    l10n.org.quartz.jobStore.useProperties=true
    l10n.org.quartz.scheduler.instanceId=AUTO
    l10n.org.quartz.jobStore.isClustered=true
    l10n.org.quartz.threadPool.threadCount=10
    l10n.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
    l10n.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
    l10n.org.quartz.jobStore.dataSource=myDS
    l10n.org.quartz.dataSource.myDS.driver=com.mysql.jdbc.Driver
    l10n.org.quartz.dataSource.myDS.URL=jdbc:mysql://localhost:3306/${DB_NAME}?characterEncoding=UTF-8&useUnicode=true
    l10n.org.quartz.dataSource.myDS.user=${DB_USERNAME}
    l10n.org.quartz.dataSource.myDS.password=${DB_PASSWORD}
    l10n.org.quartz.dataSource.myDS.maxConnections=12
    l10n.org.quartz.dataSource.myDS.validationQuery=select 1


Note that `utf8mb4` setup has been tested on MySQL `5.7`. The server will probably needs some configuration too, for
example by editing `my.cnf` (if installed with brew: `/usr/local/etc/my.cnf`) with something like:

    [client]
    default-character-set = utf8mb4

    [mysqld]
    character-set-server = utf8mb4

Depending on the file size that will be processed, it might be required to increase the max allowed package size
    
    [mysqld]
    max_allowed_packet = 256M
    

If using a older version of MySQL, there is a [known issue](https://github.com/box/mojito/issues/120) when creating the schema. One workaround is to use `utf8`
instead `utf8mb4` but it has its limitation in term of character support.

We recommand to run both MySQL and the Java service using `UTC` timezone (or a least make sure they both the same timezone). To set
`UTC` as default use the following:

```properties
[mysqld]
default-time-zone = '+00:00'
```


## Server Configuration

The default server configuration of {{ site.mojito_green }} to run on port 8080.

	server.port=8080


### Project Request Configuration

The project request configuration is for the offline translation requests.  These settings define where {{ site.mojito_green }} stores xliff files.

By default, {{ site.mojito_green }} uses local file system to manage xliff files.

    l10n.dropExporter.type=FILE_SYSTEM
    l10n.fileSystemDropExporter.basePath=(Java system property for java.io.tmpdir)/fileSystemDropExporter

Let's say that `java.io.tmpdir` is `/tmp`.  When you create a new project request for a repository, {{ site.mojito_green }} generates the following directories.

    /tmp/fileSystemDropExporter/<repository name>/<project name>
        |-> Imported Files
        |-> Localized Files
        |-> Queries
        |-> Quotes
        |-> Source Files
            |-> fr-FR_mm-dd-yy.xliff
            |-> ja-JP_mm-dd-yy.xliff

{{ site.mojito_green }} exports xliff files in `Source Files` directory.  You should give them to the translators to translate.

When translators are done, translated xliff files should be put in the `Localized Files` directory.  {{ site.mojito_green }} imports xliff files from this directory.

You can override this default configuration and have project requests to be managed on Box instead of local file system.  Refer to [Integrating with Box]({{ site.url }}/docs/guides/integrating-with-box/).


### Database Authentication

The default user authentication setting in {{ site.mojito_green }} is to use database.  User information is stored in database.  {{ site.mojito_green }} initially is set up with one default user `admin/ChangeMe`.  You can override the default user settings.  These values are only respected on initial bootstrapping.

    l10n.security.authenticationType=DATABASE
    l10n.bootstrap.defaultUser.username=admin
    l10n.bootstrap.defaultUser.password=ChangeMe

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
    l10n.resttemplate.authentication.password=ChangeMe

If you want to authenticate the user running CLI on command-line, set the following configuration to prompt the user for the password.

    l10n.resttemplate.authentication.credentialProvider=CONSOLE

Please note that the the username defaults to the current user (Java system property for user.name) running the CLI instead of `admin`.  Before updating this configuration, it is strongly recommended to add usernames of users that would run the CLI as described in [Database Authentication]({{ site.url }}/docs/refs/configurations/#database-authentication).

You can override Java system property for user.name with `-Duser.name=admin`.  For example, to run {{ site.mojito_green }} CLI as `admin` user, enter the following:

    java -Duser.name=admin -jar mojito-cli-<version>.jar <cli-commands>

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
