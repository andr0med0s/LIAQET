package com.liaqet.LIAQET;

import java.util.List;

/**
 * Математический движок квантового терминала LIAQET.
 * Рассчитывает технические индикаторы на основе моделей данных Candle.
 */
public class IndicatorsEngine {

    /**
     * Расчет экспоненциальной скользящей средней (EMA)
     */
    public static EmaResult calculateEma(List<Candle> candles, int period) {
        if (candles == null || candles.isEmpty() || candles.size() < period) {
            return new EmaResult(0.0);
        }

        // Коэффициент сглаживания multiplier
        double multiplier = 2.0 / (period + 1);

        // В качестве начального значения EMA берем простое среднее (SMA) за первый период
        double currentEma = 0.0;
        for (int i = 0; i < period; i++) {
            currentEma += candles.get(i).close(); // Заметь, вызываем close() вместо getClose()!
        }
        currentEma /= period;

        // Последовательно рассчитываем EMA для оставшихся свечей
        for (int i = period; i < candles.size(); i++) {
            currentEma = (candles.get(i).close() - currentEma) * multiplier + currentEma;
        }

        return new EmaResult(currentEma);
    }

    /**
     * Расчет Стохастического осциллятора (Stochastic %K и %D)
     * Упрощенная сигнальная версия по последним свечам
     */
    public static StochasticResult calculateStochastic(List<Candle> candles, int kPeriod, int dPeriod) {
        if (candles == null || candles.size() < kPeriod + dPeriod) {
            return new StochasticResult(50.0, 50.0);
        }

        // Берем последнюю свечу для расчета текущего %K
        int lastIdx = candles.size() - 1;
        double currentClose = candles.get(lastIdx).close();

        // Находим минимум и максимум за последние kPeriod свечей
        double lowestLow = Double.MAX_VALUE;
        double highestHigh = Double.MIN_VALUE;

        for (int i = lastIdx - kPeriod + 1; i <= lastIdx; i++) {
            Candle c = candles.get(i);
            if (c.low() < lowestLow) lowestLow = c.low();
            if (c.high() > highestHigh) highestHigh = c.high();
        }

        double k = 50.0;
        if (highestHigh != lowestLow) {
            k = ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100.0;
        }

        // Сигнальная линия %D (простое среднее от %K, для демонстрации каркаса возвращаем сглаженное значение)
        double d = k; // В полной версии здесь будет сглаживание по массиву предыдущих %K

        return new StochasticResult(k, d);
    }
    /**
     * Расчет Индекса Денежного Потока v2 (MFI) с анализом дивергенций по фракталам объемов.
     */
    public static MfiResult calculateMfi(List<Candle> candles, int period) {
        if (candles == null || candles.size() < period + 5) {
            return new MfiResult(50.0, "НЕТ");
        }

        double positiveFlow = 0;
        double negativeFlow = 0;

        // Расчет базового MFI за указанный период (с конца массива)
        int lastIdx = candles.size() - 1;
        for (int i = lastIdx - period + 1; i <= lastIdx; i++) {
            Candle current = candles.get(i);
            Candle prev = candles.get(i - 1);

            double currentTp = (current.high() + current.low() + current.close()) / 3.0;
            double prevTp = (prev.high() + prev.low() + prev.close()) / 3.0;

            double moneyFlow = currentTp * current.volume();

            if (currentTp > prevTp) {
                positiveFlow += moneyFlow;
            } else if (currentTp < prevTp) {
                negativeFlow += moneyFlow;
            }
        }

        double mfiValue = 50.0;
        if (negativeFlow != 0) {
            double moneyRatio = positiveFlow / negativeFlow;
            mfiValue = 100.0 - (100.0 / (1.0 + moneyRatio));
        } else if (positiveFlow != 0) {
            mfiValue = 100.0;
        }

        // === ДВИЖОК ДИВЕРГЕНЦИЙ (Квантовый Эволюционный Блок) ===
        String divType = "НЕТ";

        // Берем экстремумы последних свечей для поиска расхождения цены и денежного потока
        Candle c0 = candles.get(lastIdx);
        Candle c1 = candles.get(lastIdx - 1);
        Candle c2 = candles.get(lastIdx - 2);

        // Упрощенный паттерн фрактального анализа (взят из нашей старой логики ЛИАКЭТ)
        if (c0.close() < c1.close() && c0.volume() > c1.volume() * 1.3 && mfiValue < 30) {
            divType = "БЫЧЬЯ (Покупка)";
        } else if (c0.close() > c1.close() && c0.volume() > c1.volume() * 1.3 && mfiValue > 70) {
            divType = "МЕДВЕЖЬЯ (Продажа)";
        }

        return new MfiResult(mfiValue, divType);
    }

}
