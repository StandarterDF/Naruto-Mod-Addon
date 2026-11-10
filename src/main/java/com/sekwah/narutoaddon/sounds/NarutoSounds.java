package com.sekwah.narutoaddon.sounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class NarutoSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "narutoaddon");

    public static final RegistryObject<SoundEvent> SHADOW_CLONE_POOF = register("clone_poof");

    private static RegistryObject<SoundEvent> register(String key) {
        return SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("narutoaddon", key)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}