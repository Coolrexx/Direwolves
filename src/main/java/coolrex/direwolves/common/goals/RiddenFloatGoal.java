package coolrex.direwolves.common.goals;

import coolrex.direwolves.common.DirewolfEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraftforge.common.ForgeMod;

public class RiddenFloatGoal extends FloatGoal {
    private final DirewolfEntity mob;

    public RiddenFloatGoal(Mob mob) {
        super(mob);
        this.mob = ((DirewolfEntity)mob);
    }

    public boolean canUse() {
        return this.mob.isVehicle() && this.mob.isInWater() && this.mob.getFluidTypeHeight(ForgeMod.WATER_TYPE.get()) > this.mob.getEyeHeight();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.mob.getRandom().nextFloat() < 0.8F) {
            this.mob.executeRidersJump(0.2D);
        }
    }
}
