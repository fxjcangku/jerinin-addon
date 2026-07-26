package com.example.addon;

import com.example.addon.modules.CobbleSeller;
import com.example.addon.systems.CobbleSellerHud;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    public static final Category JERININADDONE = new Category("Jerinin-addon", Items.EMERALD.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Initializing Jerinin Addon");

        // 先注册 HUD，确保模块激活时可以自动添加面板
        Hud.get().register(CobbleSellerHud.INFO);

        // Modules
        Modules.get().add(new CobbleSeller());
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
