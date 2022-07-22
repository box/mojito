---
layout: doc
title:  "Installation and Setup (Spring Boot 2 on master)"
categories: guides
permalink: /docs/guides/install-springboot2/
---

## Installation

No binaries are available for the `Spring Boot 2` at the moment. It should be built from the `master` branch.

Assuming `Java 8` is installed, `./mvnw install -DskipTests` should be enough to build the `jar` files.

For detail instructions on development environement setup, [see here]({{ site.url }}/docs/guides/open-source-contributors/). 

### Using Executable Jars

`Java 1.8` is required.

Run the Webapp with:

```bash
java -jar mojito-webapp-*-exec.jar
```
Run the CLI with:

```bash
java -jar mojito-cli-*-exec.jar
```

As {{ site.mojito_green }} is based on Spring Boot, it can be [configured](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config) in many ways.

One simple solution is to add an `application.properties` next to the `jar`. To use a different location use `--spring.config.additional-location=optional:/path/to/your/application.properties`.

### CLI install script

The server provides an entry point to fetch a `bash` script that downloads the latest CLI from the server and create a bash 
wrapper to easily run the CLI.

It can be called with a one liner to make the bash command available rigth away in the current console. Replace 
`http://localhost:8080` with the actual URL if needed. 

```bash
# bash 4:
source <(curl -L -N -s http://localhost:8080/cli/install.sh)

# bash 3 (mac):
source /dev/stdin <<< "$(curl -L -N -s http://localhost:8080/cli/install.sh)"

# Optional: specify the install directory: 
source <(curl -L -N -s http://localhost:8080/cli/install.sh?installDirectory=mydirectory)
```

After that in the current console, `mojito` is available
```bash
mojito -v
```

If the server is running behind a load balancer, use the following setting to make sure the links in the bash script
use the load balancer URL:

```properties
server.forward-headers-strategy=native
```

## Setup

The default setup comes with `HSQL` in-memory database, database authentication and runs on port `8080`.
For production, `MySQL` should be setup. Different types of [authentication](/docs/guides/authentication-springboot2/) are 
available too.

On the first Webapp startup, a user: `admin/ChangeMe` is created. This can be customized with configuration, 
see [Manage Users]({{ site.url }}/docs/guides/manage-users/#bootstraping).

### Server port

The port can be changed with the `server.port` property.

### MySQL

[Install MySQL 5.7](http://dev.mysql.com/doc/refman/5.7/en/installing.html) and then create a database for {{ site.mojito_green }} 
(with Brew: `brew install mysql@5.7`). 

Connect to MySQL DB as root user

```sql
mysql -u root
```

Create user `${DB_USERNAME}` with `${DB_PASSWORD}`

```sql
mysql> CREATE USER '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
```

Create database `${DB_NAME}` and give `${DB_USERNAME}` full access to the database

```sql
mysql> CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_bin';
mysql> GRANT ALL ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
mysql> FLUSH PRIVILEGES;
```

Configure {{ site.mojito_green }} to use MySQL. When using MySQL, Flyway must be turned on and it is strongly 
recommended to explicitly disable the "database clean" features ([more info](#database-protection)). 

```properties
spring.flyway.enabled=true
spring.jpa.defer-datasource-initialization=false
spring.flyway.clean-disabled=true 
l10n.flyway.clean=false
spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?characterEncoding=UTF-8&useUnicode=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver

l10n.org.quartz.jobStore.useProperties=true
l10n.org.quartz.scheduler.instanceId=AUTO
l10n.org.quartz.jobStore.isClustered=true
l10n.org.quartz.threadPool.threadCount=10
l10n.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
l10n.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
l10n.org.quartz.jobStore.dataSource=myDS
l10n.org.quartz.dataSource.myDS.provider=hikaricp
l10n.org.quartz.dataSource.myDS.driver=com.mysql.jdbc.Driver
l10n.org.quartz.dataSource.myDS.URL=jdbc:mysql://localhost:3306/${DB_NAME}?characterEncoding=UTF-8&useUnicode=true
l10n.org.quartz.dataSource.myDS.user=${DB_USERNAME}
l10n.org.quartz.dataSource.myDS.password=${DB_PASSWORD}
l10n.org.quartz.dataSource.myDS.maxConnections=12
l10n.org.quartz.dataSource.myDS.validationQuery=select 1
```

Note that `utf8mb4` setup has been tested on MySQL `5.7`. The server will probably needs some configuration too, for
example by editing `my.cnf` (if installed with brew: `/usr/local/etc/my.cnf`) with something like:

```properties
[client]
default-character-set = utf8mb4

[mysqld]
character-set-server = utf8mb4
```

Depending on the file size that will be processed, it might be required to increase the max allowed package size
    
```properties
[mysqld]
max_allowed_packet = 256M
```
    
If using a older version of MySQL, there is a [known issue](https://github.com/box/mojito/issues/120) when creating the schema. One workaround is to use `utf8`
instead `utf8mb4` but it has its limitation in term of character support.

We recommand to run both MySQL and the Java service using `UTC` timezone (or a least make sure they both the same timezone). To set
`UTC` as default use the following:

```properties
[mysqld]
default-time-zone = '+00:00'
```

### CLI

The default CLI configuration maps to the server default configuration and allows to access the server without
having to enter credential.

To access a production instance, the server url and port should be configured and it is also common to use the console to enter credential.

```properties
l10n.resttemplate.host=${HOSTNAME}
l10n.resttemplate.port=${PORT}
l10n.resttemplate.authentication.credentialProvider=CONSOLE
```

### Database protection

When Flyway is used for DB migration, the Mojito setting to clean the database and the Flyway built-in setting to prevent
 database cleanup are useful features but it can turn out to be very dangerous if wrong values ever leak to production.

It is strongly recommended to explicitly disable the Mojito cleanup feature (it is disabled by default but may prevent bad 
configuration to propagate) and to configure Flyway to disable cleanup as well (this is not the default settings).

In short, recommanded settings are:

```properties
spring.flyway.clean-disabled=true 
l10n.flyway.clean=false
```
An additional protection which is not based on settings is also available. The clean operation can be prevented by
adding a flag in the database using following commands:

```sql
CREATE TABLE flyway_clean_protection(enabled boolean default true);
INSERT INTO flyway_clean_protection (enabled) VALUES (1);
```

Note that this check is optimistic and if for some reason the query fails it will consider that the database not 
 protected. This is just an additional protection in case the settings are missued but you should not rely exclusively
 on it.  

