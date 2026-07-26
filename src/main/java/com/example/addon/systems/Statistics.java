package com.example.addon.systems;

/**
 * 时间戳 — 出售冷却 / 回服计时
 */
public class Statistics {

    private long lastSellTime  = 0;
    private long lastReconnect = 0;

    public long getLastSellTime() {
        return lastSellTime;
    }

    public void setLastSellTime(long t) {
        lastSellTime = t;
    }

    public long getLastReconnect() {
        return lastReconnect;
    }

    public void setLastReconnect(long t) {
        lastReconnect = t;
    }
}