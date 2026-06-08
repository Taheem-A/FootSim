// Exporting as package
package gamemechanics;

public class TeamTactics {
    // Instance fields
    private TacticalStyle style;
    private ManagerDecision activeDecision;
    private int talkAttackModifier;
    private int talkMidfieldModifier;
    private int talkDefenceModifier;
    private int talkEventChanceModifier;
    private int talkConversionModifier;
    private int talkCardRiskModifier;

    // Default constructor
    public TeamTactics() {
        this(TacticalStyle.BALANCED);
    }

    // Main constructor
    public TeamTactics(TacticalStyle style) {
        if (style == null) throw new IllegalArgumentException("Tactical style cannot be null.");
        this.style = style;
        this.activeDecision = ManagerDecision.KEEP_SHAPE;
        clearTeamTalkModifiers();
    }

    /* Getters */
    public TacticalStyle getStyle() {
        return this.style;
    }

    public ManagerDecision getActiveDecision() {
        return this.activeDecision;
    }

    public int getAttackModifier() {
        return style.getAttackModifier()
            + activeDecision.getAttackModifier()
            + this.talkAttackModifier;
    }

    public int getMidfieldModifier() {
        return style.getMidfieldModifier()
            + activeDecision.getMidfieldModifier()
            + this.talkMidfieldModifier;
    }

    public int getDefenceModifier() {
        return style.getDefenceModifier()
            + activeDecision.getDefenceModifier()
            + this.talkDefenceModifier;
    }

    public int getEventChanceModifier() {
        return style.getEventChanceModifier()
            + activeDecision.getEventChanceModifier()
            + this.talkEventChanceModifier;
    }

    public int getConversionModifier() {
        return style.getConversionModifier()
            + activeDecision.getConversionModifier()
            + this.talkConversionModifier;
    }

    public int getCardRiskModifier() {
        return style.getCardRiskModifier()
            + activeDecision.getCardRiskModifier()
            + this.talkCardRiskModifier;
    }
    /* */

    /* Tactical modifiers */
    public void applyManagerDecision(ManagerDecision decision) {
        if (decision == null) throw new IllegalArgumentException("Manager decision cannot be null.");
        this.activeDecision = decision;
    }

    public int applyTeamTalk(TeamTalk talk, boolean losing, boolean winning) {
        if (talk == null) throw new IllegalArgumentException("Team talk cannot be null.");

        int momentumChange = talk.getMomentumChange();

        if (talk == TeamTalk.CRITICIZE) {
            if (losing) {
                this.talkAttackModifier += 9;
                this.talkEventChanceModifier += 5;
                this.talkConversionModifier += 5;
                momentumChange = 10;
            } else {
                this.talkAttackModifier -= 4;
                this.talkMidfieldModifier -= 3;
                this.talkConversionModifier -= 4;
                momentumChange = -6;
            }
        } else {
            this.talkAttackModifier += talk.getAttackModifier();
            this.talkMidfieldModifier += talk.getMidfieldModifier();
            this.talkDefenceModifier += talk.getDefenceModifier();
            this.talkEventChanceModifier += talk.getEventChanceModifier();
            this.talkConversionModifier += talk.getConversionModifier();
            this.talkCardRiskModifier += talk.getCardRiskModifier();
        }

        clampTalkModifiers();
        return momentumChange;
    }

    private void clearTeamTalkModifiers() {
        this.talkAttackModifier = 0;
        this.talkMidfieldModifier = 0;
        this.talkDefenceModifier = 0;
        this.talkEventChanceModifier = 0;
        this.talkConversionModifier = 0;
        this.talkCardRiskModifier = 0;
    }

    private void clampTalkModifiers() {
        this.talkAttackModifier = clamp(this.talkAttackModifier, -10, 12);
        this.talkMidfieldModifier = clamp(this.talkMidfieldModifier, -10, 12);
        this.talkDefenceModifier = clamp(this.talkDefenceModifier, -10, 12);
        this.talkEventChanceModifier = clamp(this.talkEventChanceModifier, -10, 12);
        this.talkConversionModifier = clamp(this.talkConversionModifier, -10, 12);
        this.talkCardRiskModifier = clamp(this.talkCardRiskModifier, -5, 10);
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    /* */

    // Returns a copy so each team can have independent tactics.
    public TeamTactics copy() {
        TeamTactics copy = new TeamTactics(this.style);
        copy.activeDecision = this.activeDecision;
        copy.talkAttackModifier = this.talkAttackModifier;
        copy.talkMidfieldModifier = this.talkMidfieldModifier;
        copy.talkDefenceModifier = this.talkDefenceModifier;
        copy.talkEventChanceModifier = this.talkEventChanceModifier;
        copy.talkConversionModifier = this.talkConversionModifier;
        copy.talkCardRiskModifier = this.talkCardRiskModifier;
        return copy;
    }

    @Override
    public String toString() {
        return this.style.getDisplayName() + " | Current Decision: " + this.activeDecision.getDisplayName();
    }
}
