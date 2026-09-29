# Imagen generica para cualquier microservicio Java. Uso: --build-arg MODULE=catalogo-service
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
# La cache de ~/.m2 se reutiliza entre builds (BuildKit, activado por defecto en Docker Desktop)
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -ntp -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
