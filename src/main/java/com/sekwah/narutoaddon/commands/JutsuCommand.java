package com.sekwah.narutoaddon.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sekwah.narutoaddon.abilities.Ability;
import com.sekwah.narutoaddon.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutoaddon.registries.NarutoRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.stream.Stream;

public class JutsuCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> jutsu = Commands.literal("jutsu")
                .then(Commands.literal("list").executes(ctx -> {
                    Stream<Map.Entry<ResourceKey<Ability>, Ability>> abilities = NarutoRegistries.ABILITIES.getEntries().stream().filter(entry -> isCastableCombo(entry.getValue().defaultCombo()));
                    CommandSourceStack source = ctx.getSource();

                    source.sendSuccess(Component::empty,false);

                    abilities.forEach(entry -> {
                        var comboComponent = Component.literal("");
                        for (char comboKey: String.valueOf(entry.getValue().defaultCombo()).toCharArray()) {
                            comboComponent.append(" ").append(Component.keybind("naruto.keys.key" + comboKey));
                        }

                        source.sendSuccess(() -> Component.translatable(entry.getKey().location().toString())
                                .append(" -")
                                .append(comboComponent), false);
                    });
                    source.sendSuccess(() -> Component.translatable("naruto.keys.leap")
                            .append(" - ")
                            .append(Component.keybind("naruto.keys.leap")), false);
                    source.sendSuccess(Component::empty, false);

                    return Command.SINGLE_SUCCESS;
                }));

        // Debug helper: wipe learned technique progress. Cheat/op only (permission level 2), since it
        // is a testing tool and no ordinary player should be able to throw their mastery away.
        jutsu.then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                            .ifPresent(data -> data.getMasteryData().reset());
                    ctx.getSource().sendSuccess(() ->
                            Component.translatable("commands.narutomod.mastery.reset.all"), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("technique", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                NarutoRegistries.ABILITIES.getKeys(), builder))
                        .executes(ctx -> {
                            ResourceLocation technique = ResourceLocationArgument.getId(ctx, "technique");
                            var player = ctx.getSource().getPlayerOrException();
                            player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                                    .ifPresent(data -> data.getMasteryData().reset(technique));
                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                    "commands.narutomod.mastery.reset.one", technique.toString()), true);
                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(jutsu);
    }

    /**
     * A combo is spelled with the three jutsu keys the base mod registers, so a digit outside 1..3
     * is not a combo the player can press. Techniques fired by other means use such a value as a
     * "no combo" marker, and printing it here would show raw key names.
     */
    private static boolean isCastableCombo(long combo) {
        if (combo == -1) {
            return false;
        }
        for (char digit : String.valueOf(combo).toCharArray()) {
            if (digit < '1' || digit > '3') {
                return false;
            }
        }
        return true;
    }

}
