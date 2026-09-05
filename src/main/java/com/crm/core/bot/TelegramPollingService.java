package com.crm.core.bot;

import com.crm.core.dto.DebtorReportDto;
import com.crm.core.repository.LessonRepository;
import com.crm.core.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.crm.core.entity.Lesson;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramPollingService {

    private final CrmTelegramBot telegramBot;
    private final SubscriptionRepository subscriptionRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // Добавлен инструмент для парсинга

    private final String botToken = "8576635521:AAFlsGW9FuLDDG5ldU2l8LjS7cQDwhjmt6w";
    private long lastUpdateId = 0;

    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final LessonRepository lessonRepository;

    @Scheduled(fixedDelay = 2000)
    public void fetchUpdates() {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1);

            // 1. Получаем сырой JSON в виде обычной строки (String)
            String jsonResponse = restTemplate.getForObject(url, String.class);
            if (jsonResponse == null) return;

            // 2. Вручную превращаем строку в дерево JsonNode
            JsonNode response = objectMapper.readTree(jsonResponse);

            if (response.has("ok") && response.get("ok").asBoolean()) {
                JsonNode results = response.get("result");

                for (JsonNode update : results) {
                    lastUpdateId = update.get("update_id").asLong();
                    System.out.println("⏳ Найдено новое событие (ID: " + lastUpdateId + ")");

                    if (update.has("message")) {
                        JsonNode message = update.get("message");
                        if (message.has("text")) {
                            String text = message.get("text").asText();
                            String chatId = message.get("chat").get("id").asText();
                            System.out.println("💬 Получено сообщение: '" + text + "' от чата: " + chatId);

                            if ("/debtors".equals(text)) {
                                System.out.println("🚀 Запущена генерация отчета...");
                                sendDebtorsReport(chatId);
                            }

                            if ("/debtors".equals(text)) {
                                System.out.println("🚀 Запущена генерация отчета...");
                                sendDebtorsReport(chatId);
                            } else if ("/schedule".equals(text)) { // <--- Добавили обработчик
                                System.out.println("🗓 Запрошено расписание...");
                                sendSchedule(chatId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка парсинга Telegram API: " + e.getMessage());
        }
    }

    private void sendSchedule(String chatId) {
        try {
            // Ищем уроки, начиная с текущего момента
            List<Lesson> lessons = lessonRepository.findUpcomingLessonsByChatId(chatId, LocalDateTime.now());

            if (lessons.isEmpty()) {
                telegramBot.sendMessage(chatId, "📭 У вас пока нет запланированных занятий.");
                return;
            }

            StringBuilder report = new StringBuilder("📅 Ваше ближайшее расписание:\n\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy в HH:mm");

            // Берем только первые 5 занятий, чтобы не перегружать сообщение
            lessons.stream().limit(5).forEach(l -> {
                report.append("🔹 ").append(l.getStartTime().format(formatter))
                        .append(" — ").append(l.getTopic() != null ? l.getTopic() : "Занятие")
                        .append("\n");
            });

            telegramBot.sendMessage(chatId, report.toString());
            System.out.println("✅ Расписание отправлено");
        } catch (Exception e) {
            System.err.println("❌ Ошибка при формировании расписания: " + e.getMessage());
        }
    }

    private void sendDebtorsReport(String chatId) {
        try {
            List<DebtorReportDto> debtors = subscriptionRepository.findDebtorsByTenantId(tenantId);
            if (debtors.isEmpty()) {
                telegramBot.sendMessage(chatId, "✅ Все абонементы оплачены, должников нет.");
                return;
            }

            StringBuilder report = new StringBuilder("⚠️ Список должников:\n");
            for (DebtorReportDto d : debtors) {
                report.append("- ").append(d.firstName()).append(" ").append(d.lastName())
                        .append(" (Остаток: ").append(d.lessonsRemaining()).append(")\n");
            }
            telegramBot.sendMessage(chatId, report.toString());
            System.out.println("✅ Отчет успешно отправлен");
        } catch (Exception e) {
            System.err.println("❌ Ошибка при формировании отчета: " + e.getMessage());
        }
    }
}