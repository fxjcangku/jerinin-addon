package com.example.addon;

import com.example.addon.modules.ChineseTranslationToggle;
import com.example.addon.modules.CobbleSeller;
import com.example.addon.modules.LibrarianRoller;
import com.example.addon.modules.TpMace;
import com.example.addon.settings.EnchantedBookListSetting;
import com.example.addon.systems.*;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(AddonTemplate.class);
    public static final Category JERININADDONE = new Category("Jerininaddon", Items.EMERALD.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Initializing Addon Template");
        EnchantedBookListSetting.registerWidgetFactory();

        // 检测并初始化 Baritone
        if (FabricLoader.getInstance().isModLoaded("baritone")) {
            LOG.info("✓ Baritone detected, pathfinding enabled");
        } else {
            LOG.warn("⚠ Baritone not found, pathfinding features disabled");
        }

        // 先注册 HUD，确保模块激活时可以自动添加面板
        Hud.get().register(CobbleSellerHud.INFO);

        // 注册模块
        Modules.get().add(new LibrarianRoller());
        Modules.get().add(new TpMace());
        Modules.get().add(new CobbleSeller());
        Modules.get().add(new ChineseTranslationToggle());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(JERININADDONE);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
