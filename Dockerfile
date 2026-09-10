# =====================================================================
# Build multi-estagio.
#
# Estagio 1 compila com o JDK completo (~500 MB).
# Estagio 2 leva SO o jar para uma imagem de runtime (~200 MB).
# A imagem final nao contem Maven, codigo-fonte nem o cache do build.
# Menos peso, menos superficie de ataque.
# =====================================================================

# ----------------------- Estagio 1: build ----------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copiar primeiro apenas os arquivos de dependencia e baixa-las em uma
# camada separada. Enquanto o pom.xml nao mudar, o Docker reaproveita essa
# camada e o build seguinte pula o download inteiro.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Agora sim o codigo. Mudanca em codigo invalida so as camadas daqui pra baixo.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ----------------------- Estagio 2: runtime --------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sem privilegios. Container rodando como root é risco
# desnecessario: se a aplicacao for comprometida, o atacante herda root.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

USER spring

EXPOSE 8080

# Limita a heap ao percentual da memoria do container. Sem isso a JVM
# calcula a heap pela memoria da MAQUINA e o container leva OOMKill.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
