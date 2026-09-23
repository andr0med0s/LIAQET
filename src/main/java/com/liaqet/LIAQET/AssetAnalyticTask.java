package com.liaqet.LIAQET;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Модернизированная управляющая задача.
 * Рассчитывает индикаторы и безопасно выводит результаты прямо на экран JavaFX UI.
 */
public class AssetAnalyticTask implements Runnable {
    private final TInvestClient client;
    private final String assetUid;

    // Ссылки на графические элементы для вывода данных
    private final Label stochLabel;
    private final Label emaLabel;
    private final Label mfiLabel;
    private final TextArea aiArea;

    public AssetAnalyticTask(String token, String assetUid, Label stochLabel, Label emaLabel, Label mfiLabel, TextArea aiArea) {
        this.client = new TInvestClient(token);
        this.assetUid = assetUid;
        this.stochLabel = stochLabel;
        this.emaLabel = emaLabel;
        this.mfiLabel = mfiLabel;
        this.aiArea = aiArea;
    }

    @Override
    public void run() {
        System.out.println("[Диспетчер] Запуск асинхронного расчета для актива: " + assetUid);

        // Временные рамки для запроса: от 4 дней назад до текущего момента
        Instant to = Instant.now();
        Instant from = to.minus(4, ChronoUnit.DAYS);

        // 1. Загружаем свечи через наш рабочий сетевой шлюз
        List<Candle> candles = client.fetchCandles(assetUid, "CANDLE_INTERVAL_30_MIN", from, to);
        System.out.println("[Диспетчер] Загружено свечей: " + candles.size());

        if (candles.isEmpty()) {
            Platform.runLater(() -> aiArea.setText("Ошибка: Не удалось загрузить свечи с сервера брокера. Проверьте FIGI или токен."));
            return;
        }

        // 2. Считаем индикаторы на основе боевых данных
        StochasticResult stoch = IndicatorsEngine.calculateStochastic(candles, 14, 3);
        EmaResult javaEma = IndicatorsEngine.calculateEma(candles, 50);
        MfiResult mfi = IndicatorsEngine.calculateMfi(candles, 14);

        // 3. БЕЗОПАСНЫЙ ВЫВОД НА ЭКРАН через Platform.runLater()
        Platform.runLater(() -> {
            stochLabel.setText(String.format("Stochastic %%K: %.2f | %%D: %.2f", stoch.k(), stoch.d()));
            emaLabel.setText(String.format("EMA 50 (Скользящая средняя): %.2f", javaEma.value()));
            mfiLabel.setText(String.format("MFI v2 (Денежный поток): %.2f | Дивергенция: %s", mfi.value(), mfi.divergenceType()));
            aiArea.setText("Данные успешно обновлены с реального рынка!\nСистема готова к запуску Робота-Аналитика.");
        });

        System.out.println("[Диспетчер] Графический интерфейс успешно обновлен боевыми метриками.");
    }
}
