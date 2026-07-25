package com.example.addon.systems;

import com.example.addon.modules.CobbleSeller;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * HUD 渲染器 — Action Bar 状态显示 + 闲置隐藏
 */
public class HudRenderer {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Statistics stats;

    private final Setting<Integer> sellThreshold;
    private final Setting<Integer> idleTimeout;
    private final Setting<Boolean> enableReconnect;
    private final Setting<Boolean> enablePostCmd;

    public HudRenderer(Statistics stats,
                       Setting<Integer> sellThreshold,
                       Setting<Integer> idleTimeout,
                       Setting<Boolean> enableReconnect,
                       Setting<Boolean> enablePostCmd) {
        this.stats = stats;
        this.sellThreshold = sellThreshold;
        this.idleTimeout = idleTimeout;
        this.enableReconnect = enableReconnect;
        this.enablePostCmd = enablePostCmd;
    }

    // ---- 启动横幅 ----

    public void showStartupBanner(CobbleSeller mod) {
        mod.info("§b§m==================================================");
        mod.info("[ 圆石出售 ] §e§l模块已加载");
        mod.info("§7关闭「开启自动出售」即可暂停，回服/防掉线仍运行");
        mod.info("§b§m==================================================");
    }

    // ---- 暂停状态 ----

    public void showPauseStatus(CobbleSeller mod) {
        String reconnect = enableReconnect.get() ? "§aON" : "§cOFF";
        String postcmd   = enablePostCmd.get()    ? "§aON" : "§cOFF";
        mod.info("§c§l[出售模式] 已关闭");
        mod.info("§7自动回服: " + reconnect + " §7| 回服后指令: " + postcmd);
    }

    // ---- 主 HUD 渲染 ----

    public void render(CobbleSeller mod, int count, boolean isLobby) {
        if (mc.player == null) return;

        // 闲置超时 → 隐藏 HUD
        int timeoutSec = idleTimeout.get();
        if (timeoutSec > 0) {
            long idleMs = System.currentTimeMillis() - mod.lastPickupTime;
            if (idleMs >= timeoutSec * 1000L) {
                if (!mod.idleAlerted) {
                    mod.idleAlerted = true;
                    mod.warning("[!] 已 " + timeoutSec + " 秒无圆石产出，HUD 已收起");
                }
                mc.player.sendMessage(Text.literal(""), true);
                return;
            }
        }

        // 状态标识
        String status;
        CobbleSeller.State s = mod.state;
        if (s == CobbleSeller.State.SELL_WAIT_GUI || s == CobbleSeller.State.RETRY_DELAY) {
            status = "§6§lSELLING...";
        } else if (s == CobbleSeller.State.RECONNECT_WAIT_GUI
                || s == CobbleSeller.State.RECONNECT_CLICK_DELAY
                || isLobby) {
            status = "§d§lRECONNECTING...";
        } else {
            status = "§a§lON";
        }

        // 收益
        String profitText = "§6§l" + String.format("%.1f", stats.getTotalMoney() / 10000.0) + "W";

        // 速度
        double hours = stats.getTotalHours();
        String speedText = "§7§lCalc...";
        if (hours > 0.003) {
            double speedW = (stats.getTotalSold() / hours) / 10000.0;
            String color = speedW < 100 ? "§c§l" : "§a§l";
            speedText = color + String.format("%.1f", speedW)
                      + "W/h" + (speedW < 100 ? "(Low)" : "");
        }

        mc.player.sendMessage(Text.literal(
            "§f§lCOBBLE §7§l| " + status
            + " §7§l| STOCK: §e§l" + count + "§7§l/§c§l" + sellThreshold.get()
            + " §7§l| SPEED: " + speedText
            + " §7§l| PROFIT: " + profitText
        ), true);
    }
}