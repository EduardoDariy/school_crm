package com.crm.core.bot;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CrmTelegramBot {

    // Ваш токен указан напрямую (без всяких файлов настроек)
    private String botToken = "8576635521:AAFlsGW9FuLDDG5ldU2l8LjS7cQDwhjmt6w";

    public void sendMessage(String chatId, String text) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, String> request = new HashMap<>();
            request.put("chat_id", chatId);
            request.put("text", text);

            restTemplate.postForObject(url, request, String.class);
            System.out.println("Уведомление успешно отправлено в Telegram");
        } catch (Exception e) {
            System.err.println("Ошибка отправки в Telegram: " + e.getMessage());
        }
    }
}