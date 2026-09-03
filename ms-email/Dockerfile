# Estágio 1: Compilação usando a mesma versão do Java (26)
FROM maven:3.9.9-amazoncorretto-21 AS builder
WORKDIR /build

# Copia os arquivos de configuração e o código fonte
COPY pom.xml .
COPY src ./src

# Compila o projeto gerando o JAR
RUN mvn clean package -DskipTests

# Estágio 2: Imagem final de execução leve
FROM amazoncorretto:21
WORKDIR /app

# Busca o arquivo JAR gerado no estágio anterior
COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
