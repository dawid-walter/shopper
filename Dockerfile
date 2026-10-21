FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew :${SERVICE}:bootJar --no-daemon -q

FROM eclipse-temurin:25-jre
ARG SERVICE
COPY --from=build /src/${SERVICE}/build/libs/${SERVICE}.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
