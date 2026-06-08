// Exporting as package
package gamemechanics.core;

// Importing necessary classes
import java.util.ArrayList;

public class Event {
    // Colour constants
    private static final String RESET = "\033[0m";
    private static final String BRIGHT_BLACK = "\033[90m";
    private static final String BRIGHT_RED = "\033[91m";
    private static final String RED = "\033[31m";
    private static final String BRIGHT_GREEN = "\033[92m";
    private static final String GREEN = "\033[32m";
    private static final String BRIGHT_CYAN = "\033[96m";
    private static final String BRIGHT_YELLOW = "\033[93m";
    private static final String YELLOW = "\033[33m";

    // Instance fields
    private int minute;
    private EventType type;
    private Team team;
    private Player player;
    private String description;
    private boolean bigChance;
    private ArrayList<String> choices;
    private String selectedChoice;
    private boolean successful;
    private boolean resolved;

    // Constructor for regular events
    public Event(int minute, EventType type, Team team, Player player, String description) {
        this(minute, type, team, player, description, false, new ArrayList<>());
    }

    // Constructor for big chance events
    public Event(int minute, EventType type, Team team, Player player, String description, boolean bigChance, ArrayList<String> choices) {
        if (minute < 0 || minute > 120) throw new IllegalArgumentException("Minute must be between 0 and 120.");
        this.minute = minute;

        if (type == null) throw new IllegalArgumentException("Event type cannot be null.");
        this.type = type;

        if (description == null || description.trim().isEmpty()) throw new IllegalArgumentException("Event description cannot be blank/empty.");
        this.description = description.trim();
        
        this.bigChance = bigChance;
        this.choices = new ArrayList<>();
        this.selectedChoice = "";
        this.successful = false;
        this.resolved = false;

        if ((!(this.type == EventType.KICKOFF || this.type == EventType.HALF_TIME || this.type == EventType.FULL_TIME || this.type == EventType.COMMENTARY)) && team == null) {
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
        return this.type.name();
    }

    public EventType getEventType() {
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

    /* Setters / custom modifiers */
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

    /* Event checking methods */
    public boolean isGoal() {
        return this.type == EventType.GOAL;
    }

    public boolean isPenalty() {
        return this.type == EventType.PENALTY;
    }

    public boolean isCard() {
        return this.type == EventType.YELLOW_CARD || this.type == EventType.RED_CARD;
    }
    /* */

    // Overridden 'toString()' method
    @Override
    public String toString() {
        String eventTypeColour = switch (this.type) {
            case GOAL -> GREEN;
            case PENALTY -> BRIGHT_GREEN;
            case YELLOW_CARD -> BRIGHT_YELLOW;
            case RED_CARD -> BRIGHT_RED;
            case SHOT, BIG_CHANCE -> BRIGHT_CYAN;
            case MISS -> RED;
            case FOUL -> YELLOW;
            default -> BRIGHT_BLACK;
        };
        String result = String.format("%2d", this.minute) + "' [" + eventTypeColour + this.type + RESET + "] " + this.description;

        if (this.bigChance && !this.selectedChoice.isEmpty()) {
            result += " | Choice: " + this.selectedChoice;
        }

        if (this.bigChance && this.resolved) {
            result += this.successful ? " | " + BRIGHT_GREEN + "Successful" + RESET : " | " + BRIGHT_RED + "Unsuccessful" + RESET;
        }

        return result;
    }
}
