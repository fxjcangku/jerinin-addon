package com.example.addon.gui;

import com.example.addon.settings.EnchantedBookListSetting;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;

import java.util.Locale;
import java.util.Optional;

public class EnchantedBookListSettingScreen extends WindowScreen {
    private final EnchantedBookListSetting setting;
    private final WTextBox search;
    private final WVerticalList list;

    public EnchantedBookListSettingScreen(GuiTheme theme, EnchantedBookListSetting setting) {
        super(theme, "选择目标附魔");
        this.setting = setting;

        search = super.add(theme.textBox("", "搜索附魔")).minWidth(400).expandX().widget();
        search.setFocused(true);
        search.action = this::refresh;

        list = super.add(theme.verticalList()).expandX().widget();
        refresh();
    }

    @Override
    public <W extends WWidget> meteordevelopment.meteorclient.gui.utils.Cell<W> add(W widget) {
        return list.add(widget);
    }

    @Override
    public void initWidgets() {
    }

    private void refresh() {
        list.clear();
        String filter = search.get().trim().toLowerCase(Locale.ROOT);

        WTable table = list.add(theme.table()).expandX().widget();
        for (EnchantedBookListSetting.Option option : EnchantedBookListSetting.OPTIONS) {
            String searchText = (option.name() + " " + option.key().getValue()).toLowerCase(Locale.ROOT);
            if (!filter.isEmpty() && !searchText.contains(filter)) continue;

            table.add(theme.item(createBook(option.key())));
            table.add(theme.label(option.name())).expandX();

            boolean selected = setting.get().contains(option.key());
            WButton button = table.add(theme.button(selected ? "−" : "+"))
                .expandCellX().right().widget();
            button.tooltip = selected ? "从目标列表移除" : "添加到目标列表";
            button.action = () -> {
                if (selected) setting.get().remove(option.key());
                else setting.get().add(option.key());
                setting.onChanged();
                refresh();
            };
            table.row();
        }
    }

    private ItemStack createBook(RegistryKey<Enchantment> key) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return book;

        Optional<RegistryEntry.Reference<Enchantment>> entry = mc.world.getRegistryManager()
            .getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(key.getValue());
        if (entry.isEmpty()) return book;

        ItemEnchantmentsComponent.Builder enchantments =
            new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        enchantments.add(entry.get(), entry.get().comp_349().getMaxLevel());
        book.set(DataComponentTypes.STORED_ENCHANTMENTS, enchantments.build());
        return book;
    }
}
