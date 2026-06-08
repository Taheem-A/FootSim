// Exporting as package
package gamemechanics.tournament;

import gamemechanics.core.Team;

/*
    The 'implements Comparable<TournamentStanding>' allows for sorting
    The sorting uses an overriden 'compareTo' method
    - This method is automatically called when using Collections.sort()
*/
public class TournamentStanding implements Comparable<TournamentStanding> {
    // Instance fields
    private final Team team;
    private int points;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;

    // Main constructor
    public TournamentStanding(Team team) {
        if (team == null) throw new IllegalArgumentException("Team cannot be null.");

        this.team = team;
        this.points = 0;
        this.wins = 0;
        this.draws = 0;
        this.losses = 0;
        this.goalsFor = 0;
        this.goalsAgainst = 0;
    }

    /* Getters */
    public Team getTeam() {
        return this.team;
    }

    public int getPoints() {
        return this.points;
    }

    public int getWins() {
        return this.wins;
    }

    public int getDraws() {
        return this.draws;
    }

    public int getLosses() {
        return this.losses;
    }

    public int getGoalsFor() {
        return this.goalsFor;
    }

    public int getGoalsAgainst() {
        return this.goalsAgainst;
    }

    public int getGoalDifference() {
        return this.goalsFor - this.goalsAgainst;
    }
    /* */

    // Updates the standing after one played match
    public void recordMatch(int goalsFor, int goalsAgainst) {
        if (goalsFor < 0 || goalsAgainst < 0) throw new IllegalArgumentException("Goals cannot be negative.");

        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;

        if (goalsFor > goalsAgainst) {
            this.points += 3;
            this.wins++;
        } else if (goalsFor < goalsAgainst) this.losses++;
        else {
            this.points++;
            this.draws++;
        }
    }

    // Returns a formatted table row
    public String getFormattedStanding() {
        return String.format(
            "%-24s | Pts: %2d | W: %2d | D: %2d | L: %2d | GD: %3d | GF: %2d",
            this.team.getName(),
            this.points,
            this.wins,
            this.draws,
            this.losses,
            getGoalDifference(),
            this.goalsFor
        );
    }

    // Overriden 'compareTo' method 
    @Override
    public int compareTo(TournamentStanding other) {
        if (other == null) throw new IllegalArgumentException("Other standing cannot be null.");

        if (this.points != other.points) return other.points - this.points;
        if (this.getGoalDifference() != other.getGoalDifference()) return other.getGoalDifference() - this.getGoalDifference();
        if (this.goalsFor != other.goalsFor) return other.goalsFor - this.goalsFor;

        return (int) Math.round(other.team.getAverageOverallRating() - this.team.getAverageOverallRating());
    }
}
