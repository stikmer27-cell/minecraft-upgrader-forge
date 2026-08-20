package net.execheinz.upgrader.client;

import net.execheinz.upgrader.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientBootstrap {
    private ClientBootstrap() {}

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ClientBootstrap::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ModMenus.UPGRADER.get(), UpgraderScreen::new));
    }
}
