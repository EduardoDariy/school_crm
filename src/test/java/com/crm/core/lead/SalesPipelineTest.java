package com.crm.core.lead;

import com.crm.core.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalesPipelineTest extends ApiTestSupport {

    School school;
    String managerId;
    String manager;
    String pipelineId;
    List<String> stages;

    @BeforeEach
    void setUp() throws Exception {
        school = createSchool();
        managerId = createStaff(school, "manager@school.kz", "MANAGER");
        manager = login(school.subdomain(), "manager@school.kz", PASSWORD);

        String body = mvc.perform(bearer(manager, get("/api/pipelines")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        pipelineId = JsonPath.read(body, "$[0].id");
        stages = JsonPath.read(body, "$[0].stages[*].id");
    }

    @Test
    void newSchoolGetsDefaultPipeline() throws Exception {
        mvc.perform(bearer(manager, get("/api/pipelines")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Продажи"))
                .andExpect(jsonPath("$[0].defaultPipeline").value(true))
                .andExpect(jsonPath("$[0].lostReasonMode").value("OPTIONAL"))
                .andExpect(jsonPath("$[0].stages[*].name")
                        .value(contains("Новое обращение", "Взят в работу", "Пробное занятие", "Принимает решение")))
                .andExpect(jsonPath("$[0].lostReasons.length()").value(5));
    }

    @Test
    void leadGoesThroughFunnelAndIsWon() throws Exception {
        String leadId = createLead(manager, "Мария Иванова", "8 (701) 123-45-67");

        mvc.perform(bearer(manager, get("/api/leads/{id}", leadId)))
                .andExpect(jsonPath("$.phoneNumber").value("+77011234567"))
                .andExpect(jsonPath("$.stageId").value(stages.get(0)))
                .andExpect(jsonPath("$.status").value("OPEN"))
                // Создавший менеджер становится ответственным
                .andExpect(jsonPath("$.managerId").value(managerId));

        moveTo(leadId, stages.get(2)).andExpect(status().isOk());

        mvc.perform(bearer(manager, post("/api/leads/{id}/win", leadId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.personId").isNotEmpty())
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // Закрытый лид нельзя двигать, пока не вернули в работу
        moveTo(leadId, stages.get(1)).andExpect(status().isBadRequest());

        mvc.perform(bearer(manager, get("/api/leads/{id}/history", leadId)))
                .andExpect(jsonPath("$[*].type").value(contains("CREATED", "STAGE_CHANGED", "WON")))
                .andExpect(jsonPath("$[1].fromStageId").value(stages.get(0)))
                .andExpect(jsonPath("$[1].toStageId").value(stages.get(2)))
                .andExpect(jsonPath("$[1].actorId").value(managerId));

        // Карточка ученика найдётся в поиске клиентов
        mvc.perform(bearer(manager, get("/api/persons").param("query", "Мария")))
                .andExpect(jsonPath("$.content[0].student").value(true));
    }

    @Test
    void repeatedContactOfExistingClientReusesPersonCard() throws Exception {
        String first = createLead(manager, "Пётр", "+77015550000");
        String personId = read(mvc.perform(bearer(manager, post("/api/leads/{id}/win", first))), "$.personId");
        mvc.perform(bearer(manager, post("/api/leads/{id}/archive", first))).andExpect(status().isOk());

        // Повторное обращение с тем же номером — дубль лида, ответ указывает на прежнюю карточку
        mvc.perform(bearer(manager, post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Пётр\",\"phoneNumber\":\"+7 701 555 00 00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingId").value(first));

        // Вернули из архива и снова в работу — при выигрыше используется та же карточка человека
        mvc.perform(bearer(manager, post("/api/leads/{id}/unarchive", first))).andExpect(status().isOk());
        mvc.perform(bearer(manager, post("/api/leads/{id}/reopen", first))).andExpect(status().isOk());
        mvc.perform(bearer(manager, post("/api/leads/{id}/win", first)))
                .andExpect(jsonPath("$.personId").value(personId));
    }

    @Test
    void lostReasonFollowsPipelineSetting() throws Exception {
        String leadId = createLead(manager, "Олег", "+77010000010");
        String reasonId = read(mvc.perform(bearer(manager, get("/api/pipelines"))), "$[0].lostReasons[0].id");

        // Администратор делает причину обязательной
        mvc.perform(bearer(school.adminToken(), put("/api/pipelines/{id}", pipelineId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Продажи\",\"lostReasonMode\":\"REQUIRED\"}"))
                .andExpect(status().isOk());

        mvc.perform(bearer(manager, post("/api/leads/{id}/lose", leadId)))
                .andExpect(status().isBadRequest());
        mvc.perform(bearer(manager, post("/api/leads/{id}/lose", leadId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lostReasonId\":\"" + reasonId + "\",\"comment\":\"Нашёл дешевле\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.lostReasonId").value(reasonId));

        // Выключенную причину выбрать нельзя
        mvc.perform(bearer(school.adminToken(), put("/api/pipelines/{p}/lost-reasons/{r}", pipelineId, reasonId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Дорого\",\"active\":false}"))
                .andExpect(status().isOk());
        String other = createLead(manager, "Анна", "+77010000011");
        mvc.perform(bearer(manager, post("/api/leads/{id}/lose", other)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lostReasonId\":\"" + reasonId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminConfiguresStages() throws Exception {
        String admin = school.adminToken();
        // Лид на первом этапе — до перестановки этапов
        String leadId = createLead(manager, "Лид на удаляемом этапе", "+77010000012");
        String firstStage = stages.get(0);
        mvc.perform(bearer(admin, post("/api/pipelines/{id}/stages", pipelineId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Договор\"}"))
                .andExpect(jsonPath("$.stages.length()").value(5))
                .andExpect(jsonPath("$.stages[4].name").value("Договор"));

        // Обратный порядок
        String body = mvc.perform(bearer(admin, get("/api/pipelines/{id}", pipelineId)))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.stages[*].id");
        List<String> reversed = ids.reversed();
        mvc.perform(bearer(admin, put("/api/pipelines/{id}/stages/order", pipelineId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageIds\":[\"" + String.join("\",\"", reversed) + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].name").value("Договор"));
        // Неполный список этапов — ошибка
        mvc.perform(bearer(admin, put("/api/pipelines/{id}/stages/order", pipelineId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stageIds\":[\"" + ids.get(0) + "\"]}"))
                .andExpect(status().isBadRequest());

        // Удаление этапа с лидами требует указать, куда их перенести
        mvc.perform(bearer(admin, delete("/api/pipelines/{p}/stages/{s}", pipelineId, firstStage)))
                .andExpect(status().isBadRequest());
        mvc.perform(bearer(admin, delete("/api/pipelines/{p}/stages/{s}", pipelineId, firstStage)
                        .param("moveToStageId", stages.get(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages.length()").value(4));
        mvc.perform(bearer(manager, get("/api/leads/{id}", leadId)))
                .andExpect(jsonPath("$.stageId").value(stages.get(1)));
        // История лида сохраняет ссылку на удалённый этап
        mvc.perform(bearer(manager, get("/api/leads/{id}/history", leadId)))
                .andExpect(jsonPath("$[0].toStageId").value(firstStage));
    }

    @Test
    void managerCannotConfigurePipelines() throws Exception {
        mvc.perform(bearer(manager, post("/api/pipelines/{id}/stages", pipelineId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(bearer(manager, put("/api/pipelines/{id}", pipelineId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"lostReasonMode\":\"DISABLED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void responsibleMustBeActiveManagerOrAdmin() throws Exception {
        String leadId = createLead(manager, "Лид", "+77010000013");
        String teacherId = createStaff(school, "teacher@school.kz", "TEACHER");

        mvc.perform(bearer(manager, post("/api/leads/{id}/manager", leadId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":\"" + teacherId + "\"}"))
                .andExpect(status().isBadRequest());

        String second = createStaff(school, "second@school.kz", "MANAGER");
        mvc.perform(bearer(manager, post("/api/leads/{id}/manager", leadId)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":\"" + second + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerId").value(second));

        // Фильтр «мои лиды»
        mvc.perform(bearer(manager, get("/api/leads").param("mine", "true")))
                .andExpect(jsonPath("$.length()").value(0));
        // Справочник сотрудников доступен менеджеру
        mvc.perform(bearer(manager, get("/api/staff")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void searchFindsByNamePhoneAndMessenger() throws Exception {
        createLead(manager, "Алия Нурланова", "+77019998877");
        mvc.perform(bearer(manager, post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Инстаграм-лид\",\"messengerId\":\"@aliya_ig\",\"source\":\"Instagram\"}"))
                .andExpect(status().isCreated());

        mvc.perform(bearer(manager, get("/api/leads").param("query", "нурлан")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(bearer(manager, get("/api/leads").param("query", "9998877")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(bearer(manager, get("/api/leads").param("query", "ALIYA_IG")))
                .andExpect(jsonPath("$[0].source").value("Instagram"));
        mvc.perform(bearer(manager, get("/api/leads").param("stageId", stages.get(0))))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String createLead(String token, String name, String phone) throws Exception {
        return read(mvc.perform(bearer(token, post("/api/leads")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isCreated()), "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions moveTo(String leadId, String stageId) throws Exception {
        return mvc.perform(bearer(manager, post("/api/leads/{id}/stage", leadId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stageId\":\"" + stageId + "\"}"));
    }
}
