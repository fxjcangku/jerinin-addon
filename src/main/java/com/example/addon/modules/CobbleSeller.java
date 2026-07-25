package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class CobbleSeller extends Module {

    // ================================================================
    //  §  设  置  组
    // ================================================================

    private final SettingGroup sgSell      = settings.createGroup("[ 出售设置 ]");
    private final SettingGroup sgReconnect = settings.createGroup("[ 回服设置 ]");
    private final SettingGroup sgLobby     = settings.createGroup("[ 大厅检测 ]");
    private final SettingGroup sgHUD       = settings.createGroup("[ 界面设置 ]");

    // ================================================================
    //  §  出  售  设  置
    // ================================================================

    private final Setting<Boolean> enableSelling = sgSell.add(new BoolSetting.Builder()
        .name("开启自动出售").description("关闭后回服/待机HUD仍运行").defaultValue(true).build());

    private final Setting<String> sellCommand = sgSell.add(new StringSetting.Builder()
        .name("出售指令").description("出售圆石的指令 (不含/)").defaultValue("sell").build());

    private final Setting<Integer> sellThreshold = sgSell.add(new IntSetting.Builder()
        .name("出售阈值").description("背包圆石达到此数量时自动出售").defaultValue(1700).min(1).sliderMax(5000).build());

    private final Setting<Double> pricePerCobble = sgSell.add(new DoubleSetting.Builder()
        .name("圆石单价").description("每颗圆石的价值").defaultValue(0.5).min(0).build());

    private final Setting<Integer> sellCooldown = sgSell.add(new IntSetting.Builder()
        .name("出售冷却(ms)").description("两次出售之间的最小间隔").defaultValue(1500).min(100).build());

    private final Setting<Integer> sellMaxRetries = sgSell.add(new IntSetting.Builder()
        .name("出售最大重试").description("出售失败后的最大重试次数").defaultValue(5).min(1).sliderMax(20).build());

    private final Setting<Integer> sellSlot = sgSell.add(new IntSetting.Builder()
        .name("出售按钮槽位").description("出售GUI中出售按钮的格子编号").defaultValue(49).min(0).max(53).build());

    private final Setting<Integer> sellSubmitSlot = sgSell.add(new IntSetting.Builder()
        .name("提交槽位").description("出售GUI中提交按钮的格子编号").defaultValue(50).min(0).max(53).build());

    // ================================================================
    //  §  回  服  设  置
    // ================================================================

    private final Setting<Boolean> enableReconnect = sgReconnect.add(new BoolSetting.Builder()
        .name("开启自动回服").description("检测到大厅后自动回生存服").defaultValue(true).build());

    private final Setting<String> menuSlots = sgReconnect.add(new StringSetting.Builder()
        .name("菜单槽位").description("回服需点击的格子, 多个用逗号分隔 (如: 13,22)").defaultValue("13").build());

    private final Setting<String> menuCommand = sgReconnect.add(new StringSetting.Builder()
        .name("菜单指令").description("打开菜单的指令 (不含/)").defaultValue("menu").build());

    private final Setting<Integer> menuClickInterval = sgReconnect.add(new IntSetting.Builder()
        .name("点击间隔(ms)").description("多个槽位时每次点击之间的延迟").defaultValue(500).min(100).sliderMax(2000).build());

    private final Setting<Integer> reconnectMaxRetries = sgReconnect.add(new IntSetting.Builder()
        .name("回服最大重试").description("回服失败后的最大重试次数").defaultValue(5).min(1).sliderMax(20).build());

    private final Setting<Boolean> enablePostCmd = sgReconnect.add(new BoolSetting.Builder()
        .name("回服后执行指令").description("回服成功后自动执行一条指令").defaultValue(false).build());

    private final Setting<String> postCmd = sgReconnect.add(new StringSetting.Builder()
        .name("回服后指令").description("回服后自动执行的指令 (不含/)").defaultValue("home zr").build());

    private final Setting<Integer> postCmdDelay = sgReconnect.add(new IntSetting.Builder()
        .name("指令延迟(ms)").description("回服后等地形加载再执行指令").defaultValue(2500).min(0).sliderMax(10000).build());

    // ================================================================
    //  §  大  厅  检  测  设  置
    // ================================================================

    private final Setting<Integer> lobbyX = sgLobby.add(new IntSetting.Builder()
        .name("大厅X坐标").description("大厅位置的X轴坐标").defaultValue(45).build());
    private final Setting<Integer> lobbyZ = sgLobby.add(new IntSetting.Builder()
        .name("大厅Z坐标").description("大厅位置的Z轴坐标").defaultValue(68).build());

    private final Setting<Integer> lobbyRadius = sgLobby.add(new IntSetting.Builder()
        .name("检测半径").description("以大厅坐标为中心的回城检测范围").defaultValue(10).min(1).build());

    private final Setting<Boolean> enableCoordinateCheck = sgLobby.add(new BoolSetting.Builder()
        .name("坐标检测").description("根据坐标判断是否在大厅").defaultValue(true).build());

    private final Setting<Boolean> enableKeywordCheck = sgLobby.add(new BoolSetting.Builder()
        .name("关键词检测").description("备用: 检测Tab列表中的大厅关键词").defaultValue(false).build());

    private final Setting<String> lobbyKeywords = sgLobby.add(new StringSetting.Builder()
        .name("大厅关键词").description("逗号分隔").defaultValue("大厅,大廳,Lobby,lobby,Rubik SMP").build());

    private final Setting<Boolean> enablePlayerCheck = sgLobby.add(new BoolSetting.Builder()
        .name("玩家名检测").description("检查列表中缺少生存服玩家=已掉入大厅").defaultValue(false).build());

    private final Setting<String> survivalPlayers = sgLobby.add(new StringSetting.Builder()
        .name("生存服玩家").description("逗号分隔的生存服玩家名, 列表里一个都没=在大厅").defaultValue("").build());

    // ================================================================
    //  §  界  面  设  置
    // ================================================================

    private final Setting<Integer> idleTimeout = sgHUD.add(new IntSetting.Builder()
        .name("闲置超时(秒)").description("多少秒没捡到圆石就隐藏HUD, 0=关闭").defaultValue(10).min(0).sliderMax(600).build());

    private final Setting<Boolean> enableAntiAFK = sgHUD.add(new BoolSetting.Builder()
        .name("防掉线").description("定期自动跳跃防止被服务器踢出").defaultValue(false).build());

    private final Setting<Integer> antiAFKInterval = sgHUD.add(new IntSetting.Builder()
        .name("防掉线间隔(秒)").description("多久执行一次防掉线动作").defaultValue(30).min(5).sliderMax(300).build());

    // ================================================================
    //  §  状  态  机
    // ================================================================

    private enum State {
        IDLE,                    // 空闲, 等待触发
        RECONNECT_WAIT_GUI,      // 已发 /menu, 等待 GUI 打开
        RECONNECT_CLICK_DELAY,   // 多槽位模式下等待点击间隔
        SELL_WAIT_GUI,           // 已发出售指令, 等待 GUI 打开
        RETRY_DELAY,             // 出售后等待验证
        COOLDOWN                 // 冷却
    }

    private State state = State.IDLE;

    // ---- 定时/计数 ----
    private int tickDelay        = 0;
    private int guiWaitTicks     = 0;
    private int sellRetryCount   = 0;
    private int reconnectRetryCount = 0;
    private int menuClickIndex   = 0;
    private int[] parsedMenuSlots = new int[0];

    // ---- 回服后指令 ----
    private boolean pendingPostCmd  = false;
    private int     postCmdTickDelay = 0;

    // ---- COOLDOWN 来源标记 (区分出售/回服消息) ----
    private boolean cooldownFromSell = false;

    // ---- 防掉线 ----
    private long lastAntiAFKTime = 0;

    // ================================================================
    //  §  统  计
    // ================================================================

    private long   totalSold       = 0;
    private double totalMoney       = 0;
    private int    exactCountBeforeSell = 0;
    private long   sessionStartTime = 0;
    private long   accumulatedTime  = 0;
    private long   lastSellTime     = 0;
    private long   lastReconnect    = 0;

    // ================================================================
    //  §  闲  置  检  测  +  暂  停  通  知
    // ================================================================

    private long    lastPickupTime      = 0;
    private int     lastKnownCount      = -1;
    private boolean idleAlerted         = false;
    private boolean salePausedNotified  = false;

    // ================================================================
    //  §  大  厅  检  测  缓  存  (性能优化, 避免每 tick 反射)
    // ================================================================

    private long    lastLobbyCheckTime = 0;
    private boolean cachedLobbyResult = false;
    private static final long LOBBY_CACHE_MS = 500;

    // 反射 Field 缓存 (只获取一次)
    private static Field headerField;
    private static Field footerField;
    private static boolean reflectionReady;
    private static boolean reflectionFailed;

    // ================================================================
    //  §  构  造  函  数
    // ================================================================

    public CobbleSeller() {
        super(AddonTemplate.JERININADDONE, "圆石出售",
            "自动出售圆石 \uff0b 大厅检测回服 \uff0b 防掉线监控");
    }

    // ================================================================
    //  §  生  命  周  期
    // ================================================================

    @Override
    public void onActivate() {
        state     = State.IDLE;
        tickDelay = 0;
        sellRetryCount   = 0;
        reconnectRetryCount = 0;
        menuClickIndex   = 0;
        parsedMenuSlots  = new int[0];
        sessionStartTime = System.currentTimeMillis();
        lastPickupTime   = System.currentTimeMillis();
        lastKnownCount   = -1;
        idleAlerted         = false;
        salePausedNotified  = false;
        lastAntiAFKTime     = System.currentTimeMillis();
        lobbyCacheReset();
        showStartupBanner();
    }

    @Override
    public void onDeactivate() {
        accumulatedTime += (System.currentTimeMillis() - sessionStartTime);
        long totalMin = accumulatedTime / 60000;
        long hours = totalMin / 60;
        long mins  = totalMin % 60;
        double moneyW = totalMoney / 10000.0;
        info("§c§l[系统] 模块已关闭 (防掉线监控停止)");
        info(String.format(
            "§e§l[统计] 累计挂机: §b§l%d §f§l小时 §b§l%d §f§l分钟 §7§l| §f§l累计收益: §6§l%.1fW",
            hours, mins, moneyW));
    }

    private void showStartupBanner() {
        info("§b§m==================================================");
        info("[ 圆石出售 ] §e§l模块已加载");
        info("§7关闭「开启自动出售」即可暂停，回服/防掉线仍运行");
        info("§b§m==================================================");
    }

    // ================================================================
    //  §  主  循  环  (TickEvent)
    // ================================================================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        boolean selling = enableSelling.get();
        int count = countCobblestone();

        // ---- 闲置检测 ----
        updateIdleDetection(count);

        // ---- 回服后指令倒计时 (无论出售开关都运行) ----
        if (pendingPostCmd) {
            postCmdTickDelay--;
            if (postCmdTickDelay <= 0) {
                pendingPostCmd = false;
                if (enablePostCmd.get()) {
                    mc.player.networkHandler.sendChatCommand(postCmd.get());
                    info("§a§l[回服] 指令 [" + postCmd.get() + "] 已触发！");
                }
            }
        }

        // ---- 防掉线: 定期跳跃 (无论出售开关都运行) ----
        if (enableAntiAFK.get() && mc.player.isOnGround()
            && mc.currentScreen == null
            && System.currentTimeMillis() - lastAntiAFKTime > antiAFKInterval.get() * 1000L) {
            mc.player.jump();
            lastAntiAFKTime = System.currentTimeMillis();
        }

        // ---- 暂停出售模式 ----
        if (!selling) {
            if (!salePausedNotified) {
                salePausedNotified = true;
                showPauseStatus();
            }
            mc.player.sendMessage(Text.literal(""), true);
            // 允许回服状态机继续运行
            runReconnectStatesOnly();
            return;
        }
        salePausedNotified = false;

        // ---- 冷却中 ----
        if (tickDelay > 0) {
            tickDelay--;
            updateActiveHUD(count);
            return;
        }

        updateActiveHUD(count);

        // ---- 状态机 ----
        runStateMachine(count);
    }

    // ================================================================
    //  §  状  态  机  逻  辑
    // ================================================================

    // ---- 共享回服状态处理 (消除代码重复) ----
    // 返回 true = 已处理 (回服相关状态), false = 出售相关状态, 需调用方自行处理
    private boolean handleReconnectState() {
        return switch (state) {
            case IDLE -> {
                if (!isLobby()) reconnectRetryCount = 0;
                if (enableReconnect.get() && isLobby()
                    && System.currentTimeMillis() - lastReconnect > 5000) {
                    if (reconnectRetryCount < reconnectMaxRetries.get()) {
                        startReconnectSequence();
                    } else if (System.currentTimeMillis() - lastReconnect > 30000) {
                        reconnectRetryCount = 0;
                        startReconnectSequence();
                    }
                }
                yield true;
            }
            case RECONNECT_WAIT_GUI -> {
                if (hasContainerGUI()) {
                    tickDelay = menuClickInterval.get() / 50;
                    state = State.RECONNECT_CLICK_DELAY;
                } else if (guiWaitTicks >= 60) {
                    info("§c§l[系统] 高延迟警告：等待 3 秒仍未加载菜单！");
                    state = State.IDLE;
                } else {
                    guiWaitTicks++;
                }
                yield true;
            }
            case RECONNECT_CLICK_DELAY -> {
                if (hasContainerGUI()) {
                    clickReconnectSlot();
                } else {
                    reconnectRetryWithMenu();
                }
                yield true;
            }
            case COOLDOWN -> {
                if (!pendingPostCmd) {
                    if (cooldownFromSell) {
                        // 出售成功不刷屏，HUD 已显示
                        cooldownFromSell = false;
                    } else if (!isLobby()) {
                        info("§a§l[系统] 回服成功！");
                        reconnectRetryCount = 0;
                    } else {
                        info("§e§l[系统] 回服未确认，等待冷却后重试...");
                    }
                }
                state = State.IDLE;
                yield true;
            }
            default -> false;
        };
    }

    private void runStateMachine(int count) {
        if (handleReconnectState()) {
            // 回服状态已处理 (含 IDLE), 如果仍在 IDLE 且不在大厅则检查出售
            if (state == State.IDLE && !isLobby()
                && count >= sellThreshold.get()
                && System.currentTimeMillis() - lastSellTime > sellCooldown.get()) {
                startSellSequence(count);
            }
            return;
        }
        // 出售相关状态
        switch (state) {
            case SELL_WAIT_GUI -> handleSellGUI();
            case RETRY_DELAY -> verifySellResult();
        }
    }

    // ================================================================
    //  §  出  售  逻  辑
    // ================================================================

    private void startSellSequence(int count) {
        exactCountBeforeSell = count;
        sellRetryCount = 0;
        mc.player.networkHandler.sendChatCommand(sellCommand.get());
        state = State.SELL_WAIT_GUI;
        guiWaitTicks = 0;
    }

    private void handleSellGUI() {
        if (hasContainerGUI()) {
            int syncId = mc.player.currentScreenHandler.syncId;
            int finalCheck = countCobblestone();
            if (finalCheck > exactCountBeforeSell) exactCountBeforeSell = finalCheck;

            try {
                if (!mc.player.currentScreenHandler.getSlot(sellSlot.get()).getStack().isEmpty()) {
                    mc.interactionManager.clickSlot(syncId, sellSubmitSlot.get(), 0,
                        SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(syncId, sellSlot.get(), 0,
                        SlotActionType.PICKUP, mc.player);
                    mc.player.closeHandledScreen();
                    tickDelay = 20;
                } else {
                    tickDelay = 10;
                }
            } catch (IndexOutOfBoundsException e) {
                info("§c§l[系统] 出售槽位越界！请检查出售按钮/提交槽位配置");
                mc.player.closeHandledScreen();
                tickDelay = 10;
            }
            state = State.RETRY_DELAY;
        } else if (guiWaitTicks >= 30) {
            state = State.RETRY_DELAY;
            tickDelay = 10;
        } else {
            guiWaitTicks++;
        }
    }

    private void verifySellResult() {
        int afterCount = countCobblestone();
        int maxRetries = sellMaxRetries.get();

        if (afterCount < sellThreshold.get()) {
            // 出售成功
            totalSold  += exactCountBeforeSell;
            totalMoney += exactCountBeforeSell * pricePerCobble.get();
            lastSellTime = System.currentTimeMillis();
            state   = State.COOLDOWN;
            tickDelay = sellCooldown.get() / 50;
            cooldownFromSell = true;
        } else if (sellRetryCount < maxRetries) {
            // 出售失败, 重试
            sellRetryCount++;
            info("§c§l[系统] 出售未生效，重试 ("
                + sellRetryCount + "/" + maxRetries + ")...");
            mc.player.networkHandler.sendChatCommand(sellCommand.get());
            state = State.SELL_WAIT_GUI;
            guiWaitTicks = 0;
        } else {
            // 放弃
            info("§c§l[系统] 出售失败 " + maxRetries + " 次，放弃本次出售");
            lastSellTime = System.currentTimeMillis();
            state   = State.COOLDOWN;
            tickDelay = sellCooldown.get() / 50;
            cooldownFromSell = true;
        }
    }

    // ================================================================
    //  §  回  服  逻  辑
    // ================================================================

    private void startReconnectSequence() {
        parsedMenuSlots = parseSlots();
        menuClickIndex  = 0;
        lobbyCacheReset();  // 清除大厅缓存，确保下次检测准确
        reconnectRetryCount++;  // 递增重试计数
        info("§e§l[系统] 检测到大厅状态，启动自动回服序列... (第 "
            + reconnectRetryCount + "/" + reconnectMaxRetries.get() + " 次)");
        mc.player.networkHandler.sendChatCommand(menuCommand.get());
        state = State.RECONNECT_WAIT_GUI;
        guiWaitTicks = 0;
        lastReconnect = System.currentTimeMillis();
    }

    private void clickReconnectSlot() {
        int syncId = mc.player.currentScreenHandler.syncId;
        int slot = parsedMenuSlots[menuClickIndex];
        mc.interactionManager.clickSlot(syncId, slot, 0,
            SlotActionType.PICKUP, mc.player);
        info("§d§l[回服] 点击槽位 " + slot
            + " (" + (menuClickIndex + 1) + "/" + parsedMenuSlots.length + ")");

        menuClickIndex++;
        if (menuClickIndex >= parsedMenuSlots.length) {
            // 全部点完, 进入冷却等指令执行
            pendingPostCmd   = true;
            postCmdTickDelay = postCmdDelay.get() / 50;
            state     = State.COOLDOWN;
            tickDelay = 40;
        } else {
            // 还有下一个槽位, 等待间隔
            state     = State.RECONNECT_CLICK_DELAY;
            tickDelay = menuClickInterval.get() / 50;
        }
    }

    private void reconnectRetryWithMenu() {
        // 冷却检查：避免疯狂重发菜单
        if (System.currentTimeMillis() - lastReconnect < 5000) {
            state = State.IDLE;
            return;
        }
        if (reconnectRetryCount >= reconnectMaxRetries.get()) {
            info("§c§l[系统] 回服重试已达上限，放弃本次回服");
            state = State.IDLE;
            return;
        }
        info("§c§l[系统] 菜单意外关闭，重新打开...");
        mc.player.networkHandler.sendChatCommand(menuCommand.get());
        state = State.RECONNECT_WAIT_GUI;
        guiWaitTicks = 0;
        menuClickIndex = 0;
        lastReconnect = System.currentTimeMillis();
    }

    // ---- 后台回服 (出售暂停时使用, 复用共享状态机) ----
    private void runReconnectStatesOnly() {
        if (tickDelay > 0) { tickDelay--; return; }

        if (!handleReconnectState()) {
            // 出售相关状态, 直接复位
            state = State.IDLE;
        }
    }

    // ---- 解析逗号分隔的槽位号 ----
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

    // ---- 检测是否有容器 GUI 打开 (不依赖屏幕类型) ----
    private boolean hasContainerGUI() {
        return mc.player != null
            && mc.player.currentScreenHandler != null
            && mc.player.currentScreenHandler.syncId != 0;
    }

    // ================================================================
    //  §  大  厅  检  测
    // ================================================================

    private boolean isLobby() {
        if (mc.player == null) return false;

        // 缓存: 500ms 内返回相同结果, 避免每 tick 重复检测
        long now = System.currentTimeMillis();
        if (now - lastLobbyCheckTime < LOBBY_CACHE_MS) {
            return cachedLobbyResult;
        }
        lastLobbyCheckTime = now;

        // 1. 坐标检测 (独立开关)
        if (enableCoordinateCheck.get() && checkByCoordinate()) {
            return cachedLobbyResult = true;
        }

        // 2. Tab 关键词检测 (独立开关)
        if (enableKeywordCheck.get() && checkByTabKeyword()) {
            return cachedLobbyResult = true;
        }

        // 3. 玩家名反向检测 (独立开关)
        if (enablePlayerCheck.get() && checkByPlayerAbsence()) {
            return cachedLobbyResult = true;
        }

        return cachedLobbyResult = false;
    }

    private void lobbyCacheReset() {
        lastLobbyCheckTime = 0;
        cachedLobbyResult  = false;
    }

    private boolean checkByCoordinate() {
        double dx = mc.player.getX() - lobbyX.get();
        double dz = mc.player.getZ() - lobbyZ.get();
        return Math.sqrt(dx * dx + dz * dz) <= lobbyRadius.get();
    }

    private boolean checkByTabKeyword() {
        ensureReflectionReady();
        String[] keywords = lobbyKeywords.get().split(",");
        for (String kw : keywords) {
            String k = kw.trim();
            if (k.isEmpty()) continue;

            // 检测 Tab 栏 header/footer
            try {
                Text header = (Text) headerField.get(mc.inGameHud.getPlayerListHud());
                Text footer = (Text) footerField.get(mc.inGameHud.getPlayerListHud());
                if ((header != null && header.getString().contains(k))
                 || (footer != null && footer.getString().contains(k))) return true;
            } catch (Exception ignored) {}

            // 检测计分板 (Scoreboard sidebar)
            try {
                Scoreboard sb = mc.world.getScoreboard();
                if (sb != null) {
                    ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                    if (obj != null && obj.getDisplayName() != null
                        && obj.getDisplayName().getString().contains(k)) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean checkByPlayerAbsence() {
        String raw = survivalPlayers.get().trim();
        if (raw.isEmpty()) return false;

        // Tab 列表为空 (刚进服) → 无法判断，不触发
        var playerList = mc.getNetworkHandler().getPlayerList();
        if (playerList.isEmpty()) return false;

        String[] names = raw.split(",");
        for (String name : names) {
            String n = name.trim();
            if (n.isEmpty()) continue;
            try {
                for (var entry : playerList) {
                    String playerName = entry.getDisplayName() != null
                        ? entry.getDisplayName().getString() : "";
                    if (playerName.equalsIgnoreCase(n)) {
                        return false;  // 找到生存服玩家 → 不在大厅
                    }
                }
            } catch (Exception ignored) {}
        }
        // 一个都没找到 → 在大厅
        return true;
    }

    // 缓存反射 Field (只获取一次)
    private static void ensureReflectionReady() {
        if (reflectionReady || reflectionFailed) return;
        try {
            headerField = PlayerListHud.class.getDeclaredField("header");
            headerField.setAccessible(true);
            footerField = PlayerListHud.class.getDeclaredField("footer");
            footerField.setAccessible(true);
            reflectionReady = true;
        } catch (Exception e) {
            reflectionFailed = true;
        }
    }

    // ================================================================
    //  §  圆  石  计  数
    // ================================================================

    private int countCobblestone() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COBBLESTONE) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return count;
    }

    // ================================================================
    //  §  闲  置  检  测
    // ================================================================

    private void updateIdleDetection(int count) {
        int timeoutSec = idleTimeout.get();
        if (timeoutSec <= 0) return;

        if (lastKnownCount >= 0 && count > lastKnownCount) {
            lastPickupTime = System.currentTimeMillis();
            idleAlerted = false;
        }
        lastKnownCount = count;
    }

    // ================================================================
    //  §  暂  停  状  态  播  报
    // ================================================================

    private void showPauseStatus() {
        String reconnect = enableReconnect.get() ? "§aON" : "§cOFF";
        String postcmd   = enablePostCmd.get()    ? "§aON" : "§cOFF";
        info("§c§l[出售模式] 已关闭");
        info("§7自动回服: " + reconnect + " §7| 回服后指令: " + postcmd);
    }

    // ================================================================
    //  §  H U D
    // ================================================================

    private void updateActiveHUD(int count) {
        // ---- 闲置超时: 隐藏 HUD ----
        int timeoutSec = idleTimeout.get();
        if (timeoutSec > 0) {
            long idleMs = System.currentTimeMillis() - lastPickupTime;
            if (idleMs >= timeoutSec * 1000L) {
                if (!idleAlerted) {
                    idleAlerted = true;
                    warning("[!] 已 " + timeoutSec + " 秒无圆石产出，HUD 已收起");
                }
                mc.player.sendMessage(Text.literal(""), true);
                return;
            }
        }

        // ---- 状态标识 ----
        String status;
        if (state == State.SELL_WAIT_GUI || state == State.RETRY_DELAY) {
            status = "§6§lSELLING...";
        } else if (state == State.RECONNECT_WAIT_GUI
                || state == State.RECONNECT_CLICK_DELAY
                || isLobby()) {
            status = "§d§lRECONNECTING...";
        } else {
            status = "§a§lON";
        }

        // ---- 收益计算 ----
        String profitText = "§6§l" + String.format("%.1f", totalMoney / 10000.0) + "W";

        // ---- 速度计算 ----
        long currentMs = System.currentTimeMillis() - sessionStartTime;
        long totalMs  = accumulatedTime + currentMs;
        double hours  = totalMs / 3600000.0;
        String speedText = "§7§lCalc...";
        if (hours > 0.003) {
            double speedW = (totalSold / hours) / 10000.0;
            String color = speedW < 100 ? "§c§l" : "§a§l";
            speedText = color + String.format("%.1f", speedW)
                      + "W/h" + (speedW < 100 ? "(Low)" : "");
        }

        // ---- 组装 HUD ----
        mc.player.sendMessage(Text.literal(
            "§f§lCOBBLE §7§l| " + status
            + " §7§l| STOCK: §e§l" + count + "§7§l/§c§l" + sellThreshold.get()
            + " §7§l| SPEED: " + speedText
            + " §7§l| PROFIT: " + profitText
        ), true);
    }
}
