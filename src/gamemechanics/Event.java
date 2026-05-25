// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;

public class Event {
    // Instance fields
    private int minute;
    private String type;
    private Team team;
    private Player player;
    private String description;
    private boolean bigChance;
    private ArrayList<String> choices;
    private String selectedChoice;
    private boolean successful;
    private boolean resolved;

    // Constructor for regular events
    public Event(int minute, String type, Team team, Player player, String description) {
        this(minute, type, team, player, description, false, new ArrayList<>());
    }

    // Constructor for big chance / interactive events
    public Event(int minute, String type, Team team, Player player, String description, boolean bigChance, ArrayList<String> choices) {
        this.minute = validateMinute(minute);
        this.type = validateType(type);
        this.description = validateDescription(description);
        this.bigChance = bigChance;
        this.choices = new ArrayList<>();
        this.selectedChoice = "";
        this.successful = false;
        this.resolved = false;

        if (requiresTeam(this.type) && team == null) {
            throw new IllegalArgumentException("This event type requires a team.");
        }

        this.team = team;
        this.player = player;

        if (bigChance) {
            if (choices == null || choices.isEmpty()) {
                throw new IllegalArgumentException("A big chance event must have choices.");
            }

            for (String choice : choices) {
                addChoice(choice);
            }
        }
    }

    /* Getters */
    public int getMinute() {
        return this.minute;
    }

    public String getType() {
        return this.type;
    }

    public Team getTeam() {
        return this.team;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isBigChance() {
        return this.bigChance;
    }

    public ArrayList<String> getChoices() {
        return new ArrayList<>(this.choices);
    }

    public String getSelectedChoice() {
        return this.selectedChoice;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public boolean isResolved() {
        return this.resolved;
    }
    /* */

    /* Setters / Special Modifiers */
    public void setSelectedChoice(String selectedChoice) {
        if (!this.bigChance) {
            throw new IllegalStateException("Only big chance events can have a selected choice.");
        }

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected choice cannot be blank/empty.");
        }

        selectedChoice = selectedChoice.trim();

        for (String choice : this.choices) {
            if (choice.equalsIgnoreCase(selectedChoice)) {
                this.selectedChoice = choice;
                return;
            }
        }

        throw new IllegalArgumentException("Selected choice must be one of the available choices.");
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
        this.resolved = true;
    }

    private void addChoice(String choice) {
        if (choice == null || choice.trim().isEmpty()) {
            throw new IllegalArgumentException("Choice cannot be blank/empty.");
        }

        this.choices.add(choice.trim());
    }
    /* */

    /* Event Checking Methods */
    public boolean hasTeam() {
        return this.team != null;
    }

    public boolean hasPlayer() {
        return this.player != null;
    }

    public boolean isGoal() {
        return this.type.equals("GOAL");
    }

    public boolean isPenalty() {
        return this.type.equals("PENALTY");
    }

    public boolean isCard() {
        return this.type.equals("YELLOW_CARD") || this.type.equals("RED_CARD");
    }

    public boolean isScoringChance() {
        return this.type.equals("SHOT") || this.type.equals("BIG_CHANCE") || this.type.equals("PENALTY") || this.type.equals("GOAL");
    }
    /* */

    /* Validation Methods */
    private int validateMinute(int minute) {
        if (minute < 0 || minute > 120) {
            throw new IllegalArgumentException("Minute must be between 0 and 120.");
        }

        return minute;
    }

    private String validateType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be blank/empty.");
        }

        type = type.trim().toUpperCase().replace(" ", "_");

        String[] validTypes = {
            "KICKOFF",
            "SHOT",
            "BIG_CHANCE",
            "GOAL",
            "SAVE",
            "MISS",
            "FOUL",
            "YELLOW_CARD",
            "RED_CARD",
            "PENALTY",
            "HALF_TIME",
            "FULL_TIME",
            "COMMENTARY"
        };

        for (String validType : validTypes) {
            if (type.equals(validType)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid event type.");
    }

    private String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Event description cannot be blank/empty.");
        }

        return description.trim();
    }

    private boolean requiresTeam(String type) {
        return !(type.equals("KICKOFF") || type.equals("HALF_TIME") || type.equals("FULL_TIME") || type.equals("COMMENTARY"));
    }
    /* */

    // Overridden 'toString()' method
    @Override
    public String toString() {
        String result = this.minute + "' [" + this.type + "] " + this.description;

        if (this.bigChance && !this.selectedChoice.isEmpty()) {
            result += " | Choice: " + this.selectedChoice;
        }

        if (this.bigChance && this.resolved) {
            result += this.successful ? " | Successful" : " | Unsuccessful";
        }

        return result;
    }
}