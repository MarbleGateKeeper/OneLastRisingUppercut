package dev.marblegate.olru.common.attachment.skill;

public record SkillDisplayData(
        SkillStateType mode,
        float cdFraction,
        int currentCharges,
        int maxCharges,
        boolean usable) {
    static SkillDisplayData unboundPlaceholder(SkillStateType mode) {
        return new SkillDisplayData(mode, 1f, 0, 0, false);
    }
}
