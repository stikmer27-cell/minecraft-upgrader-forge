package net.execheinz.upgrader.registry;

import net.execheinz.upgrader.UpgraderMod;
import net.execheinz.upgrader.item.UpgraderItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, UpgraderMod.MOD_ID);
    public static final RegistryObject<Item> UPGRADER = REGISTER.register("upgrader", () -> new UpgraderItem(new Item.Properties().stacksTo(1)));
    private ModItems() {}
}
