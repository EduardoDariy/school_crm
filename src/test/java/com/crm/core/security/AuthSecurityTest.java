package com.crm.core.security;

import com.crm.core.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthSecurityTest extends ApiTestSupport {

    School school;

    @BeforeEach
    void setUp() throws Exception {
        school = createSchool();
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/leads")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/platform/organizations")).andExpect(status().isUnauthorized());
        mvc.perform(bearer("not-a-jwt", get("/api/leads"))).andExpect(status().isUnauthorized());

        // Подделанная подпись
        String token = school.adminToken();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        mvc.perform(bearer(tampered, get("/api/leads"))).andExpect(status().isUnauthorized());
    }

    @Test
    void schoolAndPlatformScopesAreSeparated() throws Exception {
        mvc.perform(bearer(school.adminToken(), get("/api/platform/organizations")))
                .andExpect(status().isForbidden());
        // Представитель CRM по умолчанию не видит данные школ
        mvc.perform(bearer(platformToken(), get("/api/leads")))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedAndSuccessfulLoginsAreJournaled() throws Exception {
        String email = "admin@" + school.subdomain() + ".kz";

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(school.subdomain(), email, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Неверный email или пароль"));
        // Несуществующий логин — тот же ответ, чтобы нельзя было перебирать email
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(school.subdomain(), "nobody@x.kz", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Неверный email или пароль"));

        mvc.perform(bearer(school.adminToken(), get("/api/users/login-journal")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].failureReason").value("UNKNOWN_USER"))
                .andExpect(jsonPath("$.content[1].failureReason").value("BAD_PASSWORD"))
                .andExpect(jsonPath("$.content[2].success").value(true));
    }

    @Test
    void blockedUserLosesAccessImmediately() throws Exception {
        String managerId = createStaff(school, "manager@school.kz", "MANAGER");
        String managerToken = login(school.subdomain(), "manager@school.kz", PASSWORD);
        mvc.perform(bearer(managerToken, get("/api/leads"))).andExpect(status().isOk());

        mvc.perform(bearer(school.adminToken(), post("/api/users/{id}/block", managerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mvc.perform(bearer(managerToken, get("/api/leads"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(school.subdomain(), "manager@school.kz", PASSWORD)))
                .andExpect(status().isForbidden());

        mvc.perform(bearer(school.adminToken(), post("/api/users/{id}/unblock", managerId)))
                .andExpect(status().isOk());
        mvc.perform(bearer(managerToken, get("/api/leads"))).andExpect(status().isOk());
    }

    @Test
    void roleChangeTakesEffectWithoutRelogin() throws Exception {
        String userId = createStaff(school, "staff@school.kz", "MANAGER");
        String token = login(school.subdomain(), "staff@school.kz", PASSWORD);
        mvc.perform(bearer(token, get("/api/leads"))).andExpect(status().isOk());

        mvc.perform(bearer(school.adminToken(), put("/api/users/{id}", userId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Сотрудник\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        mvc.perform(bearer(token, get("/api/leads"))).andExpect(status().isForbidden());
    }

    @Test
    void rolesRestrictSections() throws Exception {
        createStaff(school, "teacher@school.kz", "TEACHER");
        createStaff(school, "finance@school.kz", "FINANCE");
        createStaff(school, "manager@school.kz", "MANAGER");
        String teacher = login(school.subdomain(), "teacher@school.kz", PASSWORD);
        String finance = login(school.subdomain(), "finance@school.kz", PASSWORD);
        String manager = login(school.subdomain(), "manager@school.kz", PASSWORD);

        // Преподаватель изолирован от финансов, лидов и сотрудников
        mvc.perform(bearer(teacher, get("/api/reports/debtors"))).andExpect(status().isForbidden());
        mvc.perform(bearer(teacher, get("/api/leads"))).andExpect(status().isForbidden());
        mvc.perform(bearer(teacher, get("/api/users"))).andExpect(status().isForbidden());
        mvc.perform(bearer(teacher, get("/api/groups"))).andExpect(status().isOk());

        mvc.perform(bearer(finance, get("/api/reports/debtors"))).andExpect(status().isOk());
        mvc.perform(bearer(finance, get("/api/leads"))).andExpect(status().isForbidden());

        mvc.perform(bearer(manager, get("/api/users"))).andExpect(status().isForbidden());
        mvc.perform(bearer(manager, get("/api/reports/debtors"))).andExpect(status().isForbidden());

        // Текущий пользователь доступен любой роли
        mvc.perform(bearer(teacher, get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.tenantId").value(school.id()));
    }

    @Test
    void teacherSeesAndMarksOnlyOwnGroups() throws Exception {
        String ownTeacherId = createStaff(school, "own@school.kz", "TEACHER");
        createStaff(school, "other@school.kz", "TEACHER");
        String own = login(school.subdomain(), "own@school.kz", PASSWORD);
        String other = login(school.subdomain(), "other@school.kz", PASSWORD);

        String groupId = read(mvc.perform(bearer(school.adminToken(), post("/api/groups"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"English B1\",\"teacherId\":\"" + ownTeacherId + "\"}"))
                .andExpect(status().isOk()), "$.id");
        String lessonId = read(mvc.perform(bearer(school.adminToken(), post("/api/groups/{g}/lessons", groupId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2026-12-01T10:00:00\",\"endTime\":\"2026-12-01T11:00:00\"}"))
                .andExpect(status().isOk()), "$.id");

        mvc.perform(bearer(own, get("/api/groups"))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(bearer(other, get("/api/groups"))).andExpect(jsonPath("$.length()").value(0));

        mvc.perform(bearer(other, post("/api/lessons/{id}/attendance", lessonId))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isNotFound());
        mvc.perform(bearer(own, post("/api/lessons/{id}/attendance", lessonId))
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk());

        // Назначить преподавателем можно только сотрудника с ролью TEACHER
        String managerId = createStaff(school, "m@school.kz", "MANAGER");
        mvc.perform(bearer(school.adminToken(), post("/api/groups")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"teacherId\":\"" + managerId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void schoolKeepsAtLeastOneActiveAdmin() throws Exception {
        String adminId = read(mvc.perform(bearer(school.adminToken(), get("/api/auth/me"))), "$.id");

        mvc.perform(bearer(school.adminToken(), post("/api/users/{id}/block", adminId)))
                .andExpect(status().isBadRequest());

        String secondAdmin = createStaff(school, "admin2@school.kz", "ADMIN");
        String second = login(school.subdomain(), "admin2@school.kz", PASSWORD);
        // Второй администратор может заблокировать первого...
        mvc.perform(bearer(second, post("/api/users/{id}/block", adminId))).andExpect(status().isOk());
        // ...но не может разжаловать себя, оставшись последним
        mvc.perform(bearer(second, put("/api/users/{id}", secondAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordsArePolicedAndNeverExposed() throws Exception {
        mvc.perform(bearer(school.adminToken(), post("/api/users")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"short@school.kz\",\"fullName\":\"X\",\"role\":\"MANAGER\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(bearer(school.adminToken(), post("/api/users")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"fullName\":\"X\",\"role\":\"MANAGER\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(bearer(school.adminToken(), post("/api/users")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ok@school.kz\",\"fullName\":\"X\",\"role\":\"MANAGER\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(content().string(not(containsString("password"))));

        mvc.perform(bearer(school.adminToken(), get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void userCanChangeOwnPassword() throws Exception {
        createStaff(school, "self@school.kz", "MANAGER");
        String token = login(school.subdomain(), "self@school.kz", PASSWORD);

        mvc.perform(bearer(token, post("/api/auth/password")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(bearer(token, post("/api/auth/password")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(school.subdomain(), "self@school.kz", PASSWORD)))
                .andExpect(status().isUnauthorized());
        login(school.subdomain(), "SELF@school.kz", "new-password-1"); // email без учёта регистра
    }

    private static String loginBody(String subdomain, String email, String password) {
        return "{\"subdomain\":\"" + subdomain + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
