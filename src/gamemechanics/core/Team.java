// Exporting as package
package gamemechanics.core;

// Importing necessary classes
import java.util.ArrayList;

public class Team {
    // Instance fields
    private String name;
    private ArrayList<Player> players;

    // Default constructor
    public Team() {
        // The 'this()' calls another constructor matching the parameters and allows for less repetition
        this("Unnamed Team");
    }

    // Overloaded constructor
    public Team(String name) {
        this(name, new ArrayList<>());
    }

    // Second overloaded constructor
    public Team(String name, ArrayList<Player> players) {
        this.name = validateName(name);
        this.players = new ArrayList<>();

        if (players != null) for (Player player : players) addPlayer(player);
    }

    /* Getters */
    public String getName() {
        return this.name;
    }

    public ArrayList<Player> getPlayers() {
        return new ArrayList<>(this.players);
    }
    /* */

    // Method to get the goalkeeper of the team
    public Player getGoalkeeper() {
        for (Player player : this.players) if (player.getPosition().equalsIgnoreCase("GK")) return player;

        return null;
    }

    // Method to get the overall team rating\
    public double getAverageOverallRating() {
        if (players.isEmpty()) return 0;

        double total = 0;

        for (Player player : players) total += player.getOverallRating();
        return total / players.size();
    }

    /* Special Modifiers for 'players' field */
    public final boolean addPlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Cannot add a null player.");

        if (findPlayerByName(player.getName()) != null) return false;

        players.add(player);
        return true;
    }

    public Player findPlayerByName(String name) {
        if (this.players == null) return null;
        if (name == null || name.trim().isEmpty()) return null;

        name = name.trim();
        for (Player player : this.players) if (player.getName().equalsIgnoreCase(name)) return player;
        return null;
    }
    /* */

    /* Team Rating Methods */
    public double getTeamAttackRating() {
        return calculateAverageByCategory("ATK", null);
    }

    public double getTeamMidfieldRating() {
        return calculateAverageByCategory("MID", null);
    }

    public double getTeamDefenceRating() {
        return calculateAverageByCategory("DEF", null);
    }

    public double getTeamGoalkeeperRating() {
        Player goalkeeper = getGoalkeeper();

        if (goalkeeper == null) return 50;

        return goalkeeper.getGoalkeepingRating();
    }

    public double getTeamAttackRating(ArrayList<Player> excludedPlayers) {
        return calculateAverageByCategory("ATK", excludedPlayers);
    }

    public double getTeamMidfieldRating(ArrayList<Player> excludedPlayers) {
        return calculateAverageByCategory("MID", excludedPlayers);
    }

    public double getTeamDefenceRating(ArrayList<Player> excludedPlayers) {
        return calculateAverageByCategory("DEF", excludedPlayers);
    }

    /* */

    /* Helper Methods */
    private double calculateAverageByCategory(String category, ArrayList<Player> excludedPlayers) {
        if (this.players.isEmpty()) return 0;

        double total = 0;

        // Excluded players are skipped to simulate real life consequnces
        for (Player player : this.players) if (!(excludedPlayers != null && findPlayer(player, excludedPlayers))) total += getPlayerRatingByCategory(player, category);

        return total / this.players.size();
    }

    private double getPlayerRatingByCategory(Player player, String category) {
        if (category == null) return player.getOverallRating();

        category = category.trim().toUpperCase();

        return switch (category) {
            case "ATK" -> player.getAttackRating();
            case "MID" -> player.getMidfieldRating();
            case "DEF" -> player.getDefenceRating();
            case "GK" -> player.getGoalkeepingRating();
            default -> player.getOverallRating();
        };
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Team name cannot be empty/blank.");
        return name.trim();
    }

    private int roundVal(double val) {
        return (int) Math.round(val);
    }

    private boolean findPlayer(Player player, ArrayList<Player> playerList) {
        if (player == null || playerList == null) throw new IllegalArgumentException("Player and player list cannot be null.");
        for (Player p : playerList) if (p.getName().equalsIgnoreCase(player.getName())) return true;
        return false;
    }

    /* */

    // Overridden 'toString()' method
    @Override
    public String toString() {
        String result = this.name + "\n"
            + "Players: " + this.players.size() + "\n"
            + "Overall: " + roundVal(getAverageOverallRating()) + "\n"
            + "Attack: " + roundVal(getTeamAttackRating()) + "\n"
            + "Midfield: " + roundVal(getTeamMidfieldRating()) + "\n"
            + "Defence: " + roundVal(getTeamDefenceRating()) + "\n"
            + "Goalkeeper: " + roundVal(getTeamGoalkeeperRating()) + "\n"
            + "Roster:";

        for (Player player : players) result += "\n- " + player.getName();

        return result;
    }
}
