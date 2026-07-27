package com.example.addon.librarian;

import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**
 * 封装 Baritone 自动寻路操作。
 * 通过 Meteor 内置的 PathManagers 调用，不直接依赖裸 Baritone API。
 */
public class BaritoneWalker {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** Baritone 是否在当前客户端中可用（Meteor 已内置，通常为 true）。 */
    public static boolean isAvailable() {
        return BaritoneUtils.IS_AVAILABLE;
    }

    /** 当前 Baritone 是否正在寻路（被本模块或其他功能占用）。 */
    public boolean isPathing() {
        if (!isAvailable()) return false;
        return PathManagers.get().isPathing();
    }

    /**
     * 开始向目标坐标寻路。
     *
     * @param pos 目标 BlockPos
     */
    public void walkTo(BlockPos pos) {
        if (!isAvailable()) return;
        PathManagers.get().moveTo(pos, false);
    }

    /** 停止寻路。 */
    public void stop() {
        if (!isAvailable()) return;
        PathManagers.get().stop();
    }

    /**
     * 判断玩家是否已足够靠近目标实体（可以进行交互）。
     *
     * @param target   目标实体
     * @param maxDist  最大距离（格），通常用 4.0
     * @return 是否已足够近
     */
    public boolean isNearEntity(Entity target, double maxDist) {
        if (mc.player == null) return false;
        return mc.player.squaredDistanceTo(target) <= maxDist * maxDist;
    }
}
