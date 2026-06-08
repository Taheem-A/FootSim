// Exporting as package
package gamemechanics;

public enum TournamentFormat {
    /* Enum constants with their respective modifiers and descriptions */
    KNOCKOUT_ONLY("Knockout Only", 16),
    CLASSIC_GROUP_STAGE("Classic Group Stage", 32),
    MODERN_LEAGUE_PHASE("Modern League Phase", 36);
    /* */

    // Instance fields
    private final String displayName;
    private final int defaultTeamCount;

    // Main constructor
    TournamentFormat(String displayName, int defaultTeamCount) {
        this.displayName = displayName;
        this.defaultTeamCount = defaultTeamCount;
    }

    // Returns the  format name.
    public String getDisplayName() {
        return this.displayName;
    }

    // Returns the number of teams for the associated tournament format.
    public int getDefaultTeamCount() {
        return this.defaultTeamCount;
    }
}
