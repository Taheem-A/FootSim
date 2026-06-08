// Exporting as package
package gamemechanics;

/*
    Functional interface for running a tournament match.
    Allows for different match-running implementations
*/
@FunctionalInterface
public interface TournamentMatchRunner {
    Match playMatch(Team homeTeam, Team awayTeam, boolean userTeamMatch);
}
