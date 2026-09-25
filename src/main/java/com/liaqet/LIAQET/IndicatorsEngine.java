package com.liaqet.LIAQET;

import java.util.List;

public class IndicatorsEngine {

    /**
     * Точный расчет Экспоненциальной скользящей средней (EMA) с детекцией флэта
     */
    public static EmaResult calculateEma(List<Candle> candles, int period) {
        int size = candles.size();
        if (candles == null || size < period) {
            return new EmaResult();
        }

        double[] emaValues = new double[size];
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += candles.get(i).close();
        }
        emaValues[period - 1] = sum / period;

        double multiplier = 2.0 / (period + 1);
        for (int i = period; i < size; i++) {
            emaValues[i] = (candles.get(i).close() - emaValues[i - 1]) * multiplier + emaValues[i - 1];
        }

        double currentEma = emaValues[size - 1];
        double prevEma = emaValues[size - 2];
        double currentClose = candles.get(size - 1).close();

        double delta = currentEma - prevEma;
        double threshold = currentEma * 0.0001;

        String trendDirection = "флэт";
        if (delta > threshold) trendDirection = "вверх";
        else if (delta < -threshold) trendDirection = "вниз";

        double distancePercent = ((currentClose - currentEma) / currentEma) * 100.0;

        return new EmaResult(currentEma, distancePercent, trendDirection);
    }

    /**
     * Полный расчет осциллятора Стохастик (5, 3, 3) со сглаживанием линий %K и %D
     */
    public static StochasticResult calculateStochastic533(List<Candle> candles) {
        int size = candles.size();
        if (candles == null || size < 9) { // Минимум 9 свечей для построения сглаживания (5 + 2 + 2)
            return new StochasticResult();
        }

        double[] fastK = new double[size];
        for (int i = 4; i < size; i++) {
            double maxH = candles.get(i).high();
            double minL = candles.get(i).low();
            for (int j = i - 4; j <= i; j++) {
                if (candles.get(j).high() > maxH) maxH = candles.get(j).high();
                if (candles.get(j).low() < minL) minL = candles.get(j).low();
            }
            fastK[i] = (maxH - minL == 0) ? 50.0 : ((candles.get(i).close() - minL) / (maxH - minL)) * 100.0;
        }

        double[] smoothK = new double[size];
        for (int i = 6; i < size; i++) {
            smoothK[i] = (fastK[i] + fastK[i - 1] + fastK[i - 2]) / 3.0;
        }

        for (int i = 8; i < size; i++) {
            double d = (smoothK[i] + smoothK[i - 1] + smoothK[i - 2]) / 3.0;
            if (i == size - 1) {
                return new StochasticResult(smoothK[i], d);
            }
        }
        return new StochasticResult();
    }

    /**
     * Настоящий TradingView MFI Divergence v2 движок с фрактальным поиском пиков (плечо 5 свечей)
     */
    public static MfiResult calculateMfiDivergenceV2(List<Candle> candles, int mfiPeriod) {
        int size = candles.size();
        if (candles == null || size < mfiPeriod + 20) {
            return new MfiResult();
        }

        double[] typicalPrices = new double[size];
        double[] moneyFlow = new double[size];
        double[] mfi = new double[size];

        // Шаг 1: Расчет типичной цены и объема денежного потока
        for (int i = 0; i < size; i++) {
            typicalPrices[i] = (candles.get(i).high() + candles.get(i).low() + candles.get(i).close()) / 3.0;
            moneyFlow[i] = typicalPrices[i] * candles.get(i).volume();
        }

        // Шаг 2: Расчет стандартного осциллятора MFI
        for (int i = mfiPeriod; i < size; i++) {
            double posFlow = 0, negFlow = 0;
            for (int j = i - mfiPeriod + 1; j <= i; j++) {
                if (typicalPrices[j] > typicalPrices[j - 1]) posFlow += moneyFlow[j];
                else if (typicalPrices[j] < typicalPrices[j - 1]) negFlow += moneyFlow[j];
            }
            mfi[i] = (negFlow == 0) ? 100.0 : 100.0 - (100.0 / (1.0 + (posFlow / negFlow)));
        }

        // Шаг 3: Математический поиск Pivot Points (Фракталов) плечом в 5 свечей
        String divType = "NONE";
        int current = size - 1;
        int leftLeft = 5;

        // Ищем последний подтвержденный Pivot High цены
        int currentPivotHighIdx = -1;
        for (int i = current - leftLeft; i > mfiPeriod + leftLeft; i--) {
            boolean isPivot = true;
            for (int j = 1; j <= leftLeft; j++) {
                if (candles.get(i).high() < candles.get(i - j).high() || candles.get(i).high() < candles.get(i + j).high()) {
                    isPivot = false;
                    break;
                }
            }
            if (isPivot) { currentPivotHighIdx = i; break; }
        }

        // Ищем предыдущий Pivot High для сравнения вершин
        int prevPivotHighIdx = -1;
        if (currentPivotHighIdx != -1) {
            for (int i = currentPivotHighIdx - leftLeft - 1; i > mfiPeriod + leftLeft; i--) {
                boolean isPivot = true;
                for (int j = 1; j <= leftLeft; j++) {
                    if (candles.get(i).high() < candles.get(i - j).high() || candles.get(i).high() < candles.get(i + j).high()) {
                        isPivot = false;
                        break;
                    }
                }
                if (isPivot) { prevPivotHighIdx = i; break; }
            }
        }

        // Детекция Медвежьей Дивергенции
        if (currentPivotHighIdx != -1 && prevPivotHighIdx != -1) {
            if (candles.get(currentPivotHighIdx).high() > candles.get(prevPivotHighIdx).high() &&
                    mfi[currentPivotHighIdx] < mfi[prevPivotHighIdx]) {
                if (current - currentPivotHighIdx <= 8) divType = "BEARISH";
            }
        }

        // Ищем последний подтвержденный Pivot Low цены
        int currentPivotLowIdx = -1;
        for (int i = current - leftLeft; i > mfiPeriod + leftLeft; i--) {
            boolean isPivot = true;
            for (int j = 1; j <= leftLeft; j++) {
                if (candles.get(i).low() > candles.get(i - j).low() || candles.get(i).low() > candles.get(i + j).low()) {
                    isPivot = false;
                    break;
                }
            }
            if (isPivot) { currentPivotLowIdx = i; break; }
        }

        // Ищем предыдущий Pivot Low для сравнения низин
        int prevPivotLowIdx = -1;
        if (currentPivotLowIdx != -1) {
            for (int i = currentPivotLowIdx - leftLeft - 1; i > mfiPeriod + leftLeft; i--) {
                boolean isPivot = true;
                for (int j = 1; j <= leftLeft; j++) {
                    if (candles.get(i).low() > candles.get(i - j).low() || candles.get(i).low() > candles.get(i + j).low()) {
                        isPivot = false;
                        break;
                    }
                }
                if (isPivot) { prevPivotLowIdx = i; break; }
            }
        }

        // Детекция Бычьей Дивергенции
        if (currentPivotLowIdx != -1 && prevPivotLowIdx != -1 && divType.equals("NONE")) {
            if (candles.get(currentPivotLowIdx).low() < candles.get(prevPivotLowIdx).low() &&
                    mfi[currentPivotLowIdx] > mfi[prevPivotLowIdx]) {
                if (current - currentPivotLowIdx <= 8) divType = "BULLISH";
            }
        }

        return new MfiResult(mfi[current], divType);
    }
}
