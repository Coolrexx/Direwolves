package coolrex.direwolves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import coolrex.direwolves.Direwolves;
import coolrex.direwolves.common.DirewolfEntity;;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static coolrex.direwolves.client.DirewolfModel.DIREWOLF_LAYER;

public class DirewolfRenderer extends MobRenderer<DirewolfEntity, DirewolfModel<DirewolfEntity>> {
        public DirewolfRenderer(EntityRendererProvider.Context context) {
        super(context, new DirewolfModel<>(context.bakeLayer(DIREWOLF_LAYER)), 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(DirewolfEntity entity) {
        return new ResourceLocation(Direwolves.MOD_ID,"textures/entity/direwolf_pale.png");
    }

    @Override
    public void render(DirewolfEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isBaby()) {
            poseStack.scale(0.5f, 0.5f, 0.5f);
        } else {
            poseStack.scale(1.05f, 1.05f, 1.05f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
