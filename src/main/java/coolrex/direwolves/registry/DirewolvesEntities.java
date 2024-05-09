package coolrex.direwolves.registry;

import coolrex.direwolves.Direwolves;
import coolrex.direwolves.common.DirewolfEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DirewolvesEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Direwolves.MOD_ID);

    public static final RegistryObject<EntityType<DirewolfEntity>> DIREWOLF = ENTITIES.register("direwolf", () -> EntityType.Builder.of(DirewolfEntity::new, MobCategory.CREATURE).sized(1.5f, 1.5f).build("direwolf"));
}
