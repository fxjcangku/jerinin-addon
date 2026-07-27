package com.example.addon.librarian;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 负责在世界中扫描图书管理员村民，并定位其附近的讲台方块。
 * 仅做只读的世界查询，不产生任何交互动作。
 */
public class VillagerScanner {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * 在指定范围内寻找距离玩家最近的图书管理员村民。
     * 已绑定的村民（UUID 在 excludeUUIDs 中）会被跳过。
     *
     * @param range        以玩家为中心的搜索半径（格）
     * @param excludeUUIDs 要排除的村民 UUID 集合（已绑定/已处理过的）
     * @return 最近的符合条件的图书管理员，找不到时返回 null
     */
    public VillagerEntity findNearestLibrarian(double range, Set<UUID> excludeUUIDs) {
        if (mc.player == null || mc.world == null) return null;

        Box box = mc.player.getBoundingBox().expand(range);
        List<VillagerEntity> villagers =
            mc.world.getEntitiesByClass(VillagerEntity.class, box, e -> true);

        VillagerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (VillagerEntity villager : villagers) {
            if (!villager.isAlive()) continue;
            if (!isLibrarian(villager)) continue;
            if (excludeUUIDs != null && excludeUUIDs.contains(villager.getUuid())) continue;

            double dist = mc.player.squaredDistanceTo(villager);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = villager;
            }
        }
        return nearest;
    }

    /** 不排除任何村民的便捷重载。 */
    public VillagerEntity findNearestLibrarian(double range) {
        return findNearestLibrarian(range, null);
    }

    /** 判断村民当前是否为图书管理员。 */
    public boolean isLibrarian(VillagerEntity villager) {
        VillagerData data = villager.getVillagerData();
        if (data == null) return false;
        RegistryEntry<VillagerProfession> profession = data.profession();
        return profession != null && profession.matchesKey(VillagerProfession.LIBRARIAN);
    }

    /** 判断村民当前是否已失去职业（无业或傻子）。 */
    public boolean isJobless(VillagerEntity villager) {
        VillagerData data = villager.getVillagerData();
        if (data == null) return true;
        RegistryEntry<VillagerProfession> profession = data.profession();
        if (profession == null) return true;
        return profession.matchesKey(VillagerProfession.NONE)
            || profession.matchesKey(VillagerProfession.NITWIT);
    }

    /**
     * 判断村民是否为真正的无业村民（不是傻子），可以接受职业。
     * 傻子（Nitwit）外表无职业但永远无法接受工作站，不能用来刷书。
     */
    public boolean isUnemployed(VillagerEntity villager) {
        VillagerData data = villager.getVillagerData();
        if (data == null) return false;
        RegistryEntry<VillagerProfession> profession = data.profession();
        if (profession == null) return false;
        // 只有 NONE 才能接受讲台职业，Nitwit 永远无法上职业
        return profession.matchesKey(VillagerProfession.NONE);
    }

    /**
     * 在范围内寻找最近的傻子村民（用于检测并播报）。
     *
     * @param range 搜索半径
     * @return 最近的傻子村民，找不到返回 null
     */
    public VillagerEntity findNearestNitwit(double range, Set<UUID> excludeUUIDs) {
        if (mc.player == null || mc.world == null) return null;
        Box box = mc.player.getBoundingBox().expand(range);
        List<VillagerEntity> villagers =
            mc.world.getEntitiesByClass(VillagerEntity.class, box, e -> true);
        VillagerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (VillagerEntity villager : villagers) {
            if (!villager.isAlive()) continue;
            if (excludeUUIDs != null && excludeUUIDs.contains(villager.getUuid())) continue;
            VillagerData data = villager.getVillagerData();
            if (data == null) continue;
            RegistryEntry<VillagerProfession> profession = data.profession();
            if (profession == null) continue;
            if (!profession.matchesKey(VillagerProfession.NITWIT)) continue;
            double dist = mc.player.squaredDistanceTo(villager);
            if (dist < nearestDist) { nearestDist = dist; nearest = villager; }
        }
        return nearest;
    }

    /**
     * 在范围内寻找最近的可用无业村民（排除傻子和已排除的 UUID）。
     *
     * @param range        搜索半径
     * @param excludeUUIDs 排除的村民 UUID（已处理过的）
     * @return 最近的无业村民，找不到返回 null
     */
    public VillagerEntity findNearestUnemployed(double range, Set<UUID> excludeUUIDs) {
        if (mc.player == null || mc.world == null) return null;

        Box box = mc.player.getBoundingBox().expand(range);
        List<VillagerEntity> villagers =
            mc.world.getEntitiesByClass(VillagerEntity.class, box, e -> true);

        VillagerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (VillagerEntity villager : villagers) {
            if (!villager.isAlive()) continue;
            if (!isUnemployed(villager)) continue;
            if (excludeUUIDs != null && excludeUUIDs.contains(villager.getUuid())) continue;

            double dist = mc.player.squaredDistanceTo(villager);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = villager;
            }
        }
        return nearest;
    }

    /**
     * 在村民脚下及周围一圈范围内寻找讲台方块。
     * 讲台是图书管理员的工作站，通常紧邻村民。
     *
     * @param villager 目标村民
     * @return 讲台坐标，找不到返回 null
     */
    public BlockPos findLecternPos(VillagerEntity villager) {
        if (mc.world == null) return null;

        BlockPos base = villager.getBlockPos();
        // 在村民周围 2 格水平、上下 1 格范围内搜索讲台
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = base.add(dx, dy, dz);
                    if (LecternController.isLectern(pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
