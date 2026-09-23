package com.liaqet.LIAQET;

import java.time.Instant;

public record Candle(Instant time, double open, double high, double low, double close, long volume) {}
