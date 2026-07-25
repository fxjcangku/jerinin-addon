package com.example.addon.systems;

import com.example.addon.modules.CobbleSeller;
import meteordevelopment.meteorclient.settings.Setting;

/**
 * HUD 辅助 — 启动横幅 + 暂停状态播报 (HUD 渲染已移至 CobbleSellerHud)
 */
public class HudRenderer {

    private final Setting<Boolean> enableReconnect;
    private final Setting<Boolean> enablePostCmd;

    public HudRenderer(Setting<Boolean> enableReconnect,
                       Setting<Boolean> enablePostCmd) {
        this.enableReconnect = enableReconnect;
        this.enablePostCmd = enablePostCmd;
    }

    public void showStartupBanner(CobbleSeller mod) {
        mod.info("§b§m==================================================");
        mod.info("[ 圆石出售 ] §e§l模块已加载");
        mod.info("§7关闭「开启自动出售」即可暂停，回服/防掉线仍运行");
        mod.info("§b§m==================================================");
    }

    public void showPauseStatus(CobbleSeller mod) {
        String reconnect = enableReconnect.get() ? "§aON" : "§cOFF";
        String postcmd   = enablePostCmd.get()    ? "§aON" : "§cOFF";
        mod.info("§c§l[出售模式] 已关闭");
        mod.info("§7自动回服: " + reconnect + " §7| 回服后指令: " + postcmd);
    }
}