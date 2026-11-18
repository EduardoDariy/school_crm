package com.crm.core.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Поддержка переменной {@code DATABASE_URL} в формате облачных провайдеров
 * ({@code postgresql://user:password@host:port/db?sslmode=require}: Neon, Render, Heroku и др.).
 * Строка превращается в {@code spring.datasource.url/username/password}, её можно вставить как есть.
 * Явно заданный {@code SPRING_DATASOURCE_URL} имеет приоритет.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "databaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>(toDatasourceProperties(databaseUrl));

        // Neon: приложение ходит через пул (DATABASE_URL, PgBouncer), а миграции Flyway — напрямую:
        // advisory-блокировки и SET сессии через PgBouncer в режиме transaction не работают.
        String unpooledUrl = environment.getProperty("DATABASE_URL_UNPOOLED");
        if (unpooledUrl != null && !unpooledUrl.isBlank()) {
            toDatasourceProperties(unpooledUrl).forEach((key, value) -> properties.put(
                    // у Flyway логин называется user, а не username
                    key.replace("spring.datasource.username", "spring.flyway.user")
                            .replace("spring.datasource.", "spring.flyway."), value));
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, properties));
    }

    static Map<String, Object> toDatasourceProperties(String databaseUrl) {
        URI uri = URI.create(unquote(databaseUrl.trim()));
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException("DATABASE_URL должен начинаться с postgresql://");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        String query = jdbcQuery(uri.getRawQuery());
        if (!query.isEmpty()) {
            jdbcUrl.append('?').append(query);
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl.toString());
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            properties.put("spring.datasource.username", decode(colon < 0 ? userInfo : userInfo.substring(0, colon)));
            if (colon >= 0) {
                properties.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return properties;
    }

    /** Параметры libpq, которые JDBC-драйвер понимает под другим именем, переименовываются. */
    private static String jdbcQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isEmpty())
                .map(param -> param.startsWith("channel_binding=") ? "channelBinding=" + param.substring(16) : param)
                .collect(Collectors.joining("&"));
    }

    /** Значение из .env-файла может прийти в кавычках (docker --env-file их не снимает). */
    private static String unquote(String value) {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
