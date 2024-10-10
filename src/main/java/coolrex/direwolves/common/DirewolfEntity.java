package coolrex.direwolves.common;

import coolrex.direwolves.common.goals.DirewolfMeleeAttackGoal;
import coolrex.direwolves.common.goals.RiddenFloatGoal;
import coolrex.direwolves.registry.DirewolvesEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

public class DirewolfEntity extends TamableAnimal implements NeutralMob, PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> REMAINING_ANGER_TIME = SynchedEntityData.defineId(DirewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DirewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(DirewolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    @Nullable
    private UUID persistentAngerTarget;
    public boolean isJumping;
    public boolean isSprinting;
    public boolean playerJump;

    public DirewolfEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        //this.goalSelector.addGoal(1, new RiddenFloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(2, new DirewolfMeleeAttackGoal(this, 1.75D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
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
        return Animal.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25f).add(Attributes.MAX_HEALTH, 45D).add(Attributes.ATTACK_DAMAGE, 7.5D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        DirewolfEntity direwolf = DirewolvesEntities.DIREWOLF.get().create(level);
        if (direwolf != null) {
            DirewolfEntity.Variant variant = this.random.nextBoolean() ? this.getVariant() : ((DirewolfEntity)mob).getVariant();
            direwolf.setVariant(variant);
        }

        UUID uuid = this.getOwnerUUID();
        if (uuid != null) {
            direwolf.setOwnerUUID(uuid);
            direwolf.setTame(true);
        }

        this.setPersistenceRequired();
        return direwolf;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item.isEdible() && stack.getFoodProperties(this).isMeat();
    }

    /*TODO:
    figure out why sitting is delayed
    figure out another way of setting up mount & sitting (shift + leftclick to mount didnt work for now obvious reasons)
    make direwolf float/swim in water while mounted rather than sink
    finalise spawning biomes
    make direwolves spawn with a wolf pack rarely
    */

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult interactionresult = super.mobInteract(player, hand);
        ItemStack stack = player.getItemInHand(hand);

        if (this.isTame()) {
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                //food
                this.heal((float)stack.getFoodProperties(this).getNutrition());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.gameEvent(GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            } else if (!interactionresult.consumesAction() && player.isShiftKeyDown() && this.isOwnedBy(player)) {
                //sit
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            } else if (!interactionresult.consumesAction() && this.isOwnedBy(player) && !this.isInSittingPose() && !this.isBaby()) {
                //ride
                player.setYRot(this.getYRot());
                player.setXRot(this.getXRot());
                player.startRiding(this);
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            } else {
                //interaction does nothing?
                return interactionresult;
            }
        } else {
            //not tamed
            if (stack.is(Items.BONE) && !this.isAngry()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
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

                this.setPersistenceRequired();
                return InteractionResult.SUCCESS;
            } else {
                return interactionresult;
            }
        }
    }


    //region Data
    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }

    public void setIsJumping(boolean b) {
        this.isJumping = b;
    }

    public boolean isJumping() {
        return this.isJumping;
    }

    public void setPlayerJump(boolean b) {
        this.playerJump = b;
    }

    public boolean playerJump() {
        return this.playerJump;
    }

    public void setIsSprinting(boolean b) {
        this.isSprinting = b;
    }

    public boolean isSprinting() {
        return this.isSprinting;
    }

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
        this.entityData.define(ATTACKING, false);
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
        SNOW(2),
        BLACK(3),
        SPEC(4);

        private static final DirewolfEntity.Variant[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(DirewolfEntity.Variant::getId)).toArray(DirewolfEntity.Variant[]::new);
        private final int id;
        public int getId() {
            return this.id;
        }
        Variant(int id) {
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
    //endregion


    //region Spawning
    public static boolean canSpawn(EntityType<? extends Mob> entityType, ServerLevelAccessor world, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return world.getBlockState(pos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isMoonPhaseToSpawn(world) && isDarkEnoughToSpawn(world, pos, random);
    }

    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance instance, MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        Variant variant;

        if (world.getLevel().getMoonPhase() == 0) {
            variant = Variant.byId(this.randomVariant(0, 2));
        } else if (world.getLevel().getMoonPhase() == 4) {
            variant = Variant.byId(this.randomVariant(3, 4));
        } else if (spawnType == MobSpawnType.SPAWN_EGG){
            variant = Variant.byId(this.randomVariant(0, 4));
        } else {
            variant = Variant.byId(this.randomVariant(0, 4));
        }
        setVariant(variant);

        return super.finalizeSpawn(world, instance, spawnType, groupData, tag);
    }

    public static boolean isMoonPhaseToSpawn(ServerLevelAccessor world) {
        if (world.getLevel().getMoonPhase() == 0 || world.getLevel().getMoonPhase() == 4) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isDarkEnoughToSpawn(ServerLevelAccessor world, BlockPos pos, RandomSource random) {
        if (world.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        } else {
            DimensionType dimensiontype = world.dimensionType();
            int i = dimensiontype.monsterSpawnBlockLightLimit();
            if (i < 15 && world.getBrightness(LightLayer.BLOCK, pos) > i) {
                return false;
            } else {
                if (world.getLevel().isThundering()) {
                    return false;
                }
                int j = world.getMaxLocalRawBrightness(pos);
                return j <= dimensiontype.monsterSpawnLightTest().sample(random);
            }
        }

    }

    public boolean removeWhenFarAway(double p_34559_) {
        return !this.isPersistenceRequired();
    }
    //endregion


    //region Mountable
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
            if (this.isJumping()) {
                moveSpeed = 0.1;
            } else {
                if (this.isSprinting) {
                    moveSpeed = 0.2;
                } else
                moveSpeed = 0.05;
            }
            this.setDeltaMovement(this.getDeltaMovement().add(moveX * moveSpeed, 0, moveZ * moveSpeed));
        } else if (Minecraft.getInstance().options.keySprint.isDown()){
            moveSpeed = 0.8;
            this.setIsSprinting(true);
            this.setDeltaMovement(this.getDeltaMovement().add(moveX * moveSpeed, 0, moveZ * moveSpeed));
        } else {
            moveSpeed = 0.2;
            this.setDeltaMovement(this.getDeltaMovement().add(moveX * moveSpeed, 0, moveZ * moveSpeed));
            if (!Minecraft.getInstance().options.keySprint.isDown() && this.isSprinting) {
                this.setIsSprinting(false);
            }
        }

        if (this.onGround()) {
            this.setIsJumping(false);
            if (this.playerJump() && !this.isJumping()) {
                this.executeRidersJump(0.7D);
            }
            this.setPlayerJump(false);
        }

        if (this.wasTouchingWater) {
            this.setIsJumping(false);
            if (!this.isJumping() && this.isUnderWater() && getRandom().nextFloat() < 0.8F) {
                this.executeRidersJump(0.2D);
            }
            if (this.playerJump() && this.isInWater()) {
                this.executeRidersJump(0.4D);
            }
            this.setPlayerJump(false);
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
        double strafeX = -Math.sin((-riddenRotation.y - 90) * (Math.PI / 180.0));
        double strafeZ = -Math.cos((-riddenRotation.y - 90) * (Math.PI / 180.0));

        double moveX = forwardX * forwardInput + strafeX * strafeInput;
        double moveZ = forwardZ * forwardInput + strafeZ * strafeInput;

        return new Vec3(moveX, 0, moveZ);
    }

    public void executeRidersJump(Double jumpVec) {
        Vec3 vec3 = this.getDeltaMovement();
        this.setIsJumping(true);
        this.hasImpulse = true;
        net.minecraftforge.common.ForgeHooks.onLivingJump(this);

        this.setDeltaMovement(vec3.x, jumpVec, vec3.z);
    }

    @Override
    public void onPlayerJump(int power) {
        if (power > 0) {
            this.setPlayerJump(true);
        }
    }

    @Override
    public boolean canJump() {
        return this.hasControllingPassenger();
    }

    @Override
    public void handleStartJump(int jump) {
        this.playJumpSound();
    }

    @Override
    public void handleStopJump() {
    }
    //endregion

    
    //region Damage
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

    public boolean causeFallDamage(float entityHeight, float damageHeight, DamageSource fallDamage) {
        if (entityHeight > 1.5F) {
            this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
        }

        int i = this.calculateFallDamage(entityHeight, damageHeight);
        if (i <= 0) {
            return false;
        } else {
            this.hurt(fallDamage, (float)i);
            if (this.isVehicle()) {
                for(Entity entity : this.getIndirectPassengers()) {
                    entity.hurt(fallDamage, (float)i);
                }
            }

            this.playBlockFallSound();
            return true;
        }
    }

    protected int calculateFallDamage(float entity, float damage) {
        return Mth.ceil((entity * 0.5F - 4.0F) * damage);
    }
    //endregion


    //region NeutralMob
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
    //endregion


    //region Animations
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    public final AnimationState sitAnimationState = new AnimationState();
    private int sitAnimationTimeout = 0;
    public final AnimationState attackAnimationState = new AnimationState();
    public int attackAnimationTimeout = 0;
    public final AnimationState angryAnimationState = new AnimationState();

    public final AnimationState scratchIdleState = new AnimationState();
    private int scratchIdleTimeout = 0;
    public final AnimationState laydownIdleState = new AnimationState();
    private int laydownIdleTimeout = 0;

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            setupAnimationStates();
        }
    }

    private void setupAnimationStates() {
        float f = random.nextFloat();

        if(this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 51;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        if(this.isInSittingPose()) {
            this.idleAnimationState.stop();
            if (this.sitAnimationTimeout <= 0) {
                this.sitAnimationTimeout = 79;
                this.sitAnimationState.start(this.tickCount);
            } else {
                --this.sitAnimationTimeout;
            }
        } else {
            this.sitAnimationState.stop();
            this.scratchIdleState.stop();
            this.laydownIdleState.stop();
        }

        if (this.sitAnimationState.isStarted() && !this.laydownIdleState.isStarted()) {
            if (f <= 0.1f && !this.scratchIdleState.isStarted()) {
                this.scratchIdleTimeout = 160;
                this.scratchIdleState.start(this.tickCount);
            } else if (this.scratchIdleTimeout >= 0) {
                --this.scratchIdleTimeout;
            } else {
                this.scratchIdleState.stop();
            }
        }

        if (this.sitAnimationState.isStarted() && !this.scratchIdleState.isStarted()) {
            if (f <= 1.0f && !this.laydownIdleState.isStarted() && this.sitAnimationTimeout <= 0) {
                this.laydownIdleTimeout  = 480;
                this.laydownIdleState.start(this.tickCount);
            } else if (this.laydownIdleTimeout >= 0) {
                --this.laydownIdleTimeout;
            } else {
                this.laydownIdleState.stop();
            }
        }

        if (this.isAngry() || this.isAttacking()) {
            this.angryAnimationState.start(this.tickCount);
        } else {
            this.angryAnimationState.stop();
        }

        if(this.isAttacking()) {
            if (this.attackAnimationTimeout <= 0) {
                this.attackAnimationTimeout = 10;
                this.attackAnimationState.start(this.tickCount);
            } else {
                --this.attackAnimationTimeout;
            }
        } else {
            this.attackAnimationState.stop();
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
    //endregion


    //region Sounds
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    protected void playJumpSound() {
        this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
    }

    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.73F;
    }
    //endregion
}
