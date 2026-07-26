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
    private final Setting<Integer> sellCooldown;
    private final Setting<Integer> sellMaxRetries;
    private final Setting<Integer> sellSlot;
    private final Setting<Integer> sellSubmitSlot;

    public SellManager(Statistics stats,
                       Setting<String>  sellCommand,
                       Setting<Integer> sellThreshold,
                       Setting<Integer> sellCooldown,
                       Setting<Integer> sellMaxRetries,
                       Setting<Integer> sellSlot,
                       Setting<Integer> sellSubmitSlot) {
        this.stats = stats;
        this.sellCommand = sellCommand;
        this.sellThreshold = sellThreshold;
        this.sellCooldown = sellCooldown;
        this.sellMaxRetries = sellMaxRetries;
        this.sellSlot = sellSlot;
        this.sellSubmitSlot = sellSubmitSlot;
    }

    // ---- 状态机入口 ----

    /** @return true = 已处理出售相关状态, false = 不在出售状态 */
    public boolean handleState(CobbleSeller mod) {
        return switch (mod.getState()) {
            case SELL_WAIT_GUI  -> { handleSellGUI(mod);  yield true; }
            case RETRY_DELAY    -> { verifySellResult(mod); yield true; }
            case SELL_COOLDOWN  -> { mod.setState(CobbleSeller.State.IDLE); yield true; }
            default -> false;
        };
    }

    // ---- 启动出售 ----

    public void start(CobbleSeller mod, int count) {
        mod.setSellRetryCount(0);
        mc.player.networkHandler.sendChatCommand(sellCommand.get());
        mod.setState(CobbleSeller.State.SELL_WAIT_GUI);
        mod.setGuiWaitTicks(0);
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
                    mod.setTickDelay(20);
                } else {
                    mod.setTickDelay(10);
                }
            } catch (IndexOutOfBoundsException e) {
                mod.info("§c§l[系统] 出售槽位越界！请检查出售按钮/提交槽位配置");
                mc.player.closeHandledScreen();
                mod.setTickDelay(10);
            }
            mod.setState(CobbleSeller.State.RETRY_DELAY);
        } else if (mod.getGuiWaitTicks() >= 30) {
            mod.setState(CobbleSeller.State.RETRY_DELAY);
            mod.setTickDelay(10);
        } else {
            mod.setGuiWaitTicks(mod.getGuiWaitTicks() + 1);
        }
    }

    // ---- 验证出售结果 ----

    private void verifySellResult(CobbleSeller mod) {
        int afterCount = mod.countCobblestone();
        int maxRetries = sellMaxRetries.get();

        if (afterCount < sellThreshold.get()) {
            stats.setLastSellTime(System.currentTimeMillis());
            mod.setState(CobbleSeller.State.SELL_COOLDOWN);
            mod.setTickDelay(sellCooldown.get() / CobbleSeller.TICK_MS);
        } else if (mod.getSellRetryCount() < maxRetries) {
            mod.setSellRetryCount(mod.getSellRetryCount() + 1);
            mod.info("§c§l[系统] 出售未生效，重试 ("
                + mod.getSellRetryCount() + "/" + maxRetries + ")...");
            mc.player.networkHandler.sendChatCommand(sellCommand.get());
            mod.setState(CobbleSeller.State.SELL_WAIT_GUI);
            mod.setGuiWaitTicks(0);
        } else {
            mod.info("§c§l[系统] 出售失败 " + maxRetries + " 次，放弃本次出售");
            stats.setLastSellTime(System.currentTimeMillis());
            mod.setState(CobbleSeller.State.SELL_COOLDOWN);
            mod.setTickDelay(sellCooldown.get() / CobbleSeller.TICK_MS);
        }
    }
}