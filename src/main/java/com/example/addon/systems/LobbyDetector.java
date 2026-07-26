package com.example.addon.systems;

import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 大厅检测器 — 三种独立检测方式: 坐标 / Tab 关键词 / 玩家名缺失
 */
public class LobbyDetector {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // ---- 缓存 ----
    private long    lastCheckTime = 0;
    private boolean cachedResult  = false;
    private String  cachedReason  = "";
    private static final long CACHE_MS = 500;

    // 玩家检测只在名单里的玩家曾被确认在线后，才将其全部缺失视为大厅。
    private final Set<String> observedSurvivalPlayers = new HashSet<>();
    private String playerConfig = "";

    // ---- 反射 ----
    private static Field headerField;
    private static Field footerField;
    private static boolean reflectionReady;
    private static boolean reflectionFailed;

    // ---- 设置引用 ----
    private final Setting<Boolean> enableCoordinateCheck;
    private final Setting<Integer> lobbyX;
    private final Setting<Integer> lobbyZ;
    private final Setting<Integer> lobbyRadius;
    private final Setting<Boolean> enableKeywordCheck;
    private final Setting<String>  lobbyKeywords;
    private final Setting<Boolean> enablePlayerCheck;
    private final Setting<String>  survivalPlayers;

    public LobbyDetector(
            Setting<Boolean> enableCoordinateCheck,
            Setting<Integer> lobbyX,
            Setting<Integer> lobbyZ,
            Setting<Integer> lobbyRadius,
            Setting<Boolean> enableKeywordCheck,
            Setting<String>  lobbyKeywords,
            Setting<Boolean> enablePlayerCheck,
            Setting<String>  survivalPlayers) {
        this.enableCoordinateCheck = enableCoordinateCheck;
        this.lobbyX = lobbyX;
        this.lobbyZ = lobbyZ;
        this.lobbyRadius = lobbyRadius;
        this.enableKeywordCheck = enableKeywordCheck;
        this.lobbyKeywords = lobbyKeywords;
        this.enablePlayerCheck = enablePlayerCheck;
        this.survivalPlayers = survivalPlayers;
    }

    // ---- 主检测入口 ----

    public boolean isLobby() {
        if (mc.player == null) return false;

        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CACHE_MS) {
            return cachedResult;
        }
        lastCheckTime = now;

        if (enableCoordinateCheck.get() && checkByCoordinate()) {
            cachedReason = "坐标";
            return cachedResult = true;
        }
        if (enableKeywordCheck.get() && checkByTabKeyword()) {
            cachedReason = "关键词";
            return cachedResult = true;
        }
        if (enablePlayerCheck.get() && checkByPlayerAbsence()) {
            cachedReason = "玩家名";
            return cachedResult = true;
        }
        cachedReason = "";
        return cachedResult = false;
    }

    public String getLastDetectionReason() {
        return cachedReason;
    }

    public void cacheReset() {
        lastCheckTime = 0;
        cachedResult  = false;
        cachedReason  = "";
    }

    // ---- 坐标检测 ----

    private boolean checkByCoordinate() {
        double dx = mc.player.getX() - lobbyX.get();
        double dz = mc.player.getZ() - lobbyZ.get();
        return Math.sqrt(dx * dx + dz * dz) <= lobbyRadius.get();
    }

    // ---- Tab 关键词检测 ----

    private boolean checkByTabKeyword() {
        String raw = lobbyKeywords.get().trim();
        if (raw.isEmpty() || mc.getNetworkHandler() == null || mc.world == null) return false;

        StringBuilder visibleText = new StringBuilder();

        ensureReflectionReady();
        if (reflectionReady) {
            try {
                Text header = (Text) headerField.get(mc.inGameHud.getPlayerListHud());
                Text footer = (Text) footerField.get(mc.inGameHud.getPlayerListHud());
                if (header != null) visibleText.append(header.getString()).append('\n');
                if (footer != null) visibleText.append(footer.getString()).append('\n');
            } catch (Exception ignored) {}
        }

        // 有些服务器把大厅标识放在 Tab 玩家显示名中。
        for (var entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getDisplayName() != null) {
                visibleText.append(entry.getDisplayName().getString()).append('\n');
            }
        }

        try {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (obj != null && obj.getDisplayName() != null) {
                visibleText.append(obj.getDisplayName().getString());
            }
        } catch (Exception ignored) {}

        String haystack = visibleText.toString().toLowerCase(Locale.ROOT);
        for (String keyword : raw.split(",")) {
            String normalized = keyword.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && haystack.contains(normalized)) return true;
        }
        return false;
    }

    // ---- 玩家名反向检测 ----

    private boolean checkByPlayerAbsence() {
        String raw = survivalPlayers.get().trim();
        if (raw.isEmpty()) return false;

        // 设置变化后重置已确认名单，防止旧配置影响新检测。
        if (!raw.equals(playerConfig)) {
            playerConfig = raw;
            observedSurvivalPlayers.clear();
        }

        var playerList = mc.getNetworkHandler().getPlayerList();
        if (playerList.isEmpty()) return false;

        // 使用账户真实名称，避免 Tab 称号或队伍前缀导致误判。
        Set<String> onlinePlayers = new HashSet<>();
        for (var entry : playerList) {
            String name = entry.getProfile().name();
            if (!name.isEmpty()) onlinePlayers.add(name.toLowerCase());
        }

        Set<String> configuredPlayers = new HashSet<>();
        for (String name : raw.split(",")) {
            String normalized = name.trim().toLowerCase();
            if (!normalized.isEmpty()) configuredPlayers.add(normalized);
        }
        if (configuredPlayers.isEmpty()) return false;

        // 先确认至少有一个配置里的账号实际出现在 Tab；未确认时绝不触发回服。
        for (String name : configuredPlayers) {
            if (onlinePlayers.contains(name)) observedSurvivalPlayers.add(name);
        }
        if (observedSurvivalPlayers.isEmpty()) return false;

        // 已确认过的生存服账号全部从 Tab 消失，才判定为进入大厅。
        for (String name : observedSurvivalPlayers) {
            if (onlinePlayers.contains(name)) return false;
        }
        return true;
    }

    // ---- 反射 (静态, 只初始化一次) ----

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
}