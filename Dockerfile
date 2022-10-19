FROM eclipse-temurin:17 as jarBuilder

COPY ./src ./src
COPY ./gradle ./gradle
COPY ./build.gradle ./build.gradle
COPY ./settings.gradle ./settings.gradle
COPY ./gradle.properties ./gradle.properties
COPY ./gradlew ./gradlew

RUN chmod +x ./gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:17 as layerExtractor

COPY --from=jarBuilder build/templater.jar ./templater.jar

RUN java -Djarmode=layertools -jar templater.jar extract

FROM mcr.microsoft.com/playwright/java:v1.27.0-focal

ENV spring_profiles_active=prod

RUN mkdir -p /templates

COPY --from=layerExtractor dependencies/ ./
COPY --from=layerExtractor snapshot-dependencies/ ./
COPY --from=layerExtractor spring-boot-loader/ ./
COPY --from=layerExtractor application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
