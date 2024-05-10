package coolrex.direwolves;

import com.mojang.logging.LogUtils;
import coolrex.direwolves.client.DirewolfModel;
import coolrex.direwolves.client.DirewolfRenderer;
import coolrex.direwolves.common.DirewolfEntity;
import coolrex.direwolves.registry.DirewolvesEntities;
import coolrex.direwolves.registry.DirewolvesItems;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import static coolrex.direwolves.registry.DirewolvesItems.DIREWOLF_SPAWN_EGG;

@Mod(Direwolves.MOD_ID)
public class Direwolves {
    public static final String MOD_ID = "direwolves";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Direwolves()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityAttributes);

        MinecraftForge.EVENT_BUS.register(this);
        DirewolvesEntities.ENTITIES.register(modEventBus);
        DirewolvesItems.ITEMS.register(modEventBus);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS)
            event.accept(DIREWOLF_SPAWN_EGG);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(DirewolvesEntities.DIREWOLF.get(), DirewolfEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(DirewolvesEntities.DIREWOLF.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.WORLD_SURFACE_WG, DirewolfEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.AND);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(DirewolfModel.DIREWOLF_LAYER, DirewolfModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(DirewolvesEntities.DIREWOLF.get(), DirewolfRenderer::new);

        }
    }

}
