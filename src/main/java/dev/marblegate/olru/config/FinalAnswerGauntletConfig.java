package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FinalAnswerGauntletConfig {
    public final BioticGraspConfig BIOTIC_GRASP;
    public final BioticSprayConfig BIOTIC_SPRAY;
    public final FadeConfig FADE;
    public final BioticOrbConfig BIOTIC_ORB;
    public final CoalescenceConfig COALESCENCE;

    FinalAnswerGauntletConfig(ModConfigSpec.Builder builder) {
        builder.push("biotic_grasp").comment("Biotic Grasp - life-draining short cone attack that builds biotic energy");
        BIOTIC_GRASP = new BioticGraspConfig(builder);
        builder.pop();

        builder.push("biotic_spray").comment("Biotic Spray - channeled biotic energy cone that heals allies");
        BIOTIC_SPRAY = new BioticSprayConfig(builder);
        builder.pop();

        builder.push("fade").comment("Fade - brief untargetable burst of speed and invisibility");
        FADE = new FadeConfig(builder);
        builder.pop();

        builder.push("biotic_orb").comment("Biotic Orb - slow bouncing sphere that pulses damage to nearby hostiles");
        BIOTIC_ORB = new BioticOrbConfig(builder);
        builder.pop();

        builder.push("coalescence").comment("Coalescence - channeled long-range beam that heals allies and damages enemies");
        COALESCENCE = new CoalescenceConfig(builder);
        builder.pop();
    }
}
