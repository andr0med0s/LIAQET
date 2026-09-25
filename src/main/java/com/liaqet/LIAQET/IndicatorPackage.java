package com.liaqet.LIAQET;

/**
 * Пакет индикаторов для хранения всех 7 таймфреймов квантового ядра LIAQET.
 */
public class IndicatorPackage {
    // 1 Группа: Неделя, День, 4 Часа
    public StochasticResult tfWeek, tfDay;
    public EmaResult tfWeekEma, tfDayEma;
    public MfiResult tfWeekMfi, tfDayMfi;

    // 2 Группа: 4 Часа, 1 Час, 30 Минут
    public StochasticResult tf4h, tf1h;
    public EmaResult tf4hEma, tf1hEma;
    public MfiResult tf4hMfi, tf1hMfi;

    // 3 Группа: 30 Минут, 15 Минут, 5 Минут
    public StochasticResult tf30m, tf15m, tf5m;
    public EmaResult tf30mEma, tf15mEma, tf5mEma;
    public MfiResult tf30mMfi, tf15mMfi, tf5mMfi;
}
