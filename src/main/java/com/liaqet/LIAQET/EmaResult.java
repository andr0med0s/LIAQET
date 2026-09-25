package com.liaqet.LIAQET;

public record EmaResult(double value, double distancePercent, String trendDirection, boolean isError) {
    // Конструктор по умолчанию для вывода ошибок
    public EmaResult() {
        this(0.0, 0.0, "ошибка", true);
    }
    // Конструктор для успешного расчета
    public EmaResult(double value, double distancePercent, String trendDirection) {
        this(value, distancePercent, trendDirection, false);
    }
}
