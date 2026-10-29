package com.kestrel.alertfanout.infra.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final String botToken;
    private final String defaultChatId;
    private final String apiUrl;
    private final RestClient restClient;

    public record TelegramDeliveryResult(boolean success, String status, int statusCode, String message) {
        public TelegramDeliveryResult(boolean success, String status, String message) {
            this(success, status, success ? 200 : ("BLOCKED".equalsIgnoreCase(status) ? 403 : 500), message);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TelegramBotClient(
            @Value("${kestrel.telegram.bot-token:}") String botToken,
            @Value("${kestrel.telegram.chat-id:}") String defaultChatId,
            @Value("${kestrel.telegram.api-url:https://api.telegram.org}") String apiUrl) {
        this(botToken, defaultChatId, apiUrl, RestClient.builder().baseUrl(apiUrl).build());
    }

    public TelegramBotClient(
            String botToken,
            String defaultChatId,
            String apiUrl,
            RestClient restClient) {
        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
        this.apiUrl = apiUrl;
        this.restClient = restClient != null ? restClient : RestClient.builder().baseUrl(apiUrl).build();
    }

    public TelegramDeliveryResult sendMessage(String chatId, String message) {
        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : this.defaultChatId;

        if (botToken == null || botToken.isBlank()) {
            log.info("Telegram dispatch simulated (no bot-token configured). Target chat: {}", targetChatId);
            return new TelegramDeliveryResult(false, "SIMULATED", 200, "Telegram bot token not configured; dispatch simulated");
        }

        if (targetChatId == null || targetChatId.isBlank()) {
            log.warn("Telegram dispatch skipped (no chat-id configured)");
            return new TelegramDeliveryResult(false, "SKIPPED_NO_CHAT_ID", 400, "Target chat ID is missing");
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
            return new TelegramDeliveryResult(true, "DELIVERED", 200, "Sent to Telegram chat: " + targetChatId);
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 403) {
                log.warn("Telegram API returned 403 Forbidden for chat_id={}: user blocked bot", targetChatId);
                return new TelegramDeliveryResult(false, "BLOCKED", 403, "Telegram bot was blocked by the user: " + ex.getResponseBodyAsString());
            } else if (code == 429) {
                log.warn("Telegram API returned 429 Too Many Requests for chat_id={}", targetChatId);
                return new TelegramDeliveryResult(false, "RATE_LIMITED", 429, "Telegram API rate limit exceeded: " + ex.getResponseBodyAsString());
            } else {
                log.warn("Telegram API error {} for chat_id={}: {}", code, targetChatId, ex.getMessage());
                return new TelegramDeliveryResult(false, "FAILED_" + code, code, "Telegram HTTP error " + code + ": " + ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch to Telegram: {}", ex.getMessage());
            return new TelegramDeliveryResult(false, "FAILED", 500, "Telegram delivery error: " + ex.getMessage());
        }
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank();
    }

    public String getDefaultChatId() {
        return defaultChatId;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
