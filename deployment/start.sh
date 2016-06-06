# install java and maven: yum install java-1.7.0-openjdk-devel-1.7.0.65-2.5.1.2.el6_5.x86_64 maven
# source must be checkout in ~/l10n, in home directory: git clone git@gitenterprise.inside-box.net:Box/l10n.git
# script to be copied and run in home directory: cp l10n/deployment/start.sh ~
# add application-tmp.properties with prod credential for LDAP
# You must also have maven installed as well (yum install maven)

set -x #echo on

export JAVA_HOME="/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.75.x86_64/"
export PATH="/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.75.x86_64/bin:$PATH"

cd l10n
git fetch origin
git reset --hard origin/master
mvn clean install -DskipTests
cd ..
kill -9 $(cat application.pid)
export JAR_FILE=`ls l10n/webapp/target/mojito-webapp-*-SNAPSHOT.jar`
nohup java -jar $JAR_FILE  --spring.profiles.active=prod,ldapprod,tmp$1  --server.port=8080 &
