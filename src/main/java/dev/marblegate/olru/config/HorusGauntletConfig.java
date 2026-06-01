package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class HorusGauntletConfig {
    public final BioticRoundConfig BIOTIC_ROUND;
    public final FieldExtractionConfig FIELD_EXTRACTION;
    public final SedativeDartConfig SEDATIVE_DART;
    public final BioticGrenadeConfig BIOTIC_GRENADE;
    public final NanoSurgeConfig NANO_SURGE;

    HorusGauntletConfig(ModConfigSpec.Builder builder) {
        builder.push("biotic_round").comment("Biotic Round - light heal or light damage shot");
        BIOTIC_ROUND = new BioticRoundConfig(builder);
        builder.pop();

        builder.push("field_extraction").comment("Field Extraction - charged ally pull or self tactical reposition");
        FIELD_EXTRACTION = new FieldExtractionConfig(builder);
        builder.pop();

        builder.push("sedative_dart").comment("Sedative Dart - interrupt and briefly suppress hostile targets");
        SEDATIVE_DART = new SedativeDartConfig(builder);
        builder.pop();

        builder.push("biotic_grenade").comment("Biotic Grenade - thrown biotic splash heal and damage projectile");
        BIOTIC_GRENADE = new BioticGrenadeConfig(builder);
        builder.pop();

        builder.push("nano_surge").comment("Nano Surge - team heal and short combat stimulant");
        NANO_SURGE = new NanoSurgeConfig(builder);
        builder.pop();
    }
}
