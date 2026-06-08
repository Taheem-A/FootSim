// Exporting as package
package gamemechanics;

public enum ManagerDecision {
    /* Enum constants with their respective modifiers and descriptions */
    KEEP_SHAPE(
        "Keep Current Shape",
        "Stay disciplined and continue with the current plan.",
        0, 0, 0, 0, 0, 0, 0
    ),
    PUSH_FORWARD(
        "Push Forward",
        "Commit more players into attack.",
        7, 2, -6, 4, 5, 3, 4
    ),
    SIT_DEEPER(
        "Sit Deeper",
        "Protect the box and reduce defensive risk.",
        -5, 0, 8, -4, -2, -2, 2
    ),
    INCREASE_PRESSING(
        "Increase Pressing",
        "Press aggressively to force mistakes.",
        5, 4, -3, 6, 3, 7, 3
    ),
    SLOW_TEMPO(
        "Slow Tempo",
        "Control the ball and calm the match down.",
        -2, 6, 4, -5, 0, -3, 2
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
    private final int momentumChange;

    // Main constructor
    ManagerDecision(
        String displayName,
        String description,
        int attackModifier,
        int midfieldModifier,
        int defenceModifier,
        int eventChanceModifier,
        int conversionModifier,
        int cardRiskModifier,
        int momentumChange
    ) {
        this.displayName = displayName;
        this.description = description;
        this.attackModifier = attackModifier;
        this.midfieldModifier = midfieldModifier;
        this.defenceModifier = defenceModifier;
        this.eventChanceModifier = eventChanceModifier;
        this.conversionModifier = conversionModifier;
        this.cardRiskModifier = cardRiskModifier;
        this.momentumChange = momentumChange;
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

    public int getMomentumChange() {
        return this.momentumChange;
    }
    /* */
}
