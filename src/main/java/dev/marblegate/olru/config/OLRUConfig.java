package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OLRUConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final LegacyPrimeGauntletConfig LEGACY_PRIME;
    public static final HorusGauntletConfig HORUS;
    public static final FinalAnswerGauntletConfig FINAL_ANSWER;
    public static final AxiomGauntletConfig THE_AXIOM;

    static {
        BUILDER.push("legacy_prime").comment("Configuration for Legacy Prime");
        LEGACY_PRIME = new LegacyPrimeGauntletConfig(BUILDER);
        BUILDER.pop();

        BUILDER.push("legacy_of_horus").comment("Configuration for Legacy of Horus");
        HORUS = new HorusGauntletConfig(BUILDER);
        BUILDER.pop();

        BUILDER.push("final_answer").comment("Configuration for The Final Answer");
        FINAL_ANSWER = new FinalAnswerGauntletConfig(BUILDER);
        BUILDER.pop();

        BUILDER.push("the_axiom").comment("Configuration for The Axiom");
        THE_AXIOM = new AxiomGauntletConfig(BUILDER);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
