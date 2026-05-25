import gamemechanics.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        // Creating players for home team
        Player homeStk = new Player("Cristiano Ronaldo", "ATK", 88, 92, 75, 84, 35, 90);
        Player homeMid = new Player("Declan Rice", "MID", 82, 73, 88, 78, 87, 86);
        Player homeDef = new Player("Virgil van Dijk", "DEF", 74, 60, 72, 70, 94, 91);
        Player homeGk = new Player("David Raya", "GK", 45, 20, 74, 58, 88, 65);

        // Creating players for away team
        Player awayStk = new Player("Kylian Mbappe", "ATK", 97, 91, 80, 93, 36, 78);
        Player awayMid = new Player("Kevin De Bruyne", "MID", 74, 86, 94, 88, 64, 78);
        Player awayDef = new Player("Ruben Dias", "DEF", 63, 54, 72, 68, 90, 89);
        Player awayGk = new Player("Thibaut Courtois", "GK", 40, 18, 70, 55, 90, 78);

        // Creating teams
        Team homeTeam = new Team("Home Team", new ArrayList<>(Arrays.asList(homeStk, homeMid, homeDef, homeGk)));
        Team awayTeam = new Team("Away Team", new ArrayList<>(Arrays.asList(awayStk, awayMid, awayDef, awayGk)));

        // Displaying teams
        System.out.println("=== TEAM INFORMATION ===");
        System.out.println(homeTeam);
        System.out.println();
        System.out.println(awayTeam);
        System.out.println();

        // Creating match
        Match match = new Match(homeTeam, awayTeam);

        System.out.println("=== MATCH CREATED ===");
        System.out.println(match);
        System.out.println();

        // Starting match
        match.startMatch();

        // Creating normal events
        match.createNormalShot(homeTeam, 12);
        match.createFoul(awayTeam, 24);
        match.createNormalShot(awayTeam, 31);

        // Creating and resolving a big chance
        Event bigChance = match.createBigChance(homeTeam, 56);

        System.out.println("=== BIG CHANCE ===");
        System.out.println(bigChance.getDescription());
        System.out.println("Available choices:");

        for (String choice : bigChance.getChoices()) {
            System.out.println("- " + choice);
        }

        // For prototype testing, we choose one automatically
        match.resolveChance(bigChance, "Finesse Shot");
        System.out.println();

        // Creating and resolving a penalty
        Event penalty = match.createPenalty(awayTeam, 75);

        System.out.println("=== PENALTY ===");
        System.out.println(penalty.getDescription());
        System.out.println("Available choices:");

        for (String choice : penalty.getChoices()) {
            System.out.println("- " + choice);
        }

        // For prototype testing, we choose one automatically
        match.resolveChance(penalty, "Shoot Left");
        System.out.println();

        // Ending match
        match.endMatch();

        // Displaying final match result
        System.out.println("=== FINAL MATCH SUMMARY ===");
        System.out.println(match);
        System.out.println();

        System.out.println("Winner: " + match.getWinner());
        System.out.println();

        // Displaying cards
        System.out.println("=== CARD SUMMARY ===");
        System.out.println("Yellow-carded players: " + match.getYellowCardedPlayers().size());
        System.out.println("Red-carded players: " + match.getRedCardedPlayers().size());
        System.out.println();

        // Displaying timeline
        System.out.println("=== MATCH TIMELINE ===");
        System.out.println(match.getFormattedTimeline());
    }
}