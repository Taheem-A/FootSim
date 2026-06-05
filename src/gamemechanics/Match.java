// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;
import java.util.Collections;

public class Match {
    // Static fields
    private static final int MATCH_LENGTH = 90;

    // Instance fields
    private final Team homeTeam;
    private final Team awayTeam;
    private int homeScore;
    private int awayScore;
    private int currentMinute;
    private final ArrayList<Event> events;
    private final ArrayList<Player> yellowCardedPlayers;
    private final ArrayList<Player> redCardedPlayers;
    private boolean started;
    private boolean finished;

    // Constructor
    public Match(Team homeTeam, Team awayTeam) {
        validateTeams(homeTeam, awayTeam);

        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = 0;
        this.awayScore = 0;
        this.currentMinute = 0;
        this.events = new ArrayList<>();
        this.yellowCardedPlayers = new ArrayList<>();
        this.redCardedPlayers = new ArrayList<>();
        this.started = false;
        this.finished = false;
    }

    /* Getters */
    public Team getHomeTeam() {
        return this.homeTeam;
    }

    public Team getAwayTeam() {
        return this.awayTeam;
    }

    public int getHomeScore() {
        return this.homeScore;
    }

    public int getAwayScore() {
        return this.awayScore;
    }

    public int getCurrentMinute() {
        return this.currentMinute;
    }

    public int getMatchLength() {
        return MATCH_LENGTH;
    }

    public ArrayList<Event> getEvents() {
        return new ArrayList<>(this.events);
    }

    public ArrayList<Player> getYellowCardedPlayers() {
        return new ArrayList<>(this.yellowCardedPlayers);
    }

    public ArrayList<Player> getRedCardedPlayers() {
        return new ArrayList<>(this.redCardedPlayers);
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isFinished() {
        return this.finished;
    }
    /* */

    // Main match methods
    public void startMatch() {
        if (this.started) throw new IllegalStateException("Match has already started.");

        this.started = true;
        this.currentMinute = 0;

        addEvent(new Event(
            0,
            EventType.KICKOFF,
            null,
            null,
            "Kickoff: " + this.homeTeam.getName() + " vs " + this.awayTeam.getName() + "."
        ));
    }

    // Advances the match minute by a specified amount
    public void advanceMinute(int amount) {
        validateMatchInProgress();

        if (amount <= 0) throw new IllegalArgumentException("Minute amount must be greater than 0.");

        this.currentMinute += amount;
        if (this.currentMinute > MATCH_LENGTH) this.currentMinute = MATCH_LENGTH;
    }

    // Ends the match and records a full time event
    public void endMatch() {
        if (!this.started) throw new IllegalStateException("Match cannot end before it starts.");

        if (this.finished) throw new IllegalStateException("Match has already finished.");

        this.currentMinute = MATCH_LENGTH;

        addEvent(new Event(
            MATCH_LENGTH,
            EventType.FULL_TIME,
            null,
            null,
            "Full time: " + getScoreLine() + "."
        ));

        this.finished = true;
    }

    // Adds an event to the match
    public void addEvent(Event event) {
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");

        this.events.add(event);
    }

    // Adds a goal for a team
    public void addGoal(Team scoringTeam) {
        validateMatchInProgress();
        validateTeamInMatch(scoringTeam);

        if (isHomeTeam(scoringTeam)) this.homeScore++;
        else this.awayScore++;
    }



    // Records a yellow card for a player
    public void recordYellowCard(Player player) {
        validateMatchInProgress();
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");

        if (!findPlayer(player, this.yellowCardedPlayers) && !findPlayer(player, this.redCardedPlayers)) {
            this.yellowCardedPlayers.add(player);
        }
    }

    // Records a red card for a player
    public void recordRedCard(Player player) {
        validateMatchInProgress();
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");

        if (!findPlayer(player, this.redCardedPlayers)) {
            this.redCardedPlayers.add(player);
        }

        this.yellowCardedPlayers.removeIf(p -> p.getName().equalsIgnoreCase(player.getName()));
    }
    /* */

    /* Display methods */
    public String getScoreLine() {
        return this.homeTeam.getName() + " " + this.homeScore + " - " + this.awayScore + " " + this.awayTeam.getName();
    }

    public String getWinner() {
        if (this.homeScore > this.awayScore) return this.homeTeam.getName();
        else if (this.awayScore > this.homeScore) return this.awayTeam.getName();

        return "Draw";
    }

    public String getFormattedTimeline() {
        if (this.events.isEmpty()) return "No match events recorded.";

        ArrayList<Event> sortedEvents = new ArrayList<>(this.events);

        Collections.sort(sortedEvents, (Event event1, Event event2) -> event1.getMinute() - event2.getMinute());

        String result = "Match Timeline:\n";

        for (Event event : sortedEvents) result += "- " + event + "\n";

        return result;
    }

    // Overridden 'toString()' method
    @Override
    public String toString() {
        String status;
        if (this.finished) status = "Finished";
        else if (this.started) status = "In Progress";
        else status = "Not Started";

        String result = String.format("""
            Match Summary
            Home Team: %s
            Away Team: %s
            Score: %s
            Minute: %d'
            Status: %s
            """
            , this.homeTeam.getName(), this.awayTeam.getName(), getScoreLine(), this.currentMinute, status);

        return result;
    }
    /* */

    /* Helper Methods */
    private void validateTeams(Team homeTeam, Team awayTeam) {
        if (homeTeam == null || awayTeam == null) throw new IllegalArgumentException("Both teams must exist.");

        if (homeTeam == awayTeam) throw new IllegalArgumentException("A team cannot play against itself.");
    }

    public void validateMatchInProgress() {
        if (!this.started) throw new IllegalStateException("Match has not started yet.");

        if (this.finished) throw new IllegalStateException("Match has already finished.");
    }

    public void validateTeamInMatch(Team team) {
        if (!isTeamInMatch(team)) throw new IllegalArgumentException("Team must be part of this match.");
    }

    public boolean isTeamInMatch(Team team) {
        return team != null && (team == this.homeTeam || team == this.awayTeam);
    }

    public boolean isHomeTeam(Team team) {
        return team == this.homeTeam;
    }

    public Team getOpponent(Team team) {
        validateTeamInMatch(team);

        if (isHomeTeam(team)) return this.awayTeam;

        return this.homeTeam;
    }

    private boolean findPlayer(Player player, ArrayList<Player> playerList) {
        if (player == null || playerList == null) {
            throw new IllegalArgumentException("Player and player list cannot be null.");
        }
        for (Player p : playerList) {
            if (p.getName().equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }
    /* */
}
