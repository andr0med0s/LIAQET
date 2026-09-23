package com.liaqet.LIAQET;

/**
 * Результаты расчета Стохастического осциллятора для ЛИАКЭТ.
 * Хранит значения быстрой (%K) и медленной (%D) линий индикатора.
 */
public record StochasticResult(double k, double d) {}
