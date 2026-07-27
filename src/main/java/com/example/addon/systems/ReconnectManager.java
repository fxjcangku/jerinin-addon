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
                tryStartReconnect(mod);
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
                yield true;
            }
            default -> false;
        };
    }

    // ---- IDLE 大厅检测 / 回服启动 ----

    public boolean tryStartReconnect(CobbleSeller mod) {
        if (!enableReconnect.get()) return false;
        if (mod.getState() == CobbleSeller.State.RECONNECT_WAIT_GUI
            || mod.getState() == CobbleSeller.State.RECONNECT_CLICK_DELAY
            || mod.getState() == CobbleSeller.State.RECONNECT_COOLDOWN) return false;

        boolean inLobby = lobby.isLobby();
        if (!inLobby) {
            if (mod.getState() == CobbleSeller.State.IDLE) mod.setReconnectRetryCount(0);
            return false;
        }

        long elapsed = System.currentTimeMillis() - stats.getLastReconnect();
        if (elapsed <= 5000) return false;

        if (mod.getReconnectRetryCount() >= reconnectMaxRetries.get()) {
            if (elapsed <= 30000) return false;
            mod.setReconnectRetryCount(0);
        }

        startReconnectSequence(mod);
        return true;
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
        if (!mod.isPendingPostCmd() || mod.getState() != CobbleSeller.State.RECONNECT_COOLDOWN) return;
        if (mc.player == null) return;

        // 仍被任一检测方式判断为大厅时不开始倒计时；5 秒后回到 IDLE 重试菜单。
        if (lobby.isLobby()) {
            mod.setGuiWaitTicks(mod.getGuiWaitTicks() + 1);
            if (mod.getGuiWaitTicks() >= 100) {
                mod.setPendingPostCmd(false);
                mod.setState(CobbleSeller.State.IDLE);
                mod.info("§e§l[系统] 回服未确认，等待冷却后重试...");
            }
            return;
        }

        mod.setPostCmdTickDelay(mod.getPostCmdTickDelay() - 1);
        if (mod.getPostCmdTickDelay() > 0) return;

        mod.setPendingPostCmd(false);
        if (enablePostCmd.get()) {
            String command = postCmd.get().trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (!command.isEmpty()) {
                mc.player.networkHandler.sendChatCommand(command);
                mod.info("§a§l[回服] 指令 [" + command + "] 已触发！");
            } else {
                mod.info("§c§l[回服] 回服后指令为空，已跳过");
            }
        }
        mod.info("§a§l[系统] 回服成功！");
        mod.setReconnectRetryCount(0);
        mod.setState(CobbleSeller.State.IDLE);
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
        String command = menuCommand.get().trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isEmpty()) {
            mod.info("§c§l[系统] 菜单指令为空，无法执行回服");
            stats.setLastReconnect(System.currentTimeMillis());
            return;
        }

        mod.setParsedMenuSlots(parseSlots());
        mod.setMenuClickIndex(0);
        String reason = lobby.getLastDetectionReason();
        lobby.cacheReset();
        mod.setReconnectRetryCount(mod.getReconnectRetryCount() + 1);
        mod.info("§e§l[系统] " + reason + "检测到大厅，启动自动回服序列... (第 "
            + mod.getReconnectRetryCount() + "/" + reconnectMaxRetries.get() + " 次)");
        mc.player.networkHandler.sendChatCommand(command);
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
            mod.setPostCmdTickDelay(Math.max(1, postCmdDelay.get() / CobbleSeller.TICK_MS));
            mod.setGuiWaitTicks(0);
            mod.setState(CobbleSeller.State.RECONNECT_COOLDOWN);
            mod.setTickDelay(0);
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
        // tickPostCmd 负责等待离开大厅、执行回服后指令并结束回服状态。
        if (mod.isPendingPostCmd()) return;
        mod.setState(CobbleSeller.State.IDLE);
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
