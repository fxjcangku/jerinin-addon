package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TpMace extends Module {
    private static Field selectedSlotField;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAttack = settings.createGroup("攻击设置");
    private final SettingGroup sgTotem = settings.createGroup("图腾绕过");
    private final SettingGroup sgTarget = settings.createGroup("目标筛选");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("攻击范围").description("搜索攻击目标的最大距离。")
        .defaultValue(20).min(1).sliderRange(1, 128).build());

    private final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("攻击间隔").description("两轮攻击之间的等待时间，单位为游戏刻。")
        .defaultValue(10).min(0).sliderMax(40).build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换重锤").description("攻击前自动切换到快捷栏中的重锤。")
        .defaultValue(true).build());

    private final Setting<Boolean> silentSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("静默切换").description("仅通知服务器切换重锤，客户端画面不切换槽位。")
        .defaultValue(false).visible(autoSwitch::get).build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("面向目标").description("攻击前向服务器发送面向目标的视角。")
        .defaultValue(true).build());

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("显示挥手").description("攻击时在客户端显示主手挥动动画。")
        .defaultValue(false).build());

    private final Setting<Boolean> returnPos = sgGeneral.add(new BoolSetting.Builder()
        .name("返回原位").description("攻击后使用分段移动包返回攻击前的位置。")
        .defaultValue(true).build());

    private final Setting<Double> moveDistance = sgAttack.add(new DoubleSetting.Builder()
        .name("移动包步长").description("每个移动包允许移动的最大三维距离。")
        .defaultValue(8).min(1).sliderRange(1, 32).build());

    private final Setting<Boolean> maxPower = sgAttack.add(new BoolSetting.Builder()
        .name("最大化伤害").description("优先使用最高 170 格的可用下落高度。")
        .defaultValue(true).build());

    private final Setting<Integer> fallHeight = sgAttack.add(new IntSetting.Builder()
        .name("攻击高度").description("关闭最大化伤害后使用的下落高度。")
        .defaultValue(30).min(1).sliderRange(1, 170)
        .visible(() -> !maxPower.get()).build());

    private final Setting<Boolean> airCheck = sgAttack.add(new BoolSetting.Builder()
        .name("空气检测").description("检查目标上方的移动路径，找不到安全高度时跳过目标。")
        .defaultValue(true).build());

    private final Setting<Boolean> totemBypass = sgTotem.add(new BoolSetting.Builder()
        .name("图腾绕过").description("先用较低高度攻击多次，再进行完整高度攻击。")
        .defaultValue(false).build());

    private final Setting<Integer> totemAttacks = sgTotem.add(new IntSetting.Builder()
        .name("前置攻击次数").description("完整高度攻击前执行的低高度攻击次数。")
        .defaultValue(3).min(1).sliderMax(10).visible(totemBypass::get).build());

    private final Setting<Integer> totemHeight = sgTotem.add(new IntSetting.Builder()
        .name("前置攻击高度").description("消耗图腾时使用的下落高度。")
        .defaultValue(4).min(1).sliderMax(20).visible(totemBypass::get).build());

    private enum ListMode { 关闭, 白名单, 黑名单 }

    private final Setting<Boolean> players = sgTarget.add(new BoolSetting.Builder()
        .name("攻击玩家").description("允许选择玩家作为目标。")
        .defaultValue(true).build());

    private final Setting<Boolean> entities = sgTarget.add(new BoolSetting.Builder()
        .name("攻击生物").description("允许选择非玩家生物作为目标。")
        .defaultValue(false).build());

    private final Setting<Boolean> throughWalls = sgTarget.add(new BoolSetting.Builder()
        .name("穿墙选择").description("允许选择视线被方块遮挡的目标。")
        .defaultValue(false).build());

    private final Setting<Boolean> ignoreNamed = sgTarget.add(new BoolSetting.Builder()
        .name("忽略命名生物").description("不攻击带有自定义名称的生物。")
        .defaultValue(true).build());

    private final Setting<ListMode> listMode = sgTarget.add(new EnumSetting.Builder<ListMode>()
        .name("名单模式").description("对玩家名称使用额外的白名单或黑名单筛选。")
        .defaultValue(ListMode.关闭).build());

    private final Setting<String> playerList = sgTarget.add(new StringSetting.Builder()
        .name("玩家名单").description("多个玩家名使用英文逗号分隔。")
        .defaultValue("").visible(() -> listMode.get() != ListMode.关闭).build());

    private enum Phase { IDLE, DELAY, RETURN_DELAY }

    private Phase phase = Phase.IDLE;
    private int delayTicks;
    private int attackCount;
    private Vec3d originalPos;
    private Vec3d lastPacketPos;
    private LivingEntity target;
    private int originalSlot = -1;
    private int maceSlot = -1;
    private boolean swapped;

    public TpMace() {
        super(AddonTemplate.JERININADDONE, "传送重锤", "传送到目标上方模拟下落，并使用重锤发动攻击。");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        swapBack();
        resetState();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        switch (phase) {
            case IDLE -> tickIdle();
            case DELAY -> tickDelay();
            case RETURN_DELAY -> tickReturnDelay();
        }
    }

    private void tickIdle() {
        if (++delayTicks < attackDelay.get()) return;
        delayTicks = 0;

        target = findTarget();
        if (target == null || !prepareWeapon()) return;

        originalPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        lastPacketPos = originalPos;
        attackCount = 0;

        if (totemBypass.get() && target instanceof PlayerEntity) {
            doAttack(totemHeight.get(), false);
        } else {
            int height = getAttackHeight();
            if (height <= 0) {
                warning("[传送重锤] 目标上方没有可用的攻击空间，已跳过。");
                swapBack();
                resetState();
                return;
            }
            doAttack(height, true);
        }
    }

    private void tickDelay() {
        if (++delayTicks < 3) return;
        delayTicks = 0;

        if (!isTargetValid(target) || !ensureMaceSelected()) {
            swapBack();
            resetState();
            return;
        }

        attackCount++;
        if (attackCount >= totemAttacks.get()) {
            int height = getAttackHeight();
            if (height <= 0) {
                warning("[传送重锤] 目标上方没有可用的最终攻击空间，已停止。");
                swapBack();
                resetState();
                return;
            }
            doAttack(height, true);
        } else {
            doAttack(totemHeight.get(), false);
        }
    }

    private void tickReturnDelay() {
        if (++delayTicks >= 2) resetState();
    }

    private void doAttack(int height, boolean finalAttack) {
        if (!isTargetValid(target)) {
            swapBack();
            resetState();
            return;
        }

        Vec3d top = new Vec3d(target.getX(), target.getY() + height, target.getZ());
        Vec3d landing = new Vec3d(target.getX(), target.getY() + 1.1, target.getZ());

        if (rotate.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                getYawTo(target), getPitchTo(target), mc.player.isOnGround(), false));
        }

        sendSegmented(lastPacketPos, top, false);
        sendSegmented(top, landing, false);
        lastPacketPos = landing;
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);

        if (finalAttack) finishAttack();
        else phase = Phase.DELAY;
    }

    private void finishAttack() {
        swapBack();
        if (returnPos.get() && originalPos != null && lastPacketPos != null) {
            sendSegmented(lastPacketPos, originalPos, mc.player.isOnGround());
            lastPacketPos = originalPos;
            delayTicks = 0;
            phase = Phase.RETURN_DELAY;
        } else {
            resetState();
        }
    }

    private void sendSegmented(Vec3d from, Vec3d to, boolean finalOnGround) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance / moveDistance.get()));
        for (int i = 1; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3d pos = from.lerp(to, progress);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z, finalOnGround && i == steps, false));
        }
    }

    private boolean prepareWeapon() {
        originalSlot = getSelectedSlot();
        maceSlot = -1;
        swapped = false;

        if (!autoSwitch.get()) {
            if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
                warning("[传送重锤] 主手没有重锤，已跳过攻击。");
                return false;
            }
            return true;
        }

        FindItemResult mace = InvUtils.findInHotbar(Items.MACE);
        if (!mace.found()) {
            warning("[传送重锤] 快捷栏中没有重锤。");
            return false;
        }

        maceSlot = mace.slot();
        if (maceSlot == originalSlot) return true;

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        if (!silentSwap.get()) setSelectedSlot(mc.player.getInventory(), maceSlot);
        swapped = true;
        return true;
    }

    private boolean ensureMaceSelected() {
        if (!autoSwitch.get()) return mc.player.getMainHandStack().isOf(Items.MACE);
        if (maceSlot < 0 || !mc.player.getInventory().getStack(maceSlot).isOf(Items.MACE)) return false;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        if (!silentSwap.get()) setSelectedSlot(mc.player.getInventory(), maceSlot);
        return true;
    }

    private void swapBack() {
        if (!swapped || originalSlot < 0 || mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        if (!silentSwap.get()) setSelectedSlot(mc.player.getInventory(), originalSlot);
        swapped = false;
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !entityCheck(living)) continue;
            double distance = mc.player.squaredDistanceTo(living);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    private boolean isTargetValid(LivingEntity entity) {
        return entity != null && !entity.isRemoved() && entity.isAlive() && entityCheck(entity);
    }

    private boolean entityCheck(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive() || entity.isRemoved()) return false;
        if (mc.player.distanceTo(entity) > range.get()) return false;
        if (!throughWalls.get() && !mc.player.canSee(entity)) return false;

        if (entity instanceof PlayerEntity player) {
            if (!players.get() || player.isSpectator()) return false;
            if (Friends.get().isFriend(player) || !Friends.get().shouldAttack(player)) return false;

            List<String> list = parsePlayerList();
            String name = player.getName().getString();
            if (listMode.get() == ListMode.白名单 && !list.contains(name)) return false;
            if (listMode.get() == ListMode.黑名单 && list.contains(name)) return false;
        } else if (!entities.get()) {
            return false;
        }

        return !ignoreNamed.get() || !entity.hasCustomName();
    }

    private List<String> parsePlayerList() {
        List<String> result = new ArrayList<>();
        for (String value : playerList.get().split(",")) {
            String name = value.trim();
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }

    private int getAttackHeight() {
        int requested = maxPower.get() ? 170 : fallHeight.get();
        int worldLimit = mc.world.getTopYInclusive() - target.getBlockY() - 1;
        int maximum = Math.min(requested, worldLimit);
        if (maximum <= 0) return 0;
        if (!airCheck.get()) return maximum;

        for (int height = maximum; height >= 1; height--) {
            if (isVerticalPathClear(target.getBlockPos(), height)) return height;
        }
        return 0;
    }

    private boolean isVerticalPathClear(BlockPos targetPos, int height) {
        for (int offset = 1; offset <= height + 1; offset++) {
            BlockPos pos = targetPos.up(offset);
            if (!mc.world.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    private float getYawTo(Entity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private float getPitchTo(Entity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dy = entity.getEyeY() - mc.player.getEyeY();
        double dz = entity.getZ() - mc.player.getZ();
        return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private int getSelectedSlot() {
        try {
            if (selectedSlotField == null) {
                selectedSlotField = PlayerInventory.class.getDeclaredField("selectedSlot");
                selectedSlotField.setAccessible(true);
            }
            return selectedSlotField.getInt(mc.player.getInventory());
        } catch (ReflectiveOperationException exception) {
            error("[传送重锤] 无法读取当前快捷栏槽位。");
            return -1;
        }
    }

    private void setSelectedSlot(PlayerInventory inventory, int slot) {
        try {
            if (selectedSlotField == null) {
                selectedSlotField = PlayerInventory.class.getDeclaredField("selectedSlot");
                selectedSlotField.setAccessible(true);
            }
            selectedSlotField.setInt(inventory, slot);
        } catch (ReflectiveOperationException exception) {
            error("[传送重锤] 无法同步客户端快捷栏槽位。");
        }
    }

    private void resetState() {
        phase = Phase.IDLE;
        delayTicks = 0;
        attackCount = 0;
        target = null;
        originalPos = null;
        lastPacketPos = null;
        originalSlot = -1;
        maceSlot = -1;
        swapped = false;
    }

    @Override
    public String getInfoString() {
        return target == null ? "等待目标" : target.getName().getString();
    }
}
