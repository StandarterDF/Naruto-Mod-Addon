package com.sekwah.narutoaddon.client.renderer;

import com.sekwah.narutoaddon.client.model.item.model.*;
import com.sekwah.narutoaddon.item.NarutoItems;
import com.sekwah.narutoaddon.item.armor.NarutoArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class NarutoResourceManager extends BlockEntityWithoutLevelRenderer {

    private static final ModelResourceLocation KATANA_ICON = new ModelResourceLocation("narutomod", "katana", "inventory");
    public static final ModelResourceLocation KATANA_MODEL = new ModelResourceLocation("narutomod", "katana_model", "inventory");

    public NarutoResourceManager() {
        super(null, null);
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();

        // The ANBU masks, flak jackets and the Akatsuki cloak are the base mod's items (they are
        // aliased in NarutoItems, not registered again here), so they are of the base mod's
        // NarutoArmorItem class; the headbands below are the addon's own. Both carry the same
        // model/texture setters, which is what the helper casts to.
        setArmorLook(NarutoItems.RED_ANBU_MASK.get(),
                new AnbuMaskModel(modelSet.bakeLayer(AnbuMaskModel.LAYER_LOCATION)),
                "textures/models/armor/anbu_mask/red_anbu_mask.png");

        setArmorLook(NarutoItems.BLUE_ANBU_MASK.get(),
                new AnbuMaskModel(modelSet.bakeLayer(AnbuMaskModel.LAYER_LOCATION)),
                "textures/models/armor/anbu_mask/blue_anbu_mask.png");

        setArmorLook(NarutoItems.GREEN_ANBU_MASK.get(),
                new AnbuMaskModel(modelSet.bakeLayer(AnbuMaskModel.LAYER_LOCATION_WITHOUT_EARS)),
                "textures/models/armor/anbu_mask/green_anbu_mask.png");

        setArmorLook(NarutoItems.MIST_ANBU_MASK.get(),
                new AnbuMaskModel(modelSet.bakeLayer(AnbuMaskModel.LAYER_LOCATION_WITHOUT_EARS)),
                "textures/models/armor/anbu_mask/mist_anbu_mask.png");

        setArmorLook(NarutoItems.YELLOW_ANBU_MASK.get(),
                new AnbuMaskModel(modelSet.bakeLayer(AnbuMaskModel.LAYER_LOCATION_WITHOUT_EARS)),
                "textures/models/armor/anbu_mask/yellow_anbu_mask.png");

        setArmorLook(NarutoItems.FLAK_JACKET_NEW.get(),
                new FlakJacketNewModel(modelSet.bakeLayer(FlakJacketNewModel.LAYER_LOCATION)),
                "textures/models/armor/flak_jacket_new.png");

        setArmorLook(NarutoItems.FLAK_JACKET.get(),
                new FlakJacketModel(modelSet.bakeLayer(FlakJacketModel.LAYER_LOCATION)),
                "textures/models/armor/flak_jacket.png");

        setArmorLook(NarutoItems.ANBU_ARMOR.get(),
                new AnbuArmorModel(modelSet.bakeLayer(AnbuArmorModel.LAYER_LOCATION)),
                "textures/models/armor/anbu_armor.png");

        setArmorLook(NarutoItems.AKATSUKI_CLOAK.get(),
                new AkatsukiCloakModel(modelSet.bakeLayer(AkatsukiCloakModel.LAYER_LOCATION)),
                "textures/models/armor/akatsuki_cloak.png");

        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_BLUE, "textures/models/armor/headband/headband_blue_blank.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_BLACK, "textures/models/armor/headband/headband_black_blank.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_RED, "textures/models/armor/headband/headband_red_blank.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_CUSTARD, "textures/models/armor/headband/headband_custard.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_LEAF, "textures/models/armor/headband/headband_leafvillage.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_LEAF_SCRATCHED, "textures/models/armor/headband/headband_leafvillage_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_LEAF_BLACK, "textures/models/armor/headband/headband_leafvillage_black.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_LEAF_BLACK_SCRATCHED, "textures/models/armor/headband/headband_leafvillage_black_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_ROCK, "textures/models/armor/headband/headband_rock.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_ROCK_SCRATCHED, "textures/models/armor/headband/headband_rock_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_SAND, "textures/models/armor/headband/headband_sandblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_SAND_SCRATCHED, "textures/models/armor/headband/headband_sandblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_SOUND, "textures/models/armor/headband/headband_soundvillage.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_SOUND_SCRATCHED, "textures/models/armor/headband/headband_soundvillage_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_MIST, "textures/models/armor/headband/headband_mistblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_MIST_SCRATCHED, "textures/models/armor/headband/headband_mistblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_WATERFALL, "textures/models/armor/headband/headband_waterfallblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_WATERFALL_SCRATCHED, "textures/models/armor/headband/headband_waterfallblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_CLOUD, "textures/models/armor/headband/headband_cloudblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_CLOUD_SCRATCHED, "textures/models/armor/headband/headband_cloudblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_RAIN, "textures/models/armor/headband/headband_rainblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_RAIN_SCRATCHED, "textures/models/armor/headband/headband_rainblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_GRASS, "textures/models/armor/headband/headband_grassblack.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_GRASS_SCRATCHED, "textures/models/armor/headband/headband_grassblack_scratched.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_PRIDE, "textures/models/armor/headband/headband_pride.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_YOUTUBE, "textures/models/armor/headband/headband_youtube.png");
        setHeadbandRenderer(modelSet, NarutoItems.HEADBAND_LAVA, "textures/models/armor/headband/headband_lavavillage.png");
    }

    /**
     * Dresses an armour item in a model and a texture.
     *
     * <p>Not every item here belongs to this mod: the masks and jackets are aliased to the base
     * mod's registry entries, so they are instances of the base mod's {@code NarutoArmorItem}
     * while the headbands are this mod's own class. Casting to either one directly crashed the
     * client on resource reload; both classes expose the same setters, so take whichever fits.</p>
     */
    private static void setArmorLook(Item item, HumanoidModel<? extends LivingEntity> model, String texture) {
        if (item instanceof com.sekwah.narutomod.item.armor.NarutoArmorItem base) {
            base.setArmorModel(model).setArmorTexture(texture);
        } else if (item instanceof NarutoArmorItem addon) {
            addon.setArmorModel(model).setArmorTexture(texture);
        }
    }

    private void setHeadbandRenderer(EntityModelSet modelSet, RegistryObject<Item> item, String texture) {
        setArmorLook(item.get(),
                new HeadbandModel(modelSet.bakeLayer(HeadbandModel.LAYER_LOCATION)),
                texture);
    }
}
