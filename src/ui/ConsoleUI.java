// Exporting as package
package ui;

// Importing all necessary classes
import gamemechanics.Event;
import gamemechanics.Match;
import gamemechanics.Player;
import gamemechanics.Team;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;
import simulation.SimulationEngine;

public class ConsoleUI {
    // Colour constants
    private static final String RESET = "\033[0m";
    private static final String BRIGHT_BLACK = "\033[90m";
    private static final String BRIGHT_RED = "\033[91m";
    private static final String BRIGHT_GREEN = "\033[92m";
    private static final String BRIGHT_CYAN = "\033[96m";
    private static final String BRIGHT_YELLOW = "\033[93m";

    // All necessary fields
    private final Scanner console;
    private final SimulationEngine engine;
    private Match currentMatch;

    // Main constructor
    public ConsoleUI() {
        this.console = new Scanner(System.in);
        this.engine = new SimulationEngine();
        this.currentMatch = null;
    }

    // Main method to start the console UI
    public void start() {
        int mainMenuChoice;

        clearConsole();

        do {
            displayMainMenu();
            mainMenuChoice = validateInput("Your choice: ", 1, 4);

            switch (mainMenuChoice) {
                case 1 -> {
                    this.currentMatch = createSampleMatch();
                    runMatchMenu();
                }
                case 2 -> {
                    if (this.currentMatch == null) {
                        System.out.println(RESET + "No match has been created yet. " + BRIGHT_BLACK + "[ENTER]" + RESET);
                        console.nextLine();
                    } else {
                        runMatchMenu();
                    }
                }
                case 3 -> {
                    displayInstructions();
                    pause();
                }
                default -> {
                    clearConsole();
                    System.out.println(RESET + "See you soon!");
                }
            }
        } while (mainMenuChoice != 4);

        console.close();
    }

    /* Main menu methods */
    private void runMatchMenu() {
        int matchMenuChoice;
        boolean returnToMainMenu = false;

        do {
            displayMatchMenu();
            matchMenuChoice = validateInput("Your choice: ", 1, 6);

            switch (matchMenuChoice) {
                case 1 -> watchLiveMatch();
                case 2 -> simulateRestOfMatch();
                case 3 -> {
                    displayTeams();
                    pause();
                }
                case 4 -> {
                    displayTimeline();
                    pause();
                }
                case 5 -> {
                    this.currentMatch = createSampleMatch();
                    System.out.println(RESET + "New match created. " + BRIGHT_BLACK + "[ENTER]" + RESET);
                    console.nextLine();
                }
                default -> returnToMainMenu = true;
            }
        } while (!returnToMainMenu);
    }

    private void watchLiveMatch() {
        clearConsole();

        if (this.currentMatch == null) {
            System.out.println(RESET + "No match has been created yet. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        if (this.currentMatch.isFinished()) {
            System.out.println(RESET + "This match has already finished. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║          FOOTSIM - LIVE MATCH        ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();
        System.out.println(currentMatch.getHomeTeam().getName() + " vs " + currentMatch.getAwayTeam().getName());
        System.out.println("Starting live simulation...");
        System.out.println();

        Thread liveThread = new Thread(() -> {
            try {
                while (!currentMatch.isFinished()) {
                    ArrayList<Event> newEvents = engine.advanceMatch(currentMatch, 1, false);

                    if (!newEvents.isEmpty()) {
                        displayLiveEvents(newEvents);
                        resolveUserChoiceEvents(newEvents);
                    } else if (currentMatch.getCurrentMinute() > 0 && currentMatch.getCurrentMinute() % 15 == 0) {
                        System.out.println(BRIGHT_BLACK + currentMatch.getCurrentMinute() + "' No major events. " + RESET + "Score: " + currentMatch.getScoreLine());
                    }

                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        liveThread.start();

        try {
            liveThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println(BRIGHT_GREEN + "Match finished!" + RESET);
        System.out.println("Final Score: " + currentMatch.getScoreLine());
        System.out.println("Winner: " + currentMatch.getWinner());
        pause();
    }

    private void simulateRestOfMatch() {
        clearConsole();

        if (this.currentMatch == null) {
            System.out.println(RESET + "No match has been created yet. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        if (this.currentMatch.isFinished()) {
            System.out.println(RESET + "This match has already finished. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        System.out.println(RESET + "Simulating the rest of the match instantly...");
        System.out.println(BRIGHT_BLACK + "Note: Big chance and penalty choices are auto-selected in instant mode." + RESET);
        System.out.println();

        engine.simulateMatch(this.currentMatch);

        System.out.println(BRIGHT_GREEN + "Simulation complete!" + RESET);
        System.out.println("Final Score: " + currentMatch.getScoreLine());
        System.out.println("Winner: " + currentMatch.getWinner());
        pause();
    }

    private void displayLiveEvents(ArrayList<Event> events) {
        for (Event event : events) {
            System.out.println(formatEvent(event));
        }

        System.out.println("Score: " + currentMatch.getScoreLine());
        System.out.println();
    }

    // User choice processing
    private void resolveUserChoiceEvents(ArrayList<Event> events) {
        for (Event event : events) {
            if (event.isBigChance() && !event.isResolved()) {
                String selectedChoice = askForChanceChoice(event);
                ArrayList<Event> resultEvents = engine.resolveChance(currentMatch, event, selectedChoice);

                System.out.println();
                System.out.println(BRIGHT_YELLOW + "Result:" + RESET);

                for (Event resultEvent : resultEvents) {
                    System.out.println(formatEvent(resultEvent));
                }

                System.out.println("Score: " + currentMatch.getScoreLine());
                System.out.println();
            }
        }
    }

    // User choice handling
    private String askForChanceChoice(Event event) {
        ArrayList<String> choices = event.getChoices();

        System.out.println(BRIGHT_YELLOW + "User decision required!" + RESET);
        System.out.println(event.getDescription());
        System.out.println();

        for (int i = 0; i < choices.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + choices.get(i));
        }

        System.out.println();

        int choice = validateInput("Your choice: ", 1, choices.size());
        return choices.get(choice - 1);
    }


    /* Display methods for teams, timeline, main menu, match menu, and instructions */
    private void displayTeams() {
        clearConsole();

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║             TEAM INFORMATION         ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(BRIGHT_YELLOW + "HOME TEAM" + RESET);
        System.out.println(currentMatch.getHomeTeam());
        System.out.println();

        System.out.println(BRIGHT_YELLOW + "AWAY TEAM" + RESET);
        System.out.println(currentMatch.getAwayTeam());
        System.out.println();
    }

    private void displayTimeline() {
        clearConsole();

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║             MATCH TIMELINE           ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        if (currentMatch == null) {
            System.out.println("No match has been created yet.");
        } else {
            System.out.println(currentMatch.getFormattedTimeline());
        }
    }

    private void displayMainMenu() {
        clearConsole();

        System.out.print(RESET
                   + "╔══════════════════════════════════╗"
            + "\n" + "║        FOOTSIM - Main Menu       ║"
            + "\n" + "╚══════════════════════════════════╝"
            + "\n"
            + "\n" + " [1] New Match"
            + "\n" + " [2] Continue Match"
            + "\n" + " [3] Instructions"
            + "\n" + " [4] Quit"
            + "\n"
            + "\n" + "────────────────────────────────────"
            + "\n"
        );
    }

    private void displayMatchMenu() {
        clearConsole();

        String scoreLine = currentMatch.getScoreLine();
        String status = getMatchStatus();
        int currentMinute = currentMatch.getCurrentMinute();

        System.out.printf(RESET
                   + "╔══════════════════════════════════════╗"
            + "\n" + "║       FOOTSIM - Match Centre         ║"
            + "\n" + "╚══════════════════════════════════════╝"
            + "\n"
            + "\n" + " %s"
            + "\n" + " Minute: %d'"
            + "\n" + " Status: %s"
            + "\n"
            + "\n" + "╔══════════════════════════════════════╗"
            + "\n" + "║ [1] Watch Live Match                 ║"
            + "\n" + "║ [2] Simulate Rest Instantly          ║"
            + "\n" + "║ [3] View Teams                       ║"
            + "\n" + "║ [4] View Match Timeline              ║"
            + "\n" + "║ [5] New Match                        ║"
            + "\n" + "║ [6] Main Menu                        ║"
            + "\n" + "╚══════════════════════════════════════╝"
            + "\n"
            + "\n",
            scoreLine,
            currentMinute,
            status
        );
    }

    private void displayInstructions() {
        clearConsole();

        System.out.println(RESET
            + "╔═════════════════════════════════════════════════════════════════════╗"
            + "\n" + "║ " + BRIGHT_BLACK + "OBJECTIVE" + RESET + "                                                           ║"
            + "\n" + "║  FootSim simulates a football match using player and team ratings.  ║"
            + "\n" + "║  The goal is to demonstrate how the Player, Team, Match, Event,     ║"
            + "\n" + "║  SimulationEngine, and live match logic work together.              ║"
            + "\n" + "╠═════════════════════════════════════════════════════════════════════╣"
            + "\n" + "║ " + BRIGHT_BLACK + "LIVE MATCH" + RESET + "                                                          ║"
            + "\n" + "║  Watch Live Match advances the match one simulated minute at a time.║"
            + "\n" + "║  Goals, fouls, cards, penalties, and big chances can occur.         ║"
            + "\n" + "╠═════════════════════════════════════════════════════════════════════╣"
            + "\n" + "║ " + BRIGHT_BLACK + "USER CHOICES" + RESET + "                                                        ║"
            + "\n" + "║  During big chances and penalties, you choose what the player does. ║"
            + "\n" + "║  The simulation engine uses the selected choice and player ratings  ║"
            + "\n" + "║  to decide whether the chance succeeds or fails.                    ║"
            + "\n" + "╠═════════════════════════════════════════════════════════════════════╣"
            + "\n" + "║ " + BRIGHT_BLACK + "MENU OPTIONS" + RESET + "                                                        ║"
            + "\n" + "║  New Match creates a new Arsenal vs Barcelona pseudo-data match.    ║"
            + "\n" + "║  Continue Match returns to the current match if one exists.         ║"
            + "\n" + "║  View Teams shows player rosters and calculated team ratings.       ║"
            + "\n" + "║  View Timeline shows all events recorded so far.                    ║"
            + "\n" + "╚═════════════════════════════════════════════════════════════════════╝"
            + "\n"
        );
    }
    /* */

    // Sample team creations
    private Match createSampleMatch() {
        // Arsenal FC (Home Team) pseudo-data
        Player arsGk   = new Player("David Raya", "GK", 83, 40, 80, 84, 48, 77);
        Player arsDef1 = new Player("William Saliba", "DEF", 81, 40, 70, 73, 87, 83);
        Player arsDef2 = new Player("Gabriel Magalhaes", "DEF", 69, 45, 62, 63, 86, 84);
        Player arsDef3 = new Player("Ben White", "DEF", 77, 54, 76, 77, 82, 76);
        Player arsDef4 = new Player("Jurriën Timber", "DEF", 79, 53, 71, 79, 80, 78);
        Player arsMid1 = new Player("Declan Rice", "MID", 75, 66, 77, 79, 83, 83);
        Player arsMid2 = new Player("Martin Odegaard", "MID", 76, 81, 89, 88, 58, 64);
        Player arsMid3 = new Player("Mikel Merino", "MID", 67, 75, 77, 80, 79, 81);
        Player arsAtk1 = new Player("Bukayo Saka", "ATK", 86, 81, 83, 87, 45, 69);
        Player arsAtk2 = new Player("Kai Havertz", "ATK", 80, 81, 79, 82, 47, 77);
        Player arsAtk3 = new Player("Leandro Trossard", "ATK", 78, 82, 80, 84, 34, 65);

        // FC Barcelona (Away Team) pseudo-data
        Player barGk   = new Player("Marc-André ter Stegen", "GK", 84, 83, 88, 87, 47, 85);
        Player barDef1 = new Player("Ronald Araújo", "DEF", 82, 49, 63, 65, 85, 84);
        Player barDef2 = new Player("Jules Koundé", "DEF", 80, 45, 74, 76, 84, 77);
        Player barDef3 = new Player("Pau Cubarsí", "DEF", 68, 33, 72, 70, 79, 66);
        Player barDef4 = new Player("Alejandro Balde", "DEF", 91, 47, 71, 78, 74, 64);
        Player barMid1 = new Player("Frenkie de Jong", "MID", 79, 69, 85, 87, 77, 78);
        Player barMid2 = new Player("Pedri", "MID", 77, 70, 86, 87, 69, 64);
        Player barMid3 = new Player("Gavi", "MID", 77, 68, 77, 83, 72, 72);
        Player barAtk1 = new Player("Robert Lewandowski", "ATK", 75, 88, 71, 84, 45, 81);
        Player barAtk2 = new Player("Lamine Yamal", "ATK", 84, 77, 78, 86, 33, 53);
        Player barAtk3 = new Player("Raphinha", "ATK", 89, 80, 80, 85, 54, 73);

        Team homeTeam = new Team("Arsenal FC", new ArrayList<>(Arrays.asList(
            arsGk, arsDef1, arsDef2, arsDef3, arsDef4, arsMid1, arsMid2, arsMid3, arsAtk1, arsAtk2, arsAtk3
        )));

        Team awayTeam = new Team("FC Barcelona", new ArrayList<>(Arrays.asList(
            barGk, barDef1, barDef2, barDef3, barDef4, barMid1, barMid2, barMid3, barAtk1, barAtk2, barAtk3
        )));

        return new Match(homeTeam, awayTeam);
    }

    /* Helper methods */
    private String formatEvent(Event event) {
        String marker = "";

        if (event.isGoal()) marker = BRIGHT_GREEN + " GOAL " + RESET;
        else if (event.isPenalty()) marker = BRIGHT_YELLOW + " PENALTY " + RESET;
        else if (event.isCard()) marker = BRIGHT_YELLOW + " CARD " + RESET;
        else if (event.isBigChance()) marker = BRIGHT_CYAN + " CHANCE " + RESET;

        return marker + event;
    }

    private String getMatchStatus() {
        if (currentMatch.isFinished()) return "Finished";
        if (currentMatch.hasStarted()) return "In Progress";
        return "Not Started";
    }

    private int validateInput(String prompt, int min, int max) {
        boolean validInput = false;
        int input = 0;

        do {
            System.out.print(prompt);

            try {
                input = console.nextInt();
                console.nextLine();

                if (input >= min && input <= max) {
                    validInput = true;
                } else {
                    System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Use a number between " + min + " and " + max + ".");
                }
            } catch (InputMismatchException e) {
                console.nextLine();
                System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Use a number between " + min + " and " + max + ".");
            }
        } while (!validInput);

        return input;
    }

    private void pause() {
        System.out.println(BRIGHT_BLACK + "[ENTER]" + RESET);
        console.nextLine();
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    /* */
}
