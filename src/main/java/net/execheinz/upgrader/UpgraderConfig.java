package net.execheinz.upgrader;

import java.util.List;
import net.execheinz.upgrader.logic.ResultAmountPolicy;
import net.minecraftforge.common.ForgeConfigSpec;

public final class UpgraderConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue PLAYER_FACTOR = BUILDER.comment("Small player advantage used by the server-side odds formula.").defineInRange("playerFactor", 1.03D, 0.5D, 1.25D);
    public static final ForgeConfigSpec.DoubleValue MIN_CHANCE = BUILDER.defineInRange("minChance", 0.005D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue MAX_CHANCE = BUILDER.defineInRange("maxChance", 0.98D, 0.01D, 1.0D);
    public static final ForgeConfigSpec.IntValue SPIN_TICKS = BUILDER.defineInRange("spinTicks", 88, 80, 120);
    public static final ForgeConfigSpec.IntValue MAX_RESULT_AMOUNT = BUILDER.defineInRange("maxResultAmount", 64, 1, ResultAmountPolicy.HARD_MAX);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER.defineListAllowEmpty("blacklist", List.of(), value -> value instanceof String);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TARGET_ALLOWLIST = BUILDER.comment("Survival target IDs that should override the automatic technical-item filter.").defineListAllowEmpty("targetAllowlist", List.of(), value -> value instanceof String);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_OVERRIDES = BUILDER.comment("Entries use namespace:item=value.").defineListAllowEmpty("valueOverrides", List.of(), value -> value instanceof String);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private UpgraderConfig() {}
}
