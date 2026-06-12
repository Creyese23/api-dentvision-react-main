# ---------- ETAPA 1: BUILD ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar archivos de configuración primero
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar el código fuente
COPY src ./src

# Compilar el proyecto (sin tests para mayor rapidez)
RUN mvn clean package -DskipTests

# ---------- ETAPA 2: RUNTIME ----------
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copiar el jar generado desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Comando de ejecución
ENTRYPOINT ["java","-jar","app.jar"]