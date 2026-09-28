FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S oneleft && adduser -S oneleft -G oneleft
USER oneleft
WORKDIR /app
COPY target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
