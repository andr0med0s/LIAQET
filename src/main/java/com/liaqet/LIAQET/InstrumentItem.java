package com.liaqet.LIAQET;

/**
 * Модель торгового инструмента для хранения в Избранном.
 */
public record InstrumentItem(String name, String ticker, String uid, String type) {
    @Override
    public String toString() {
        return String.format("%s (%s) | %s", name, ticker, type);
    }
}
