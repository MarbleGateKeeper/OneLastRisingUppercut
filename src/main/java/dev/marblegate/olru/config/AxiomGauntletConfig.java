package dev.marblegate.olru.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AxiomGauntletConfig {
    public final HyperspheresConfig HYPERSPHERES;
    public final ExperimentalBarrierConfig EXPERIMENTAL_BARRIER;
    public final KineticGraspConfig KINETIC_GRASP;
    public final AccretionConfig ACCRETION;
    public final GraviticFluxConfig GRAVITIC_FLUX;

    AxiomGauntletConfig(ModConfigSpec.Builder builder) {
        builder.push("hyperspheres").comment("Hyperspheres - paired gravity spheres that bounce once and implode");
        HYPERSPHERES = new HyperspheresConfig(builder);
        builder.pop();

        builder.push("experimental_barrier").comment("Experimental Barrier - floating projectile-absorbing barrier");
        EXPERIMENTAL_BARRIER = new ExperimentalBarrierConfig(builder);
        builder.pop();

        builder.push("kinetic_grasp").comment("Kinetic Grasp - frontal cone that absorbs projectiles into a temporary shield");
        KINETIC_GRASP = new KineticGraspConfig(builder);
        builder.pop();

        builder.push("accretion").comment("Accretion - hurled debris boulder that damages and knocks down enemies");
        ACCRETION = new AccretionConfig(builder);
        builder.pop();

        builder.push("gravitic_flux").comment("Gravitic Flux - lifts enemies in a zone, suspends them, then slams them down");
        GRAVITIC_FLUX = new GraviticFluxConfig(builder);
        builder.pop();
    }
}
