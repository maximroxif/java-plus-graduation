FROM amazoncorretto:21
WORKDIR /stats
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]