// Exporting as package
package gamemechanics;

public enum TeamTalk {
    /* Enum constants with their respective modifiers and descriptions */
    ENCOURAGE(
        "Encourage the Team",
        "Boost morale and confidence without adding much risk.",
        5, 3, 3, 2, 2, 0, 7
    ),
    DEMAND_MORE(
        "Demand More Effort",
        "Push the players to attack harder, but increase risk.",
        7, 2, -2, 4, 4, 4, 6
    ),
    STAY_CALM(
        "Stay Calm and Composed",
        "Improve control and defensive stability.",
        0, 6, 5, -2, 1, -2, 5
    ),
    CRITICIZE(
        "Criticize Performance",
        "A risky talk that can fire the team up if they are losing.",
        3, 0, 0, 2, 3, 2, 0
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
    TeamTalk(
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
