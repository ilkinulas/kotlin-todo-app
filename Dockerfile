FROM openjdk:8-jre-alpine

COPY ./build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar /opt/todo/

WORKDIR /opt/todo
EXPOSE 9000

ENV DB_URL=jdbc:h2:mem:tododb

ENTRYPOINT java -DDB_URL=$DB_URL -jar kotlin-todo-app-1.0-SNAPSHOT-all.jar