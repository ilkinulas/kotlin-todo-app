FROM gradle:4.6.0-jdk8-alpine AS builder
USER root
RUN mkdir /home/gradle/project

WORKDIR /home/gradle/project
COPY . .
RUN gradle clean shadowJar -s

FROM openjdk:8-jre-alpine

COPY --from=builder /home/gradle/project/build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar /opt/todo/

WORKDIR /opt/todo
EXPOSE 9000

ENV DB_URL=jdbc:h2:mem:tododb

ENTRYPOINT java -DDB_URL=$DB_URL -jar kotlin-todo-app-1.0-SNAPSHOT-all.jar