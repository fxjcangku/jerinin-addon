package com.example.addon.systems;

import com.example.addon.modules.CobbleSeller;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 出售管理器 — 出售状态机 + GUI 点击
 */
public class SellManager {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Statistics stats;

    private final Setting<String>  sellCommand;
    private final Setting<Integer> sellThreshold;
    private final Setting<Double>  pricePerCobble;
    private final Setting<Integer> sellCooldown;
    private final Setting<Integer> sellMaxRetries;
    private final Setting<Integer> sellSlot;
    private final Setting<Integer> sellSubmitSlot;

    public SellManager(Statistics stats,
                       Setting<String>  sellCommand,
                       Setting<Integer> sellThreshold,
                       Setting<Double>  pricePerCobble,
                       Setting<Integer> sellCooldown,
                       Setting<Integer> sellMaxRetries,
                       Setting<Integer> sellSlot,
                       Setting<Integer> sellSubmitSlot) {
        this.stats = stats;
        this.sellCommand = sellCommand;
        this.sellThreshold = sellThreshold;
        this.pricePerCobble = pricePerCobble;
        this.sellCooldown = sellCooldown;
        this.sellMaxRetries = sellMaxRetries;
        this.sellSlot = sellSlot;
        this.sellSubmitSlot = sellSubmitSlot;
    }

    // ---- 状态机入口 ----

    /** @return true = 已处理出售相关状态, false = 不在出售状态 */
    public boolean handleState(CobbleSeller mod) {
        return switch (mod.state) {
            case SELL_WAIT_GUI -> { handleSellGUI(mod); yield true; }
            case RETRY_DELAY   -> { verifySellResult(mod); yield true; }
            default -> false;
        };
    }

    // ---- 启动出售 ----

    public void start(CobbleSeller mod, int count) {
        stats.setExactCountBeforeSell(count);
        mod.sellRetryCount = 0;
        mc.player.networkHandler.sendChatCommand(sellCommand.get());
        mod.state = CobbleSeller.State.SELL_WAIT_GUI;
        mod.guiWaitTicks = 0;
    }

    // ---- 出售 GUI 处理 ----

    private void handleSellGUI(CobbleSeller mod) {
        if (mod.hasContainerGUI()) {
            int syncId = mc.player.currentScreenHandler.syncId;

            try {
                if (!mc.player.currentScreenHandler.getSlot(sellSlot.get()).getStack().isEmpty()) {
                    mc.interactionManager.clickSlot(syncId, sellSubmitSlot.get(), 0,
                        SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(syncId, sellSlot.get(), 0,
                        SlotActionType.PICKUP, mc.player);
                    mc.player.closeHandledScreen();
                    mod.tickDelay = 20;
                } else {
                    mod.tickDelay = 10;
                }
            } catch (IndexOutOfBoundsException e) {
                mod.info("§c§l[系统] 出售槽位越界！请检查出售按钮/提交槽位配置");
                mc.player.closeHandledScreen();
                mod.tickDelay = 10;
            }
            mod.state = CobbleSeller.State.RETRY_DELAY;
        } else if (mod.guiWaitTicks >= 30) {
            mod.state = CobbleSeller.State.RETRY_DELAY;
            mod.tickDelay = 10;
        } else {
            mod.guiWaitTicks++;
        }
    }

    // ---- 验证出售结果 ----

    private void verifySellResult(CobbleSeller mod) {
        int afterCount = mod.countCobblestone();
        int maxRetries = sellMaxRetries.get();

        if (afterCount < sellThreshold.get()) {
            stats.recordSale(stats.getExactCountBeforeSell(), pricePerCobble.get());
            mod.state = CobbleSeller.State.COOLDOWN;
            mod.tickDelay = sellCooldown.get() / 50;
            mod.cooldownFromSell = true;
        } else if (mod.sellRetryCount < maxRetries) {
            mod.sellRetryCount++;
            mod.info("§c§l[系统] 出售未生效，重试 ("
                + mod.sellRetryCount + "/" + maxRetries + ")...");
            mc.player.networkHandler.sendChatCommand(sellCommand.get());
            mod.state = CobbleSeller.State.SELL_WAIT_GUI;
            mod.guiWaitTicks = 0;
        } else {
            mod.info("§c§l[系统] 出售失败 " + maxRetries + " 次，放弃本次出售");
            stats.recordSellFail();
            mod.state = CobbleSeller.State.COOLDOWN;
            mod.tickDelay = sellCooldown.get() / 50;
            mod.cooldownFromSell = true;
        }
    }
}