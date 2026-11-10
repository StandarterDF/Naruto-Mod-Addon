package com.sekwah.narutoaddon.bridge;

import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.abilities.NarutoAbilities;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class NarutoAddonAbilities {

    public static final DeferredRegister<com.sekwah.narutomod.abilities.Ability> ABILITY =
            DeferredRegister.create(NarutoRegistries.ABILITY_REGISTRY_LOC, "narutoaddon");

    static {
        for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            String name = entry.getId().getPath();
            ABILITY.register(name, () -> new AbilityProxy(entry.get()));
        }
    }

    public static void register(IEventBus eventBus) {
        ABILITY.register(eventBus);
    }
}
