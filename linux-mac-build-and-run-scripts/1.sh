#Build all projects
mvn -f ../dphe-core/pom.xml clean install -U
mvn -f ../dphe-onto-db/pom.xml clean install -U
mvn -f ../dphe-neo4j/pom.xml clean install -U
mvn -f ../dphe-neo4j-plugin/pom.xml clean install -U -DskipTests
mvn -f ../dphe-stream/pom.xml clean install -U -DskipTests
mvn -f ../dphe-cli/pom.xml clean install -U -DskipTests

