package com.example.addon.systems;

/**
 * 统计管理器 — 累计出售、收益、挂机时间
 */
public class Statistics {

    private long   totalSold           = 0;
    private double totalMoney          = 0;
    private int    exactCountBeforeSell = 0;
    private long   sessionStartTime     = 0;
    private long   accumulatedTime      = 0;
    private long   lastSellTime         = 0;
    private long   lastReconnect        = 0;

    // ---- 会话 ----

    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
    }

    public void addAccumulatedTime(long ms) {
        accumulatedTime += ms;
    }

    public long getTotalMs() {
        return accumulatedTime + (System.currentTimeMillis() - sessionStartTime);
    }

    public long getTotalMin() {
        return getTotalMs() / 60000;
    }

    public double getTotalHours() {
        return getTotalMs() / 3600000.0;
    }

    // ---- 出售记录 ----

    public void recordSale(int count, double pricePerCobble) {
        totalSold  += count;
        totalMoney += count * pricePerCobble;
        lastSellTime = System.currentTimeMillis();
    }

    public void recordSellFail() {
        lastSellTime = System.currentTimeMillis();
    }

    // ---- 出售前数量 ----

    public void setExactCountBeforeSell(int count) {
        exactCountBeforeSell = count;
    }

    public int getExactCountBeforeSell() {
        return exactCountBeforeSell;
    }

    // ---- 时间戳 ----

    public long getLastSellTime() {
        return lastSellTime;
    }

    public long getLastReconnect() {
        return lastReconnect;
    }

    public void setLastReconnect(long t) {
        lastReconnect = t;
    }

    // ---- 查询 ----

    public long getTotalSold() {
        return totalSold;
    }

    public double getTotalMoney() {
        return totalMoney;
    }

    public long getAccumulatedTime() {
        return accumulatedTime;
    }
}