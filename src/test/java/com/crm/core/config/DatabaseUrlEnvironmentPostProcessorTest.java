package com.crm.core.config;

import org.junit.jupiter.api.Test;

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
    void rejectsOtherSchemes() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
