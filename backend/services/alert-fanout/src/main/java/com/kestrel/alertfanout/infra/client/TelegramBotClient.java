package com.kestrel.alertfanout.infra.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final String botToken;
    private final String defaultChatId;
    private final String apiUrl;
    private final RestClient restClient;

    public record TelegramDeliveryResult(boolean success, String status, String message) {}

    public TelegramBotClient(
            @Value("${kestrel.telegram.bot-token:}") String botToken,
            @Value("${kestrel.telegram.chat-id:}") String defaultChatId,
            @Value("${kestrel.telegram.api-url:https://api.telegram.org}") String apiUrl) {
        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
        this.apiUrl = apiUrl;
        this.restClient = RestClient.builder().baseUrl(apiUrl).build();
    }

    public TelegramDeliveryResult sendMessage(String chatId, String message) {
        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : this.defaultChatId;

        if (botToken == null || botToken.isBlank()) {
            log.info("Telegram dispatch simulated (no bot-token configured). Target chat: {}", targetChatId);
            return new TelegramDeliveryResult(false, "SIMULATED", "Telegram bot token not configured; dispatch simulated");
        }

        if (targetChatId == null || targetChatId.isBlank()) {
            log.warn("Telegram dispatch skipped (no chat-id configured)");
            return new TelegramDeliveryResult(false, "SKIPPED_NO_CHAT_ID", "Target chat ID is missing");
        }

        try {
            log.info("Dispatching alert to Telegram Bot API: chat_id={}", targetChatId);
            String url = "/bot" + botToken + "/sendMessage";
            Map<String, Object> body = Map.of(
                    "chat_id", targetChatId,
                    "text", message
            );

            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully delivered alert to Telegram chat: {}", targetChatId);
            return new TelegramDeliveryResult(true, "DELIVERED", "Sent to Telegram chat: " + targetChatId);
        } catch (Exception ex) {
            log.warn("Failed to dispatch to Telegram: {}", ex.getMessage());
            return new TelegramDeliveryResult(false, "FAILED", "Telegram delivery error: " + ex.getMessage());
        }
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }

    public String getDefaultChatId() {
        return defaultChatId;
    }
}
