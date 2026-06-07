// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;

public class MatchHistory {
    // Instance fields
    private final ArrayList<Match> matches;

    // Main constructor
    public MatchHistory() {
        this.matches = new ArrayList<>();
    }

    // Adds a completed or in-progress match to the history list.
    public void addMatch(Match match) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        this.matches.add(match);
    }

    // Returns the total number of saved matches.
    public int getNumberOfMatches() {
        return this.matches.size();
    }

    // Checks whether the match history is empty.
    public boolean isEmpty() {
        return this.matches.isEmpty();
    }

    // Returns a safe copy of the saved matches.
    public ArrayList<Match> getMatches() {
        return new ArrayList<>(this.matches);
    }

    // Displays all saved matches in a clean numbered format.
    public String getFormattedHistory() {
        if (this.matches.isEmpty()) return "No matches have been played yet.";

        String result = "MATCH HISTORY\n";

        for (int i = 0; i < this.matches.size(); i++) {
            Match match = this.matches.get(i);

            result += "\n[" + (i + 1) + "] "
                + match.getScoreLine()
                + " | Winner: " + match.getWinner();
        }

        return result;
    }
}
