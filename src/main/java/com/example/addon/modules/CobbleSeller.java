package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.systems.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class CobbleSeller extends Module {

    // ================================================================
    //  §  设  置  组
    // ================================================================

    private final SettingGroup sgSell      = settings.createGroup("[ 出售设置 ]");
    private final SettingGroup sgReconnect = settings.createGroup("[ 回服设置 ]");
    private final SettingGroup sgLobby     = settings.createGroup("[ 大厅检测 ]");
    private final SettingGroup sgHUD       = settings.createGroup("[ 界面设置 ]");

    // ---- 出售设置 ----
    public final Setting<Boolean> enableSelling = sgSell.add(new BoolSetting.Builder()
        .name("开启自动出售").description("关闭后回服/待机HUD仍运行").defaultValue(true).build());
    final Setting<String> sellCommand = sgSell.add(new StringSetting.Builder()
        .name("出售指令").description("出售圆石的指令 (不含/)").defaultValue("sell").build());
    public final Setting<Integer> sellThreshold = sgSell.add(new IntSetting.Builder()
        .name("出售阈值").description("背包圆石达到此数量时自动出售").defaultValue(1700).min(1).sliderMax(5000).build());
    final Setting<Double> pricePerCobble = sgSell.add(new DoubleSetting.Builder()
        .name("圆石单价").description("每颗圆石的价值").defaultValue(0.5).min(0).build());
    final Setting<Integer> sellCooldown = sgSell.add(new IntSetting.Builder()
        .name("出售冷却(ms)").description("两次出售之间的最小间隔").defaultValue(1500).min(100).build());
    final Setting<Integer> sellMaxRetries = sgSell.add(new IntSetting.Builder()
        .name("出售最大重试").description("出售失败后的最大重试次数").defaultValue(5).min(1).sliderMax(20).build());
    final Setting<Integer> sellSlot = sgSell.add(new IntSetting.Builder()
        .name("出售按钮槽位").description("出售GUI中出售按钮的格子编号").defaultValue(49).min(0).max(53).build());
    final Setting<Integer> sellSubmitSlot = sgSell.add(new IntSetting.Builder()
        .name("提交槽位").description("出售GUI中提交按钮的格子编号").defaultValue(50).min(0).max(53).build());

    // ---- 回服设置 ----
    final Setting<Boolean> enableReconnect = sgReconnect.add(new BoolSetting.Builder()
        .name("开启自动回服").description("检测到大厅后自动回生存服").defaultValue(true).build());
    final Setting<String> menuSlots = sgReconnect.add(new StringSetting.Builder()
        .name("菜单槽位").description("回服需点击的格子, 多个用逗号分隔 (如: 13,22)").defaultValue("13").build());
    final Setting<String> menuCommand = sgReconnect.add(new StringSetting.Builder()
        .name("菜单指令").description("打开菜单的指令 (不含/)").defaultValue("menu").build());
    final Setting<Integer> menuClickInterval = sgReconnect.add(new IntSetting.Builder()
        .name("点击间隔(ms)").description("多个槽位时每次点击之间的延迟").defaultValue(500).min(100).sliderMax(2000).build());
    final Setting<Integer> reconnectMaxRetries = sgReconnect.add(new IntSetting.Builder()
        .name("回服最大重试").description("回服失败后的最大重试次数").defaultValue(5).min(1).sliderMax(20).build());
    final Setting<Boolean> enablePostCmd = sgReconnect.add(new BoolSetting.Builder()
        .name("回服后执行指令").description("回服成功后自动执行一条指令").defaultValue(false).build());
    final Setting<String> postCmd = sgReconnect.add(new StringSetting.Builder()
        .name("回服后指令").description("回服后自动执行的指令 (不含/)").defaultValue("home zr").build());
    final Setting<Integer> postCmdDelay = sgReconnect.add(new IntSetting.Builder()
        .name("指令延迟(ms)").description("回服后等地形加载再执行指令").defaultValue(2500).min(0).sliderMax(10000).build());

    // ---- 大厅检测设置 ----
    final Setting<Integer> lobbyX = sgLobby.add(new IntSetting.Builder()
        .name("大厅X坐标").description("大厅位置的X轴坐标").defaultValue(45).build());
    final Setting<Integer> lobbyZ = sgLobby.add(new IntSetting.Builder()
        .name("大厅Z坐标").description("大厅位置的Z轴坐标").defaultValue(68).build());
    final Setting<Integer> lobbyRadius = sgLobby.add(new IntSetting.Builder()
        .name("检测半径").description("以大厅坐标为中心的回城检测范围").defaultValue(10).min(1).build());
    final Setting<Boolean> enableCoordinateCheck = sgLobby.add(new BoolSetting.Builder()
        .name("坐标检测").description("根据坐标判断是否在大厅").defaultValue(true).build());
    final Setting<Boolean> enableKeywordCheck = sgLobby.add(new BoolSetting.Builder()
        .name("关键词检测").description("备用: 检测Tab列表中的大厅关键词").defaultValue(false).build());
    final Setting<String> lobbyKeywords = sgLobby.add(new StringSetting.Builder()
        .name("大厅关键词").description("逗号分隔").defaultValue("大厅,大廳,Lobby,lobby,Rubik SMP").build());
    final Setting<Boolean> enablePlayerCheck = sgLobby.add(new BoolSetting.Builder()
        .name("玩家名检测").description("检查列表中缺少生存服玩家=已掉入大厅").defaultValue(false).build());
    final Setting<String> survivalPlayers = sgLobby.add(new StringSetting.Builder()
        .name("生存服玩家").description("逗号分隔的生存服玩家名, 列表里一个都没=在大厅").defaultValue("").build());

    // ---- 界面设置 ----
    public final Setting<Integer> idleTimeout = sgHUD.add(new IntSetting.Builder()
        .name("闲置超时(秒)").description("多少秒没捡到圆石就隐藏HUD, 0=关闭").defaultValue(10).min(0).sliderMax(600).build());
    final Setting<Boolean> enableAntiAFK = sgHUD.add(new BoolSetting.Builder()
        .name("防掉线").description("定期自动跳跃防止被服务器踢出").defaultValue(false).build());
    final Setting<Integer> antiAFKInterval = sgHUD.add(new IntSetting.Builder()
        .name("防掉线间隔(秒)").description("多久执行一次防掉线动作").defaultValue(30).min(5).sliderMax(300).build());

    // ================================================================
    //  §  状  态  机 (共享)
    // ================================================================

    public enum State {
        IDLE, RECONNECT_WAIT_GUI, RECONNECT_CLICK_DELAY,
        SELL_WAIT_GUI, RETRY_DELAY, COOLDOWN
    }

    public State state = State.IDLE;

    // ---- 定时/计数 ----
    public int tickDelay        = 0;
    public int guiWaitTicks     = 0;
    public int sellRetryCount   = 0;
    public int reconnectRetryCount = 0;
    public int menuClickIndex   = 0;
    public int[] parsedMenuSlots = new int[0];

    // ---- 回服后指令 ----
    public boolean pendingPostCmd  = false;
    public int     postCmdTickDelay = 0;

    // ---- COOLDOWN 来源标记 ----
    public boolean cooldownFromSell = false;

    // ---- 防掉线时间戳 ----
    public long lastAntiAFKTime = 0;

    // ---- 闲置检测 ----
    public long    lastPickupTime = 0;
    public int     lastKnownCount = -1;
    public boolean idleAlerted    = false;
    private boolean salePausedNotified = false;

    // ================================================================
    //  §  子  系  统
    // ================================================================

    private final Statistics stats = new Statistics();
    private final LobbyDetector lobby;
    private final SellManager sell;
    private final ReconnectManager reconnect;
    private final HudRenderer hud;

    public Statistics getStats() { return stats; }

    // ================================================================
    //  §  构  造  函  数
    // ================================================================

    public CobbleSeller() {
        super(AddonTemplate.JERININADDONE, "圆石出售",
            "自动出售圆石 \uff0b 大厅检测回服 \uff0b 防掉线监控");

        lobby = new LobbyDetector(
            enableCoordinateCheck, lobbyX, lobbyZ, lobbyRadius,
            enableKeywordCheck, lobbyKeywords,
            enablePlayerCheck, survivalPlayers);

        sell = new SellManager(stats,
            sellCommand, sellThreshold, pricePerCobble, sellCooldown,
            sellMaxRetries, sellSlot, sellSubmitSlot);

        reconnect = new ReconnectManager(stats, lobby,
            enableReconnect, menuSlots, menuCommand, menuClickInterval,
            reconnectMaxRetries, enablePostCmd, postCmd, postCmdDelay,
            enableAntiAFK, antiAFKInterval);

        hud = new HudRenderer(enableReconnect, enablePostCmd);
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
        pendingPostCmd   = false;
        cooldownFromSell = false;
        lastAntiAFKTime  = System.currentTimeMillis();
        lastPickupTime   = System.currentTimeMillis();
        lastKnownCount   = -1;
        idleAlerted      = false;
        salePausedNotified = false;
        stats.startSession();
        lobby.cacheReset();
        hud.showStartupBanner(this);
    }

    @Override
    public void onDeactivate() {
        stats.addAccumulatedTime(System.currentTimeMillis() - stats.getTotalMs() + stats.getAccumulatedTime());
        // actually simpler: just compute directly
        long totalMin = stats.getTotalMin();
        long hours = totalMin / 60;
        long mins  = totalMin % 60;
        double moneyW = stats.getTotalMoney() / 10000.0;
        info("§c§l[系统] 模块已关闭 (防掉线监控停止)");
        info(String.format(
            "§e§l[统计] 累计挂机: §b§l%d §f§l小时 §b§l%d §f§l分钟 §7§l| §f§l累计收益: §6§l%.1fW",
            hours, mins, moneyW));
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

        // ---- 回服后指令 + 防掉线 (无论出售开关都运行) ----
        reconnect.tickPostCmd(this);
        reconnect.tickAntiAFK(this);

        // ---- 暂停出售模式 ----
        if (!selling) {
            if (!salePausedNotified) {
                salePausedNotified = true;
                hud.showPauseStatus(this);
            }
            reconnect.runReconnectOnly(this);
            return;
        }
        salePausedNotified = false;

        // ---- 冷却中 ----
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        // ---- 状态机 ----
        if (reconnect.handleState(this)) {
            if (state == State.IDLE && !lobby.isLobby()
                && count >= sellThreshold.get()
                && System.currentTimeMillis() - stats.getLastSellTime() > sellCooldown.get()) {
                sell.start(this, count);
            }
            return;
        }
        sell.handleState(this);
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

        // 闲置超时告警 (一次性)
        if (!idleAlerted && timeoutSec > 0) {
            long idleMs = System.currentTimeMillis() - lastPickupTime;
            if (idleMs >= timeoutSec * 1000L) {
                idleAlerted = true;
                warning("[!] 已 " + timeoutSec + " 秒无圆石产出，HUD 已收起");
            }
        }
    }

    // ================================================================
    //  §  圆  石  计  数  (SellManager 也需要)
    // ================================================================

    public int countCobblestone() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COBBLESTONE) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return count;
    }

    // ================================================================
    //  §  GUI 检  测  (ReconnectManager / SellManager 都需要)
    // ================================================================

    public boolean hasContainerGUI() {
        return mc.player != null
            && mc.player.currentScreenHandler != null
            && mc.player.currentScreenHandler.syncId != 0;
    }
}