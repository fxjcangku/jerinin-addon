package com.example.addon.mixin;

import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Setting.class, remap = false)
public interface SettingTextAccessor {
    @Mutable
    @Accessor("title")
    void jerinin$setTitle(String title);

    @Mutable
    @Accessor("description")
    void jerinin$setDescription(String description);
}
