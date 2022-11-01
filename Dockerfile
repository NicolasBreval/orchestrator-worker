FROM ghcr.io/graalvm/graalvm-ce:22.3.0

RUN gu install js

WORKDIR /opt/orchestrator/build
COPY gradle ./gradle
COPY gradlew .
RUN ./gradlew clean build --no-daemon > /dev/null 2>&1 || true
COPY build.gradle.kts .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY src ./src
RUN ./gradlew clean build --no-daemon -x test
RUN cp ./build/libs/orchestrator-worker-0.0.1-all.jar /opt/orchestrator

WORKDIR /opt/orchestrator
ENV JAVA_OPTS ''
CMD java -jar $JAVA_OPTS ./orchestrator-worker-0.0.1-all.jar
