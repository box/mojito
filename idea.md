# Add private config, deployment code, etc. for easy access

The local/ is excluded from Git. It can be used to seem like to private config, deployment code, etc.

```shell
cd local/
ln -s ~/.l10n/config
ln -s ~/code/mojito-deploy/
```

## Simplest way to get everything to work quickly (but slower that intellij incremental update)

https://www.jetbrains.com/help/idea/delegate-build-and-run-actions-to-maven.html

Build a project with Maven﻿
* Click Maven settings in the Maven tool window. Alternatively, from the main menu select File | Settings/Preferences | Build, Execution, Deployment |Build Tools | Maven.
* Click Maven and from the list, select Runner.
* On the Runner page, select Delegate IDE build/run actions to maven.

## Keep default intellij and use maven manually

Some useful command
* mvn install -DskipTests -P'!frontend,!git-commit-id-plugin' --projects webapp --also-make
* mvn install -DskipTests -P'!frontend,!git-commit-id-plugin' --projects cli --also-make
* mvn clean install -DskipTests -P'!frontend,!git-commit-id-plugin' --projects webapp --also-make

This can be configured in Maven UI too.

## Debug commands with Maven

### Debug the CLI
mvn test -Dtest=LeveragingCommandTest#copyTMModeTargetBranchName -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

### Rebuild and debug the CLI
cd ..; mvn install -DskipTests -P'!frontend'; cd - ;  mvn test -Dtest=LeveragingCommandTest#copyTMModeTargetBranchName -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

### Attach debugger in Intellij
Edit configuration (run/debug ) > Remote JVM debugging > just name the config and use this to attach

## Failed attempt at getting aspectj, processor, etc to work

The goal there is to reuse Intellij incremental build that is way faster but I couldn't get it to work properly

Import project as Maven project, but it shows an error with

Some attempt at making annotation processor and aspectj all work:
- Add dependency provided in common.pom
  <dependency>
  <!-- Needed for intellij to setup processor properly, not needed with maven only -->
  <groupId>org.immutables</groupId>
  <artifactId>value</artifactId>
  <version>${immutables-value.version}</version>
  <scope>provided</scope>
  </dependency>

and in webapp.pom

<dependency>
            <groupId>org.hibernate</groupId>
            <artifactId>hibernate-jpamodelgen</artifactId>
            <version>${hibernate.version}</version>
            <!-- Needed for intellij to setup processor properly, not needed with maven only -->
            <scope>provided</scope>
        </dependency>


# HSQL server for debugging test

Start an HSQL Server:

```shell
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.5.2/hsqldb-2.5.2.jar org.hsqldb.server.Server --database.0 file:~/tmp/hsqldb/mojito --dbname.0 mojito
```

It should run on `9001`

```shell
[Server@506e1b77]: 2023-12-15 00:09:46.887 HSQLDB server 2.6.0 is online on port 9001
```

Then use following properties:

```properties
spring.datasource.url=jdbc:hsqldb:hsql://localhost:9001/mojito
spring.jpa.hibernate.ddl-auto=create
spring.jpa.defer-datasource-initialization=false
spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:/db/hsql/data.sql
```

For quick plain SQL query debugging 

```java

@Autowired JdbcTemplate jdbcTemplate;

jdbcTemplate.queryForList(
  "select unix_timestamp(), unix_timestamp(mblob.created_date), current_timestamp, mblob.* from mblob ")
  .forEach(System.out::println);
```


