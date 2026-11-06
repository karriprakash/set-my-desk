FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 setmydesk && mkdir -p /app/data && chown setmydesk /app/data
COPY --from=build /src/target/setmydesk-engine-*.jar app.jar
USER setmydesk
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
