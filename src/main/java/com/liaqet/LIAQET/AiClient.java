package com.liaqet.LIAQET;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Клиент для интеграции ЛИАКЭТ с локальным сервером ИИ LM Studio.
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
     * Отправляет метрики индикаторов в локальный ИИ и возвращает торговый вердикт.
     */
    public String sendAnalysisRequest(String assetName, double stochK, double stochD, double ema, double mfi, String mfiDiv) {
        try {
            // Формируем системный промпт - жесткие правила для Hermes
            String systemPrompt = "Ты — Квантовый Робот-Аналитик терминала ЛИАКЭТ. Твоя задача — провести экспресс-анализ индикаторов. " +
                    "Используй стратегию Mean Reversion (возврат к среднему). Отвечай строго по делу, профессиональным языком трейдера. " +
                    "В конце обязательно укажи четкий вердикт: ПОКУПКА, ПРОДАЖА или НАБЛЮДЕНИЕ, а также уровни Цели и Стоп-Лосса.";

            // Формируем пользовательские данные
            String userMessage = String.format(
                    "Проведи технический анализ для инструмента: %s\n" +
                            "Текущие показатели индикаторов (Таймфрейм 30м):\n" +
                            "- Stochastic %%K: %.2f | %%D: %.2f\n" +
                            "- EMA 50 (Линия баланса): %.2f\n" +
                            "- MFI v2 (Индекс денежного потока): %.2f\n" +
                            "- Фрактальная Дивергенция объемов: %s\n\n" +
                            "Выдай структурированный вердикт.",
                    assetName, stochK, stochD, ema, mfi, mfiDiv
            );

            // Собираем JSON структуру по стандарту OpenAI API
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "hermes-3-llama-3.1-8b-lorablated");
            requestBody.put("temperature", 0.0); // Жесткая математическая точность без галлюцинаций

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userMessage));
            requestBody.put("messages", messages);

            // Собираем HTTP запрос к LM Studio
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // Отправляем запрос на локальный сервер
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                return "[Ошибка ИИ] Сервер LM Studio вернул код: " + response.statusCode();
            }

        } catch (Exception e) {
            return "[Ошибка Сети ИИ] Не удалось связаться с LM Studio: " + e.getMessage();
        }
    }
}
