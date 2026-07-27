package com.example.addon.translation;

import com.example.addon.AddonTemplate;
import com.example.addon.mixin.ModuleTextAccessor;
import com.example.addon.mixin.SettingTextAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class TranslationManager {
    private static final String LANGUAGE_RESOURCE = "/assets/jerinin-addon/lang/zh_cn.json";
    private static final Map<String, String> TRANSLATIONS = loadTranslations();
    private static final Map<Module, OriginalText> MODULE_TEXT = new IdentityHashMap<>();
    private static final Map<Setting<?>, OriginalText> SETTING_TEXT = new IdentityHashMap<>();
    private static boolean chineseEnabled;

    private TranslationManager() {
    }

    public static void setChinese(boolean enabled) {
        chineseEnabled = enabled;

        for (Module module : Modules.get().getAll()) {
            OriginalText moduleText = MODULE_TEXT.computeIfAbsent(module,
                ignored -> new OriginalText(module.title, module.description));
            ModuleTextAccessor moduleAccessor = (ModuleTextAccessor) module;

            if (enabled) {
                moduleAccessor.jerinin$setTitle(translate("Module.Meteor." + module.name, moduleText.title()));
                moduleAccessor.jerinin$setDescription(translate("Module.Meteor." + module.name + ".Description", moduleText.description()));
            } else {
                moduleAccessor.jerinin$setTitle(moduleText.title());
                moduleAccessor.jerinin$setDescription(moduleText.description());
            }

            for (SettingGroup group : module.settings) {
                for (Setting<?> setting : group) {
                    OriginalText settingText = SETTING_TEXT.computeIfAbsent(setting,
                        ignored -> new OriginalText(setting.title, setting.description));
                    SettingTextAccessor settingAccessor = (SettingTextAccessor) setting;

                    if (enabled) {
                        settingAccessor.jerinin$setTitle(translate("Setting.Meteor." + setting.name, settingText.title()));
                        settingAccessor.jerinin$setDescription(translate("Setting.Meteor." + setting.name + ".Description", settingText.description()));
                    } else {
                        settingAccessor.jerinin$setTitle(settingText.title());
                        settingAccessor.jerinin$setDescription(settingText.description());
                    }
                }
            }

            module.settings.invalidate();
        }
    }

    public static boolean isChineseEnabled() {
        return chineseEnabled;
    }

    private static String translate(String key, String fallback) {
        return TRANSLATIONS.getOrDefault(key, fallback);
    }

    private static Map<String, String> loadTranslations() {
        Map<String, String> translations = new HashMap<>();
        try (InputStream stream = TranslationManager.class.getResourceAsStream(LANGUAGE_RESOURCE)) {
            if (stream == null) {
                AddonTemplate.LOG.error("Missing translation resource: {}", LANGUAGE_RESOURCE);
                return translations;
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception exception) {
            AddonTemplate.LOG.error("Failed to load Chinese translations", exception);
        }
        return translations;
    }

    private record OriginalText(String title, String description) {
    }
}
