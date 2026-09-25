package com.liaqet.LIAQET;

public record MfiResult(double value, String divergenceType, boolean isError) {
    // Конструктор по умолчанию для вывода ошибок
    public MfiResult() {
        this(50.0, "NONE", true);
    }
    // Конструктор для успешного расчета
    public MfiResult(double value, String divergenceType) {
        this(value, divergenceType, false);
    }
}
