package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.systems.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CobbleSeller extends Module {

    /** 20 TPS → 1 tick = 50ms */
    public static final int TICK_MS = 50;

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
    final Setting<Boolean> enableAntiAFK = sgHUD.add(new BoolSetting.Builder()
        .name("防掉线").description("定期自动跳跃防止被服务器踢出").defaultValue(false).build());
    final Setting<Integer> antiAFKInterval = sgHUD.add(new IntSetting.Builder()
        .name("防掉线间隔(秒)").description("多久执行一次防掉线动作").defaultValue(30).min(5).sliderMax(300).build());

    // ================================================================
    //  §  状  态  机  (出售/回服 各自冷却独立)
    // ================================================================

    public enum State {
        IDLE, RECONNECT_WAIT_GUI, RECONNECT_CLICK_DELAY,
        SELL_WAIT_GUI, RETRY_DELAY, SELL_COOLDOWN, RECONNECT_COOLDOWN
    }

    // ================================================================
    //  §  运  行  时  状  态  (私有, 通过 getter/setter 访问)
    // ================================================================

    private State state = State.IDLE;
    private int tickDelay        = 0;
    private int guiWaitTicks     = 0;
    private int sellRetryCount   = 0;
    private int reconnectRetryCount = 0;
    private int menuClickIndex   = 0;
    private int[] parsedMenuSlots = new int[0];
    private boolean pendingPostCmd  = false;
    private int     postCmdTickDelay = 0;
    private long lastAntiAFKTime = 0;
    private boolean salePausedNotified = false;

    // ================================================================
    //  §  Getter / Setter
    // ================================================================

    public State getState() { return state; }
    public void setState(State s) { this.state = s; }

    public int getTickDelay() { return tickDelay; }
    public void setTickDelay(int d) { this.tickDelay = d; }

    public int getGuiWaitTicks() { return guiWaitTicks; }
    public void setGuiWaitTicks(int t) { this.guiWaitTicks = t; }

    public int getSellRetryCount() { return sellRetryCount; }
    public void setSellRetryCount(int c) { this.sellRetryCount = c; }

    public int getReconnectRetryCount() { return reconnectRetryCount; }
    public void setReconnectRetryCount(int c) { this.reconnectRetryCount = c; }

    public int getMenuClickIndex() { return menuClickIndex; }
    public void setMenuClickIndex(int i) { this.menuClickIndex = i; }

    public int[] getParsedMenuSlots() { return parsedMenuSlots; }
    public void setParsedMenuSlots(int[] s) { this.parsedMenuSlots = s; }

    public boolean isPendingPostCmd() { return pendingPostCmd; }
    public void setPendingPostCmd(boolean b) { this.pendingPostCmd = b; }

    public int getPostCmdTickDelay() { return postCmdTickDelay; }
    public void setPostCmdTickDelay(int d) { this.postCmdTickDelay = d; }

    public long getLastAntiAFKTime() { return lastAntiAFKTime; }
    public void setLastAntiAFKTime(long t) { this.lastAntiAFKTime = t; }

    // ================================================================
    //  §  子  系  统
    // ================================================================

    private final Statistics stats = new Statistics();
    private final LobbyDetector lobby;
    private final SellManager sell;
    private final ReconnectManager reconnect;
    private final HudRenderer hud;

    // ================================================================
    //  §  构  造  函  数
    // ================================================================

    public CobbleSeller() {
        super(AddonTemplate.JERININADDONE, "圆石出售",
            "自动出售圆石 ＋ 大厅检测回服 ＋ 防掉线监控");

        lobby = new LobbyDetector(
            enableCoordinateCheck, lobbyX, lobbyZ, lobbyRadius,
            enableKeywordCheck, lobbyKeywords,
            enablePlayerCheck, survivalPlayers);

        sell = new SellManager(stats,
            sellCommand, sellThreshold, sellCooldown,
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
        lastAntiAFKTime  = System.currentTimeMillis();
        salePausedNotified = false;
        lobby.cacheReset();
        ensureSingleHud();
        hud.showStartupBanner(this);
    }

    @Override
    public void onDeactivate() {
        info("§c§l[系统] 模块已关闭");
    }

    // ================================================================
    //  §  主  循  环  (TickEvent)
    // ================================================================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        boolean selling = enableSelling.get();
        int count = countCobblestone();

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

    private void ensureSingleHud() {
        boolean found = false;
        List<HudElement> duplicates = new ArrayList<>();

        for (HudElement element : Hud.get()) {
            if (!element.info.name.equals(CobbleSellerHud.INFO.name)) continue;
            if (found) duplicates.add(element);
            else found = true;
        }

        if (!found) Hud.get().add(CobbleSellerHud.INFO, 4, 4);
        for (HudElement duplicate : duplicates) duplicate.remove();
    }

    // ================================================================
    //  §  圆  石  计  数
    // ================================================================

    public int countCobblestone() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COBBLESTONE) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return count;
    }

    // ================================================================
    //  §  GUI 检  测
    // ================================================================

    public boolean hasContainerGUI() {
        return mc.player != null
            && mc.player.currentScreenHandler != null
            && mc.player.currentScreenHandler.syncId != 0;
    }
}
