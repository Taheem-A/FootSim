// Exporting as package
package gamemechanics;

@FunctionalInterface
public interface TournamentMatchRunner {
    Match playMatch(Team homeTeam, Team awayTeam, boolean userTeamMatch);
}
