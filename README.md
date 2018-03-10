# Todo Web Application
This repo contains a *todo* web application written in kotlin. 

The demo app is developed to provide samples to blog posts at [http://ilkinulas.github.io/](http://ilkinulas.github.io/).    


### Docker

./gradlew clean shadowJar
docker build -t ilkinulas/todoapp:1.0 .
docker run -p 9000:9000 ilkinulas/todoapp:1.0


publish