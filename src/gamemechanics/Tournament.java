// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import simulation.SimulationEngine;

public class Tournament {
    // Static fields
    private static final int CLASSIC_GROUP_COUNT = 8;
    private static final int CLASSIC_TEAMS_PER_GROUP = 4;
    private static final int MODERN_LEAGUE_MATCHES_PER_TEAM = 8;

    // Instance fields
    private final String name;
    private final ArrayList<Team> teams;
    private final ArrayList<Match> matches;
    private final ArrayList<String> log;
    private final SimulationEngine engine;
    private final Random random;
    private Team userTeam;
    private Team champion;

    // Main constructor
    public Tournament(String name, ArrayList<Team> teams, SimulationEngine engine) {
        this(name, teams, engine, null);
    }

    // Overloaded constructor that supports tracking the team chosen by the user.
    public Tournament(String name, ArrayList<Team> teams, SimulationEngine engine, Team userTeam) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Tournament name cannot be blank.");
        if (teams == null || teams.size() < 2) throw new IllegalArgumentException("Tournament must have at least two teams.");
        if (engine == null) throw new IllegalArgumentException("Simulation engine cannot be null.");

        this.name = name.trim();
        this.teams = new ArrayList<>(teams);
        this.matches = new ArrayList<>();
        this.log = new ArrayList<>();
        this.engine = engine;
        this.random = new Random();
        this.userTeam = userTeam;
        this.champion = null;
    }

    /* Main tournament simulation methods */
    // Simulates a simple knockout-only tournament.
    public Team simulateKnockoutTournament() {
        validatePowerOfTwo(this.teams.size(), "Knockout tournament");

        log.add("=== " + this.name + " - Knockout Tournament ===");
        logUserTeam();

        this.champion = simulateKnockoutStage(new ArrayList<>(this.teams), "Knockout Stage");

        log.add("");
        log.add("Champion: " + this.champion.getName());

        return this.champion;
    }

    // Simulates the older Champions League style: 32 teams, 8 groups of 4, then a 16-team knockout.
    public Team simulateClassicGroupStage() {
        if (this.teams.size() != TournamentFormat.CLASSIC_GROUP_STAGE.getDefaultTeamCount()) {
            throw new IllegalStateException("Classic Group Stage requires exactly 32 teams.");
        }

        log.add("=== " + this.name + " - Classic Group Stage ===");
        logUserTeam();
        log.add("");
        log.add("Format: 32 teams, 8 groups of 4, top 2 from each group qualify for the Round of 16.");

        ArrayList<Team> shuffledTeams = new ArrayList<>(this.teams);
        Collections.shuffle(shuffledTeams, random);

        ArrayList<Team> groupWinners = new ArrayList<>();
        ArrayList<Team> groupRunnersUp = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < CLASSIC_GROUP_COUNT; groupIndex++) {
            int startIndex = groupIndex * CLASSIC_TEAMS_PER_GROUP;
            ArrayList<Team> groupTeams = new ArrayList<>(shuffledTeams.subList(startIndex, startIndex + CLASSIC_TEAMS_PER_GROUP));

            ArrayList<TournamentStanding> standings = simulateGroup("Group " + (char) ('A' + groupIndex), groupTeams);

            groupWinners.add(standings.get(0).getTeam());
            groupRunnersUp.add(standings.get(1).getTeam());
        }

        ArrayList<Team> roundOf16Teams = buildClassicRoundOf16Draw(groupWinners, groupRunnersUp);
        this.champion = simulateKnockoutStage(roundOf16Teams, "Round of 16");

        log.add("");
        log.add("Champion: " + this.champion.getName());

        return this.champion;
    }

    // Simulates the newer Champions League style: 36-team league phase, play-offs, then Round of 16.
    public Team simulateModernLeaguePhase() {
        if (this.teams.size() != TournamentFormat.MODERN_LEAGUE_PHASE.getDefaultTeamCount()) {
            throw new IllegalStateException("Modern League Phase requires exactly 36 teams.");
        }

        log.add("=== " + this.name + " - Modern League Phase ===");
        logUserTeam();
        log.add("");
        log.add("Format: 36-team league phase, 8 matches per team, top 8 go to Round of 16, 9th-24th enter play-offs.");

        ArrayList<TournamentStanding> standings = simulateLeaguePhase();
        Collections.sort(standings);

        log.add("");
        log.add("=== Final League Phase Table ===");
        for (int i = 0; i < standings.size(); i++) {
            String status;

            if (i < 8) status = " [Round of 16]";
            else if (i < 24) status = " [Play-Offs]";
            else status = " [Eliminated]";

            log.add(String.format("%2d. %s%s", i + 1, standings.get(i).getFormattedStanding(), status));
        }

        ArrayList<Team> directRoundOf16Teams = new ArrayList<>();
        ArrayList<Team> playoffTeams = new ArrayList<>();

        for (int i = 0; i < standings.size(); i++) {
            if (i < 8) directRoundOf16Teams.add(standings.get(i).getTeam());
            else if (i < 24) playoffTeams.add(standings.get(i).getTeam());
        }

        ArrayList<Team> playoffWinners = simulatePlayoffRound(playoffTeams);
        ArrayList<Team> roundOf16Teams = buildModernRoundOf16Draw(directRoundOf16Teams, playoffWinners);

        this.champion = simulateKnockoutStage(roundOf16Teams, "Round of 16");

        log.add("");
        log.add("Champion: " + this.champion.getName());

        return this.champion;
    }

    // Backwards-compatible method name from the old version.
    public Team simulateGroupStageAndKnockout() {
        return simulateClassicGroupStage();
    }
    /* */

    /* Getters */
    // Returns all matches played in the tournament.
    public ArrayList<Match> getMatches() {
        return new ArrayList<>(this.matches);
    }

    // Returns the tournament champion after simulation.
    public Team getChampion() {
        return this.champion;
    }

    // Returns a formatted tournament report.
    public String getFormattedReport() {
        if (this.log.isEmpty()) return "Tournament has not been simulated yet.";

        String result = "";

        for (String line : this.log) {
            result += line + "\n";
        }

        return result;
    }
    /* */

    /* Classic group stage methods */
    // Simulates one four-team group and returns the sorted standings.
    private ArrayList<TournamentStanding> simulateGroup(String groupName, ArrayList<Team> groupTeams) {
        ArrayList<TournamentStanding> standings = createStandings(groupTeams);

        log.add("");
        log.add("=== " + groupName + " ===");

        for (Team team : groupTeams) {
            log.add("- " + team.getName());
        }

        log.add("");

        for (int i = 0; i < groupTeams.size(); i++) {
            for (int j = i + 1; j < groupTeams.size(); j++) {
                Team homeTeam = groupTeams.get(i);
                Team awayTeam = groupTeams.get(j);

                Match match = engine.simulateMatch(homeTeam, awayTeam);
                matches.add(match);
                updateStandings(standings, match);

                log.add(match.getScoreLine());
            }
        }

        Collections.sort(standings);

        log.add("");
        log.add(groupName + " Standings:");
        for (int i = 0; i < standings.size(); i++) {
            log.add((i + 1) + ". " + standings.get(i).getFormattedStanding());
        }

        log.add("Qualified: " + standings.get(0).getTeam().getName() + ", " + standings.get(1).getTeam().getName());

        return standings;
    }

    // Builds a Round of 16 draw where group winners face runners-up from different groups.
    private ArrayList<Team> buildClassicRoundOf16Draw(ArrayList<Team> groupWinners, ArrayList<Team> groupRunnersUp) {
        ArrayList<Team> draw = new ArrayList<>();

        for (int i = 0; i < groupWinners.size(); i++) {
            draw.add(groupWinners.get(i));
            draw.add(groupRunnersUp.get(groupRunnersUp.size() - 1 - i));
        }

        return draw;
    }
    /* */

    /* Modern league phase methods */
    // Simulates eight league-phase rounds using a rotating schedule.
    private ArrayList<TournamentStanding> simulateLeaguePhase() {
        ArrayList<Team> scheduledTeams = new ArrayList<>(this.teams);
        Collections.shuffle(scheduledTeams, random);

        ArrayList<TournamentStanding> standings = createStandings(scheduledTeams);

        for (int round = 1; round <= MODERN_LEAGUE_MATCHES_PER_TEAM; round++) {
            log.add("");
            log.add("=== League Phase Round " + round + " ===");

            for (int i = 0; i < scheduledTeams.size() / 2; i++) {
                Team teamA = scheduledTeams.get(i);
                Team teamB = scheduledTeams.get(scheduledTeams.size() - 1 - i);

                Team homeTeam;
                Team awayTeam;

                if ((round + i) % 2 == 0) {
                    homeTeam = teamA;
                    awayTeam = teamB;
                } else {
                    homeTeam = teamB;
                    awayTeam = teamA;
                }

                Match match = engine.simulateMatch(homeTeam, awayTeam);
                matches.add(match);
                updateStandings(standings, match);

                log.add(match.getScoreLine());
            }

            rotateTeams(scheduledTeams);
        }

        return standings;
    }

    // Rotates all teams except the first one. This creates different opponents each round.
    private void rotateTeams(ArrayList<Team> scheduledTeams) {
        if (scheduledTeams.size() <= 2) return;

        Team lastTeam = scheduledTeams.remove(scheduledTeams.size() - 1);
        scheduledTeams.add(1, lastTeam);
    }

    // Simulates the play-off round for league phase teams ranked 9th-24th.
    private ArrayList<Team> simulatePlayoffRound(ArrayList<Team> playoffTeams) {
        ArrayList<Team> playoffWinners = new ArrayList<>();

        log.add("");
        log.add("=== Knockout Play-Off Round ===");
        log.add("Teams ranked 9th-24th play for the final eight Round of 16 spots.");

        for (int i = 0; i < playoffTeams.size() / 2; i++) {
            Team higherSeed = playoffTeams.get(i);
            Team lowerSeed = playoffTeams.get(playoffTeams.size() - 1 - i);

            Match match = engine.simulateMatch(higherSeed, lowerSeed);
            matches.add(match);

            MatchResult result = getMatchResult(match);
            playoffWinners.add(result.winner);

            log.add(match.getScoreLine() + " | Winner: " + result.winner.getName());
            if (!result.penaltyNote.isEmpty()) log.add(result.penaltyNote);
        }

        return playoffWinners;
    }

    // Pairs direct Round of 16 teams with play-off winners.
    private ArrayList<Team> buildModernRoundOf16Draw(ArrayList<Team> directTeams, ArrayList<Team> playoffWinners) {
        ArrayList<Team> draw = new ArrayList<>();

        for (int i = 0; i < directTeams.size(); i++) {
            draw.add(directTeams.get(i));
            draw.add(playoffWinners.get(playoffWinners.size() - 1 - i));
        }

        return draw;
    }
    /* */

    /* Knockout stage methods */
    // Simulates a knockout stage and returns the winner.
    private Team simulateKnockoutStage(ArrayList<Team> knockoutTeams, String openingRoundName) {
        validatePowerOfTwo(knockoutTeams.size(), openingRoundName);

        ArrayList<Team> remainingTeams = new ArrayList<>(knockoutTeams);
        int roundNumber = 1;

        while (remainingTeams.size() > 1) {
            ArrayList<Team> winners = new ArrayList<>();
            log.add("");
            log.add(getRoundName(remainingTeams.size(), roundNumber, openingRoundName));

            for (int i = 0; i < remainingTeams.size(); i += 2) {
                Team homeTeam = remainingTeams.get(i);
                Team awayTeam = remainingTeams.get(i + 1);

                Match match = engine.simulateMatch(homeTeam, awayTeam);
                matches.add(match);

                MatchResult result = getMatchResult(match);
                winners.add(result.winner);

                log.add(match.getScoreLine() + " | Winner: " + result.winner.getName());
                if (!result.penaltyNote.isEmpty()) log.add(result.penaltyNote);
            }

            remainingTeams = winners;
            roundNumber++;
        }

        return remainingTeams.get(0);
    }

    // Finds a winner. Draws are resolved by simulated penalties.
    private MatchResult getMatchResult(Match match) {
        if (match.getHomeScore() > match.getAwayScore()) return new MatchResult(match.getHomeTeam(), "");
        if (match.getAwayScore() > match.getHomeScore()) return new MatchResult(match.getAwayTeam(), "");

        Team penaltyWinner = decidePenaltyWinner(match.getHomeTeam(), match.getAwayTeam());
        return new MatchResult(penaltyWinner, "Penalty shootout: " + penaltyWinner.getName() + " advances.");
    }

    // Simulates a simple penalty tiebreaker using goalkeeper and attacking ratings.
    private Team decidePenaltyWinner(Team teamA, Team teamB) {
        int teamAScore = 0;
        int teamBScore = 0;

        for (int i = 0; i < 5; i++) {
            if (scorePenalty(teamA, teamB)) teamAScore++;
            if (scorePenalty(teamB, teamA)) teamBScore++;
        }

        while (teamAScore == teamBScore) {
            if (scorePenalty(teamA, teamB)) teamAScore++;
            if (scorePenalty(teamB, teamA)) teamBScore++;
        }

        return teamAScore > teamBScore ? teamA : teamB;
    }

    // Calculates whether one penalty is scored.
    private boolean scorePenalty(Team shootingTeam, Team defendingTeam) {
        double chance = 65
            + shootingTeam.getTeamAttackRating() * 0.20
            - defendingTeam.getTeamGoalkeeperRating() * 0.15;

        return random.nextDouble() * 100 <= chance;
    }

    // Names the knockout round based on the number of teams remaining.
    private String getRoundName(int teamsRemaining, int roundNumber, String openingRoundName) {
        if (teamsRemaining == 16) return "=== " + openingRoundName + " ===";
        if (teamsRemaining == 8) return "=== Quarter-Finals ===";
        if (teamsRemaining == 4) return "=== Semi-Finals ===";
        if (teamsRemaining == 2) return "=== Final ===";

        return "=== Round " + roundNumber + " ===";
    }
    /* */

    /* Standing helper methods */
    // Creates a standings object for every team in the list.
    private ArrayList<TournamentStanding> createStandings(ArrayList<Team> teams) {
        ArrayList<TournamentStanding> standings = new ArrayList<>();

        for (Team team : teams) {
            standings.add(new TournamentStanding(team));
        }

        return standings;
    }

    // Updates standings after a match.
    private void updateStandings(ArrayList<TournamentStanding> standings, Match match) {
        TournamentStanding homeStanding = findStanding(standings, match.getHomeTeam());
        TournamentStanding awayStanding = findStanding(standings, match.getAwayTeam());

        homeStanding.recordMatch(match.getHomeScore(), match.getAwayScore());
        awayStanding.recordMatch(match.getAwayScore(), match.getHomeScore());
    }

    // Finds the standing object belonging to a team.
    private TournamentStanding findStanding(ArrayList<TournamentStanding> standings, Team team) {
        for (TournamentStanding standing : standings) {
            if (standing.getTeam() == team) return standing;
        }

        throw new IllegalArgumentException("Team was not found in standings.");
    }
    /* */

    /* Validation and display helpers */
    private void validatePowerOfTwo(int amount, String modeName) {
        if (amount < 2 || amount % 2 != 0 || (amount & (amount - 1)) != 0) {
            throw new IllegalStateException(modeName + " requires a power-of-two number of teams.");
        }
    }

    private void logUserTeam() {
        if (this.userTeam != null) {
            log.add("User Team: " + this.userTeam.getName());
        }
    }
    /* */

    // Inner class used to store knockout match results.
    private class MatchResult {
        private Team winner;
        private String penaltyNote;

        private MatchResult(Team winner, String penaltyNote) {
            this.winner = winner;
            this.penaltyNote = penaltyNote;
        }
    }
}
