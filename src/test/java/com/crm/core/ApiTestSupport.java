package com.crm.core;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Общая основа для API-тестов: одна БД в контейнере и помощники для входа. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "secret-password";

    @Autowired
    protected MockMvc mvc;

    /** Школа с администратором: id организации, поддомен и токен администратора. */
    protected record School(String id, String subdomain, String adminToken) {
    }

    protected String platformToken() throws Exception {
        return token(post("/api/platform/auth/login"),
                "{\"email\":\"root@crm.test\",\"password\":\"root-password\"}");
    }

    protected School createSchool() throws Exception {
        String platform = platformToken();
        String subdomain = "school-" + shortId();
        String id = read(mvc.perform(bearer(platform, post("/api/platform/organizations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Школа " + subdomain + "\",\"subdomain\":\"" + subdomain + "\"}"))
                .andExpect(status().isCreated()), "$.id");

        mvc.perform(bearer(platform, post("/api/platform/organizations/{id}/admins", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@" + subdomain + ".kz\",\"fullName\":\"Администратор\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isCreated());

        return new School(id, subdomain, login(subdomain, "admin@" + subdomain + ".kz", PASSWORD));
    }

    /** Создаёт сотрудника от имени администратора школы и возвращает его id. */
    protected String createStaff(School school, String email, String role) throws Exception {
        return read(mvc.perform(bearer(school.adminToken(), post("/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"fullName\":\"Сотрудник\",\"role\":\"" + role
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated()), "$.id");
    }

    protected String login(String subdomain, String email, String password) throws Exception {
        return token(post("/api/auth/login"), "{\"subdomain\":\"" + subdomain + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\"}");
    }

    protected static MockHttpServletRequestBuilder bearer(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static String read(org.springframework.test.web.servlet.ResultActions result, String path)
            throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    protected static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String token(MockHttpServletRequestBuilder request, String body) throws Exception {
        return read(mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()), "$.accessToken");
    }
}
