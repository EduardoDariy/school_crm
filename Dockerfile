# Используем легковесный образ с Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем собранный Spring Boot архив из папки target
COPY target/*.jar app.jar

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]