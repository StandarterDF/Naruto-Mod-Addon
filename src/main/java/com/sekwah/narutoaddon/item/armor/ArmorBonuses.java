package com.sekwah.narutoaddon.item.armor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The armour bonuses of pieces this mod does not own as a class.
 *
 * <p>A bonus normally rides on {@link NarutoArmorItem} itself, which is how the headbands carry
 * theirs. The ANBU masks, the flak jackets, the ANBU field armour and the Akatsuki cloak, however,
 * are registered by the base 1.8.1 and only aliased here, so their item is an instance of the base
 * class and has no bonus field at all. Without this table those pieces would give nothing, which is
 * exactly the difference between "kit" and "plate" that the fork's armour is built around.</p>
 *
 * <p>The table is filled once, after the registries are frozen, from {@code item/NarutoItems}, and is
 * keyed by item instance because that is what a lookup from an {@link ItemStack} can answer without
 * building a string.</p>
 */
public final class ArmorBonuses {

    private static final Map<Item, NarutoArmorItem.Bonus> BONUSES = new IdentityHashMap<>();

    private ArmorBonuses() {
    }

    /** Records the bonus of one piece; called once per item during common setup. */
    public static void register(RegistryObject<? extends Item> item, NarutoArmorItem.Bonus bonus) {
        if (item == null || bonus == null || bonus.isEmpty()) {
            return;
        }
        Item value = item.get();
        if (value != null) {
            BONUSES.put(value, bonus);
        }
    }

    /**
     * @return the bonus of the piece in the stack, or {@code null} when it carries none
     */
    public static NarutoArmorItem.Bonus bonusFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return BONUSES.get(stack.getItem());
    }
}
