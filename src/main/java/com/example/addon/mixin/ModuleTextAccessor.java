package com.example.addon.mixin;

import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Module.class, remap = false)
public interface ModuleTextAccessor {
    @Mutable
    @Accessor("title")
    void jerinin$setTitle(String title);

    @Mutable
    @Accessor("description")
    void jerinin$setDescription(String description);
}
