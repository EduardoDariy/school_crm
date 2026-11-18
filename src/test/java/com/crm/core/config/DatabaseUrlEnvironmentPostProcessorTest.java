package com.crm.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsNeonStyleUrl() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://crm_owner:p%40ss:w0rd@ep-cool-name-123.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require");

        assertThat(props)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-cool-name-123.eu-central-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require")
                .containsEntry("spring.datasource.username", "crm_owner")
                .containsEntry("spring.datasource.password", "p@ss:w0rd");
    }

    @Test
    void keepsPortAndWorksWithoutQuery() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgres://user:secret@db.example.com:6543/crm");

        assertThat(props)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.com:6543/crm")
                .containsEntry("spring.datasource.username", "user")
                .containsEntry("spring.datasource.password", "secret");
    }

    @Test
    void acceptsQuotedValueFromEnvFile() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "\"postgresql://u:p@ep-x-pooler.aws.neon.tech/neondb?sslmode=require\"");

        assertThat(props).containsEntry("spring.datasource.url",
                "jdbc:postgresql://ep-x-pooler.aws.neon.tech/neondb?sslmode=require");
    }

    @Test
    void unpooledUrlGoesToFlyway() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgresql://app:pooled@ep-x-pooler.aws.neon.tech/neondb?sslmode=require",
                "DATABASE_URL_UNPOOLED", "postgresql://app:direct@ep-x.aws.neon.tech/neondb?sslmode=require")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-x-pooler.aws.neon.tech/neondb?sslmode=require");
        assertThat(environment.getProperty("spring.flyway.url"))
                .isEqualTo("jdbc:postgresql://ep-x.aws.neon.tech/neondb?sslmode=require");
        assertThat(environment.getProperty("spring.flyway.user")).isEqualTo("app");
        assertThat(environment.getProperty("spring.flyway.password")).isEqualTo("direct");
    }

    @Test
    void rejectsOtherSchemes() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
