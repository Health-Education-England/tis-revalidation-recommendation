FROM eclipse-temurin:21-jre-alpine
COPY application/target/revalidation-uber.jar app.jar
ENV JAVA_OPTS=${JVM_OPTS:-"-XX:+UseG1GC"}
ENTRYPOINT ["java","-jar","app.jar"]