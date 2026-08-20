package net.execheinz.upgrader;

import net.execheinz.upgrader.client.ClientBootstrap;
import net.execheinz.upgrader.network.ModNetwork;
import net.execheinz.upgrader.registry.ModItems;
import net.execheinz.upgrader.registry.ModMenus;
import net.execheinz.upgrader.registry.ModSounds;
import net.execheinz.upgrader.server.AttemptManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;

@Mod(UpgraderMod.MOD_ID)
public final class UpgraderMod {
    public static final String MOD_ID = "upgrader";

    public UpgraderMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        modBus.addListener(this::addCreativeTab);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UpgraderConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(AttemptManager.INSTANCE);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::register);
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(ModItems.UPGRADER);
    }
}
