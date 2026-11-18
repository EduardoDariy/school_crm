package com.crm.core;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Документация API: http://localhost:8080/swagger-ui.html
 * Войти: вызвать login, скопировать accessToken и вставить в «Authorize».
 * Отключается в production через SPRINGDOC_API_DOCS_ENABLED=false и SPRINGDOC_SWAGGER_UI_ENABLED=false.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer";

    @Bean
    OpenAPI crmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("School CRM API")
                        .version("MVP")
                        .description("""
                                Как войти:
                                1. Выполните `POST /api/auth/login` (школа) или `POST /api/platform/auth/login` \
                                (представитель CRM — группа «Платформа» в списке справа вверху).
                                2. Скопируйте `accessToken` из ответа.
                                3. Нажмите **Authorize** и вставьте токен (без слова Bearer).
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    GroupedOpenApi schoolApi() {
        return GroupedOpenApi.builder()
                .group("1-school")
                .displayName("Школа")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/platform/**")
                .build();
    }

    @Bean
    GroupedOpenApi platformApi() {
        return GroupedOpenApi.builder()
                .group("2-platform")
                .displayName("Платформа (представитель CRM)")
                .pathsToMatch("/api/platform/**")
                .build();
    }
}
