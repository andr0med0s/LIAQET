package com.liaqet.LIAQET;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Комплексный диспетчер расчетов.
 * Асинхронно собирает и рассчитывает все 7 таймфреймов для 3-х боевых групп.
 */
public class AssetAnalyticTask implements Runnable {
    private final TInvestClient client;
    private final String assetUid;
    private final MainApp mainApp;
    private final TextArea aiArea;

    public AssetAnalyticTask(String token, String assetUid, MainApp mainApp, TextArea aiArea) {
        this.client = new TInvestClient(token);
        this.assetUid = assetUid;
        this.mainApp = mainApp;
        this.aiArea = aiArea;
    }

    @Override
    public void run() {
        System.out.println("[Диспетчер] Запуск комплексного квантового расчета для: " + assetUid);
        Instant now = Instant.now();

        try {
            IndicatorPackage pack = new IndicatorPackage();

            // === 1 ГРУППА: ИНВЕСТИЦИОННАЯ (Неделя, День, 4ч) ===
            List<Candle> cWeek = client.fetchCandles(assetUid, "CANDLE_INTERVAL_WEEK", now.minus(365, ChronoUnit.DAYS), now);
            pack.tfWeek = IndicatorsEngine.calculateStochastic533(cWeek);
            pack.tfWeekEma = IndicatorsEngine.calculateEma(cWeek, 50);
            pack.tfWeekMfi = IndicatorsEngine.calculateMfiDivergenceV2(cWeek, 14);

            List<Candle> cDay = client.fetchCandles(assetUid, "CANDLE_INTERVAL_DAY", now.minus(60, ChronoUnit.DAYS), now);
            pack.tfDay = IndicatorsEngine.calculateStochastic533(cDay);
            pack.tfDayEma = IndicatorsEngine.calculateEma(cDay, 50);
            pack.tfDayMfi = IndicatorsEngine.calculateMfiDivergenceV2(cDay, 14);

            List<Candle> c4h = client.fetchCandles(assetUid, "CANDLE_INTERVAL_4_HOUR", now.minus(30, ChronoUnit.DAYS), now);
            pack.tf4h = IndicatorsEngine.calculateStochastic533(c4h);
            pack.tf4hEma = IndicatorsEngine.calculateEma(c4h, 50);
            pack.tf4hMfi = IndicatorsEngine.calculateMfiDivergenceV2(c4h, 14);

            // === 2 ГРУППА: СРЕДНЕСРОЧНАЯ (4ч, 1ч, 30м) ===
            // (4ч уже рассчитан выше и занесен в pack)
            List<Candle> c1h = client.fetchCandles(assetUid, "CANDLE_INTERVAL_HOUR", now.minus(7, ChronoUnit.DAYS), now);
            pack.tf1h = IndicatorsEngine.calculateStochastic533(c1h);
            pack.tf1hEma = IndicatorsEngine.calculateEma(c1h, 50);
            pack.tf1hMfi = IndicatorsEngine.calculateMfiDivergenceV2(c1h, 14);

            List<Candle> c30m = client.fetchCandles(assetUid, "CANDLE_INTERVAL_30_MIN", now.minus(4, ChronoUnit.DAYS), now);
            pack.tf30m = IndicatorsEngine.calculateStochastic533(c30m);
            pack.tf30mEma = IndicatorsEngine.calculateEma(c30m, 50);
            pack.tf30mMfi = IndicatorsEngine.calculateMfiDivergenceV2(c30m, 14);

            // === 3 ГРУППА: СКАЛЬПЕРСКАЯ (30м, 15м, 5м) ===
            // (30м уже рассчитан выше и занесен в pack)
            List<Candle> c15m = client.fetchCandles(assetUid, "CANDLE_INTERVAL_15_MIN", now.minus(2, ChronoUnit.DAYS), now);
            pack.tf15m = IndicatorsEngine.calculateStochastic533(c15m);
            pack.tf15mEma = IndicatorsEngine.calculateEma(c15m, 50);
            pack.tf15mMfi = IndicatorsEngine.calculateMfiDivergenceV2(c15m, 14);

            List<Candle> c5m = client.fetchCandles(assetUid, "CANDLE_INTERVAL_5_MIN", now.minus(1, ChronoUnit.DAYS), now);
            pack.tf5m = IndicatorsEngine.calculateStochastic533(c5m);
            pack.tf5mEma = IndicatorsEngine.calculateEma(c5m, 50);
            pack.tf5mMfi = IndicatorsEngine.calculateMfiDivergenceV2(c5m, 14);

            // Безопасный проброс результатов в JavaFX UI
            Platform.runLater(() -> {
                mainApp.updateUiWithPackage(pack);
                aiArea.setText("Все 3 группы таймфреймов (7 интервалов) успешно рассчитаны!\nСистема готова к запуску Робота-Аналитика.");
            });

        } catch (Exception e) {
            Platform.runLater(() -> aiArea.setText("Критический сбой в аналитическом потоке: " + e.getMessage()));
        }
    }
}
