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
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
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
     * Запрашивает исторические свечи по FIGI или UID инструмента.
     * Универсальный адаптер: автоматически определяет тип идентификатора для Т-Банка.
     */
    public List<Candle> fetchCandles(String figiOrUid, String interval, Instant from, Instant to) {
        List<Candle> candles = new ArrayList<>();
        try {
            JSONObject requestBody = new JSONObject();

            // КВАНТОВЫЙ ХАК: Если строка содержит дефисы (длина 36 символов), значит это UID.
            // Для UID сервер Т-Банка требует строго поле "instrumentId"!
            // Если дефисов нет (например, BBG004731354), это FIGI — пишем в поле "figi".
            if (figiOrUid != null && figiOrUid.contains("-") && figiOrUid.length() == 36) {
                requestBody.put("instrumentId", figiOrUid);
            } else {
                requestBody.put("figi", figiOrUid);
            }

            requestBody.put("from", from.toString());
            requestBody.put("to", to.toString());
            requestBody.put("interval", interval);

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
                System.err.println("[Сетевой шлюз свечей] Ошибка API Т-Банка: " + response.statusCode() + " -> " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[Сетевой шлюз свечей] Критический сбой при запросе свечей: " + e.getMessage());
        }
        return candles;
    }


    /**
     * Выполняет gRPC-совместимый REST-поиск инструментов на Московской Бирже.
     * Реализует двухпроходную селекцию: сначала акции TQBR, затем актуальные фьючерсы SPBFUT.
     */
    public List<InstrumentItem> searchInstruments(String query) {
        List<InstrumentItem> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return results;

        try {
            // Формируем тело запроса по спецификации T-Invest API v2
            JSONObject requestBody = new JSONObject();
            requestBody.put("query", query.trim());
            requestBody.put("instrumentKind", "INSTRUMENT_KIND_UNSPECIFIED"); // Поиск по всем типам

            String fullUrl = BASE_URL + "/tinkoff.public.invest.api.contract.v1.InstrumentsService/FindInstrument";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (!jsonResponse.has("instruments")) return results;

                JSONArray instrumentsArray = jsonResponse.getJSONArray("instruments");
                int addedCount = 0;

                // Пасс 1: Акции основного режима торгов (TQBR)
                for (int i = 0; i < instrumentsArray.length(); i++) {
                    if (addedCount >= 15) break; // Увеличим общий лимит выдачи, чтобы влезли все инструменты
                    JSONObject asset = instrumentsArray.getJSONObject(i);
                    String classCode = asset.optString("classCode", "").toUpperCase().trim();
                    String ticker = asset.optString("ticker", "").toUpperCase().trim();

                    // Исключаем служебные индексы (базовые активы)
                    if (ticker.startsWith("F") && ticker.length() > 5) {
                        continue;
                    }

                    if (classCode.equals("TQBR")) {
                        results.add(parseInstrumentFromJson(asset));
                        addedCount++;
                    }
                }

                // Пасс 2: ВСЕ ЖИВЫЕ, АКТИВНЫЕ И ТОРГУЕМЫЕ ФЬЮЧЕРСЫ
                java.time.Instant currentTime = java.time.Instant.now();

                for (int i = 0; i < instrumentsArray.length(); i++) {
                    if (addedCount >= 15) break;
                    JSONObject asset = instrumentsArray.getJSONObject(i);
                    String classCode = asset.optString("classCode", "").toUpperCase().trim();

                    if (classCode.equals("SPBFUT")) {
                        // 1. Проверяем флаг доступности торгов через API
                        boolean isTradeAvailable = asset.optBoolean("apiTradeAvailableFlag", false);

                        // 2. Проверяем дату экспирации (если она есть в ответе)
                        boolean isExpired = false;
                        if (asset.has("expirationDate")) {
                            try {
                                java.time.Instant expTime = java.time.Instant.parse(asset.getString("expirationDate"));
                                if (expTime.isBefore(currentTime)) {
                                    isExpired = true; // Контракт уже в прошлом, пропускаем
                                }
                            } catch (Exception e) {
                                // Если формат даты нестандартный, пропускаем ошибку
                            }
                        }

                        // Пропускаем мертвые или архивные инструменты
                        if (isExpired || !isTradeAvailable) {
                            continue;
                        }

                        // Дополнительный санитарный фильтр по тикеру от старых хвостов
                        String ticker = asset.optString("ticker", "").toUpperCase().trim();
                        if (ticker.endsWith("2") || ticker.endsWith("3") || ticker.endsWith("4") || ticker.endsWith("5") ||
                                ticker.contains(".22") || ticker.contains(".23") || ticker.contains(".24") || ticker.contains(".25")) {
                            continue;
                        }

                        // Инструмент полностью живой — добавляем в результаты поиска!
                        results.add(parseInstrumentFromJson(asset));
                        addedCount++;
                    }
                }
            }
            else {
                System.err.println("[Сетевой шлюз Поиска] Ошибка брокера: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[Сетевой шлюз Поиска] Критический сбой сети: " + e.getMessage());
        }
        return results;
    }

    private InstrumentItem parseInstrumentFromJson(JSONObject asset) {
        String rawKind = asset.optString("instrumentKind", "UNKNOWN");
        String cleanType = rawKind.replace("INSTRUMENT_TYPE_", "")
                .replace("INSTRUMENT_KIND_", "")
                .toLowerCase().trim();

        // Забираем персональный UID фьючерса или акции для gRPC-совместимости
        String correctId = asset.optString("uid", "—");

        return new InstrumentItem(
                asset.optString("name", "Без названия"),
                asset.optString("ticker", "—"),
                correctId,
                cleanType
        );
    }


    private double parseQuotation(JSONObject quotation) {
        if (quotation == null) return 0.0;
        long units = quotation.optLong("units", 0);
        int nano = quotation.optInt("nano", 0);
        return units + (double) nano / 1_000_000_000.0;
    }
}
