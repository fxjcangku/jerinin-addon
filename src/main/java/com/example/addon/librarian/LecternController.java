package com.example.addon.librarian;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * 负责讲台的破坏与重新放置。
 * 破坏讲台可让图书管理员失去职业，重新放置后会重新随机交易。
 */
public class LecternController {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** 判断指定坐标是否为讲台方块。 */
    public static boolean isLectern(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getBlockState(pos).isOf(Blocks.LECTERN);
    }

    /**
     * 破坏讲台。
     *
     * @param pos 讲台坐标
     * @return 是否成功发起破坏
     */
    public boolean breakLectern(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        if (!isLectern(pos)) return false;
        return BlockUtils.breakBlock(pos, true);
    }

    /**
     * 在指定坐标放置讲台。背包中需存在讲台方块。
     *
     * @param pos 目标坐标
     * @return 是否成功发起放置
     */
    public boolean placeLectern(BlockPos pos) {
        if (mc.world == null || pos == null) return false;

        FindItemResult lectern = InvUtils.find(Items.LECTERN);
        if (!lectern.found()) return false;

        // 参数: 坐标, 物品, 旋转朝向, 旋转优先级, 挥手, 换回原槽位
        return BlockUtils.place(pos, lectern, true, 50, true, true);
    }

    /** 背包中是否还有讲台可放置。 */
    public boolean hasLectern() {
        return InvUtils.find(Items.LECTERN).found();
    }
}
