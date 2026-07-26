package dev.marblegate.olru.common.item.tooltip;

import dev.marblegate.olru.config.OLRUConfig;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GauntletTooltipHelper {
    private static final ChatFormatting PRIME_ACCENT = ChatFormatting.GOLD;
    private static final ChatFormatting HORUS_ACCENT = ChatFormatting.AQUA;
    private static final ChatFormatting FINAL_ANSWER_ACCENT = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting AXIOM_ACCENT = ChatFormatting.DARK_PURPLE;
    private static final ChatFormatting LABEL = ChatFormatting.DARK_AQUA;
    private static final ChatFormatting BODY = ChatFormatting.GRAY;
    private static final ChatFormatting MUTED = ChatFormatting.DARK_GRAY;
    private static final ChatFormatting KEY = ChatFormatting.YELLOW;

    public static void appendLegacyPrime(Consumer<Component> tooltip, boolean expanded) {
        var handCannon = OLRUConfig.LEGACY_PRIME.HAND_CANNON;
        var rocketPunch = OLRUConfig.LEGACY_PRIME.ROCKET_PUNCH;
        var risingUppercut = OLRUConfig.LEGACY_PRIME.RISING_UPPERCUT;
        var seismicSlam = OLRUConfig.LEGACY_PRIME.SEISMIC_SLAM;
        var meteorStrike = OLRUConfig.LEGACY_PRIME.METEOR_STRIKE;

        appendHeader(tooltip, "tooltip.olru.legacy_prime.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip, "LMB", "skill.olru.legacy_prime.normal_attack", PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.normal_attack.info",
                        handCannon.maxCharges.get(), sec(handCannon.cooldownTicks.get()),
                        number(handCannon.range.get()), number(handCannon.damage.get())));
        appendSkill(tooltip, "RMB", "skill.olru.legacy_prime.skill_one", PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_one.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_one.info",
                        sec(rocketPunch.cooldownTicks.get()), sec(rocketPunch.maxChargeTicks.get()),
                        number(rocketPunch.maxLaunchDistance.get()),
                        number(rocketPunch.mobDamageMin.get()), number(rocketPunch.mobDamageMax.get()),
                        number(rocketPunch.wallBonusDamage.get())));
        appendSkill(tooltip, "Sft", "skill.olru.legacy_prime.skill_two", PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_two.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_two.info",
                        sec(risingUppercut.cooldownTicks.get()), number(risingUppercut.riseHeight.get()),
                        number(risingUppercut.mobDamage.get()), number(risingUppercut.frontConeRange.get()),
                        number(risingUppercut.frontConeAngleDegrees.get())));
        appendSkill(tooltip, "V", "skill.olru.legacy_prime.skill_three", PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_three.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_three.info",
                        sec(seismicSlam.cooldownTicks.get()), number(seismicSlam.impactRange.get()),
                        number(seismicSlam.impactConeAngleDegrees.get()),
                        number(seismicSlam.minDamage.get()), number(seismicSlam.maxDamage.get()),
                        sec(seismicSlam.slowTicks.get())));
        appendSkill(tooltip, "X", "skill.olru.legacy_prime.ultimate", PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.ultimate.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.ultimate.info",
                        number(meteorStrike.chargePercentPerDamage.get()), sec(meteorStrike.hoverTicks.get()),
                        number(meteorStrike.innerRadius.get()), number(meteorStrike.outerRadius.get()),
                        number(meteorStrike.outerDamageMax.get()), number(meteorStrike.outerDamageMin.get())));
    }

    public static void appendLegacyOfHorus(Consumer<Component> tooltip, boolean expanded) {
        var bioticRound = OLRUConfig.HORUS.BIOTIC_ROUND;
        var fieldExtraction = OLRUConfig.HORUS.FIELD_EXTRACTION;
        var sedativeDart = OLRUConfig.HORUS.SEDATIVE_DART;
        var bioticGrenade = OLRUConfig.HORUS.BIOTIC_GRENADE;
        var nanoSurge = OLRUConfig.HORUS.NANO_SURGE;

        appendHeader(tooltip, "tooltip.olru.legacy_of_horus.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip, "LMB", "skill.olru.legacy_of_horus.normal_attack", HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.normal_attack.info",
                        bioticRound.maxCharges.get(), sec(bioticRound.cooldownTicks.get()),
                        number(bioticRound.effectiveRange.get()), number(bioticRound.healAmount.get()),
                        number(bioticRound.damage.get())));
        appendSkill(tooltip, "RMB", "skill.olru.legacy_of_horus.skill_one", HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.info",
                        sec(fieldExtraction.cooldownTicks.get()), sec(fieldExtraction.maxChargeTicks.get()),
                        number(fieldExtraction.allyLockRange.get()), number(fieldExtraction.healAmount.get()),
                        number(fieldExtraction.selfDashDistance.get())));
        appendSkill(tooltip, "Sft", "skill.olru.legacy_of_horus.skill_two", HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_two.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_two.info",
                        sec(sedativeDart.cooldownTicks.get()), number(sedativeDart.range.get()),
                        number(sedativeDart.damage.get()), sec(sedativeDart.sleepTicks.get()),
                        sec(sedativeDart.bossSleepTicks.get())));
        appendSkill(tooltip, "V", "skill.olru.legacy_of_horus.skill_three", HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_three.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_three.info",
                        sec(bioticGrenade.cooldownTicks.get()), number(bioticGrenade.explosionRadius.get()),
                        number(bioticGrenade.healAmount.get()), sec(bioticGrenade.regenerationTicks.get()),
                        number(bioticGrenade.damage.get())));
        appendSkill(tooltip, "X", "skill.olru.legacy_of_horus.ultimate", HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.ultimate.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.ultimate.info",
                        number(nanoSurge.chargePercentPerDamage.get()), number(nanoSurge.chargePercentPerHealing.get()),
                        number(nanoSurge.range.get()), sec(nanoSurge.buffTicks.get()),
                        number(nanoSurge.healAmount.get()), number(nanoSurge.emergencyHealAmount.get())));
    }

    public static void appendFinalAnswer(Consumer<Component> tooltip, boolean expanded) {
        var bioticGrasp = OLRUConfig.FINAL_ANSWER.BIOTIC_GRASP;
        var bioticSpray = OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY;
        var fade = OLRUConfig.FINAL_ANSWER.FADE;
        var bioticOrb = OLRUConfig.FINAL_ANSWER.BIOTIC_ORB;
        var coalescence = OLRUConfig.FINAL_ANSWER.COALESCENCE;
        appendHeader(tooltip, "tooltip.olru.final_answer.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip, "LMB", "skill.olru.final_answer.normal_attack", FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.final_answer.normal_attack.info",
                        percent(bioticSpray.energyDrainPerTick.get() * 20.0),
                        number(bioticSpray.range.get()), number(bioticSpray.coneAngleDegrees.get()),
                        number(bioticSpray.healPerTick.get() * bioticSpray.pulseIntervalTicks.get()),
                        sec(bioticSpray.lingerTicks.get())));
        appendSkill(tooltip, "RMB", "skill.olru.final_answer.skill_one", FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_one.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_one.info",
                        number(bioticGrasp.range.get()), number(bioticGrasp.coneAngleDegrees.get()),
                        number(bioticGrasp.damage.get()), number(bioticGrasp.selfHeal.get()),
                        percent(bioticGrasp.energyPerHit.get())));
        appendSkill(tooltip, "Sft", "skill.olru.final_answer.skill_two", FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_two.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_two.info",
                        sec(fade.cooldownTicks.get()), sec(fade.durationTicks.get()),
                        number(fade.speedAmplifier.get()), number(fade.jumpBoostAmplifier.get())));
        appendSkill(tooltip, "V", "skill.olru.final_answer.skill_three", FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_three.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_three.info",
                        sec(bioticOrb.cooldownTicks.get()), number(bioticOrb.radius.get()),
                        number(bioticOrb.damagePerPulse.get()), number(bioticOrb.damagePool.get()),
                        sec(bioticOrb.lifeTicks.get())));
        appendSkill(tooltip, "X", "skill.olru.final_answer.ultimate", FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.ultimate.mechanic"),
                Component.translatable("tooltip.olru.final_answer.ultimate.info",
                        number(coalescence.chargePercentPerDamage.get()), number(bioticSpray.ultChargePercentPerHeal.get()),
                        number(coalescence.length.get()), sec(coalescence.durationTicks.get()),
                        number(coalescence.enemyDamagePerPulse.get()),
                        number(coalescence.allyHealPerPulse.get()), number(coalescence.selfHealPerPulse.get())));
    }

    public static void appendTheAxiom(Consumer<Component> tooltip, boolean expanded) {
        var hyperspheres = OLRUConfig.THE_AXIOM.HYPERSPHERES;
        var barrier = OLRUConfig.THE_AXIOM.EXPERIMENTAL_BARRIER;
        var grasp = OLRUConfig.THE_AXIOM.KINETIC_GRASP;
        var accretion = OLRUConfig.THE_AXIOM.ACCRETION;
        var flux = OLRUConfig.THE_AXIOM.GRAVITIC_FLUX;

        appendHeader(tooltip, "tooltip.olru.the_axiom.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip, "LMB", "skill.olru.the_axiom.normal_attack", AXIOM_ACCENT,
                Component.translatable("tooltip.olru.the_axiom.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.the_axiom.normal_attack.info",
                        hyperspheres.maxCharges.get(), sec(hyperspheres.cooldownTicks.get()),
                        number(hyperspheres.range.get()), number(hyperspheres.directDamage.get()),
                        number(hyperspheres.implosionDamage.get()), number(hyperspheres.implosionRadius.get())));
        appendSkill(tooltip, "RMB", "skill.olru.the_axiom.skill_one", AXIOM_ACCENT,
                Component.translatable("tooltip.olru.the_axiom.skill_one.mechanic"),
                Component.translatable("tooltip.olru.the_axiom.skill_one.info",
                        number(barrier.maxDurability.get()), number(barrier.width.get()), number(barrier.height.get()),
                        number(barrier.minDeployDistance.get()), number(barrier.maxDeployDistance.get()),
                        number(barrier.durabilityRegenPerSecond.get())));
        appendSkill(tooltip, "Sft", "skill.olru.the_axiom.skill_two", AXIOM_ACCENT,
                Component.translatable("tooltip.olru.the_axiom.skill_two.mechanic"),
                Component.translatable("tooltip.olru.the_axiom.skill_two.info",
                        sec(grasp.cooldownTicks.get()), sec(grasp.durationTicks.get()),
                        number(grasp.range.get()), number(grasp.coneAngleDegrees.get()),
                        number(grasp.maxShield.get())));
        appendSkill(tooltip, "V", "skill.olru.the_axiom.skill_three", AXIOM_ACCENT,
                Component.translatable("tooltip.olru.the_axiom.skill_three.mechanic"),
                Component.translatable("tooltip.olru.the_axiom.skill_three.info",
                        sec(accretion.cooldownTicks.get()), number(accretion.damage.get()),
                        number(accretion.splashDamage.get()), number(accretion.splashRadius.get()),
                        sec(accretion.knockdownTicks.get())));
        appendSkill(tooltip, "X", "skill.olru.the_axiom.ultimate", AXIOM_ACCENT,
                Component.translatable("tooltip.olru.the_axiom.ultimate.mechanic"),
                Component.translatable("tooltip.olru.the_axiom.ultimate.info",
                        number(flux.chargePercentPerDamage.get()), number(flux.zoneRadius.get()),
                        number(flux.liftHeight.get()), percent(flux.slamMaxHealthFraction.get())));
    }

    private static void appendHeader(Consumer<Component> tooltip, String styleKey, boolean expanded) {
        tooltip.accept(CommonComponents.EMPTY);
        tooltip.accept(Component.literal("  ")
                .append(Component.translatable("tooltip.olru.gauntlet.style").withStyle(LABEL, ChatFormatting.BOLD))
                .append(Component.literal(": ").withStyle(MUTED))
                .append(Component.translatable(styleKey).withStyle(BODY)));
        if (!expanded) {
            tooltip.accept(Component.literal("  [Shift] ").withStyle(KEY)
                    .append(Component.translatable("tooltip.olru.gauntlet.hold_shift")
                            .withStyle(BODY)));
        }
    }

    private static void appendSkill(Consumer<Component> tooltip, String keyLabel, String skillNameKey,
            ChatFormatting accent, Component mechanic, Component info) {
        tooltip.accept(CommonComponents.EMPTY);
        tooltip.accept(bar(accent)
                .append(Component.literal("[").withStyle(MUTED))
                .append(Component.literal(keyLabel).withStyle(KEY, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(MUTED))
                .append(Component.translatable(skillNameKey).withStyle(accent, ChatFormatting.BOLD)));
        tooltip.accept(bar(accent).append(mechanic).withStyle(BODY));
        tooltip.accept(bar(accent).append(info).withStyle(BODY));
    }

    private static MutableComponent bar(ChatFormatting accent) {
        return Component.literal("▎").withStyle(accent);
    }

    private static String sec(int ticks) {
        return number(ticks / 20.0);
    }

    private static String percent(double fraction) {
        return number(fraction * 100.0) + "%";
    }

    private static String number(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 1.0E-4) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
