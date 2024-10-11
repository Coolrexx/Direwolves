package coolrex.direwolves.common.goals;

import coolrex.direwolves.common.DirewolfEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class DirewolfTameGoal extends Goal {
    /*TODO:
     fine tune stopping at food (pretty good as is though)
     fix animation timing mismatch (food should be consumed when animation ends or maybe a bit before)
     make direwolf eat 1 item at a time
     add cooldown for eating
     add particles to indicate success/fail & eating item

     multiple items around direwolf breaks its pathing after its eaten one (potentially before too?), may be fixed with cooldown implementation
     direwolf gets stuck eating if an item is just outside eating radius but close enough to trigger
     hates jumping when trying to path to an item for some reason
     */
    private final DirewolfEntity direwolf;
    private final double speedTowardsTarget;
    private Path path;
    private ItemEntity foodItem;
    private int animDelay = 160;

    public DirewolfTameGoal(DirewolfEntity direwolf, double speed) {
        this.direwolf = direwolf;
        this.speedTowardsTarget = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.direwolf.isTame()) {
            return false;
        } else {
            List<ItemEntity> nearbyItems = this.direwolf.level().getEntitiesOfClass(
                    ItemEntity.class,
                    this.direwolf.getBoundingBox().inflate(7.0D),
                    this::isFood
            );

            if (!nearbyItems.isEmpty()) {
                this.foodItem = nearbyItems.get(0);
            } else {
                return false;
            }

            if (this.foodItem.getOwner() instanceof Player) {
                this.path = this.direwolf.getNavigation().createPath(foodItem.blockPosition(), 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.direwolf.distanceToSqr(this.foodItem) <= 7.0;
                }
            } else {
                return false;
            }
        }
    }

    public boolean isFood(ItemEntity item) {
        return item.getItem().getItem().isEdible() && item.getItem().getFoodProperties(direwolf).isMeat();
    }

    public void start() {
        this.direwolf.getNavigation().moveTo(this.path, this.speedTowardsTarget);
        this.direwolf.getLookControl().setLookAt(this.foodItem);
        animDelay = 160;
    }

    public void stop() {
        this.direwolf.getNavigation().stop();
        if (this.direwolf.isEating()) {
            this.direwolf.setEating(false);
        }
    }

    public void tick() {
        if (this.foodItem == null || !this.foodItem.isAlive()) return;
        Player player = (Player) foodItem.getOwner();

        if (this.direwolf.distanceToSqr(this.foodItem) <= 2.0) {
            this.direwolf.getNavigation().stop();
            this.direwolf.setEating(true);
            this.direwolf.getLookControl().setLookAt(foodItem.getX(), direwolf.getEyeY() - 0.2, foodItem.getZ());
            --animDelay;
            if (animDelay <= 0) {
                foodItem.getItem().shrink(1);
                this.direwolf.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);

                this.direwolf.setEating(false);
                this.attemptTaming(player);
            }
        }
    }


    public void attemptTaming(Player player) {
        Random random = new Random();
        int tamingChance = 10;

        if (random.nextInt(100) < tamingChance) {
            this.direwolf.tame(player);
            this.direwolf.playSound(SoundEvents.WOLF_WHINE, 1.0F, 1.0F);
        } else {
            this.direwolf.playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
        }
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }
}