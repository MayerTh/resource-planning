# Build the jar first (from the repo root): mvn -pl core,cli -am package
#
# Build the image (context = cli/, so target/cli.jar resolves correctly):
#   docker build -t resource-planning-cli -f cli/src/main/docker/Dockerfile cli
# Run it:
#   docker run --rm -v $(pwd)/data:/data -v $(pwd)/out:/out resource-planning-cli /out/report.html
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/cli.jar /app/cli.jar

# data/ is the git-tracked source of truth (ADR 0016) - mount it, never bake it into the image.
ENV RESOURCEPLANNING_DATA_DIR=/data
VOLUME /data

ENTRYPOINT ["java", "-jar", "/app/cli.jar"]
