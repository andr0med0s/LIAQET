package com.liaqet.LIAQET;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Клиент интеграции ЛИАКЭТ с локальным сервером ИИ LM Studio с поддержкой диалогового режима.
 */
public class AiClient {
    private final HttpClient httpClient;
    private final String apiUrl;

    public AiClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Отправляет метрики индикаторов и кастомный вопрос пользователя в локальный ИИ.
     */
    public String sendDialogueRequest(String assetName, double stochK, double stochD, double ema, double mfi, String mfiDiv, String userQuestion) {
        try {
            // Системный промпт с жестким якорем для стратегии возврата к средней (Mean Reversion)
            String systemPrompt = "Ты — Квантовый Робот-Аналитик терминала ЛИАКЭТ. Твоя задача — провести анализ индикаторов и ответить на вопрос трейдера. " +
                    "Используй стратегию Mean Reversion (возврат к среднему). Отвечай строго по делу, профессиональным языком. " +
                    "В конце обязательно укажи четкий торговый вердикт: BUY, SELL или НАБЛЮДЕНИЕ.";

            // Базовый контекст рынка, который ИИ подмешивает к твоему вопросу
            String marketContext = String.format(
                    "Текущий срез рынка по инструменту: %s\n" +
                            "- Stochastic %%K: %.2f | %%D: %.2f\n" +
                            "- Скользящая средняя (Линия баланса): %.2f\n" +
                            "- MFI v2 (Денежный поток): %.2f\n" +
                            "- Фрактальная Дивергенция объемов: %s\n",
                    assetName, stochK, stochD, ema, mfi, mfiDiv
            );

            // Формируем финальное сообщение пользователя (Контекст + его личный вопрос)
            String finalUserMessage = marketContext + "\n[ЗАПРОС ТРЕЙДЕРА]: " + userQuestion;

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "hermes-3-llama-3.1-8b-lorablated");
            requestBody.put("temperature", 0.0); // Жесткая точность без галлюцинаций

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", finalUserMessage));
            requestBody.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                return choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
            } else {
                return "[Ошибка ИИ] Сервер LM Studio вернул код: " + response.statusCode();
            }

        } catch (Exception e) {
            return "[Ошибка Сети ИИ] Не удалось связаться с LM Studio: " + e.getMessage();
        }
    }
}
