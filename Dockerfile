FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=$PORT -jar target/ai-code-reviewer-0.0.1-SNAPSHOT.jar"]
