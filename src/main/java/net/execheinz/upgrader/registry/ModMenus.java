package net.execheinz.upgrader.registry;

import net.execheinz.upgrader.UpgraderMod;
import net.execheinz.upgrader.menu.UpgraderMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, UpgraderMod.MOD_ID);
    public static final RegistryObject<MenuType<UpgraderMenu>> UPGRADER = REGISTER.register("upgrader", () -> IForgeMenuType.create((id, inventory, data) -> new UpgraderMenu(id, inventory)));
    private ModMenus() {}
}
