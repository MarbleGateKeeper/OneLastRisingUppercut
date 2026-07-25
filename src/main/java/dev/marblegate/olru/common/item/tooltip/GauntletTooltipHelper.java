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
        appendSkill(tooltip,
                "LMB",
                "skill.olru.legacy_prime.normal_attack",
                PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.normal_attack.charge"),
                null,
                Component.translatable("tooltip.olru.legacy_prime.normal_attack.stats",
                        handCannon.maxCharges.get(), seconds(handCannon.cooldownTicks.get()),
                        blocks(handCannon.range.get()), number(handCannon.damage.get())));
        appendSkill(tooltip,
                "RMB",
                "skill.olru.legacy_prime.skill_one",
                PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_one.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_one.charge",
                        seconds(rocketPunch.cooldownTicks.get())),
                Component.translatable("tooltip.olru.legacy_prime.skill_one.charge_time",
                        seconds(rocketPunch.maxChargeTicks.get())),
                Component.translatable("tooltip.olru.legacy_prime.skill_one.stats",
                        blocks(rocketPunch.maxLaunchDistance.get()), number(rocketPunch.maxLaunchSpeed.get()),
                        number(rocketPunch.mobDamageMin.get()), number(rocketPunch.mobDamageMax.get()),
                        number(rocketPunch.wallBonusDamage.get())));
        appendSkill(tooltip,
                "Sft",
                "skill.olru.legacy_prime.skill_two",
                PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_two.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_two.charge",
                        seconds(risingUppercut.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.legacy_prime.skill_two.stats",
                        blocks(risingUppercut.riseHeight.get()), number(risingUppercut.riseSpeedMob.get()),
                        number(risingUppercut.mobDamage.get()), blocks(risingUppercut.frontConeRange.get()),
                        number(risingUppercut.frontConeAngleDegrees.get())));
        appendSkill(tooltip,
                "V",
                "skill.olru.legacy_prime.skill_three",
                PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.skill_three.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.skill_three.charge",
                        seconds(seismicSlam.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.legacy_prime.skill_three.stats",
                        blocks(seismicSlam.impactRange.get()), number(seismicSlam.impactConeAngleDegrees.get()),
                        number(seismicSlam.minDamage.get()), number(seismicSlam.maxDamage.get()),
                        seconds(seismicSlam.fullDamageAirTicks.get()), seconds(seismicSlam.slowTicks.get()),
                        number(seismicSlam.leapForwardSpeed.get()), number(seismicSlam.leapUpSpeed.get())));
        appendSkill(tooltip,
                "X",
                "skill.olru.legacy_prime.ultimate",
                PRIME_ACCENT,
                Component.translatable("tooltip.olru.legacy_prime.ultimate.mechanic"),
                Component.translatable("tooltip.olru.legacy_prime.ultimate.charge",
                        number(meteorStrike.chargePercentPerDamage.get())),
                Component.translatable("tooltip.olru.legacy_prime.ultimate.hold_time",
                        seconds(meteorStrike.hoverTicks.get())),
                Component.translatable("tooltip.olru.legacy_prime.ultimate.stats",
                        number(meteorStrike.fallSpeed.get()), blocks(meteorStrike.innerRadius.get()),
                        blocks(meteorStrike.outerRadius.get()), number(meteorStrike.outerDamageMax.get()),
                        number(meteorStrike.outerDamageMin.get())));
    }

    public static void appendLegacyOfHorus(Consumer<Component> tooltip, boolean expanded) {
        var bioticRound = OLRUConfig.HORUS.BIOTIC_ROUND;
        var fieldExtraction = OLRUConfig.HORUS.FIELD_EXTRACTION;
        var sedativeDart = OLRUConfig.HORUS.SEDATIVE_DART;
        var bioticGrenade = OLRUConfig.HORUS.BIOTIC_GRENADE;
        var nanoSurge = OLRUConfig.HORUS.NANO_SURGE;

        appendHeader(tooltip, "tooltip.olru.legacy_of_horus.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip,
                "LMB",
                "skill.olru.legacy_of_horus.normal_attack",
                HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.normal_attack.charge"),
                null,
                Component.translatable("tooltip.olru.legacy_of_horus.normal_attack.stats",
                        bioticRound.maxCharges.get(), seconds(bioticRound.cooldownTicks.get()),
                        blocks(bioticRound.effectiveRange.get()), number(bioticRound.healAmount.get()),
                        number(bioticRound.damage.get())));
        appendSkill(tooltip,
                "RMB",
                "skill.olru.legacy_of_horus.skill_one",
                HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.charge",
                        seconds(fieldExtraction.cooldownTicks.get())),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.charge_time",
                        seconds(fieldExtraction.maxChargeTicks.get())),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_one.stats",
                        blocks(fieldExtraction.allyLockRange.get()), number(fieldExtraction.allyConeAngleDegrees.get()),
                        number(fieldExtraction.pullSpeed.get()), blocks(fieldExtraction.selfDashDistance.get()),
                        number(fieldExtraction.healAmount.get()), seconds(fieldExtraction.protectionTicks.get())));
        appendSkill(tooltip,
                "Sft",
                "skill.olru.legacy_of_horus.skill_two",
                HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_two.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_two.charge",
                        seconds(sedativeDart.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_two.stats",
                        blocks(sedativeDart.range.get()), number(sedativeDart.damage.get()),
                        seconds(sedativeDart.sleepTicks.get()), seconds(sedativeDart.bossSleepTicks.get()),
                        number(sedativeDart.flyingDropSpeed.get())));
        appendSkill(tooltip,
                "V",
                "skill.olru.legacy_of_horus.skill_three",
                HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_three.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.skill_three.charge",
                        seconds(bioticGrenade.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.legacy_of_horus.skill_three.stats",
                        blocks(bioticGrenade.explosionRadius.get()), number(bioticGrenade.healAmount.get()),
                        seconds(bioticGrenade.regenerationTicks.get()), number(bioticGrenade.damage.get()),
                        number(bioticGrenade.throwSpeed.get())));
        appendSkill(tooltip,
                "X",
                "skill.olru.legacy_of_horus.ultimate",
                HORUS_ACCENT,
                Component.translatable("tooltip.olru.legacy_of_horus.ultimate.mechanic"),
                Component.translatable("tooltip.olru.legacy_of_horus.ultimate.charge",
                        number(nanoSurge.chargePercentPerDamage.get()), number(nanoSurge.chargePercentPerHealing.get())),
                null,
                Component.translatable("tooltip.olru.legacy_of_horus.ultimate.stats",
                        blocks(nanoSurge.range.get()), seconds(nanoSurge.buffTicks.get()),
                        number(nanoSurge.healAmount.get()), number(nanoSurge.emergencyHealAmount.get()),
                        percent(nanoSurge.emergencyHealthFraction.get()), multiplier(nanoSurge.healingMultiplier.get())));
    }

    public static void appendFinalAnswer(Consumer<Component> tooltip, boolean expanded) {
        var bioticGrasp = OLRUConfig.FINAL_ANSWER.BIOTIC_GRASP;
        var bioticSpray = OLRUConfig.FINAL_ANSWER.BIOTIC_SPRAY;
        var fade = OLRUConfig.FINAL_ANSWER.FADE;
        var bioticOrb = OLRUConfig.FINAL_ANSWER.BIOTIC_ORB;
        var coalescence = OLRUConfig.FINAL_ANSWER.COALESCENCE;

        appendHeader(tooltip, "tooltip.olru.final_answer.style", expanded);
        if (!expanded) return;
        appendSkill(tooltip,
                "LMB",
                "skill.olru.final_answer.skill_one",
                FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.normal_attack.mechanic"),
                Component.translatable("tooltip.olru.final_answer.normal_attack.charge",
                        percent(bioticSpray.energyDrainPerTick.get() * 20.0)),
                null,
                Component.translatable("tooltip.olru.final_answer.normal_attack.stats",
                        blocks(bioticSpray.range.get()), number(bioticSpray.coneAngleDegrees.get()),
                        number(bioticSpray.healPerTick.get()), seconds(bioticSpray.lingerTicks.get())));
        appendSkill(tooltip,
                "RMB",
                "skill.olru.final_answer.normal_attack",
                FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_one.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_one.charge"),
                null,
                Component.translatable("tooltip.olru.final_answer.skill_one.stats",
                        blocks(bioticGrasp.range.get()), number(bioticGrasp.coneAngleDegrees.get()),
                        number(bioticGrasp.damage.get()), number(bioticGrasp.selfHeal.get()),
                        percent(bioticGrasp.energyPerHit.get())));
        appendSkill(tooltip,
                "Sft",
                "skill.olru.final_answer.skill_two",
                FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_two.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_two.charge",
                        seconds(fade.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.final_answer.skill_two.stats",
                        seconds(fade.durationTicks.get()), number(fade.speedAmplifier.get()),
                        number(fade.jumpBoostAmplifier.get())));
        appendSkill(tooltip,
                "V",
                "skill.olru.final_answer.skill_three",
                FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.skill_three.mechanic"),
                Component.translatable("tooltip.olru.final_answer.skill_three.charge",
                        seconds(bioticOrb.cooldownTicks.get())),
                null,
                Component.translatable("tooltip.olru.final_answer.skill_three.stats",
                        blocks(bioticOrb.radius.get()), number(bioticOrb.damagePerPulse.get()),
                        number(bioticOrb.damagePool.get()), seconds(bioticOrb.lifeTicks.get())));
        appendSkill(tooltip,
                "X",
                "skill.olru.final_answer.ultimate",
                FINAL_ANSWER_ACCENT,
                Component.translatable("tooltip.olru.final_answer.ultimate.mechanic"),
                Component.translatable("tooltip.olru.final_answer.ultimate.charge",
                        number(coalescence.chargePercentPerDamage.get()),
                        number(bioticSpray.ultChargePercentPerHeal.get())),
                null,
                Component.translatable("tooltip.olru.final_answer.ultimate.stats",
                        blocks(coalescence.length.get()), seconds(coalescence.durationTicks.get()),
                        number(coalescence.enemyDamagePerPulse.get()), number(coalescence.allyHealPerPulse.get()),
                        number(coalescence.selfHealPerPulse.get())));
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
        if (expanded) {
            tooltip.accept(Component.literal("  =================").withStyle(MUTED));
        }
    }

    private static void appendSkill(Consumer<Component> tooltip, String keyLabel, String skillNameKey, ChatFormatting accent,
            Component summary, Component chargeType, Component duration, Component keyStats) {
        tooltip.accept(Component.literal("  ----------------").withStyle(MUTED));
        tooltip.accept(Component.literal("  [").withStyle(MUTED)
                .append(Component.literal(keyLabel).withStyle(KEY, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(MUTED))
                .append(Component.translatable(skillNameKey).withStyle(accent, ChatFormatting.BOLD)));
        tooltip.accept(Component.literal("   ").append(summary).withStyle(BODY));
        MutableComponent timing = labeledInline("tooltip.olru.gauntlet.charge_type", chargeType);
        if (duration != null) {
            timing.append(Component.literal("  |  ").withStyle(MUTED))
                    .append(labeledInline("tooltip.olru.gauntlet.duration", duration));
        }
        tooltip.accept(timing);
        tooltip.accept(labeled("tooltip.olru.gauntlet.key_stats", keyStats));
    }

    private static Component labeled(String labelKey, Component value) {
        return Component.literal("   ")
                .append(Component.translatable(labelKey).withStyle(LABEL, ChatFormatting.BOLD))
                .append(Component.literal(": ").withStyle(MUTED))
                .append(value.copy().withStyle(BODY));
    }

    private static MutableComponent labeledInline(String labelKey, Component value) {
        return Component.literal("   ")
                .append(Component.translatable(labelKey).withStyle(LABEL))
                .append(Component.literal(": ").withStyle(MUTED))
                .append(value.copy().withStyle(BODY));
    }

    private static String seconds(int ticks) {
        return number(ticks / 20.0) + "s";
    }

    private static String blocks(double blocks) {
        return number(blocks);
    }

    private static String percent(double fraction) {
        return number(fraction * 100.0) + "%";
    }

    private static String multiplier(double value) {
        return "x" + number(value);
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
