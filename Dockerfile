FROM gradle:jdk21-alpine AS build
WORKDIR /app
COPY . .
# Use apenas 'assemble' em vez de 'build' para evitar rodar testes e JaCoCo no Docker
RUN gradle assemble --no-daemon

# Estágio de execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copie apenas o jar final do estágio de build
COPY --from=build /app/build/libs/*.jar app.jar
# Configure as opções da JVM
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
# Exponha a porta que a aplicação utiliza
EXPOSE 8080
# Comando para executar a aplicação
ENTRYPOINT java $JAVA_OPTS -jar app.jar