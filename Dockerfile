FROM openjdk:11 as build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x ./mvnw && ./mvnw clean install

FROM openjdk:11

ENV JAVA_OPTS='-Xmx128m' \
    HEALTHCHECK_URL=http://localhost:8080/actuator/health

COPY --from=build target/gitlab-initiator-*.jar /gitlab-initiator.jar

CMD java -jar /gitlab-initiator.jar

# Volume with shared data
VOLUME /alexandriadata

HEALTHCHECK --interval=10s --timeout=3s CMD wget --no-verbose --tries=1 --spider $HEALTHCHECK_URL || exit 1