FROM openjdk:8-jre-alpine

COPY ./build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar /opt/todo/
COPY ./src/main/resources/ /opt/todo/resources/

WORKDIR /opt/todo
EXPOSE 9000

ENTRYPOINT ["java", "-cp", "./resources:./kotlin-todo-app-1.0-SNAPSHOT-all.jar", "net.ilkinulas.WebAppKt"]