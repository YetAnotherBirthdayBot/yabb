# Importing JDK and copying required files
FROM eclipse-temurin:25 AS build
WORKDIR /app

# Copy Maven wrapper
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Set execution permission for the Maven wrapper
RUN chmod +x ./mvnw
RUN ./mvnw -B -e -DskipTests dependency:go-offline

# Copy the source files after dependencies are cached
COPY src ./src

RUN ./mvnw -B -e -DskipTests clean package

# Stage 2: Create the final Docker image using IBM Semeru Runtime
FROM ibm-semeru-runtimes:open-25-jre-noble AS runtime
RUN useradd -r -m papertrail
WORKDIR /app
VOLUME /tmp

# Copy the JAR from the build stage
COPY --from=build --chown=papertrail:papertrail /app/target/quarkus-app bot

USER papertrail
HEALTHCHECK CMD curl -f http://localhost:9000/q/health || exit 1
ENTRYPOINT ["java","-jar","bot/quarkus-run.jar"]