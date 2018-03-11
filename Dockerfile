FROM openjdk:8-jre-alpine

COPY ./build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar /opt/todo/

WORKDIR /opt/todo
EXPOSE 9000

ENV db=h2
ENV jdbc_mysql_url="jdbc:mysql://127.0.0.1:3306/tododb?nullNamePatternMatchesAll=true"

ENTRYPOINT java -Ddb=$db -Djdbc_mysql_url=$jdbc_mysql_url -jar kotlin-todo-app-1.0-SNAPSHOT-all.jar