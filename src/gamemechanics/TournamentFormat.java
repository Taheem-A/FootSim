// Exporting as package
package gamemechanics;

public enum TournamentFormat {
    KNOCKOUT_ONLY("Knockout Only", 16),
    CLASSIC_GROUP_STAGE("Classic Group Stage", 32),
    MODERN_LEAGUE_PHASE("Modern League Phase", 36);

    // Instance fields
    private final String displayName;
    private final int defaultTeamCount;

    // Main constructor
    TournamentFormat(String displayName, int defaultTeamCount) {
        this.displayName = displayName;
        this.defaultTeamCount = defaultTeamCount;
    }

    // Returns the user-friendly format name.
    public String getDisplayName() {
        return this.displayName;
    }

    // Returns the usual number of teams for this tournament format.
    public int getDefaultTeamCount() {
        return this.defaultTeamCount;
    }
}
