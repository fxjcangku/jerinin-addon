package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.librarian.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradedItem;
import net.minecraft.village.TradeOffer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 刷图书管理员附魔书模块
 * 
 * 【功能介绍】
 * 自动寻找无业村民，放置讲台让其成为图书管理员，循环刷新交易直到找到目标附魔书。
 * 
 * 【工作流程】
 * 1. 扫描附近无业村民（自动排除傻子）
 * 2. 在村民旁放置讲台，等待其获得图书管理员职业
 * 3. 打开交易界面，检查是否有目标附魔书
 * 4. 若未找到，破坏讲台，等村民失业，重新放讲台并继续刷新
 * 5. 若找到目标，自动购买绑定，排除该村民并寻找下一个无业村民
 * 6. 循环执行直到所有目标附魔都找到
 * 
 * 【两种寻路模式】
 * • 手动模式（默认）：需要玩家自己走到村民附近（4格内）
 * • 自动寻路模式：开启后使用 Baritone 自动走到村民旁边（需安装 Baritone）
 * 
 * 【使用提示】
 * • 背包需准备充足的讲台（每个村民需一个）
 * • 准备足够的绿宝石用于自动购买绑定
 * • 傻子村民（绿色条纹）无法接受职业，会自动跳过
 * • 村民需在自然村落内且有床位绑定才能正常上职业
 */
public class LibrarianRoller extends Module {

    // ================================================================
    //  设置组
    // ================================================================

    private final SettingGroup sgTarget  = settings.createGroup("核心目标");
    private final SettingGroup sgPathing = settings.createGroup("村民与寻路");
    private final SettingGroup sgTiming  = settings.createGroup("刷新控制");
    private final SettingGroup sgExtra   = settings.createGroup("其他设置");

    // ================================================================
    //  目标设置 - 最重要的配置项
    // ================================================================

    public final Setting<Set<RegistryKey<Enchantment>>> targetEnchants = sgTarget.add(
        new com.example.addon.settings.EnchantedBookListSetting.Builder()
            .name("目标附魔")
            .description(
                "选择要寻找的附魔（均为最高等级）。\n" +
                "找到一个后自动继续寻找下一个。\n\n" +
                "已包含所有村民可交易的附魔。\n" +
                "以下附魔村民不会出售，已排除：\n" +
                "· 灵魂疾速 · 迅捷潜行 · 疾风迸发\n" +
                "· 绑定诅咒 · 消失诅咒（诅咒类排除）"
            )
            .defaultValue(new java.util.HashSet<>())  // 默认空列表，用户自己勾选
            .build()
    );

    public final Setting<Integer> maxPrice = sgTarget.add(
        new IntSetting.Builder()
            .name("价格上限")
            .description("绿宝石价格上限。\n超过此价格的交易将被跳过，继续刷新。")
            .defaultValue(26).range(1, 64).sliderRange(1, 64).build()
    );

    public final Setting<Boolean> autoBuy = sgTarget.add(
        new BoolSetting.Builder()
            .name("自动交易绑定")
            .description("找到目标附魔后自动购买一次，锁定该村民的交易。\n绿宝石不足时会停止模块并提示。\n关闭此项则只提示不购买。")
            .defaultValue(true).build()
    );

    public final Setting<Boolean> removeOnFind = sgTarget.add(
        new BoolSetting.Builder()
            .name("找到后移除")
            .description("从设置中永久移除已找到的附魔。\n避免下次启动模块时重复搜寻。\n关闭则每次启动都会重新刷所有目标。")
            .defaultValue(false).build()
    );

    public final Setting<Boolean> playSound = sgTarget.add(
        new BoolSetting.Builder()
            .name("播放提示音")
            .description("找到目标附魔时播放升级音效提示。")
            .defaultValue(true).build()
    );

    // ================================================================
    //  村民与寻路 - 搜索和移动方式
    // ================================================================

    public enum PathingMode {
        手动模式,
        自动寻路
    }

    public final Setting<PathingMode> pathingMode = sgPathing.add(
        new EnumSetting.Builder<PathingMode>()
            .name("寻路模式")
            .description("手动模式：你自己走到村民旁边（4格内）再开模块。\n自动寻路：使用 Baritone 自动走到村民旁边，需安装 Baritone。")
            .defaultValue(PathingMode.手动模式).build()
    );

    public final Setting<Integer> searchRange = sgPathing.add(
        new IntSetting.Builder()
            .name("搜索范围")
            .description("以玩家为中心的村民搜索半径（格）。\n范围越大能找到的村民越多，但可能影响性能。")
            .defaultValue(32).range(8, 64).sliderRange(8, 64).build()
    );

    public final Setting<Integer> reachDistance = sgPathing.add(
        new IntSetting.Builder()
            .name("自动模式到达距离")
            .description("【仅自动寻路模式有效】\n走到距村民多少格内视为到达。")
            .defaultValue(3).range(2, 6).sliderRange(2, 6)
            .visible(() -> pathingMode.get() == PathingMode.自动寻路)
            .build()
    );

    // ================================================================
    //  刷新控制 - 各种延迟和超时
    // ================================================================

    public final Setting<Integer> actionDelay = sgTiming.add(
        new IntSetting.Builder()
            .name("操作延迟")
            .description("每次操作之间的等待时间（游戏刻）。\n1 刻 = 50 毫秒，20 刻 = 1 秒。\n延迟越高越不容易触发反作弊，但速度更慢。")
            .defaultValue(5).range(1, 20).sliderRange(1, 20).build()
    );

    public final Setting<Integer> refreshTimeout = sgTiming.add(
        new IntSetting.Builder()
            .name("职业刷新超时")
            .description("等待村民获得职业或失业的最长时间（毫秒）。\n超时后会跳过该村民继续寻找下一个。\n正常情况 2-3 秒即可，特殊情况可调高。")
            .defaultValue(5000).range(1000, 10000).sliderRange(1000, 10000).build()
    );

    public final Setting<Integer> restartDelay = sgTiming.add(
        new IntSetting.Builder()
            .name("完成后间隔")
            .description("绑定一个村民后，寻找下一个之前的等待时间（秒）。\n给服务器和客户端一些缓冲时间。")
            .defaultValue(2).range(1, 10).sliderRange(1, 10).build()
    );

    // ================================================================
    //  辅助设置 - 不常用的设置
    // ================================================================

    public final Setting<Keybind> quickStopKey = sgExtra.add(
        new KeybindSetting.Builder()
            .name("快速停止键")
            .description("按下此键立即停止模块运行。\n用于紧急情况下快速中止。")
            .defaultValue(Keybind.none())
            .action(() -> { if (isActive()) toggle(); })
            .build()
    );

    public final Setting<Boolean> debugLog = sgExtra.add(
        new BoolSetting.Builder()
            .name("调试模式")
            .description("在聊天栏显示详细的运行日志。\n用于排查问题或了解模块运行细节。\n正常使用时建议关闭。")
            .defaultValue(false).build()
    );

    // ================================================================
    //  运行时状态
    // ================================================================

    private LibrarianState state = LibrarianState.IDLE;
    private VillagerEntity currentVillager = null;
    private BlockPos currentLectern = null;
    private int tickDelay = 0;
    private long lastActionTime = 0;
    private RegistryKey<Enchantment> pendingEnchant = null;
    private int pendingInventoryCount = 0;
    private boolean startedPathing = false;

    /** 本次会话已找到的附魔（不修改用户设置，只用于跳过已找到的目标） */
    private final Set<RegistryKey<Enchantment>> foundEnchants = new HashSet<>();

    /** 本次会话已绑定的村民 UUID，扫描时排除，不重复选同一个 */
    private final Set<UUID> boundVillagers = new HashSet<>();

    // ================================================================
    //  子系统
    // ================================================================

    private final VillagerScanner scanner = new VillagerScanner();
    private final EnchantmentChecker checker = new EnchantmentChecker();
    private final LecternController lectern = new LecternController();
    private final BaritoneWalker walker = new BaritoneWalker();

    // ================================================================
    //  构造函数
    // ================================================================

    public LibrarianRoller() {
        super(AddonTemplate.JERININADDONE, "附魔助手",
            "自动寻找失业村民，放置讲台刷取指定附魔。（村民旁边是岩浆块且上方未放置工作方块）");
    }

    // ================================================================
    //  生命周期
    // ================================================================

    @Override
    public void onActivate() {
        state = LibrarianState.IDLE;
        currentVillager = null;
        currentLectern = null;
        tickDelay = 0;
        lastActionTime = System.currentTimeMillis();
        pendingEnchant = null;
        pendingInventoryCount = 0;
        startedPathing = false;
        foundEnchants.clear();
        boundVillagers.clear();

        if (targetEnchants.get().isEmpty()) {
            warn("§c未选择任何目标附魔，模块已关闭");
            toggle(); return;
        }
        if (pathingMode.get() == PathingMode.自动寻路) {
            if (!BaritoneWalker.isAvailable()) {
                warn("§cBaritone 不可用，请切换为「手动模式」后自己走到村民旁边");
                toggle(); return;
            }
            if (walker.isPathing()) {
                warn("§cBaritone 当前已被其他功能占用，请关闭其中一个寻路来源后再试");
                toggle(); return;
            }
        }
        log(String.format("§a模块启动，共 %d 个目标附魔，开始扫描无业村民...", targetEnchants.get().size()));
        state = LibrarianState.SCANNING;
    }

    @Override
    public void onDeactivate() {
        if (startedPathing && walker.isPathing()) walker.stop();
        startedPathing = false;
        pendingEnchant = null;
        if (!foundEnchants.isEmpty())
            log(String.format("§a本次共找到 %d 个目标附魔", foundEnchants.size()));
        currentVillager = null;
        currentLectern = null;
        foundEnchants.clear();
        boundVillagers.clear();
    }

    // ================================================================
    //  主循环
    // ================================================================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (tickDelay > 0) { tickDelay--; return; }

        switch (state) {
            case IDLE             -> { }
            case SCANNING         -> handleScanning();
            case WALKING          -> handleWalking();
            case PLACING_LECTERN  -> handlePlacingLectern();
            case WAITING_REFRESH  -> handleWaitingRefresh();
            case WAITING_TRADE       -> handleWaitingTrade();
            case CHECKING_TRADES     -> handleCheckingTrades();
            case CONFIRMING_PURCHASE -> handleConfirmingPurchase();
            case BREAKING_LECTERN    -> handleBreakingLectern();
            case WAITING_JOBLESS  -> handleWaitingJobless();
            case COOLDOWN         -> { state = LibrarianState.SCANNING; }
        }
    }

    // ================================================================
    //  状态处理
    // ================================================================

    /** 1. 扫描附近无业村民 */
    private void handleScanning() {
        if (getRemainingTargets().isEmpty()) {
            log("§a§l所有目标附魔已找到，模块关闭！");
            toggle(); return;
        }

        currentVillager = scanner.findNearestUnemployed(searchRange.get(), boundVillagers);
        if (currentVillager == null) {
            // 检查是否扫到了傻子村民（把傻子加入排除名单，避免无限播报）
            VillagerEntity nitwit = scanner.findNearestNitwit(searchRange.get(), boundVillagers);
            if (nitwit != null) {
                warn("§e[刷书] 检测到傻子村民，已跳过，继续寻找下一个...");
                boundVillagers.add(nitwit.getUuid());
                tickDelay = actionDelay.get();
                return;
            }
            warn("§c附近没有可用的无业村民（搜索范围 " + searchRange.get() + " 格），模块已停止");
            toggle(); return;
        }
        debug("找到无业村民，准备走过去放讲台");

        if (pathingMode.get() == PathingMode.自动寻路) {
            walker.walkTo(currentVillager.getBlockPos());
            startedPathing = true;
            state = LibrarianState.WALKING;
            lastActionTime = System.currentTimeMillis();
        } else {
            state = LibrarianState.PLACING_LECTERN;
            tickDelay = actionDelay.get();
        }
    }

    /** 2. Baritone 走向村民，到达后放讲台 */
    private void handleWalking() {
        if (currentVillager == null || !currentVillager.isAlive()) {
            walker.stop(); startedPathing = false; state = LibrarianState.SCANNING; return;
        }
        if (System.currentTimeMillis() - lastActionTime > 30_000) {
            walker.stop();
            startedPathing = false;
            warn("§c寻路超时，重新扫描");
            state = LibrarianState.SCANNING; return;
        }
        if (walker.isNearEntity(currentVillager, reachDistance.get())) {
            walker.stop();
            startedPathing = false;
            state = LibrarianState.PLACING_LECTERN;
            tickDelay = actionDelay.get();
        }
    }

    /** 3. 在村民旁边放置讲台（首次或重刷都用这个） */
    private void handlePlacingLectern() {
        if (currentVillager == null || !currentVillager.isAlive()) {
            state = LibrarianState.SCANNING; return;
        }
        if (!lectern.hasLectern()) {
            warn("§c背包中没有讲台，模块已关闭");
            toggle(); return;
        }

        // 如果已有讲台坐标（重刷），直接放原位；否则找个空位
        BlockPos placePos = currentLectern != null ? currentLectern : findPlacePosNear(currentVillager.getBlockPos());
        if (placePos == null) {
            warn("§c村民附近没有合适的位置放置讲台，跳过该村民");
            boundVillagers.add(currentVillager.getUuid());
            currentVillager = null;
            state = LibrarianState.SCANNING; return;
        }

        if (lectern.placeLectern(placePos)) {
            currentLectern = placePos;
            debug("讲台已放置在 " + placePos.toShortString() + "，等待村民上职业");
            state = LibrarianState.WAITING_REFRESH;
            lastActionTime = System.currentTimeMillis();
            tickDelay = actionDelay.get() * 4;
        } else {
            warn("§c放置讲台失败，已跳过该村民");
            boundVillagers.add(currentVillager.getUuid());
            currentVillager = null;
            currentLectern = null;
            state = LibrarianState.SCANNING;
            tickDelay = actionDelay.get();
        }
    }

    /** 4. 等待村民成为图书管理员（首次上职业 或 重刷后上职业 都用这个） */
    private void handleWaitingRefresh() {
        if (currentVillager == null || !currentVillager.isAlive()) {
            state = LibrarianState.SCANNING; return;
        }
        if (scanner.isLibrarian(currentVillager)) {
            debug("村民已成为图书管理员，开始刷附魔书");
            openTrade();
        } else if (System.currentTimeMillis() - lastActionTime > refreshTimeout.get()) {
            warn("§c村民未能上职业（可能是傻子或有床位限制），跳过");
            if (currentLectern != null) lectern.breakLectern(currentLectern);
            boundVillagers.add(currentVillager.getUuid());
            currentVillager = null; currentLectern = null;
            state = LibrarianState.SCANNING;
        }
    }

    /** 5. 等待交易界面打开 */
    private void handleWaitingTrade() {
        if (mc.player == null) { state = LibrarianState.SCANNING; return; }
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            state = LibrarianState.CHECKING_TRADES;
            debug("交易界面已打开，开始检查");
        } else if (System.currentTimeMillis() - lastActionTime > 3000) {
            warn("§c打开交易超时，已跳过该村民");
            skipCurrentVillager();
        }
    }

    /** 6. 检查交易列表 */
    private void handleCheckingTrades() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            skipCurrentVillager(); return;
        }
        int matchIndex = checker.findMatchingTrade(handler, getRemainingTargets(), maxPrice.get());
        if (matchIndex >= 0) {
            onEnchantFound(handler, matchIndex);
        } else {
            // 没刷到，关闭交易、拆讲台并继续循环
            mc.player.closeHandledScreen();
            state = LibrarianState.BREAKING_LECTERN;
            tickDelay = actionDelay.get();
        }
    }

    /** 找到目标附魔后提示、自动购买绑定并换下一个村民 */
    private void onEnchantFound(MerchantScreenHandler handler, int matchIndex) {
        String name = checker.getEnchantmentName(handler.getRecipes().get(matchIndex));
        int price   = checker.getPrice(handler.getRecipes().get(matchIndex));
        RegistryKey<Enchantment> foundKey = getMatchedKey(handler, matchIndex);

        log(String.format("§a§l[%d/%d] 找到！§r  §e%s§r  价格: §e%d§r 绿宝石",
            foundEnchants.size() + 1, targetEnchants.get().size(), name, price));
        if (playSound.get())
            mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (!autoBuy.get()) {
            finishFoundEnchant(foundKey);
            return;
        }

        // 自动交易前检查材料和背包空间。
        if (countEmeralds() < price) {
            warn(String.format("§c绿宝石不足（需 %d，有 %d），停止绑定，模块关闭", price, countEmeralds()));
            mc.player.closeHandledScreen();
            toggle(); return;
        }
        TradeOffer offer = handler.getRecipes().get(matchIndex);
        java.util.Optional<TradedItem> secondBuyOpt = offer.getSecondBuyItem();
        if (secondBuyOpt.isPresent()) {
            TradedItem tradedItem = secondBuyOpt.get();
            if (tradedItem.matches(new ItemStack(net.minecraft.item.Items.BOOK))) {
                int needBooks = tradedItem.comp_2425();
                int haveBooks = countItem(net.minecraft.item.Items.BOOK);
                if (haveBooks < needBooks) {
                    warn(String.format("§c书不足（需 %d 本，背包有 %d 本），停止绑定，模块关闭", needBooks, haveBooks));
                    mc.player.closeHandledScreen();
                    toggle(); return;
                }
            }
        }
        if (!hasEmptyInventorySlot()) {
            warn("§c背包没有空位，无法购买附魔书，模块已关闭");
            mc.player.closeHandledScreen();
            toggle(); return;
        }

        pendingEnchant = foundKey;
        pendingInventoryCount = countEnchantedBooks(foundKey);
        handler.switchTo(matchIndex);
        mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, mc.player);
        state = LibrarianState.CONFIRMING_PURCHASE;
        lastActionTime = System.currentTimeMillis();
        tickDelay = actionDelay.get();
    }

    /** 等待服务器同步背包，确认附魔书确实购买成功。 */
    private void handleConfirmingPurchase() {
        if (pendingEnchant != null && countEnchantedBooks(pendingEnchant) > pendingInventoryCount) {
            RegistryKey<Enchantment> confirmedEnchant = pendingEnchant;
            pendingEnchant = null;
            log("§a已成功交易并绑定该村民");
            finishFoundEnchant(confirmedEnchant);
            return;
        }

        if (System.currentTimeMillis() - lastActionTime > 3000) {
            pendingEnchant = null;
            warn("§c服务器未确认交易成功，目标未标记完成，模块已关闭");
            if (mc.player != null) mc.player.closeHandledScreen();
            toggle();
        }
    }

    /** 完成一个目标，并在冷却后寻找下一个村民。 */
    private void finishFoundEnchant(RegistryKey<Enchantment> foundKey) {
        if (foundKey == null) {
            warn("§c无法识别目标附魔，模块已关闭");
            if (mc.player != null) mc.player.closeHandledScreen();
            toggle();
            return;
        }

        foundEnchants.add(foundKey);
        if (removeOnFind.get()) {
            targetEnchants.get().remove(foundKey);
            targetEnchants.onChanged();
        }
        if (currentVillager != null) boundVillagers.add(currentVillager.getUuid());

        if (mc.player != null) mc.player.closeHandledScreen();
        currentVillager = null;
        currentLectern = null;

        if (getRemainingTargets().isEmpty()) {
            log("§a§l全部目标附魔已找到，模块关闭！");
            toggle();
            return;
        }

        log(String.format("§e还剩 %d 个目标，%d 秒后继续寻找下一个无业村民...",
            getRemainingTargets().size(), restartDelay.get()));
        state = LibrarianState.COOLDOWN;
        tickDelay = restartDelay.get() * 20;
    }

    /** 清理当前村民与讲台引用，避免下一名村民复用旧坐标。 */
    private void skipCurrentVillager() {
        if (currentVillager != null) boundVillagers.add(currentVillager.getUuid());
        currentVillager = null;
        currentLectern = null;
        state = LibrarianState.SCANNING;
        tickDelay = actionDelay.get();
    }

    /** 7. 破坏讲台（重刷） */
    private void handleBreakingLectern() {
        if (currentLectern == null || !LecternController.isLectern(currentLectern)) {
            state = LibrarianState.SCANNING; return;
        }
        if (lectern.breakLectern(currentLectern)) {
            debug("讲台已破坏，等待村民失业");
            state = LibrarianState.WAITING_JOBLESS;
            lastActionTime = System.currentTimeMillis();
            tickDelay = actionDelay.get() * 2;
        } else {
            warn("§c破坏讲台失败，重新扫描");
            currentVillager = null;
            currentLectern = null;
            state = LibrarianState.SCANNING;
        }
    }

    /** 8. 等待村民失业（讲台拆掉后） */
    private void handleWaitingJobless() {
        if (currentVillager == null || !currentVillager.isAlive()) {
            currentLectern = null;
            state = LibrarianState.SCANNING; return;
        }
        if (scanner.isJobless(currentVillager)) {
            debug("村民已失业，重新放置讲台");
            state = LibrarianState.PLACING_LECTERN;
            tickDelay = actionDelay.get();
        } else if (System.currentTimeMillis() - lastActionTime > refreshTimeout.get()) {
            warn("§c等待失业超时，重新扫描");
            currentVillager = null;
            currentLectern = null;
            state = LibrarianState.SCANNING;
        }
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private void openTrade() {
        Rotations.rotate(Rotations.getYaw(currentVillager), Rotations.getPitch(currentVillager));
        mc.interactionManager.interactEntity(mc.player, currentVillager, mc.player.getActiveHand());
        state = LibrarianState.WAITING_TRADE;
        tickDelay = actionDelay.get() * 2;
        lastActionTime = System.currentTimeMillis();
    }

    private Set<RegistryKey<Enchantment>> getRemainingTargets() {
        return targetEnchants.get().stream()
            .filter(k -> !foundEnchants.contains(k))
            .collect(Collectors.toSet());
    }

    private RegistryKey<Enchantment> getMatchedKey(MerchantScreenHandler handler, int index) {
        Set<RegistryKey<Enchantment>> remaining = getRemainingTargets();
        var comp = handler.getRecipes().get(index).getSellItem()
            .get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp == null) return null;
        for (var entry : comp.getEnchantmentEntries())
            for (RegistryKey<Enchantment> key : remaining)
                if (entry.getKey().matchesKey(key)) return key;
        return null;
    }

    /** 在村民旁边找岩浆块，返回岩浆块上方坐标（用于放置讲台） */
    private BlockPos findPlacePosNear(BlockPos center) {
        if (mc.world == null) return null;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos magma = center.add(dx, dy, dz);
                    if (mc.world.getBlockState(magma).isOf(net.minecraft.block.Blocks.MAGMA_BLOCK)) {
                        BlockPos above = magma.up();
                        // 岩浆块上方是空气或已有讲台（可放置）
                        if (mc.world.getBlockState(above).isAir()
                            || LecternController.isLectern(above)) {
                            return above;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean hasEmptyInventorySlot() {
        if (mc.player == null) return false;
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) return true;
        }
        return false;
    }

    private int countEnchantedBooks(RegistryKey<Enchantment> key) {
        if (mc.player == null || key == null) return 0;
        int total = 0;
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() != net.minecraft.item.Items.ENCHANTED_BOOK) continue;
            var component = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (component == null) continue;
            for (var entry : component.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(key)) {
                    total += stack.getCount();
                    break;
                }
            }
        }
        return total;
    }

    private int countEmeralds() {
        return countItem(net.minecraft.item.Items.EMERALD);
    }

    private int countItem(net.minecraft.item.Item item) {
        if (mc.player == null) return 0;
        int total = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.getItem() == item)
                total += stack.getCount();
        }
        return total;
    }

    private void debug(String msg) {
        if (debugLog.get()) info("§7[刷附魔助手 DEBUG] " + msg);
    }

    private void log(String msg) {
        info("§b[附魔助手]§r " + msg);
    }

    private void warn(String msg) {
        warning("§b[附魔助手]§r " + msg);
    }

    @Override
    public String getInfoString() {
        if (!isActive()) return null;
        return String.format("%s  (%d/%d)",
            state.name().toLowerCase().replace("_", " "),
            foundEnchants.size(), targetEnchants.get().size());
    }
}
