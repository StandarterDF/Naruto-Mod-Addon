package com.sekwah.narutoaddon.item;

import com.sekwah.narutoaddon.item.ComboSwordItem;
import com.sekwah.narutoaddon.item.NinjaTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Addon-side Samehada: a great sword that drains chakra from whatever it cuts and feeds its
 * caster when it cuts peaceful creatures. Mirrors the base mod's Samehada behaviour, but lives
 * under the addon's own namespace and namespace of registries.
 *
 * <p>Mastery tracking for the feeding roll is intentionally simplified here (constant chance) because
 * the base mod's Mastery.recordCredit(ResourceLocation, ...) overload is not available in the
 * 1.11.0 jar the addon compiles against.</p>
 */
public class SamehadaItem extends ComboSwordItem {

    private static final float CHAKRA_PER_HEALTH = 0.1f;
    private static final float MAX_CHAKRA_BITE = 6.0f;
    private static final float FEED_CHANCE = 0.2f;
    private static final int FEED_HUNGER = 2;
    private static final float FEED_SATURATION = 0.6f;

    public SamehadaItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
                        float damagePerStep, int maxCombo, int comboWindow) {
        super(tier, attackDamage, attackSpeed, properties, damagePerStep, maxCombo, comboWindow, 0.0f);
    }

    public float chakraBite(LivingEntity victim) {
        return Math.min(MAX_CHAKRA_BITE, victim.getMaxHealth() * CHAKRA_PER_HEALTH);
    }

    public boolean feedCaster(Player caster) {
        if (caster.getRandom().nextFloat() >= FEED_CHANCE) {
            return false;
        }
        FoodData food = caster.getFoodData();
        food.eat(FEED_HUNGER, FEED_SATURATION);
        return true;
    }

    public static boolean isPeaceful(LivingEntity victim) {
        return victim instanceof Animal
                || victim instanceof WaterAnimal
                || victim instanceof AmbientCreature
                || victim instanceof AbstractVillager;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.narutoaddon.samehada.drain").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.narutoaddon.samehada.feed").withStyle(ChatFormatting.GRAY));
    }
}
