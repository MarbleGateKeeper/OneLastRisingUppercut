package dev.marblegate.olru.common.attachment.skill;

public record SkillDisplayData(
        SkillStateType mode,
        float cdFraction,
        int currentCharges,
        int maxCharges,
        boolean usable,
        int remainingTicks,
        int totalTicks) {
    public SkillDisplayData(
            SkillStateType mode,
            float cdFraction,
            int currentCharges,
            int maxCharges,
            boolean usable) {
        this(mode, cdFraction, currentCharges, maxCharges, usable, 0, 0);
    }

    static SkillDisplayData unboundPlaceholder(SkillStateType mode) {
        return new SkillDisplayData(mode, 1f, 0, 0, false, 0, 0);
    }
}
