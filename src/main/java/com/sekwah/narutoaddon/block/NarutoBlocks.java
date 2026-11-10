package com.sekwah.narutoaddon.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class NarutoBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "narutoaddon");
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "narutoaddon");

    // paper_bomb and bonsai_tree are provided by the base mod (NarutoMod); alias to avoid duplication.
    public static final RegistryObject<Block> PAPER_BOMB = com.sekwah.narutomod.block.NarutoBlocks.PAPER_BOMB;
    public static final RegistryObject<Item> ITEM_PAPER_BOMB = com.sekwah.narutomod.block.NarutoBlocks.ITEM_PAPER_BOMB;

    public static final RegistryObject<Block> BONSAI_TREE = com.sekwah.narutomod.block.NarutoBlocks.BONSAI_TREE;
    public static final RegistryObject<Item> ITEM_BONSAI_TREE = com.sekwah.narutomod.block.NarutoBlocks.ITEM_BONSAI_TREE;

    /**
     * A ward laid on the ground: it chimes when a hostile mob enters its radius, and can optionally
     * keep spawns out of the perimeter. See {@link SentrySealBlock}.
     */
    public static final RegistryObject<Block> SENTRY_SEAL = BLOCKS.register("sentry_seal",
            () -> new SentrySealBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final RegistryObject<Item> ITEM_SENTRY_SEAL = BLOCK_ITEMS.register("sentry_seal",
            () -> new BlockItem(SENTRY_SEAL.get(), new Item.Properties()));

    /**
     * The wall of the Dharma cube: a block that reads as open sky. The pocket dimension has no
     * skylight of its own and its sky is the dark one of the End, so the room's own walls carry the
     * picture - pale blue with faint clouds, and lit like daylight so the room does not sit in the
     * dark. Unbreakable: it is the boundary of the dimension, not a resource.
     */
    public static final RegistryObject<Block> SKY_BLOCK = BLOCKS.register("sky_block",
            () -> new SkyBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .noOcclusion()));

    public static final RegistryObject<Item> ITEM_SKY_BLOCK = BLOCK_ITEMS.register("sky_block",
            () -> new BlockItem(SKY_BLOCK.get(), new Item.Properties()));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "narutoaddon");

    public static final RegistryObject<BlockEntityType<SentrySealBlockEntity>> SENTRY_SEAL_ENTITY =
            BLOCK_ENTITIES.register("sentry_seal", () -> BlockEntityType.Builder
                    .of(SentrySealBlockEntity::new, SENTRY_SEAL.get()).build(null));

    /** Marker entity of the sky block; the renderer hangs off it. */
    public static final RegistryObject<BlockEntityType<SkyBlockEntity>> SKY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("sky_block", () -> BlockEntityType.Builder
                    .of(SkyBlockEntity::new, SKY_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
