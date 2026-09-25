package com.liaqet.LIAQET;

public record StochasticResult(double k, double d, boolean isError) {
    // Конструктор по умолчанию для вывода ошибок
    public StochasticResult() {
        this(50.0, 50.0, true);
    }
    // Конструктор для успешного расчета
    public StochasticResult(double k, double d) {
        this(k, d, false);
    }
}
