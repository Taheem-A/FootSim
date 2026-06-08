// Exporting as package
package gamemechanics.core;

// Importing necessary classes
import java.util.ArrayList;

public class MatchHistory {
    // Instance field
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

    // Returns a copy of the saved matches.
    public ArrayList<Match> getMatches() {
        return new ArrayList<>(this.matches);
    }

    // Overriden 'toString()' method
    @Override
    public String toString() {
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
