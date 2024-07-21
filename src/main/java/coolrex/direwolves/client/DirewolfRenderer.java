package coolrex.direwolves.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import coolrex.direwolves.Direwolves;
import coolrex.direwolves.common.DirewolfEntity;;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

import static coolrex.direwolves.client.DirewolfModel.DIREWOLF_LAYER;

public class DirewolfRenderer extends MobRenderer<DirewolfEntity, DirewolfModel<DirewolfEntity>> {
        public DirewolfRenderer(EntityRendererProvider.Context context) {
        super(context, new DirewolfModel<>(context.bakeLayer(DIREWOLF_LAYER)), 0.75F);
    }

    public static final Map<DirewolfEntity.Variant, ResourceLocation> TEXTURE_VARIANTS = Util.make(Maps.newHashMap(), (map) -> {
        map.put(DirewolfEntity.Variant.PALE, new ResourceLocation(Direwolves.MOD_ID, "textures/entity/direwolf_pale.png"));
        map.put(DirewolfEntity.Variant.CYON, new ResourceLocation(Direwolves.MOD_ID, "textures/entity/direwolf_cyon.png"));
        map.put(DirewolfEntity.Variant.SNOW, new ResourceLocation(Direwolves.MOD_ID, "textures/entity/direwolf_snow.png"));
        map.put(DirewolfEntity.Variant.BLACK, new ResourceLocation(Direwolves.MOD_ID, "textures/entity/direwolf_black.png"));
        map.put(DirewolfEntity.Variant.SPEC, new ResourceLocation(Direwolves.MOD_ID, "textures/entity/direwolf_spec.png"));
    });

    @Override
    public ResourceLocation getTextureLocation(DirewolfEntity entity) {
        return TEXTURE_VARIANTS.get(entity.getVariant());
    }

    @Override
    public void render(DirewolfEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isBaby()) {
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else {
            poseStack.scale(1.4f, 1.4f, 1.4f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
