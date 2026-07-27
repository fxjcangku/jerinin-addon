package com.example.addon.settings;

import com.example.addon.gui.EnchantedBookListSettingScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class EnchantedBookListSetting extends Setting<Set<RegistryKey<Enchantment>>> {
    public record Option(RegistryKey<Enchantment> key, String name) {}

    public static final List<Option> OPTIONS = List.of(
        new Option(Enchantments.PROTECTION, "保护 IV"),
        new Option(Enchantments.FIRE_PROTECTION, "火焰保护 IV"),
        new Option(Enchantments.FEATHER_FALLING, "摔落缓冲 IV"),
        new Option(Enchantments.BLAST_PROTECTION, "爆炸保护 IV"),
        new Option(Enchantments.PROJECTILE_PROTECTION, "弹射物保护 IV"),
        new Option(Enchantments.THORNS, "荆棘 III"),
        new Option(Enchantments.RESPIRATION, "水下呼吸 III"),
        new Option(Enchantments.AQUA_AFFINITY, "水下速掘 I"),
        new Option(Enchantments.DEPTH_STRIDER, "深海探索者 III"),
        new Option(Enchantments.FROST_WALKER, "冰霜行者 II"),
        new Option(Enchantments.SHARPNESS, "锋利 V"),
        new Option(Enchantments.SMITE, "亡灵杀手 V"),
        new Option(Enchantments.BANE_OF_ARTHROPODS, "节肢杀手 V"),
        new Option(Enchantments.KNOCKBACK, "击退 II"),
        new Option(Enchantments.FIRE_ASPECT, "火焰附加 II"),
        new Option(Enchantments.LOOTING, "抢夺 III"),
        new Option(Enchantments.SWEEPING_EDGE, "横扫之刃 III"),
        new Option(Enchantments.DENSITY, "致密 V"),
        new Option(Enchantments.BREACH, "破甲 IV"),
        new Option(Enchantments.EFFICIENCY, "效率 V"),
        new Option(Enchantments.SILK_TOUCH, "精准采集 I"),
        new Option(Enchantments.UNBREAKING, "耐久 III"),
        new Option(Enchantments.FORTUNE, "时运 III"),
        new Option(Enchantments.POWER, "力量 V"),
        new Option(Enchantments.PUNCH, "冲击 II"),
        new Option(Enchantments.FLAME, "火矢 I"),
        new Option(Enchantments.INFINITY, "无限 I"),
        new Option(Enchantments.LUCK_OF_THE_SEA, "海之眷顾 III"),
        new Option(Enchantments.LURE, "饵钓 III"),
        new Option(Enchantments.LOYALTY, "忠诚 III"),
        new Option(Enchantments.IMPALING, "穿刺 V"),
        new Option(Enchantments.RIPTIDE, "激流 III"),
        new Option(Enchantments.CHANNELING, "引雷 I"),
        new Option(Enchantments.MULTISHOT, "多重射击 I"),
        new Option(Enchantments.QUICK_CHARGE, "快速装填 III"),
        new Option(Enchantments.PIERCING, "穿透 IV"),
        new Option(Enchantments.MENDING, "经验修补 I")
    );

    public EnchantedBookListSetting(String name, String description, Set<RegistryKey<Enchantment>> defaultValue,
                                    Consumer<Set<RegistryKey<Enchantment>>> onChanged,
                                    Consumer<Setting<Set<RegistryKey<Enchantment>>>> onModuleActivated,
                                    IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    public static void registerWidgetFactory() {
        SettingsWidgetFactory.registerCustomFactory(EnchantedBookListSetting.class, theme -> (table, rawSetting) -> {
            EnchantedBookListSetting setting = (EnchantedBookListSetting) rawSetting;
            WHorizontalList list = table.add(theme.horizontalList()).expandCellX().widget();

            WButton select = list.add(theme.button("打开附魔列表")).expandCellX().widget();
            select.action = () -> MinecraftClient.getInstance().setScreen(
                new EnchantedBookListSettingScreen(theme, setting));

            WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
            reset.action = setting::reset;
            reset.tooltip = "重置";
        });
    }

    @Override
    protected Set<RegistryKey<Enchantment>> parseImpl(String str) {
        Set<RegistryKey<Enchantment>> result = new HashSet<>();
        for (String part : str.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            try {
                result.add(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(part)));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    @Override
    protected boolean isValueValid(Set<RegistryKey<Enchantment>> value) {
        return value != null;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList list = new NbtList();
        for (RegistryKey<Enchantment> key : get()) list.add(NbtString.of(key.getValue().toString()));
        tag.put("value", list);
        return tag;
    }

    @Override
    public Set<RegistryKey<Enchantment>> load(NbtCompound tag) {
        get().clear();
        for (NbtElement element : tag.getList("value").orElse(new NbtList())) {
            String id = element.asString().orElse("");
            if (!id.isEmpty()) get().add(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(id)));
        }
        return get();
    }

    @Override
    public void resetImpl() {
        value = new HashSet<>(defaultValue);
    }

    public static class Builder extends SettingBuilder<Builder, Set<RegistryKey<Enchantment>>, EnchantedBookListSetting> {
        public Builder() {
            super(new HashSet<>());
        }

        @Override
        public EnchantedBookListSetting build() {
            return new EnchantedBookListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
