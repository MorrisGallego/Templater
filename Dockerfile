FROM eclipse-temurin:23 as jarBuilder

COPY ./src ./src
COPY ./gradle ./gradle
COPY ./build.gradle ./build.gradle
COPY ./settings.gradle ./settings.gradle
COPY ./gradle.properties ./gradle.properties
COPY ./gradlew ./gradlew

RUN chmod +x ./gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:23 as layerExtractor

COPY --from=jarBuilder build/libs/templater-1.1.0.jar ./templater.jar

RUN java -Djarmode=layertools -jar templater.jar extract

FROM mcr.microsoft.com/playwright/java:v1.47.0-noble

RUN apt-get -y install locales
RUN locale-gen en_US.UTF-8

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV spring_profiles_active prod

RUN mkdir -p /templates

COPY --from=layerExtractor dependencies/ ./
COPY --from=layerExtractor snapshot-dependencies/ ./
COPY --from=layerExtractor spring-boot-loader/ ./
COPY --from=layerExtractor application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
