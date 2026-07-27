package com.example.addon.librarian;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.Set;

/**
 * 负责遍历当前打开的商人交易界面，判断是否含有用户想要的附魔书。
 * 只做只读检查，不产生交互动作。
 */
public class EnchantmentChecker {

    /**
     * 在当前交易列表中逐条检查，返回第一个符合条件的交易序号，未找到返回 -1。
     *
     * @param handler  当前交易界面的 ScreenHandler
     * @param targets  目标附魔 RegistryKey 集合
     * @param maxPrice 绿宝石价格上限
     * @return 匹配的交易序号（0-based），未找到返回 -1
     */
    public int findMatchingTrade(MerchantScreenHandler handler,
                                 Set<RegistryKey<Enchantment>> targets,
                                 int maxPrice) {
        if (targets == null || targets.isEmpty()) return -1;

        TradeOfferList offers = handler.getRecipes();
        if (offers == null) return -1;

        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            if (isValidOffer(offer, targets, maxPrice)) return i;
        }
        return -1;
    }

    /**
     * 检查单条交易是否满足筛选条件：
     * 1. 交易未耗尽
     * 2. 出售物是附魔书
     * 3. 附魔书含有目标附魔
     * 4. 附魔等级达到该附魔最高等级
     * 5. 第一购买物（绿宝石）数量不超过价格上限
     */
    public boolean isValidOffer(TradeOffer offer,
                                Set<RegistryKey<Enchantment>> targets,
                                int maxPrice) {
        // 1. 交易未耗尽（isDisabled 表示 uses >= maxUses）
        if (offer.isDisabled()) return false;

        // 2. 出售物必须是附魔书
        ItemStack sellItem = offer.getSellItem();
        if (sellItem.getItem() != Items.ENCHANTED_BOOK) return false;

        // 3-4. 读取 STORED_ENCHANTMENTS 组件，逐条附魔检查
        ItemEnchantmentsComponent enchComp =
            sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchComp == null || enchComp.isEmpty()) return false;

        boolean hasTargetEnchant = false;
        for (var entry : enchComp.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchEntry = entry.getKey();
            int level = entry.getIntValue();

            // 检查是否为目标附魔
            boolean isTarget = false;
            for (RegistryKey<Enchantment> key : targets) {
                if (enchEntry.matchesKey(key)) {
                    isTarget = true;
                    break;
                }
            }
            if (!isTarget) continue;

            // 必须达到该附魔的最高等级
            int maxLevel = enchEntry.comp_349().getMaxLevel();
            if (level < maxLevel) return false;

            hasTargetEnchant = true;
        }
        if (!hasTargetEnchant) return false;

        // 5. 绿宝石价格不超过上限
        ItemStack buyItem = offer.getDisplayedFirstBuyItem();
        int price = buyItem.isEmpty() ? 0 : buyItem.getCount();
        return price <= maxPrice;
    }

    /**
     * 获取某条交易的附魔名称（用于日志显示）。
     * 返回第一个附魔的 Identifier path，找不到返回 "unknown"。
     */
    public String getEnchantmentName(TradeOffer offer) {
        ItemStack sellItem = offer.getSellItem();
        ItemEnchantmentsComponent comp =
            sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp == null) return "unknown";
        for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
            return e.getKey()
                .map(k -> k.getValue().getPath())
                .orElse("unknown");
        }
        return "unknown";
    }

    /** 获取某条交易的绿宝石价格（显示用）。 */
    public int getPrice(TradeOffer offer) {
        ItemStack buyItem = offer.getDisplayedFirstBuyItem();
        return buyItem.isEmpty() ? 0 : buyItem.getCount();
    }
}
