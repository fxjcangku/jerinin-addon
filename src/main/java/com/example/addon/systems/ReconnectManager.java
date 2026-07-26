package com.example.addon.systems;

import com.example.addon.modules.CobbleSeller;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 回服管理器 — 回服状态机 + 菜单点击 + 回服后指令 + 防掉线
 */
public class ReconnectManager {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Statistics stats;
    private final LobbyDetector lobby;

    private final Setting<Boolean> enableReconnect;
    private final Setting<String>  menuSlots;
    private final Setting<String>  menuCommand;
    private final Setting<Integer> menuClickInterval;
    private final Setting<Integer> reconnectMaxRetries;
    private final Setting<Boolean> enablePostCmd;
    private final Setting<String>  postCmd;
    private final Setting<Integer> postCmdDelay;
    private final Setting<Boolean> enableAntiAFK;
    private final Setting<Integer> antiAFKInterval;

    public ReconnectManager(Statistics stats,
                            LobbyDetector lobby,
                            Setting<Boolean> enableReconnect,
                            Setting<String>  menuSlots,
                            Setting<String>  menuCommand,
                            Setting<Integer> menuClickInterval,
                            Setting<Integer> reconnectMaxRetries,
                            Setting<Boolean> enablePostCmd,
                            Setting<String>  postCmd,
                            Setting<Integer> postCmdDelay,
                            Setting<Boolean> enableAntiAFK,
                            Setting<Integer> antiAFKInterval) {
        this.stats = stats;
        this.lobby = lobby;
        this.enableReconnect = enableReconnect;
        this.menuSlots = menuSlots;
        this.menuCommand = menuCommand;
        this.menuClickInterval = menuClickInterval;
        this.reconnectMaxRetries = reconnectMaxRetries;
        this.enablePostCmd = enablePostCmd;
        this.postCmd = postCmd;
        this.postCmdDelay = postCmdDelay;
        this.enableAntiAFK = enableAntiAFK;
        this.antiAFKInterval = antiAFKInterval;
    }

    // ---- 状态机入口 ----

    /** @return true = 已处理回服相关状态 (含 IDLE), false = 出售相关状态 */
    public boolean handleState(CobbleSeller mod) {
        return switch (mod.getState()) {
            case IDLE -> {
                if (!lobby.isLobby()) mod.setReconnectRetryCount(0);
                if (enableReconnect.get() && lobby.isLobby()
                    && System.currentTimeMillis() - stats.getLastReconnect() > 5000) {
                    if (mod.getReconnectRetryCount() < reconnectMaxRetries.get()) {
                        startReconnectSequence(mod);
                    } else if (System.currentTimeMillis() - stats.getLastReconnect() > 30000) {
                        mod.setReconnectRetryCount(0);
                        startReconnectSequence(mod);
                    }
                }
                yield true;
            }
            case RECONNECT_WAIT_GUI -> {
                if (mod.hasContainerGUI()) {
                    mod.setTickDelay(menuClickInterval.get() / CobbleSeller.TICK_MS);
                    mod.setState(CobbleSeller.State.RECONNECT_CLICK_DELAY);
                } else if (mod.getGuiWaitTicks() >= 60) {
                    mod.info("§c§l[系统] 高延迟警告：等待 3 秒仍未加载菜单！");
                    mod.setState(CobbleSeller.State.IDLE);
                } else {
                    mod.setGuiWaitTicks(mod.getGuiWaitTicks() + 1);
                }
                yield true;
            }
            case RECONNECT_CLICK_DELAY -> {
                if (mod.hasContainerGUI()) {
                    clickReconnectSlot(mod);
                } else {
                    reconnectRetryWithMenu(mod);
                }
                yield true;
            }
            case RECONNECT_COOLDOWN -> {
                handleReconnectCooldown(mod);
                mod.setState(CobbleSeller.State.IDLE);
                yield true;
            }
            default -> false;
        };
    }

    // ---- 后台 tick (出售暂停时) ----

    public void runReconnectOnly(CobbleSeller mod) {
        if (mod.getTickDelay() > 0) { mod.setTickDelay(mod.getTickDelay() - 1); return; }
        if (!handleState(mod)) {
            mod.setState(CobbleSeller.State.IDLE);
        }
    }

    // ---- 回服后指令 ----

    public void tickPostCmd(CobbleSeller mod) {
        if (!mod.isPendingPostCmd()) return;
        mod.setPostCmdTickDelay(mod.getPostCmdTickDelay() - 1);
        if (mod.getPostCmdTickDelay() <= 0) {
            mod.setPendingPostCmd(false);
            if (enablePostCmd.get()) {
                mc.player.networkHandler.sendChatCommand(postCmd.get());
                mod.info("§a§l[回服] 指令 [" + postCmd.get() + "] 已触发！");
            }
        }
    }

    // ---- 防掉线 ----

    public void tickAntiAFK(CobbleSeller mod) {
        if (!enableAntiAFK.get()) return;
        if (mc.player == null || !mc.player.isOnGround()) return;
        if (mc.currentScreen != null) return;
        if (System.currentTimeMillis() - mod.getLastAntiAFKTime() < antiAFKInterval.get() * 1000L) return;
        mc.player.jump();
        mod.setLastAntiAFKTime(System.currentTimeMillis());
    }

    // ---- 启动回服序列 ----

    private void startReconnectSequence(CobbleSeller mod) {
        mod.setParsedMenuSlots(parseSlots());
        mod.setMenuClickIndex(0);
        lobby.cacheReset();
        mod.setReconnectRetryCount(mod.getReconnectRetryCount() + 1);
        mod.info("§e§l[系统] 检测到大厅状态，启动自动回服序列... (第 "
            + mod.getReconnectRetryCount() + "/" + reconnectMaxRetries.get() + " 次)");
        mc.player.networkHandler.sendChatCommand(menuCommand.get());
        mod.setState(CobbleSeller.State.RECONNECT_WAIT_GUI);
        mod.setGuiWaitTicks(0);
        stats.setLastReconnect(System.currentTimeMillis());
    }

    // ---- 点击回服槽位 ----

    private void clickReconnectSlot(CobbleSeller mod) {
        int syncId = mc.player.currentScreenHandler.syncId;
        int slot = mod.getParsedMenuSlots()[mod.getMenuClickIndex()];
        mc.interactionManager.clickSlot(syncId, slot, 0,
            SlotActionType.PICKUP, mc.player);
        mod.info("§d§l[回服] 点击槽位 " + slot
            + " (" + (mod.getMenuClickIndex() + 1) + "/" + mod.getParsedMenuSlots().length + ")");

        mod.setMenuClickIndex(mod.getMenuClickIndex() + 1);
        if (mod.getMenuClickIndex() >= mod.getParsedMenuSlots().length) {
            mod.setPendingPostCmd(true);
            mod.setPostCmdTickDelay(postCmdDelay.get() / CobbleSeller.TICK_MS);
            mod.setState(CobbleSeller.State.RECONNECT_COOLDOWN);
            mod.setTickDelay(40);
        } else {
            mod.setState(CobbleSeller.State.RECONNECT_CLICK_DELAY);
            mod.setTickDelay(menuClickInterval.get() / CobbleSeller.TICK_MS);
        }
    }

    // ---- 菜单意外关闭重试 ----

    private void reconnectRetryWithMenu(CobbleSeller mod) {
        if (System.currentTimeMillis() - stats.getLastReconnect() < 5000) {
            mod.setState(CobbleSeller.State.IDLE);
            return;
        }
        if (mod.getReconnectRetryCount() >= reconnectMaxRetries.get()) {
            mod.info("§c§l[系统] 回服重试已达上限，放弃本次回服");
            mod.setState(CobbleSeller.State.IDLE);
            return;
        }
        mod.info("§c§l[系统] 菜单意外关闭，重新打开...");
        mc.player.networkHandler.sendChatCommand(menuCommand.get());
        mod.setState(CobbleSeller.State.RECONNECT_WAIT_GUI);
        mod.setGuiWaitTicks(0);
        mod.setMenuClickIndex(0);
        stats.setLastReconnect(System.currentTimeMillis());
    }

    // ---- RECONNECT_COOLDOWN 处理 (仅回服冷却) ----

    private void handleReconnectCooldown(CobbleSeller mod) {
        if (mod.isPendingPostCmd()) return;
        if (!lobby.isLobby()) {
            mod.info("§a§l[系统] 回服成功！");
            mod.setReconnectRetryCount(0);
        } else {
            mod.info("§e§l[系统] 回服未确认，等待冷却后重试...");
        }
    }

    // ---- 解析槽位 ----

    private int[] parseSlots() {
        String raw = menuSlots.get().trim();
        if (raw.isEmpty()) return new int[]{13};
        String[] parts = raw.split(",");
        int[] slots = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { slots[i] = Integer.parseInt(parts[i].trim()); }
            catch (Exception e) { slots[i] = 13; }
        }
        return slots;
    }
}
