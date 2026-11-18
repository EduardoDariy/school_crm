package com.crm.core.tenant;

import com.crm.core.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Данные одной школы не должны быть видны и доступны другой. */
class TenantIsolationTest extends ApiTestSupport {

    School schoolA;
    School schoolB;

    @BeforeEach
    void createSchools() throws Exception {
        schoolA = createSchool();
        schoolB = createSchool();
    }

    @Test
    void leadsOfOneSchoolAreInvisibleToAnother() throws Exception {
        String leadId = createLead(schoolA, "Иван Петров", "+77010000001");

        mvc.perform(bearer(schoolA.adminToken(), get("/api/leads")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(leadId));

        mvc.perform(bearer(schoolB.adminToken(), get("/api/leads")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Доступ по известному id чужой записи — «не найдено», а не успех
        mvc.perform(bearer(schoolB.adminToken(), post("/api/leads/{id}/win", leadId)))
                .andExpect(status().isNotFound());
        mvc.perform(bearer(schoolB.adminToken(), get("/api/leads/{id}/history", leadId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicatePhoneIsCheckedPerSchool() throws Exception {
        createLead(schoolA, "Иван", "+77010000002");

        mvc.perform(bearer(schoolA.adminToken(), post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дубль\",\"phoneNumber\":\"+77010000002\"}"))
                .andExpect(status().isConflict());

        // Тот же номер в другой школе — это другой клиент
        createLead(schoolB, "Иван", "+77010000002");
    }

    @Test
    void cannotEnrollStudentOfAnotherSchool() throws Exception {
        String leadId = createLead(schoolA, "Анна Смирнова", "+77010000003");
        String studentOfA = read(mvc.perform(bearer(schoolA.adminToken(), post("/api/leads/{id}/win", leadId)))
                .andExpect(status().isOk()), "$.personId");

        String groupOfB = read(mvc.perform(bearer(schoolB.adminToken(), post("/api/groups"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"English A1\"}"))
                .andExpect(status().isOk()), "$.id");

        mvc.perform(bearer(schoolB.adminToken(), post("/api/groups/{g}/enroll/{s}", groupOfB, studentOfA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffOfAnotherSchoolIsInvisible() throws Exception {
        String staffOfA = createStaff(schoolA, "teacher@a.kz", "TEACHER");

        mvc.perform(bearer(schoolB.adminToken(), get("/api/users/{id}", staffOfA)))
                .andExpect(status().isNotFound());
        // Нельзя назначить преподавателем сотрудника другой школы
        mvc.perform(bearer(schoolB.adminToken(), post("/api/groups")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Группа\",\"teacherId\":\"" + staffOfA + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameEmailCanExistInDifferentSchools() throws Exception {
        createStaff(schoolA, "manager@mail.kz", "MANAGER");
        createStaff(schoolB, "manager@mail.kz", "MANAGER");

        String tokenA = login(schoolA.subdomain(), "manager@mail.kz", PASSWORD);
        mvc.perform(bearer(tokenA, get("/api/organization")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schoolA.id()));
    }

    @Test
    void blockedOrganizationLosesAccessImmediately() throws Exception {
        mvc.perform(bearer(platformToken(), post("/api/platform/organizations/{id}/block", schoolA.id())))
                .andExpect(status().isOk());

        // Ранее выданный токен больше не работает
        mvc.perform(bearer(schoolA.adminToken(), get("/api/leads")))
                .andExpect(status().isForbidden());
        // И войти заново нельзя
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subdomain\":\"" + schoolA.subdomain() + "\",\"email\":\"admin@"
                                + schoolA.subdomain() + ".kz\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void subdomainMustBeValidAndUnique() throws Exception {
        String platform = platformToken();
        mvc.perform(bearer(platform, post("/api/platform/organizations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Копия\",\"subdomain\":\"" + schoolA.subdomain() + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(bearer(platform, post("/api/platform/organizations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Школа\",\"subdomain\":\"Bad_Name!\"}"))
                .andExpect(status().isBadRequest());
    }

    private String createLead(School school, String name, String phone) throws Exception {
        return read(mvc.perform(bearer(school.adminToken(), post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void pipelinesAndTasksAreIsolated() throws Exception {
        String stageOfB = read(mvc.perform(bearer(schoolB.adminToken(), get("/api/pipelines")))
                .andExpect(status().isOk()), "$[0].stages[1].id");
        String leadOfA = createLead(schoolA, "Лид", "+77010000004");

        // Этап чужой воронки нельзя использовать
        mvc.perform(bearer(schoolA.adminToken(), post("/api/leads/{id}/stage", leadOfA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stageId\":\"" + stageOfB + "\"}"))
                .andExpect(status().isBadRequest());

        // Задачу на чужой лид не поставить
        mvc.perform(bearer(schoolB.adminToken(), post("/api/tasks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Позвонить\",\"dueAt\":\"2030-01-01T10:00:00\",\"leadId\":\"" + leadOfA + "\"}"))
                .andExpect(status().isNotFound());
    }
}
