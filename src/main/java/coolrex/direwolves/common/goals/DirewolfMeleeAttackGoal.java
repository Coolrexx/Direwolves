package coolrex.direwolves.common.goals;

import coolrex.direwolves.common.DirewolfEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class DirewolfMeleeAttackGoal extends MeleeAttackGoal {
    private final DirewolfEntity entity;
    private int attackDelay = 20;
    private int ticksUntilNextAttack = 20;
    private boolean shouldCountTillNextAttack = false;

    public DirewolfMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followTarget) {
        super(mob, speedModifier, followTarget);
        entity = ((DirewolfEntity)mob);
    }

    @Override
    public void start() {
        super.start();
        attackDelay = 20;
        ticksUntilNextAttack = 20;
    }


    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTarget) {
        if (targetInAttackDist(target, distToTarget)) {
            shouldCountTillNextAttack = true;

            if(isTimeToStartAttackAnimation()) {
                entity.setAttacking(true);
            }

            if(isTimeToAttack()) {
                this.mob.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
                performAttack(target);
                entity.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.6F, 1.0F);
            }
        } else {
            resetAttackCooldown();
            shouldCountTillNextAttack = false;
            entity.setAttacking(false);
            entity.attackAnimationTimeout = 0;
        }
    }

    private boolean targetInAttackDist(LivingEntity target, double distToTarget) {
        return distToTarget <= this.getAttackReachSqr(target);
    }

    protected double getAttackReachSqr(LivingEntity target) {
        return (double)(this.mob.getBbWidth() * 4.0F * this.mob.getBbWidth() * 4.0F + target.getBbWidth());
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(attackDelay * 2);
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected boolean isTimeToStartAttackAnimation() {
        return this.ticksUntilNextAttack <= attackDelay;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }


    protected void performAttack(LivingEntity target) {
        this.resetAttackCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.doHurtTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if(shouldCountTillNextAttack) {
            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        }
    }

    @Override
    public void stop() {
        entity.setAttacking(false);
        super.stop();
    }

}