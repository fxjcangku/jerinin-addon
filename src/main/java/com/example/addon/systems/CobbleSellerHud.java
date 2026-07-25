package com.example.addon.systems;

import com.example.addon.modules.CobbleSeller;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class CobbleSellerHud extends HudElement {

    public static final HudElementInfo<CobbleSellerHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "cobble-seller-hud", "圆石出售实时面板", CobbleSellerHud::new);

    private static final Color TITLE   = new Color(255, 255, 255, 255);
    private static final Color LABEL   = new Color(170, 170, 170, 255);
    private static final Color VALUE   = new Color(255, 255, 85, 255);
    private static final Color PROFIT  = new Color(85, 255, 85, 255);
    private static final Color SELLING = new Color(255, 170, 0, 255);
    private static final Color RECON   = new Color(255, 85, 255, 255);
    private static final Color BG      = new Color(0, 0, 0, 100);

    public CobbleSellerHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        CobbleSeller mod = Modules.get().get(CobbleSeller.class);
        if (mod == null || !mod.isActive()) return;

        Statistics stats = mod.getStats();
        int count = mod.countCobblestone();

        // 闲置隐藏
        int timeoutSec = mod.idleTimeout.get();
        if (timeoutSec > 0) {
            long idleMs = System.currentTimeMillis() - mod.lastPickupTime;
            if (idleMs >= timeoutSec * 1000L) return;
        }

        double lh = renderer.textHeight();
        double pad = 4;

        // 组装各行
        String line1 = "COBBLE BOT";
        String line2 = getStatusLine(mod);
        String line3 = "BLOCK:  " + count + " / " + mod.sellThreshold.get();
        String line4 = "PROFIT: " + formatMoney(stats.getTotalMoney()) + "  |  " + formatSpeed(stats);
        String line5 = "TIME:   " + formatTime(stats.getTotalMs());

        double maxW = 0;
        for (String s : new String[]{line1, line2, line3, line4, line5}) {
            maxW = Math.max(maxW, renderer.textWidth(s));
        }

        double boxW = maxW + pad * 4;
        double boxH = lh * 5 + pad * 3;
        setSize(boxW, boxH);

        double x = this.x;
        double y = this.y;

        // 背景
        renderer.quad(x, y, boxW, boxH, BG);

        // 逐行渲染
        double cx = x + pad;
        double cy = y + pad;

        renderer.text(line1, cx, cy, TITLE, false);

        cy += lh + 2;
        Color statusColor = isSelling(mod) ? SELLING
            : (isReconnecting(mod) ? RECON : VALUE);
        renderer.text(line2, cx, cy, statusColor, false);

        cy += lh + 2;
        renderer.text(line3, cx, cy, VALUE, false);

        cy += lh + 2;
        renderer.text(line4, cx, cy, PROFIT, false);

        cy += lh + 2;
        renderer.text(line5, cx, cy, LABEL, false);
    }

    // ---- 辅助 ----

    private String getStatusLine(CobbleSeller mod) {
        if (isSelling(mod)) return "STATUS: SELLING...";
        if (isReconnecting(mod)) return "STATUS: RECONNECTING...";
        return "STATUS: ON";
    }

    private String formatMoney(double totalMoney) {
        if (totalMoney >= 10000) return String.format("%.1fW", totalMoney / 10000.0);
        return String.format("%.0f", totalMoney);
    }

    private String formatSpeed(Statistics stats) {
        double hours = stats.getTotalHours();
        if (hours < 0.003) return "Calc...";
        double speedW = (stats.getTotalSold() / hours) / 10000.0;
        return String.format("%.1fW/h", speedW);
    }

    private String formatTime(long totalMs) {
        long min = totalMs / 60000;
        long h = min / 60;
        long m = min % 60;
        return h + "h " + m + "m";
    }

    private boolean isSelling(CobbleSeller mod) {
        return mod.state == CobbleSeller.State.SELL_WAIT_GUI
            || mod.state == CobbleSeller.State.RETRY_DELAY;
    }

    private boolean isReconnecting(CobbleSeller mod) {
        return mod.state == CobbleSeller.State.RECONNECT_WAIT_GUI
            || mod.state == CobbleSeller.State.RECONNECT_CLICK_DELAY;
    }
}