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
        Hud.GROUP, "cobble-seller-hud", "圆石面板", "圆石出售 HUD 显示", CobbleSellerHud::new);

    private static final Color BG     = new Color(0, 0, 0, 120);
    private static final Color ACCENT = new Color(255, 215, 0, 255);   // 金色分隔线
    private static final Color WHITE  = new Color(255, 255, 255, 255);
    private static final Color DIM    = new Color(160, 160, 160, 255);
    private static final Color GREEN  = new Color(85,  255, 85,  255);
    private static final Color YELLOW = new Color(255, 255, 85,  255);
    private static final Color ORANGE = new Color(255, 140, 0,   255);
    private static final Color RED    = new Color(255, 85,  85,  255);

    public CobbleSellerHud() { super(INFO); }

    @Override
    public void render(HudRenderer renderer) {
        CobbleSeller mod = Modules.get().get(CobbleSeller.class);
        boolean active         = mod != null && mod.isActive();
        boolean sellingEnabled = active && mod.enableSelling.get();

        if (!active && !isInEditor()) { setSize(0, 0); return; }

        int count     = sellingEnabled ? mod.countCobblestone() : 0;
        int threshold = mod != null ? mod.sellThreshold.get() : 1700;
        float pct     = Math.min((float) count / Math.max(threshold, 1), 1.0f);
        int pctInt    = (int)(pct * 100);
        String stateStr = getStateStr(mod, active, sellingEnabled);
        Color  stateColor = getStateColor(mod, active, sellingEnabled);

        // ── 文本内容 ───────────────────────────────────────────────────────
        String line1 = "圆石出售";
        String line2 = count + " / " + threshold + "   " + pctInt + "%";
        String line3 = stateStr;

        double pad  = 6;
        double lh   = renderer.textHeight();
        double barH = 3;
        double maxW = Math.max(renderer.textWidth(line1),
                      Math.max(renderer.textWidth(line2), renderer.textWidth(line3)));
        double boxW = maxW + pad * 2;
        double boxH = pad + lh + 4 + lh + 4 + barH + 4 + lh + pad;
        setSize(boxW, boxH);

        double bx = this.x, by = this.y;

        // ── 背景 ──────────────────────────────────────────────────────────
        renderer.quad(bx, by, boxW, boxH, BG);

        double tx = bx + pad;
        double ty = by + pad;

        // ── 标题 ──────────────────────────────────────────────────────────
        renderer.text(line1, tx, ty, WHITE, false);
        ty += lh + 4;

        // ── 计数 + 百分比（百分比颜色随进度变化）────────────────────────────
        String countPart = count + " / " + threshold + "   ";
        String pctPart   = pctInt + "%";
        renderer.text(countPart, tx, ty, DIM, false);
        renderer.text(pctPart, tx + renderer.textWidth(countPart), ty,
                      pct >= 1f ? GREEN : pct >= 0.85f ? ORANGE : pct >= 0.5f ? YELLOW : WHITE,
                      false);
        ty += lh + 4;

        // ── 进度条（极细单线风格）────────────────────────────────────────────
        renderer.quad(tx, ty, maxW, barH, new Color(40, 40, 40, 180));
        Color barColor = pct >= 1f ? GREEN : pct >= 0.85f ? ORANGE : pct >= 0.5f ? YELLOW : WHITE;
        double fillW = maxW * pct;
        if (fillW > 0) renderer.quad(tx, ty, fillW, barH, barColor);
        ty += barH + 4;

        // ── 状态 ──────────────────────────────────────────────────────────
        renderer.text(line3, tx, ty, stateColor, false);
    }

    private String getStateStr(CobbleSeller mod, boolean active, boolean sellingEnabled) {
        if (!active)         return "未激活";
        if (!sellingEnabled) return "出售已暂停";
        return switch (mod.getState()) {
            case IDLE                        -> "监控中";
            case SELL_WAIT_GUI               -> "等待出售界面";
            case RETRY_DELAY                 -> "验证出售结果";
            case SELL_COOLDOWN               -> "出售冷却中";
            case RECONNECT_WAIT_GUI,
                 RECONNECT_CLICK_DELAY       -> "回服中...";
            case RECONNECT_COOLDOWN          -> "等待回服确认";
        };
    }

    private Color getStateColor(CobbleSeller mod, boolean active, boolean sellingEnabled) {
        if (!active)         return DIM;
        if (!sellingEnabled) return RED;
        return switch (mod.getState()) {
            case IDLE, SELL_COOLDOWN         -> GREEN;
            case SELL_WAIT_GUI, RETRY_DELAY,
                 RECONNECT_COOLDOWN          -> YELLOW;
            case RECONNECT_WAIT_GUI,
                 RECONNECT_CLICK_DELAY       -> ORANGE;
        };
    }
}
