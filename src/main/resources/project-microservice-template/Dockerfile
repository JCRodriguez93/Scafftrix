# Imagen de ejecucion. Se construye sobre el JAR que produce "mvn package":
#   mvn clean package
#   docker build -t @artifactId@ .
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
