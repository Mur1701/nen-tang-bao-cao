FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]