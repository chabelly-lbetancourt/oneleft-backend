FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S oneleft && adduser -S oneleft -G oneleft
USER oneleft
WORKDIR /app
COPY target/plans-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
