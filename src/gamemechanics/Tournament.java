// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import simulation.SimulationEngine;

public class Tournament {
    // Instance fields
    private final String name;
    private final ArrayList<Team> teams;
    private final ArrayList<Match> matches;
    private final ArrayList<String> log;
    private final SimulationEngine engine;
    private final Random random;
    private Team champion;

    // Main constructor
    public Tournament(String name, ArrayList<Team> teams, SimulationEngine engine) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Tournament name cannot be blank.");
        if (teams == null || teams.size() < 2) throw new IllegalArgumentException("Tournament must have at least two teams.");
        if (engine == null) throw new IllegalArgumentException("Simulation engine cannot be null.");

        this.name = name.trim();
        this.teams = new ArrayList<>(teams);
        this.matches = new ArrayList<>();
        this.log = new ArrayList<>();
        this.engine = engine;
        this.random = new Random();
        this.champion = null;
    }

    // Simulates a single-elimination knockout tournament.
    public Team simulateKnockoutTournament() {
        ArrayList<Team> remainingTeams = new ArrayList<>(this.teams);
        int roundNumber = 1;

        log.add("=== " + this.name + " - Knockout Tournament ===");

        while (remainingTeams.size() > 1) {
            ArrayList<Team> winners = new ArrayList<>();
            log.add("");
            log.add(getRoundName(remainingTeams.size(), roundNumber));

            for (int i = 0; i < remainingTeams.size(); i += 2) {
                Team homeTeam = remainingTeams.get(i);
                Team awayTeam = remainingTeams.get(i + 1);

                Match match = engine.simulateMatch(homeTeam, awayTeam);
                matches.add(match);

                Team winner = getMatchWinner(match);
                winners.add(winner);

                log.add(match.getScoreLine() + " | Winner: " + winner.getName());
            }

            remainingTeams = winners;
            roundNumber++;
        }

        this.champion = remainingTeams.get(0);
        log.add("");
        log.add("Champion: " + this.champion.getName());
        return this.champion;
    }

    // Simulates an 8-team tournament with two groups of four and a knockout phase.
    public Team simulateGroupStageAndKnockout() {
        if (this.teams.size() < 8) throw new IllegalStateException("Group stage mode requires at least 8 teams.");

        ArrayList<Team> groupA = new ArrayList<>(this.teams.subList(0, 4));
        ArrayList<Team> groupB = new ArrayList<>(this.teams.subList(4, 8));

        log.add("=== " + this.name + " - Group Stage + Knockout ===");
        log.add("");

        ArrayList<Team> groupAWinners = simulateGroup("Group A", groupA);
        ArrayList<Team> groupBWinners = simulateGroup("Group B", groupB);

        log.add("");
        log.add("=== Semi-Finals ===");

        Match semiFinal1 = engine.simulateMatch(groupAWinners.get(0), groupBWinners.get(1));
        Match semiFinal2 = engine.simulateMatch(groupBWinners.get(0), groupAWinners.get(1));

        matches.add(semiFinal1);
        matches.add(semiFinal2);

        Team finalist1 = getMatchWinner(semiFinal1);
        Team finalist2 = getMatchWinner(semiFinal2);

        log.add(semiFinal1.getScoreLine() + " | Winner: " + finalist1.getName());
        log.add(semiFinal2.getScoreLine() + " | Winner: " + finalist2.getName());

        log.add("");
        log.add("=== Final ===");

        Match finalMatch = engine.simulateMatch(finalist1, finalist2);
        matches.add(finalMatch);

        this.champion = getMatchWinner(finalMatch);

        log.add(finalMatch.getScoreLine() + " | Winner: " + this.champion.getName());
        log.add("");
        log.add("Champion: " + this.champion.getName());

        return this.champion;
    }

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

    // Simulates one group and returns the top two teams.
    private ArrayList<Team> simulateGroup(String groupName, ArrayList<Team> groupTeams) {
        ArrayList<GroupStanding> standings = new ArrayList<>();

        for (Team team : groupTeams) {
            standings.add(new GroupStanding(team));
        }

        log.add("=== " + groupName + " ===");

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
            GroupStanding standing = standings.get(i);
            log.add((i + 1) + ". " + standing.getFormattedStanding());
        }

        ArrayList<Team> qualifiedTeams = new ArrayList<>();
        qualifiedTeams.add(standings.get(0).team);
        qualifiedTeams.add(standings.get(1).team);

        log.add("Qualified: " + qualifiedTeams.get(0).getName() + ", " + qualifiedTeams.get(1).getName());
        log.add("");

        return qualifiedTeams;
    }

    // Updates group standings after a group stage match.
    private void updateStandings(ArrayList<GroupStanding> standings, Match match) {
        GroupStanding homeStanding = findStanding(standings, match.getHomeTeam());
        GroupStanding awayStanding = findStanding(standings, match.getAwayTeam());

        homeStanding.goalsFor += match.getHomeScore();
        homeStanding.goalsAgainst += match.getAwayScore();

        awayStanding.goalsFor += match.getAwayScore();
        awayStanding.goalsAgainst += match.getHomeScore();

        if (match.getHomeScore() > match.getAwayScore()) {
            homeStanding.points += 3;
            homeStanding.wins++;
            awayStanding.losses++;
        } else if (match.getAwayScore() > match.getHomeScore()) {
            awayStanding.points += 3;
            awayStanding.wins++;
            homeStanding.losses++;
        } else {
            homeStanding.points++;
            awayStanding.points++;
            homeStanding.draws++;
            awayStanding.draws++;
        }
    }

    // Finds the standing object belonging to a team.
    private GroupStanding findStanding(ArrayList<GroupStanding> standings, Team team) {
        for (GroupStanding standing : standings) {
            if (standing.team == team) return standing;
        }

        throw new IllegalArgumentException("Team was not found in standings.");
    }

    // Finds a winner. Draws are resolved by simulated penalties.
    private Team getMatchWinner(Match match) {
        if (match.getHomeScore() > match.getAwayScore()) return match.getHomeTeam();
        if (match.getAwayScore() > match.getHomeScore()) return match.getAwayTeam();

        Team penaltyWinner = decidePenaltyWinner(match.getHomeTeam(), match.getAwayTeam());
        log.add("Penalty shootout: " + penaltyWinner.getName() + " advances.");
        return penaltyWinner;
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
    private String getRoundName(int teamsRemaining, int roundNumber) {
        if (teamsRemaining == 2) return "=== Final ===";
        if (teamsRemaining == 4) return "=== Semi-Finals ===";
        if (teamsRemaining == 8) return "=== Quarter-Finals ===";

        return "=== Round " + roundNumber + " ===";
    }

    // Inner class used to track group stage standings.
    private class GroupStanding implements Comparable<GroupStanding> {
        private final Team team;
        private int points;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        private GroupStanding(Team team) {
            this.team = team;
            this.points = 0;
            this.wins = 0;
            this.draws = 0;
            this.losses = 0;
            this.goalsFor = 0;
            this.goalsAgainst = 0;
        }

        private int getGoalDifference() {
            return this.goalsFor - this.goalsAgainst;
        }

        private String getFormattedStanding() {
            return team.getName()
                + " | Pts: " + points
                + " | W: " + wins
                + " | D: " + draws
                + " | L: " + losses
                + " | GD: " + getGoalDifference()
                + " | GF: " + goalsFor;
        }

        @Override
        public int compareTo(GroupStanding other) {
            Comparator<GroupStanding> comparator = Comparator
                .comparingInt((GroupStanding standing) -> standing.points)
                .thenComparingInt(standing -> standing.getGoalDifference())
                .thenComparingInt(standing -> standing.goalsFor);

            return comparator.reversed().compare(this, other);
        }
    }
}
