# Todo Web Application
This repo contains a *todo* web application written in kotlin. 

The demo app is developed to provide samples to blog posts at [http://ilkinulas.github.io/](http://ilkinulas.github.io/).    


### Build And Run

1. `./gradlew clean shadowJar`
2. `java -jar ./build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar`

By default application uses [h2](http://www.h2database.com/html/main.html) (in memory) database.
You can configure it to use mysql database with the `-Ddb=mysql` program argument.

`java -Ddb=mysql   -jar ./build/libs/kotlin-todo-app-1.0-SNAPSHOT-all.jar`

### Docker
 
To build a docker image run:

`docker build -t ilkinulas/todoapp:1.0 .`

You can start the container by running the below command. This default setup will use the inmemory database.
`docker run -p 9000:9000 ilkinulas/todoapp:1.0`

TODO:
* publish image
* working with mysql
* docker network inspect $networkid
