// Exporting as package
package gamemechanics.tactics;

public enum TacticalStyle {
    /* Enum constants with their respective modifiers and descriptions */
    BALANCED(
        "Balanced",
        "A safe setup with no major strengths or weaknesses.",
        0, 0, 0, 0, 0, 0
    ),
    ATTACKING(
        "Attacking",
        "Creates more chances, but leaves the defence more exposed.",
        8, 2, -6, 4, 5, 2
    ),
    DEFENSIVE(
        "Defensive",
        "Protects the goal, but creates fewer attacking chances.",
        -6, 0, 9, -5, -2, -1
    ),
    COUNTER_ATTACK(
        "Counter-Attack",
        "Absorbs pressure and attacks quickly when chances appear.",
        4, -2, 4, 1, 6, 1
    ),
    HIGH_PRESS(
        "High Press",
        "Forces more action and turnovers, but increases card risk.",
        6, 3, -3, 7, 3, 8
    ),
    POSSESSION(
        "Possession",
        "Controls midfield and lowers opponent momentum.",
        1, 8, 2, -1, 2, -2
    );
    /* */

    // Instance fields
    private final String displayName;
    private final String description;
    private final int attackModifier;
    private final int midfieldModifier;
    private final int defenceModifier;
    private final int eventChanceModifier;
    private final int conversionModifier;
    private final int cardRiskModifier;

    // Main constructor
    TacticalStyle(
        String displayName,
        String description,
        int attackModifier,
        int midfieldModifier,
        int defenceModifier,
        int eventChanceModifier,
        int conversionModifier,
        int cardRiskModifier
    ) {
        this.displayName = displayName;
        this.description = description;
        this.attackModifier = attackModifier;
        this.midfieldModifier = midfieldModifier;
        this.defenceModifier = defenceModifier;
        this.eventChanceModifier = eventChanceModifier;
        this.conversionModifier = conversionModifier;
        this.cardRiskModifier = cardRiskModifier;
    }

    /* Getters */
    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public int getAttackModifier() {
        return this.attackModifier;
    }

    public int getMidfieldModifier() {
        return this.midfieldModifier;
    }

    public int getDefenceModifier() {
        return this.defenceModifier;
    }

    public int getEventChanceModifier() {
        return this.eventChanceModifier;
    }

    public int getConversionModifier() {
        return this.conversionModifier;
    }

    public int getCardRiskModifier() {
        return this.cardRiskModifier;
    }
    /* */
}
