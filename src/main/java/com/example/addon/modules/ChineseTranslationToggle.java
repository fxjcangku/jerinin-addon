package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.translation.TranslationManager;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ChineseTranslationToggle extends Module {
    public ChineseTranslationToggle() {
        super(AddonTemplate.JERININADDONE, "界面汉化", "开启后将 Meteor 界面切换为中文，关闭后恢复英文。");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        TranslationManager.setChinese(true);
    }

    @Override
    public void onDeactivate() {
        TranslationManager.setChinese(false);
    }
}
