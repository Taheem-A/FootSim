// Exporting as package
package gamemechanics.core;

public class Player {
    // Static fields
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 99;

    // Instance fields
    private String name;
    private String position;
    private int overall;
    private int pace;
    private int shooting;
    private int passing;
    private int dribbling;
    private int defence;
    private int physical;

    // Default constructor
    public Player() {
        // The 'this()' calls another constructor matching the parameters and allows for less repetition
        this("Unknown Player", "MID", 50, 50, 50, 50, 50, 50, 50);
    }

    // Overloaded constructor used when no official overall rating is available.
    public Player(String name, String position, int pace, int shooting, int passing, int dribbling, int defence, int physical) {
        this(name, position, (int) Math.round((pace + shooting + passing + dribbling + defence + physical) / 6.0), pace, shooting, passing, dribbling, defence, physical);
    }

    // Main constructor used by CSV-loaded players with official FC overall ratings.
    public Player(String name, String position, int overall, int pace, int shooting, int passing, int dribbling, int defence, int physical) {
        this.name = validateName(name);
        this.position = validatePosition(position);
        this.overall = validateStat(overall);
        this.pace = validateStat(pace);
        this.shooting = validateStat(shooting);
        this.passing = validateStat(passing);
        this.dribbling = validateStat(dribbling);
        this.defence = validateStat(defence);
        this.physical = validateStat(physical);
    }

    /* Methods for validating passed values before field assignment */
    private int validateStat(int stat) {
        if (stat < MIN_RATING || MAX_RATING < stat) throw new IllegalArgumentException("Stat value must be between " + MIN_RATING + " and " + MAX_RATING + ".");
        return stat;
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Player name cannot be blank/empty.");
        return name.trim();
    }

    private String validatePosition(String position) {
        if (position == null || position.trim().isEmpty()) throw new IllegalArgumentException("Position cannot be blank/empty.");
        position = position.trim().toUpperCase();
        if (!position.equals("ATK") && !position.equals("MID") && !position.equals("DEF") && !position.equals("GK")) throw new IllegalArgumentException("Position must be either ATK, MID, DEF, GK.");
        return position;
    }
    /* */

    /* Getters */
    public String getName() {
        return this.name;
    }

    public String getPosition() {
        return this.position;
    }

    public int getOverall() {
        return this.overall;
    }

    public int getPace() {
        return this.pace;
    }

    public int getShooting() {
        return this.shooting;
    }

    public int getPassing() {
        return this.passing;
    }

    public int getDribbling() {
        return this.dribbling;
    }

    public int getDefence() {
        return this.defence;
    }

    public int getPhysical() {
        return this.physical;
    }
    /* */

    /* Methods calculating different types of ratings */
    public double getOverallRating() {
        return this.overall;
    }

    public int getRoundedOverallRating() {
        return this.overall;
    }

    public double getAttackRating() {
        return this.pace * 0.20 + this.shooting * 0.45 + this.dribbling * 0.25 + this.physical * 0.10;
    }

    public double getMidfieldRating() {
        return this.pace * 0.10 + this.passing * 0.40 + this.dribbling * 0.25 + this.defence * 0.10 + this.physical * 0.15;
    }

    public double getDefenceRating() {
        return this.pace * 0.10 + this.passing * 0.10 + this.dribbling * 0.05 + this.defence * 0.50 + this.physical * 0.25;
    }

    public double getGoalkeepingRating() {
        return this.pace * 0.10 + this.passing * 0.15 + this.dribbling * 0.05 + this.defence * 0.50 + this.physical * 0.20;
    }
    /* */

    // Overridden 'toString()' method
    @Override
    public String toString() {
        String formattedName = this.name;
        if (this.name.length() > 21) formattedName = name.substring(0, 18) + "...";
        return """ 
            +---------------+-------+
            | %-21s |
            +---------------+-------+
            | Position      | %-5s |
            +---------------+-------+
            | Stat          | Value |
            +---------------+-------+
            | Overall       | %-5d |
            | Pace          | %-5d |
            | Shooting      | %-5d |
            | Passing       | %-5d |
            | Dribbling     | %-5d |
            | Defence       | %-5d |
            | Physical      | %-5d |
            +---------------+-------+
            """.formatted(
                formattedName,
                this.position,
                getRoundedOverallRating(),
                this.pace,
                this.shooting,
                this.passing,
                this.dribbling,
                this.defence,
                this.physical
            );
    }
}
