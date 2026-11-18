package com.crm.core.tenant;

import com.crm.core.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Данные одной школы не должны быть видны и доступны другой. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TenantIsolationTest {

    @Autowired
    MockMvc mvc;

    String schoolA;
    String schoolB;

    @BeforeEach
    void createOrganizations() throws Exception {
        schoolA = createOrganization("school-a-" + shortId());
        schoolB = createOrganization("school-b-" + shortId());
    }

    @Test
    void leadsOfOneSchoolAreInvisibleToAnother() throws Exception {
        String leadId = createLead(schoolA, "Иван Петров", "+77010000001");

        mvc.perform(as(schoolA, get("/api/leads")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tenantId").value(schoolA));

        mvc.perform(as(schoolB, get("/api/leads")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Доступ по известному id чужой записи — «не найдено», а не успех
        mvc.perform(as(schoolB, post("/api/leads/{id}/convert", leadId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicatePhoneIsCheckedPerSchool() throws Exception {
        createLead(schoolA, "Иван", "+77010000002");

        mvc.perform(as(schoolA, post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дубль\",\"phoneNumber\":\"+77010000002\"}")))
                .andExpect(status().isBadRequest());

        // Тот же номер в другой школе — это другой клиент
        createLead(schoolB, "Иван", "+77010000002");
    }

    @Test
    void cannotEnrollStudentOfAnotherSchool() throws Exception {
        String leadId = createLead(schoolA, "Анна Смирнова", "+77010000003");
        String studentOfA = JsonPath.read(mvc.perform(as(schoolA, post("/api/leads/{id}/convert", leadId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.id");

        String groupOfB = JsonPath.read(mvc.perform(as(schoolB, post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"English A1\"}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(as(schoolB, post("/api/groups/{g}/enroll/{s}", groupOfB, studentOfA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestsWithoutValidActiveOrganizationAreRejected() throws Exception {
        mvc.perform(get("/api/leads"))
                .andExpect(status().isBadRequest());
        mvc.perform(as(UUID.randomUUID().toString(), get("/api/leads")))
                .andExpect(status().isNotFound());
        mvc.perform(as("not-a-uuid", get("/api/leads")))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/platform/organizations/{id}/block", schoolA))
                .andExpect(status().isOk());
        mvc.perform(as(schoolA, get("/api/leads")))
                .andExpect(status().isForbidden());
    }

    @Test
    void subdomainMustBeValidAndUnique() throws Exception {
        String subdomain = "unique-" + shortId();
        createOrganization(subdomain);

        mvc.perform(post("/api/platform/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Копия\",\"subdomain\":\"" + subdomain + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/platform/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Школа\",\"subdomain\":\"Bad_Name!\"}"))
                .andExpect(status().isBadRequest());
    }

    private String createOrganization(String subdomain) throws Exception {
        String body = mvc.perform(post("/api/platform/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Школа " + subdomain + "\",\"subdomain\":\"" + subdomain + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timezone").value("Asia/Almaty"))
                .andExpect(jsonPath("$.industryModule").value("LANGUAGE_SCHOOL"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createLead(String tenant, String name, String phone) throws Exception {
        String body = mvc.perform(as(tenant, post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phoneNumber\":\"" + phone + "\"}")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static MockHttpServletRequestBuilder as(String tenant, MockHttpServletRequestBuilder request) {
        return request.header(TenantFilter.TENANT_HEADER, tenant);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
