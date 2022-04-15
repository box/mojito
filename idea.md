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
