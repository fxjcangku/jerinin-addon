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

    /** 20 TPS，即 1 tick = 50ms */
    public static final int TICK_MS = 50;

    // ================================================================
    //  §  设  置  组
    // ================================================================

    private final SettingGroup sgSell         = settings.createGroup("自动出售");
    private final SettingGroup sgSellAdvanced = settings.createGroup("出售高级");
    private final SettingGroup sgReconnect    = settings.createGroup("自动回服");
    private final SettingGroup sgLobby        = settings.createGroup("大厅识别");
    private final SettingGroup sgExtra        = settings.createGroup("辅助功能");

    // ---- 自动出售：日常使用时只需调整这一组 ----
    public final Setting<Boolean> enableSelling = sgSell.add(new BoolSetting.Builder()
        .name("自动出售").description("达到设定数量后自动出售圆石；关闭后自动回服和防掉线仍可运行。").defaultValue(true).build());
    public final Setting<Integer> sellThreshold = sgSell.add(new IntSetting.Builder()
        .name("出售数量").description("背包中的圆石达到此数量时开始出售。").defaultValue(1700).min(1).sliderMax(2304).visible(enableSelling::get).build());
    final Setting<String> sellCommand = sgSell.add(new StringSetting.Builder()
        .name("出售指令").description("打开出售界面的指令，不需要输入斜杠。").defaultValue("sell").visible(enableSelling::get).build());
    final Setting<Integer> sellCooldown = sgSell.add(new IntSetting.Builder()
        .name("出售间隔").description("两次出售之间的最小间隔，单位为毫秒。").defaultValue(1500).min(100).sliderMax(5000).visible(enableSelling::get).build());

    // ---- 出售高级：服务器 GUI 规则不变时无需修改 ----
    final Setting<Integer> sellSlot = sgSellAdvanced.add(new IntSetting.Builder()
        .name("出售槽位").description("出售界面中“出售”按钮的槽位编号。").defaultValue(49).min(0).max(53).visible(enableSelling::get).build());
    final Setting<Integer> sellSubmitSlot = sgSellAdvanced.add(new IntSetting.Builder()
        .name("确认槽位").description("出售界面中“确认”按钮的槽位编号。").defaultValue(50).min(0).max(53).visible(enableSelling::get).build());
    final Setting<Integer> sellMaxRetries = sgSellAdvanced.add(new IntSetting.Builder()
        .name("失败重试").description("一次出售失败后允许重新尝试的最大次数。").defaultValue(5).min(1).sliderMax(20).visible(enableSelling::get).build());

    // ---- 自动回服 ----
    final Setting<Boolean> enableReconnect = sgReconnect.add(new BoolSetting.Builder()
        .name("自动回服").description("检测到玩家位于大厅后，自动返回生存服。").defaultValue(true).build());
    final Setting<String> menuCommand = sgReconnect.add(new StringSetting.Builder()
        .name("菜单指令").description("打开服务器菜单的指令，不需要输入斜杠。").defaultValue("menu").visible(enableReconnect::get).build());
    final Setting<String> menuSlots = sgReconnect.add(new StringSetting.Builder()
        .name("点击槽位").description("返回生存服时依次点击的槽位，多个槽位用英文逗号分隔，例如 13,22。").defaultValue("13").visible(enableReconnect::get).build());
    final Setting<Integer> menuClickInterval = sgReconnect.add(new IntSetting.Builder()
        .name("点击间隔").description("连续点击多个菜单槽位时的间隔，单位为毫秒。").defaultValue(500).min(100).sliderMax(2000).visible(enableReconnect::get).build());
    final Setting<Integer> reconnectMaxRetries = sgReconnect.add(new IntSetting.Builder()
        .name("回服重试").description("返回生存服失败后的最大重试次数。").defaultValue(5).min(1).sliderMax(20).visible(enableReconnect::get).build());

    // ---- 大厅识别：每种识别方式独立开关 ----
    final Setting<Boolean> enableCoordinateCheck = sgLobby.add(new BoolSetting.Builder()
        .name("坐标识别").description("根据玩家是否进入大厅坐标范围判断掉服。").defaultValue(true).visible(enableReconnect::get).build());
    final Setting<Integer> lobbyX = sgLobby.add(new IntSetting.Builder()
        .name("大厅 X 坐标").description("大厅中心位置的 X 坐标。").defaultValue(45).visible(() -> enableReconnect.get() && enableCoordinateCheck.get()).build());
    final Setting<Integer> lobbyZ = sgLobby.add(new IntSetting.Builder()
        .name("大厅 Z 坐标").description("大厅中心位置的 Z 坐标。").defaultValue(68).visible(() -> enableReconnect.get() && enableCoordinateCheck.get()).build());
    final Setting<Integer> lobbyRadius = sgLobby.add(new IntSetting.Builder()
        .name("坐标检测半径").description("以大厅坐标为中心的检测范围。").defaultValue(10).min(1).sliderMax(50).visible(() -> enableReconnect.get() && enableCoordinateCheck.get()).build());
    final Setting<Boolean> enableKeywordCheck = sgLobby.add(new BoolSetting.Builder()
        .name("关键词识别").description("通过 Tab 玩家列表中的大厅关键词辅助判断。").defaultValue(false).visible(enableReconnect::get).build());
    final Setting<String> lobbyKeywords = sgLobby.add(new StringSetting.Builder()
        .name("大厅关键词").description("多个关键词使用英文逗号分隔。").defaultValue("大厅,大廳,Lobby,lobby,Rubik SMP").visible(() -> enableReconnect.get() && enableKeywordCheck.get()).build());
    final Setting<Boolean> enablePlayerCheck = sgLobby.add(new BoolSetting.Builder()
        .name("玩家识别").description("已确认在线的生存服玩家全部从 Tab 消失时判定大厅。").defaultValue(false).visible(enableReconnect::get).build());
    final Setting<String> survivalPlayers = sgLobby.add(new StringSetting.Builder()
        .name("生存服玩家").description("填写真实玩家名并用英文逗号分隔；不存在的名字不会触发。").defaultValue("").visible(() -> enableReconnect.get() && enablePlayerCheck.get()).build());

    // ---- 辅助功能 ----
    final Setting<Boolean> enablePostCmd = sgExtra.add(new BoolSetting.Builder()
        .name("回服后执行指令").description("成功返回生存服后自动执行一条指令。").defaultValue(false).visible(enableReconnect::get).build());
    final Setting<String> postCmd = sgExtra.add(new StringSetting.Builder()
        .name("回服后指令").description("回服后执行的指令，不需要输入斜杠。").defaultValue("home zr").visible(() -> enableReconnect.get() && enablePostCmd.get()).build());
    final Setting<Integer> postCmdDelay = sgExtra.add(new IntSetting.Builder()
        .name("执行延迟").description("回服后等待地形加载再执行指令，单位为毫秒。").defaultValue(2500).min(0).sliderMax(10000).visible(() -> enableReconnect.get() && enablePostCmd.get()).build());
    final Setting<Boolean> enableAntiAFK = sgExtra.add(new BoolSetting.Builder()
        .name("防掉线").description("定期自动跳跃，避免因长时间无操作被服务器踢出。").defaultValue(false).build());
    final Setting<Integer> antiAFKInterval = sgExtra.add(new IntSetting.Builder()
        .name("防掉线间隔").description("每隔多少秒执行一次防掉线动作。").defaultValue(30).min(5).sliderMax(300).visible(enableAntiAFK::get).build());

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

        // 大厅检测优先于出售冷却；坐标命中后立即进入回服流程。
        if (reconnect.tryStartReconnect(this)) return;

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
