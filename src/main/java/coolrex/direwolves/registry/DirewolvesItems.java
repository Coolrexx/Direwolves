package coolrex.direwolves.registry;

import coolrex.direwolves.Direwolves;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DirewolvesItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Direwolves.MOD_ID);

    public static final RegistryObject<Item> DIREWOLF_SPAWN_EGG = ITEMS.register("direwolf_spawn_egg", () -> new ForgeSpawnEggItem(DirewolvesEntities.DIREWOLF, 0x330e04, 0xc9a983, new Item.Properties()));

}
