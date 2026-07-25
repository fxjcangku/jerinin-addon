package com.example.addon.systems;

import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

/**
 * 大厅检测器 — 三种独立检测方式: 坐标 / Tab 关键词 / 玩家名缺失
 */
public class LobbyDetector {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // ---- 缓存 ----
    private long    lastCheckTime = 0;
    private boolean cachedResult  = false;
    private static final long CACHE_MS = 500;

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
            return cachedResult = true;
        }
        if (enableKeywordCheck.get() && checkByTabKeyword()) {
            return cachedResult = true;
        }
        if (enablePlayerCheck.get() && checkByPlayerAbsence()) {
            return cachedResult = true;
        }
        return cachedResult = false;
    }

    public void cacheReset() {
        lastCheckTime = 0;
        cachedResult  = false;
    }

    // ---- 坐标检测 ----

    private boolean checkByCoordinate() {
        double dx = mc.player.getX() - lobbyX.get();
        double dz = mc.player.getZ() - lobbyZ.get();
        return Math.sqrt(dx * dx + dz * dz) <= lobbyRadius.get();
    }

    // ---- Tab 关键词检测 ----

    private boolean checkByTabKeyword() {
        ensureReflectionReady();
        String raw = lobbyKeywords.get().trim();
        if (raw.isEmpty()) return false;
        String[] keywords = raw.split(",");

        for (String kw : keywords) {
            String k = kw.trim();
            if (k.isEmpty()) continue;

            try {
                Text header = (Text) headerField.get(mc.inGameHud.getPlayerListHud());
                Text footer = (Text) footerField.get(mc.inGameHud.getPlayerListHud());
                if ((header != null && header.getString().contains(k))
                 || (footer != null && footer.getString().contains(k))) return true;
            } catch (Exception ignored) {}

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

    // ---- 玩家名反向检测 ----

    private boolean checkByPlayerAbsence() {
        String raw = survivalPlayers.get().trim();
        if (raw.isEmpty()) return false;

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
                        return false;
                    }
                }
            } catch (Exception ignored) {}
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