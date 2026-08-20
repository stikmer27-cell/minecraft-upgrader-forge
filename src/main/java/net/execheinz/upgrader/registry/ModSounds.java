package net.execheinz.upgrader.registry;

import net.execheinz.upgrader.UpgraderMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UpgraderMod.MOD_ID);
    public static final RegistryObject<SoundEvent> WHEEL_START = register("wheel_start");
    public static final RegistryObject<SoundEvent> WHEEL_TICK = register("wheel_tick");
    public static final RegistryObject<SoundEvent> UPGRADE_SUCCESS = register("upgrade_success");
    public static final RegistryObject<SoundEvent> UPGRADE_FAILURE = register("upgrade_failure");
    public static final RegistryObject<SoundEvent> UI_CLICK = register("ui_click");
    public static final RegistryObject<SoundEvent> KEY_TAP = register("key_tap");
    public static final RegistryObject<SoundEvent> ITEM_SELECT = register("item_select");
    public static final RegistryObject<SoundEvent> BET_CONFIRM = register("bet_confirm");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(UpgraderMod.MOD_ID, name);
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {}
}
