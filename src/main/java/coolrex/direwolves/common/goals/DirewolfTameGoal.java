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
                    return this.direwolf.distanceTo(foodItem) < 5.0D;
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
        this.direwolf.getLookControl().setLookAt(foodItem.getX(), foodItem.getEyeY(), foodItem.getZ());
        animDelay = 160;
    }

    public void stop() {
        this.direwolf.getNavigation().stop();
        if (this.direwolf.isEating()) {
            this.direwolf.setEating(false);
        }
    }

    public void tick() {
        if (this.foodItem == null) return;
        Player player = (Player) foodItem.getOwner();

        if (this.direwolf.distanceTo(foodItem) < 1.5D) {
            this.direwolf.setEating(true);
            --animDelay;
            if (animDelay <= 0) {
                int remainingItems = foodItem.getItem().getCount();
                if (remainingItems > 1) {
                    foodItem.getItem().shrink(1);
                } else {
                    foodItem.discard();
                }

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
