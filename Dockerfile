FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/ ./
RUN npm install && npm run build

FROM maven:3.9-eclipse-temurin-17 AS api
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=api /app/target/trustvault-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]