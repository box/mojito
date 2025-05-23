---
layout: doc
title:  "Open Source Contributors"
date:   2016-02-17 15:25:25 -0800
categories: guides
permalink: /docs/guides/open-source-contributors/
---
## Prerequisites

The following requirements are needed to develop on {{ site.mojito_green }}: 

1. [Git](https://git-scm.com/about)
2. [JDK 21](https://adoptium.net/temurin/releases/?version=21) 
3. [Maven](https://maven.apache.org/download.cgi), or use the project Maven wrapper `mvnw` (version `3.8`)
4. Optional but highly recommanded, [MySQL 8](https://dev.mysql.com/downloads/mysql/8.html) 

Next are instructions to setup the developper environment on Mac OS and Ubuntu. Both have instructions to install 
`Mysql 8` that can be skipped but it is highly recommanded to work with a production like environment and persistent
data. Note that `8` is the only version tested at the moment.

Skip to the [build section](#build) if you already have everything setup!

You can also get started by using a [docker image](#docker-image) that has the build dependencies but without `Mysql`. 

### Install on Mac OS

Install `brew` following the website instructions: https://brew.sh/

Install `java 21` from `Temurin`:

```sh
brew install --cask temurin@21
```

Check that `java 21` is now in use with `java -version`. If you need multiple versions of `java` consider using 
something like `jenv` (don't forget the plugins: `jenv enable-plugin maven` & `jenv enable-plugin export` ) or `sdkman`.
    
Install `maven` (latest version should be fine):
    
```sh
brew install maven
```
          
Optionally, install `Mysql 8`

```sh
brew install mysql@8.0
```

Note, to install the exact same `maven` version as the wrapper: `brew install maven@3.8` (check the instructions since it is key-only) .

 
### Install on Unbutu 18.4 LTS

Install `java 21` from `OpenJDK`:

```sh
sudo apt-get install openjdk-21-jdk
```
Check that `java 21` is now in use with `java -version`. If not you, can set it as default with 
`sudo update-java-alternatives -s /usr/lib/jvm/java-1.21.0-openjdk-amd64` (To check the list
`sudo update-java-alternatives -l`). If you need multiple versions of `java` 
consider using something like `jenv` or `sdkman`.

Install `maven` (latest version should be fine):
    
```sh
sudo apt-get install maven
```

Install `git` 

```sh    
sudo apt-get install git
```
    
Optionally, install `Mysql 8`
 
```sh
sudo apt-get install mysql-server
```       
  
## Create Mysql Databases & configuration files

If you've decided to use `Mysql` the server needs to be configured for better Unicode support. This section also describes 
how to create 2 databases along with spring configuraitons to setup 2 envrionments that will make developpment easier.

Configure the server to use `utf-8` on `4 bytes` by default by appending these configurations to 
`/usr/local/etc/my.cnf` file on Intel Mac, `/opt/homebrew/etc/my.cnf` file on Apple Silicon Macs and to `/etc/mysql/my.cnf` file on Ubuntu:

```properties
[client]
default-character-set = utf8mb4
    
[mysqld]
character-set-server = utf8mb4
    
[mysqld]
max_allowed_packet = 256M

[mysqld]
default-time-zone = '+00:00'
```

Note, to check which type of Mac you have, run `uname -m` in the termninal. If the output is `arm64`, you have an Apple Silicon Mac, and if the output is `x86_64`, you have an Intel Mac.

The server needs to be started/restarted. 
```sh
# On Mac
mysql.server start

# On Ubuntu
sudo service mysql restart
```

We're going to create two test databases, so connect to MySQL DB as root user 
```sh
# On Mac 
mysql -u root

On Ubuntu 
sudo mysql -u root
```

The first database is to run the automation tests during developpment. Tests can also be run using `HSQL` in-memory
 DB but it is sometimes intersting to test with Mysql. This environment can also be used to work on the 
 generation of DB update schema. 

The second database is used to run a local instance of the application with data persisted to do frontend or CLI testing. 
Of course this is a suggestion and you can change the configuration to recreate the DB each time the server is started.

Run the following commands to create the databases. Change the user name, database name and password as needed. If you do
 so you'll have to update the configuration files accordingly later.
   
```sql
CREATE USER 'mojito'@'localhost' IDENTIFIED BY 'mojito';
CREATE DATABASE IF NOT EXISTS mojito CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_bin';
CREATE DATABASE IF NOT EXISTS mojito_dev CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_bin';
GRANT ALL ON mojito.* TO 'mojito'@'localhost' IDENTIFIED BY 'mojito';
GRANT ALL ON mojito_dev.* TO 'mojito'@'localhost' IDENTIFIED BY 'mojito';
FLUSH PRIVILEGES;
```

Now will create 2 configuration files, one for each environment/database.
First create the directory where the files will be stored: `mkdir -p ~/.l10n/config/webapp` 

Then create the first file: `.l10n/config/webapp/application.properties` with following content

```properties
spring.flyway.enabled=true
spring.jpa.defer-datasource-initialization=false
l10n.flyway.clean=true
spring.datasource.url=jdbc:mysql://localhost:3306/mojito?characterEncoding=UTF-8&useUnicode=true
spring.datasource.username=mojito
spring.datasource.password=mojito
spring.datasource.driverClassName=com.mysql.jdbc.Driver

l10n.org.quartz.jobStore.useProperties=true
l10n.org.quartz.scheduler.instanceId=AUTO
l10n.org.quartz.jobStore.isClustered=true
l10n.org.quartz.threadPool.threadCount=10
l10n.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
l10n.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
l10n.org.quartz.jobStore.dataSource=myDS
l10n.org.quartz.dataSource.myDS.provider=hikaricp
l10n.org.quartz.dataSource.myDS.driver=com.mysql.jdbc.Driver
l10n.org.quartz.dataSource.myDS.URL=jdbc:mysql://localhost:3306/mojito?characterEncoding=UTF-8&useUnicode=true
l10n.org.quartz.dataSource.myDS.user=mojito
l10n.org.quartz.dataSource.myDS.password=mojito
l10n.org.quartz.dataSource.myDS.maxConnections=12
l10n.org.quartz.dataSource.myDS.validationQuery=select 1
```

In the second file: `.l10n/config/webapp/application-npm.properties` just override the database URLs and the setting
to keep the data between server runs:
```properties
l10n.flyway.clean=false
spring.datasource.url=jdbc:mysql://localhost:3306/mojito_dev?characterEncoding=UTF-8&useUnicode=true
l10n.org.quartz.dataSource.myDS.URL=jdbc:mysql://localhost:3306/mojito_dev?characterEncoding=UTF-8&useUnicode=true
```
    
## Build

Now the basic setup is done the project can be built

```sh
git clone {{ site.github_url }}.git ${PROJECT_DIR}
cd ${PROJECT_DIR}
git config blame.ignoreRevsFile .git-blame-ignore-revs
mvn clean install -DskipTests=true
```
    
## Setup `npm`

It is advised to reuse the `npm` version that was downloaded during {{ site.mojito_green }}'s build.

```sh
source webapp/use_local_npm.sh
```
    
Else, make sure the global `npm` is compatible.
 
## Run {{ site.mojito_green }}

```sh
cd ${PROJECT_DIR}/webapp
npm run start-dev
```

{{ site.mojito_green }} should be running on [http://localhost:8080/login](http://localhost:8080/login).  

You can login with username: `admin` and password: `ChangeMe`.

If you didn't [configure Mysql](#create-mysql-databases--configuration-files) previously, {{ site.mojito_green }} will be running 
with in-memory `HSQL DB`. When you restart the server, all data will be lost. For persistent data you need to setup `Mysql`.

## Add Demo Data in {{ site.mojito_green }}
Make sure {{ site.mojito_green }} is up and running.

```sh
cd ${PROJECT_DIR}/webapp
npm run create-demo-data
```
    
This creates `Demo` repository in {{ site.mojito_green }} with 21 languages.  17 languages are fully translated.  A Demo directory is created in ${Project_DIR} with source file.

## Alias for the CLI

To easily run `CLI` commands using the latest code, you can create an alias that point to the `jar` that was previously built. 

```sh
alias mojito='java -Dspring.config.additional-location=optional:~/.l10n/config/cli/application.properties -jar ${PROJECT_DIR}/cli/target/mojito-cli-*-SNAPSHOT-exec.jar '
```

For example to create Demo data, you can now run: `mojito demo-create -n DemoCLI`.

Alternatively, install the CLI using the [install scripts]({{ site.url }}/docs/guides/install/#cli-install-script).

## Run Unit Tests
   
```sh
cd ${PROJECT_DIR}
mvn test
```

## Style guide
Mojito uses the [Google Java Format](https://github.com/google/google-java-format) style guidelines, which is integrated to run alongside Maven's compile phase with [spotless](https://github.com/diffplug/spotless/tree/main/plugin-maven).

If you use IntelliJ, we recommend to either import the [XML styleguide](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml) into the IDE or to install the [google-java-format plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format) and use that for re-formatting code.
 
## DB migration
DB migration is done with Flyway, and Default DB setup with MySql. The default app configuration uses HSQL hence Flyway is disabled.

When using MySql, Flyway must be turned on with `spring.flyway.enabled=true` (for dev or in production). and it also 
requires `spring.jpa.defer-datasource-initialization=false` to override an HSQL setting

### Clean the DB
Set the properties `l10n.flyway.clean=true` (and have `spring.flyway.clean-disabled=false`) to have Flyway clean the schema first 
and then recreate it from the migration files. This will allow during development to do have a similar behavior as `spring.jpa.hibernate.ddl-auto=create`.

### When working on Jpa entities
- Set `spring.jpa.hibernate.ddl-auto=update` so that Hibernate will update the DB schema as you keep doing changes
- Flyway is be used to create or just update the schema depending if the flags were set to clean the DB 
(`l10n.flyway.clean` and `spring.flyway.clean-disabled`)

### To Generate the final migration script before committing code
- Set `l10n.flyway.clean=true` (and `spring.flyway.clean-disabled=false`), Flyway will drop the database and recreate the previous version
- Set `spring.jpa.hibernate.ddl-auto=none` so that Hibernate won't perform any change on the DB schema
- Run DDLGenerator to generate the baseline schema to update the database.
- The generated code in: `target/db/migration/Vx__yy_zz.sql` must be adapted (eg. if renaming a column, hibernate will add one, so the statements must be changed)
- Move the update file to: `src/main/resources/db/migration`, rename the file (`x` is the version, `yy_zz` to provide a description: `yy zz`)
- Test the migration script

## Docker image

The `aurambaj/mojito-dev` docker image comes with Java, Maven and some other build dependencies required to build the project easily.

Build reusing host Maven repository (Recommanded):

```sh
cd ${PROJECT_DIR}
docker run -v $(pwd):/mnt/mojito -v ~/.m2:/root/.m2 -it aurambaj/mojito-dev mvn install -DskipTests
```    
   
Or start the container and run build commands in it

```sh
docker run -v $(pwd):/mnt/mojito -v ~/.m2:/root/.m2 -p 8080:8080 -it aurambaj/mojito-dev bash
    
# and then build commands
mvn install -DskipTests
cd webapp/
npm run start-dev
```
    
To build from a clean slate:

```sh
docker run -v $(pwd):/mnt/mojito -it aurambaj/mojito-dev mvn install -DskipTests
```    

## Adding/updating API endpoints

When building the project after adding or updating API endpoints, the build fails with the following error message:

`The OpenAPI specification has changed! Please rebuild the whole project to make sure that new clients and models are generated`

The next time you build the project, this error will disappear.

```sh
mvn clean install -Dupdate-openapi-checksum
```

This will update the OpenAPI file checksum; otherwise, the build will fail.

## Troubleshooting

### Check Java version 

Make sure you have Java 21 installed:

```sh
~/code/mojito [master] $ java --version
openjdk 21.0.4 2024-07-16 LTS
OpenJDK Runtime Environment Temurin-21.0.4+7 (build 21.0.4+7-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.4+7 (build 21.0.4+7-LTS, mixed mode)
```

### Check Maven version

Maven 3.8+ should work fine. Consider trying the Maven wrapper `mvnw` from the project if you have any issue with Maven or don't want to install it.
    
If you have multiple version of Java installed, make sure Maven uses the right version (forth line):

```sh
[01:21:31] ~ $ mvn -version
Apache Maven 3.8.2 (ea98e05a04480131370aa0c110b8c54cf726c06f)
Maven home: /usr/local/Cellar/maven/3.8.2/libexec
Java version: 1.8.0_292, vendor: AdoptOpenJDK, runtime: /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre
Default locale: en_IE, platform encoding: UTF-8
OS name: "mac os x", version: "10.16", arch: "x86_64", family: "mac"
```

### Caused by: java.lang.NoClassDefFoundError: javax/xml/bind/ValidationException

If you see an error like: 

`Caused by: java.lang.NoClassDefFoundError: javax/xml/bind/ValidationException`

you most likely have a wrong version of Java. Java 8 is required. See [Check Java version](#check-java-version)


