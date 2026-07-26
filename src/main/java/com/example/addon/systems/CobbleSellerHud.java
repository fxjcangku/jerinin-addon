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
        Hud.GROUP, "cobble-seller-hud", "Cobble Panel", "Cobble Seller HUD", CobbleSellerHud::new);

    private static final Color TEXT_COLOR = new Color(255, 255, 255, 255);
    private static final Color COUNT_COLOR = new Color(255, 255, 85, 255);
    private static final Color BG = new Color(0, 0, 0, 100);

    public CobbleSellerHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        CobbleSeller mod = Modules.get().get(CobbleSeller.class);
        boolean sellingEnabled = mod != null && mod.isActive() && mod.enableSelling.get();
        if (!sellingEnabled && !isInEditor()) {
            setSize(0, 0);
            return;
        }

        double pad = 4;
        double lh = renderer.textHeight();

        String title = "Cobble Bot";
        int count = sellingEnabled ? mod.countCobblestone() : 0;
        int threshold = mod != null ? mod.sellThreshold.get() : 0;
        String countText = count + " / " + threshold;

        double maxW = Math.max(renderer.textWidth(title), renderer.textWidth(countText));
        double boxW = maxW + pad * 4;
        double boxH = lh * 2 + pad * 2;
        setSize(boxW, boxH);

        renderer.quad(this.x, this.y, boxW, boxH, BG);

        double cx = this.x + pad;
        double cy = this.y + pad;

        // bold effect: 1px offset overlay
        renderer.text(title, cx + 1, cy, new Color(0, 0, 0, 180), false);
        renderer.text(title, cx, cy, TEXT_COLOR, false);

        cy += lh + 2;

        renderer.text(countText, cx + 1, cy, new Color(0, 0, 0, 180), false);
        renderer.text(countText, cx, cy, COUNT_COLOR, false);
    }
}