package coolrex.direwolves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import coolrex.direwolves.Direwolves;
import coolrex.direwolves.common.DirewolfEntity;
import coolrex.direwolves.common.animations.DirewolfAnimations;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DirewolfModel<T extends DirewolfEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation DIREWOLF_LAYER = new ModelLayerLocation(new ResourceLocation(Direwolves.MOD_ID, "direwolf_layer"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart waist;

    @Override
    public ModelPart root() {
        return root;
    }

    public DirewolfModel(ModelPart root) {
        this.root = root.getChild("root");
        this.head = root().getChild("body").getChild("torso").getChild("head");
        this.tail = root().getChild("body").getChild("waist").getChild("tail");
        this.waist = root().getChild("body").getChild("waist");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -15.0F, 2.0F));

        PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 28).addBox(-6.0F, -6.25F, 5.0F, 12.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(36, 0).addBox(-6.0F, -6.25F, -6.0F, 12.0F, 12.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.25F, -5.0F));

        PartDefinition hackles = torso.addOrReplaceChild("hackles", CubeListBuilder.create().texOffs(56, 53).addBox(-4.5F, -1.0F, -4.5F, 9.0F, 2.0F, 9.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, -5.25F, -0.5F));

        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -4.0F, -5.0F, 9.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(28, 4).mirror().addBox(-7.5F, -2.0F, -1.0F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(28, 4).addBox(4.5F, -2.0F, -1.0F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(13, 13).addBox(-2.0F, 1.0F, -5.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, 0.25F, -6.0F));

        PartDefinition leftEyebrow = head.addOrReplaceChild("leftEyebrow", CubeListBuilder.create().texOffs(0, 3).mirror().addBox(-1.0F, -2.5F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(3.25F, -0.5F, -4.0F));

        PartDefinition rightEyebrow = head.addOrReplaceChild("rightEyebrow", CubeListBuilder.create().texOffs(0, 3).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.offset(-3.25F, -2.0F, -4.0F));

        PartDefinition leftEye = head.addOrReplaceChild("leftEye", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(3.5F, -0.5F, -5.0F));

        PartDefinition rightEye = head.addOrReplaceChild("rightEye", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.offset(-3.5F, -0.5F, -5.0F));

        PartDefinition leftEyeangry = head.addOrReplaceChild("leftEyeangry", CubeListBuilder.create().texOffs(0, 1).mirror().addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(3.25F, -0.5F, -4.0F));

        PartDefinition rightEyeangry = head.addOrReplaceChild("rightEyeangry", CubeListBuilder.create().texOffs(0, 1).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.offset(-3.25F, -0.5F, -4.0F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(23, 0).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -4.0F, -1.5F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(23, 0).mirror().addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -4.0F, -1.5F));

        PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, -5.0F));

        PartDefinition nose = snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 13).addBox(-2.0F, -1.0F, -5.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition topteeth = nose.addOrReplaceChild("topteeth", CubeListBuilder.create().texOffs(16, 20).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, 3.0F, -2.5F));

        PartDefinition jaw = snout.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(18, 13).addBox(-2.0F, -0.5F, -5.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 22).addBox(-2.0F, -1.5F, -4.5F, 4.0F, 1.0F, 4.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, 4.5F, 0.0F));

        PartDefinition tongue = jaw.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(30, 1).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 0.0F));

        PartDefinition waist = body.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(34, 23).addBox(-5.0F, -4.5F, -1.0F, 10.0F, 9.0F, 13.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 0.5F, 0.0F));

        PartDefinition tail = waist.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, 11.0F));

        PartDefinition cube_r1 = tail.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(51, 45).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 7.0F));

        PartDefinition cube_r2 = tail2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(29, 52).addBox(0.0F, 0.0F, -7.0F, 0.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(33, 45).addBox(-2.0F, -1.0F, -5.0F, 4.0F, 11.0F, 5.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 10.0F));

        PartDefinition cube_r3 = tail3.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(11, 55).addBox(-2.0F, -1.0F, -5.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition leftBackleg = root.addOrReplaceChild("leftBackleg", CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -10.0F, 12.0F));

        PartDefinition rightBackleg = root.addOrReplaceChild("rightBackleg", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -10.0F, 12.0F));

        PartDefinition leftFrontleg = root.addOrReplaceChild("leftFrontleg", CubeListBuilder.create().texOffs(16, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -10.0F, -6.0F));

        PartDefinition rightFrontleg = root.addOrReplaceChild("rightFrontleg", CubeListBuilder.create().texOffs(16, 40).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -10.0F, -6.0F));

        return LayerDefinition.create(meshdefinition, 92, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch, ageInTicks);

        this.animateWalk(DirewolfAnimations.walk, limbSwing, limbSwingAmount, 2F, 2.5F);

        this.animate(((DirewolfEntity)entity).idleAnimationState, DirewolfAnimations.idle, ageInTicks, 0.85f);
    }

    private void applyHeadRotation(float pNetHeadYaw, float pHeadPitch, float pAgeInTicks) {
        pHeadPitch = Mth.clamp(pHeadPitch, -30.0F, 30.0F);
        pNetHeadYaw = Mth.clamp(pNetHeadYaw, -25.0F, 40.0F);

        this.head.xRot = (pHeadPitch * ((float)Math.PI / 180F));
        this.head.yRot = pNetHeadYaw * ((float)Math.PI / 180F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
