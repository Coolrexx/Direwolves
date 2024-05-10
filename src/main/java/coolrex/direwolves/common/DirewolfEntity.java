package coolrex.direwolves.common;

import coolrex.direwolves.registry.DirewolvesEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

public class DirewolfEntity extends TamableAnimal implements NeutralMob {
    private static final EntityDataAccessor<Integer> REMAINING_ANGER_TIME = SynchedEntityData.defineId(DirewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DirewolfEntity.class, EntityDataSerializers.INT);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    @Nullable
    private UUID persistentAngerTarget;

    public DirewolfEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setTame(false);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 12.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.7D, true));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25f).add(Attributes.MAX_HEALTH, 30D).add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return DirewolvesEntities.DIREWOLF.get().create(level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item.isEdible() && stack.getFoodProperties(this).isMeat();
    }

/*    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        player.setYRot(this.getYRot());
        player.setXRot(this.getXRot());
        player.startRiding(this);
        this.navigation.stop();

        return InteractionResult.SUCCESS;
    }*/

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level().isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || itemstack.is(Items.BONE) && !this.isTame() && !this.isAngry();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                this.heal((float)itemstack.getFoodProperties(this).getNutrition());
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                this.gameEvent(GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            } else {
                if (itemstack.is(Items.STICK)){
                    if (this.isOwnedBy(player)) {
                        System.out.println("doodoo narts");

                        player.setYRot(this.getYRot());
                        player.setXRot(this.getXRot());
                        player.startRiding(this);
                        this.navigation.stop();

                        return InteractionResult.SUCCESS;
                    }
                    return super.mobInteract(player, hand);
                }

                InteractionResult interactionresult = super.mobInteract(player, hand);
                if ((!interactionresult.consumesAction() || this.isBaby()) && this.isOwnedBy(player)) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget((LivingEntity)null);
                    return InteractionResult.SUCCESS;
                } else {
                    return interactionresult;
                }
            }
        } else if (itemstack.is(Items.BONE) && !this.isAngry()) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget((LivingEntity)null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte)7);
            } else {
                this.level().broadcastEntityEvent(this, (byte)6);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, hand);
        }
    }

    //Data woohoo
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        this.addPersistentAngerSaveData(tag);
        tag.putInt("Variant", this.getVariant().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.readPersistentAngerSaveData(this.level(), tag);
        this.entityData.set(VARIANT, tag.getInt("Variant"));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(REMAINING_ANGER_TIME, 0);
        this.entityData.define(VARIANT, 0);
    }

    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
    }

    public enum Variant {
        PALE(0),
        CYON(1),
        SNOW(2);

        private static final DirewolfEntity.Variant[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(DirewolfEntity.Variant::getId)).toArray(DirewolfEntity.Variant[]::new);
        private final int id;
        public int getId() {
            return this.id;
        }
        private Variant(int id) {
            this.id = id;
        }
        public static DirewolfEntity.Variant byId(int id) {
            return BY_ID[id % BY_ID.length];
        }
    }

    public DirewolfEntity.Variant getVariant() {
        return DirewolfEntity.Variant.BY_ID[this.entityData.get(VARIANT)];
    }

    public void setVariant(DirewolfEntity.Variant variant) {
        this.entityData.set(VARIANT, variant.getId());
    }

    public int randomVariant(int min, int max) {
        int variant = random.nextInt((max - min) + 1) + min;
        return variant;
    }

    //Spawning
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance instance, MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        DirewolfEntity.Variant variant = getVariant();

        DirewolfEntity.Variant.byId(this.randomVariant(0, 2));

        this.setVariant(variant);
        return super.finalizeSpawn(levelAccessor, instance, spawnType, groupData, tag);
    }

    //Mountable
    protected void positionRider(Entity entity, Entity.MoveFunction move) {
        super.positionRider(entity, move);
    }

    public double getPassengersRidingOffset() {
        float f = Math.min(0.25F, this.walkAnimation.speed());
        float f1 = this.walkAnimation.position();
        return (double)this.getBbHeight() - 0.10D + (double)(0.12F * Mth.cos(f1 * 1.5F) * 2.0F * f);
    }


    @Nullable
    public LivingEntity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : (LivingEntity)this.getPassengers().get(0);
    }

    public boolean isControlledByLocalInstance() {
        LivingEntity livingentity = this.getControllingPassenger();
        if (livingentity instanceof Player player) {
            return player.isLocalPlayer();
        } else {
            return this.isEffectiveAi();
        }
    }

    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
    }


    //todo: actually use the movecontrol class bc ive just remade it
    protected void tickRidden(Player player, Vec3 vec3) {
        super.tickRidden(player, vec3);
        Vec2 vec2 = this.getRiddenRotation(player);
        this.setRot(vec2.y, vec2.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        Vec3 inputVector = this.getRiddenInput(player, vec3);

        double moveX = inputVector.x;
        double moveZ = inputVector.z;
        double moveSpeed;

        if (this.isControlledByLocalInstance() && !this.onGround()) {
            moveSpeed = 0.05;
            this.setDeltaMovement(this.getDeltaMovement().add(moveX * moveSpeed, 0, moveZ * moveSpeed));
        } else {
            moveSpeed = 0.2;
            this.setDeltaMovement(this.getDeltaMovement().add(moveX * moveSpeed, 0, moveZ * moveSpeed));
        }
    }

    protected Vec2 getRiddenRotation(LivingEntity entity) {
        return new Vec2(entity.getXRot() * 0.5F, entity.getYRot());
    }

    protected Vec3 getRiddenInput(Player player, Vec3 vec3) {
        Vec2 riddenRotation = this.getRiddenRotation(player);
        float forwardInput = player.zza;
        float strafeInput = player.xxa;

        double forwardX = Math.sin(-riddenRotation.y * (Math.PI / 180.0));
        double forwardZ = Math.cos(riddenRotation.y * (Math.PI / 180.0));
        double rightX = -Math.sin((-riddenRotation.y - 90) * (Math.PI / 180.0)); // Negate to invert direction
        double rightZ = -Math.cos((-riddenRotation.y - 90) * (Math.PI / 180.0)); // Negate to invert direction

        double moveX = forwardX * forwardInput + rightX * strafeInput;
        double moveZ = forwardZ * forwardInput + rightZ * strafeInput;

        return new Vec3(moveX, 0, moveZ);
    }


    //NeutralMob
    public boolean hurt(DamageSource damageSource, float f) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            Entity entity = damageSource.getEntity();
            if (!this.level().isClientSide) {
                this.setOrderedToSit(false);
            }

            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                f = (f + 1.0F) / 2.0F;
            }

            return super.hurt(damageSource, f);
        }
    }

    public boolean canFreeze() {
        return false;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(REMAINING_ANGER_TIME);
    }

    @Override
    public void setRemainingPersistentAngerTime(int time) {
        this.entityData.set(REMAINING_ANGER_TIME, time);
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.persistentAngerTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    public boolean wantsToAttack(LivingEntity entity, LivingEntity player) {
        if (!(entity instanceof Creeper) && !(entity instanceof Ghast)) {
            if (entity instanceof DirewolfEntity) {
                DirewolfEntity direwolf = (DirewolfEntity) entity;
                return !direwolf.isTame() || direwolf.getOwner() != player;
            } else if (entity instanceof Player && player instanceof Player && !((Player)player).canHarmPlayer((Player)entity)) {
                return false;
            } else if (entity instanceof AbstractHorse && ((AbstractHorse)entity).isTamed()) {
                return false;
            } else {
                return !(entity instanceof TamableAnimal) || !((TamableAnimal)entity).isTame();
            }
        } else {
            return false;
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(this.damageSources().mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }

        return flag;
    }

    //Animations
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sitAnimationState = new AnimationState();
    public final AnimationState runAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            setupAnimationStates();
        }
    }

    private void setupAnimationStates() {
        if(this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 51;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        if(this.isInSittingPose()) {
            System.out.println("Sitting Anim Start");
            this.sitAnimationState.start(this.tickCount);
        } else {
            this.sitAnimationState.stop();
        }
    }

    @Override
    protected void updateWalkAnimation(float partialTick) {
        float f;
        if (this.getPose() == Pose.STANDING) {
            f = Math.min(partialTick * 4.0F, 1.0F);
        } else {
            f = 0.0F;
        }

        this.walkAnimation.update(f, 0.4F);
    }

    protected SoundEvent getAmbientSound() {
        if (this.isAngry()) {
            return SoundEvents.WOLF_GROWL;
        } else if (this.random.nextInt(3) == 0) {
            return this.isTame() && this.getHealth() < 10.0F ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_PANT;
        } else {
            return SoundEvents.WOLF_AMBIENT;
        }
    }

    //Sounds
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.73F;
    }
}
