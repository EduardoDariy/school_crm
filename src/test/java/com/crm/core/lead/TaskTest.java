package com.crm.core.lead;

import com.crm.core.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskTest extends ApiTestSupport {

    /** Часовой пояс школы по умолчанию. */
    static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Almaty");

    School school;
    String managerId;
    String manager;
    String otherManager;

    @BeforeEach
    void setUp() throws Exception {
        school = createSchool();
        managerId = createStaff(school, "m1@school.kz", "MANAGER");
        createStaff(school, "m2@school.kz", "MANAGER");
        manager = login(school.subdomain(), "m1@school.kz", PASSWORD);
        otherManager = login(school.subdomain(), "m2@school.kz", PASSWORD);
    }

    @Test
    void myTasksSummaryCountsOverdueAndToday() throws Exception {
        LocalDateTime now = LocalDateTime.now(SCHOOL_ZONE);
        createTask(manager, "Просрочено", now.minusHours(2), null);
        createTask(manager, "Завтра", now.plusDays(1), null);
        createTask(manager, "Через неделю", now.plusDays(7), null);
        createTask(otherManager, "Чужая", now.minusHours(1), null);

        mvc.perform(bearer(manager, get("/api/tasks/summary")))
                .andExpect(jsonPath("$.open").value(3))
                .andExpect(jsonPath("$.overdue").value(1));

        // По умолчанию — мои открытые задачи по сроку
        mvc.perform(bearer(manager, get("/api/tasks")))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Просрочено"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[2].overdue").value(false));
        mvc.perform(bearer(manager, get("/api/tasks").param("all", "true")))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void taskForLeadAndCompletion() throws Exception {
        String leadId = read(mvc.perform(bearer(manager, post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Лид\",\"phoneNumber\":\"+77010000020\"}"))
                .andExpect(status().isCreated()), "$.id");
        String taskId = createTask(manager, "Перезвонить", LocalDateTime.now(SCHOOL_ZONE).plusDays(1), leadId);

        mvc.perform(bearer(otherManager, get("/api/tasks").param("leadId", leadId)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assigneeId").value(managerId));

        // Чужую задачу менеджер изменить не может, администратор — может
        mvc.perform(bearer(otherManager, post("/api/tasks/{id}/complete", taskId)))
                .andExpect(status().isForbidden());
        mvc.perform(bearer(manager, post("/api/tasks/{id}/complete", taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedBy").value(managerId));
        mvc.perform(bearer(school.adminToken(), post("/api/tasks/{id}/reopen", taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void assigningTaskToColleague() throws Exception {
        String teacherId = createStaff(school, "t@school.kz", "TEACHER");
        String body = "{\"title\":\"Подготовить пробное\",\"dueAt\":\"2030-01-01T10:00:00\",\"assigneeId\":\"" + teacherId + "\"}";
        String taskId = read(mvc.perform(bearer(manager, post("/api/tasks")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()), "$.id");

        // Автор может изменить задачу, поставленную коллеге
        mvc.perform(bearer(manager, put("/api/tasks/{id}", taskId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Подготовить пробное занятие\",\"dueAt\":\"2030-01-02T10:00:00\",\"assigneeId\":\""
                                + managerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(managerId));

        // Заблокированному сотруднику задачу не поставить
        mvc.perform(bearer(school.adminToken(), post("/api/users/{id}/block", teacherId))).andExpect(status().isOk());
        mvc.perform(bearer(manager, post("/api/tasks")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacherHasNoAccessToTasks() throws Exception {
        createStaff(school, "teacher@school.kz", "TEACHER");
        String teacher = login(school.subdomain(), "teacher@school.kz", PASSWORD);
        mvc.perform(bearer(teacher, get("/api/tasks"))).andExpect(status().isForbidden());
    }

    private String createTask(String token, String title, LocalDateTime dueAt, String leadId) throws Exception {
        String lead = leadId == null ? "" : ",\"leadId\":\"" + leadId + "\"";
        return read(mvc.perform(bearer(token, post("/api/tasks")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"dueAt\":\"" + dueAt.withNano(0) + "\"" + lead + "}"))
                .andExpect(status().isCreated()), "$.id");
    }
}
