package com.liaqet.LIAQET;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Сетевой клиент для интеграции ЛИАКЭТ с T-Invest API.
 * Настроен на эталонный боевой REST-путь с обработкой отечественных SSL-сертификатов.
 */
public class TInvestClient {
    private final HttpClient httpClient;
    private final String token;

    // Чистый базовый адрес боевого API Т-Банка
    private static final String BASE_URL = "https://invest-public-api.tbank.ru/rest";

    public TInvestClient(String token) {
        this.token = token;

        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            }, new java.security.SecureRandom());

            this.httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .sslContext(sslContext)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("[Сетевой шлюз] Не удалось настроить SSL: " + e.getMessage());
        }
    }

    /**
     * Запрашивает исторические свечи по UID инструмента.
     */
    public List<Candle> fetchCandles(String figiOrUid, String interval, Instant from, Instant to) {
        List<Candle> candles = new ArrayList<>();
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("figi", figiOrUid);
            requestBody.put("from", from.toString());
            requestBody.put("to", to.toString());
            requestBody.put("interval", interval);

            // КЛЮЧЕВОЙ ФИКС: Точный путь по спецификации T-Invest REST API v2 с блоком 'contract'
            String fullUrl = BASE_URL + "/tinkoff.public.invest.api.contract.v1.MarketDataService/GetCandles";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("candles")) {
                    JSONArray candlesArray = jsonResponse.getJSONArray("candles");
                    for (int i = 0; i < candlesArray.length(); i++) {
                        JSONObject c = candlesArray.getJSONObject(i);

                        double open = parseQuotation(c.getJSONObject("open"));
                        double high = parseQuotation(c.getJSONObject("high"));
                        double low = parseQuotation(c.getJSONObject("low"));
                        double close = parseQuotation(c.getJSONObject("close"));
                        long volume = c.getLong("volume");
                        Instant time = Instant.parse(c.getString("time"));

                        candles.add(new Candle(time, open, high, low, close, volume));
                    }
                }
            } else {
                System.err.println("[Сетевой шлюз] Ошибка API Т-Банка: " + response.statusCode() + " -> " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[Сетевой шлюз] Критический сбой при запросе свечей: " + e.getMessage());
        }
        return candles;
    }

    private double parseQuotation(JSONObject quotation) {
        long units = quotation.optLong("units", 0);
        int nano = quotation.optInt("nano", 0);
        return units + (double) nano / 1_000_000_000.0;
    }
}
