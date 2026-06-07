// Exporting as package
package ui;

// Importing all necessary classes
import gamemechanics.Event;
import gamemechanics.Match;
import gamemechanics.MatchHistory;
import gamemechanics.Team;
import gamemechanics.TeamFactory;
import gamemechanics.Tournament;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
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
    private final MatchHistory matchHistory;
    private final LinkedHashMap<String, ArrayList<Team>> teamsByLeague;
    private final ArrayList<Team> availableTeams;
    private Match currentMatch;

    // Main constructor
    public ConsoleUI() {
        this.console = new Scanner(System.in);
        this.engine = new SimulationEngine();
        this.matchHistory = new MatchHistory();
        this.teamsByLeague = TeamFactory.createTeamsByLeague();
        this.availableTeams = flattenTeams(this.teamsByLeague);
        this.currentMatch = null;
    }

    // Main method to start the console UI
    public void start() {
        int mainMenuChoice;

        clearConsole();

        do {
            displayMainMenu();
            mainMenuChoice = validateInput("Your choice: ", 1, 7);

            switch (mainMenuChoice) {
                case 1 -> runQuickMatch();
                case 2 -> runCustomMatch();
                case 3 -> runKnockoutTournament();
                case 4 -> runGroupStageTournament();
                case 5 -> {
                    displayTeamListByLeague();
                    pause();
                }
                case 6 -> {
                    displayMatchHistory();
                    pause();
                }
                default -> {
                    clearConsole();
                    System.out.println(RESET + "See you soon!");
                }
            }
        } while (mainMenuChoice != 7);

        console.close();
    }

    /* Game mode methods */
    private void runQuickMatch() {
        Team homeTeam = TeamFactory.getRandomTeam(availableTeams, null);
        Team awayTeam = TeamFactory.getRandomTeam(availableTeams, homeTeam);

        this.currentMatch = new Match(homeTeam, awayTeam);
        runMatchCentre();
    }

    private void runCustomMatch() {
        clearConsole();
        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║          CUSTOM MATCH SETUP          ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(BRIGHT_YELLOW + "Choose the home team:" + RESET);
        Team homeTeam = chooseTeamByLeague(null);

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Choose the away team:" + RESET);
        Team awayTeam = chooseTeamByLeague(homeTeam);

        this.currentMatch = new Match(homeTeam, awayTeam);
        runMatchCentre();
    }

    private void runKnockoutTournament() {
        clearConsole();
        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║        KNOCKOUT TOURNAMENT MODE      ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println("A 4-team knockout tournament will be simulated.");
        System.out.println("Semi-Finals → Final → Champion");
        System.out.println();

        ArrayList<Team> selectedTeams = chooseMultipleTeamsByLeague(4);
        Tournament tournament = new Tournament("FootSim Cup", selectedTeams, engine);

        Team champion = tournament.simulateKnockoutTournament();

        for (Match match : tournament.getMatches()) {
            matchHistory.addMatch(match);
        }

        clearConsole();
        System.out.println(BRIGHT_GREEN + "Tournament complete!" + RESET);
        System.out.println();
        System.out.println(tournament.getFormattedReport());
        System.out.println(BRIGHT_YELLOW + "Champion: " + champion.getName() + RESET);
        pause();
    }

    private void runGroupStageTournament() {
        clearConsole();
        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║     GROUP STAGE + KNOCKOUT MODE      ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println("An 8-team tournament will be simulated.");
        System.out.println("2 groups of 4 → top 2 qualify → semi-finals → final");
        System.out.println();

        ArrayList<Team> selectedTeams = chooseMultipleTeamsByLeague(8);
        Tournament tournament = new Tournament("FootSim Champions Cup", selectedTeams, engine);

        Team champion = tournament.simulateGroupStageAndKnockout();

        for (Match match : tournament.getMatches()) {
            matchHistory.addMatch(match);
        }

        clearConsole();
        System.out.println(BRIGHT_GREEN + "Tournament complete!" + RESET);
        System.out.println();
        System.out.println(tournament.getFormattedReport());
        System.out.println(BRIGHT_YELLOW + "Champion: " + champion.getName() + RESET);
        pause();
    }

    /* Match centre methods */
    private void runMatchCentre() {
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
                    displayMatchSummary();
                    pause();
                }
                default -> returnToMainMenu = true;
            }
        } while (!returnToMainMenu);
    }

    private void watchLiveMatch() {
        clearConsole();

        if (!checkMatchExists()) return;

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

        addCurrentMatchToHistory();
        System.out.println();
        System.out.println(BRIGHT_GREEN + "Match finished!" + RESET);
        displayMatchSummary();
        pause();
    }

    private void simulateRestOfMatch() {
        clearConsole();

        if (!checkMatchExists()) return;

        if (this.currentMatch.isFinished()) {
            System.out.println(RESET + "This match has already finished. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        System.out.println(RESET + "Simulating the rest of the match instantly...");
        System.out.println(BRIGHT_BLACK + "Note: Big chance and penalty choices are auto-selected in instant mode." + RESET);
        System.out.println();

        engine.simulateMatch(this.currentMatch);
        addCurrentMatchToHistory();

        System.out.println(BRIGHT_GREEN + "Simulation complete!" + RESET);
        displayMatchSummary();
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

    /* Display methods */
    private void displayMainMenu() {
        clearConsole();

        System.out.print(RESET
                   + "╔══════════════════════════════════════╗"
            + "\n" + "║          FOOTSIM - MAIN MENU         ║"
            + "\n" + "╚══════════════════════════════════════╝"
            + "\n"
            + "\n" + " [1] Quick Match"
            + "\n" + " [2] Custom Match"
            + "\n" + " [3] Knockout Tournament"
            + "\n" + " [4] Group Stage + Knockout Tournament"
            + "\n" + " [5] View Teams"
            + "\n" + " [6] Match History"
            + "\n" + " [7] Quit"
            + "\n"
            + "\n" + "────────────────────────────────────────"
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
            + "\n" + "║       FOOTSIM - MATCH CENTRE         ║"
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
            + "\n" + "║ [5] View Match Summary               ║"
            + "\n" + "║ [6] Main Menu                        ║"
            + "\n" + "╚══════════════════════════════════════╝"
            + "\n"
            + "\n",
            scoreLine,
            currentMinute,
            status
        );
    }

    private void displayTeamListByLeague() {
        clearConsole();

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║            AVAILABLE TEAMS           ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        for (String league : teamsByLeague.keySet()) {
            System.out.println(BRIGHT_YELLOW + league + RESET);

            ArrayList<Team> leagueTeams = teamsByLeague.get(league);

            for (Team team : leagueTeams) {
                System.out.printf("- %-24s Overall: %d%n", team.getName(), roundVal(team.getAverageOverallRating()));
            }

            System.out.println();
        }
    }

    private void displayTeams() {
        clearConsole();

        if (!checkMatchExists()) return;

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

        if (!checkMatchExists()) return;

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║             MATCH TIMELINE           ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(currentMatch.getFormattedTimeline());
    }

    private void displayMatchSummary() {
        if (!checkMatchExists()) return;

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║             MATCH SUMMARY            ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println("Final Score: " + currentMatch.getScoreLine());
        System.out.println("Winner: " + currentMatch.getWinner());
        System.out.println("Yellow Cards: " + currentMatch.getYellowCardedPlayers().size());
        System.out.println("Red Cards: " + currentMatch.getRedCardedPlayers().size());
        System.out.println();

        System.out.println(BRIGHT_YELLOW + "Goal Events:" + RESET);

        boolean foundGoal = false;
        for (Event event : currentMatch.getEvents()) {
            if (event.isGoal()) {
                System.out.println("- " + event);
                foundGoal = true;
            }
        }

        if (!foundGoal) System.out.println("- No goals scored.");
    }

    private void displayMatchHistory() {
        clearConsole();

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║             MATCH HISTORY            ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(matchHistory.getFormattedHistory());
    }

    /* Team selection methods */
    private ArrayList<String> getLeagueNames() {
        return new ArrayList<>(teamsByLeague.keySet());
    }

    private String chooseLeague() {
        ArrayList<String> leagueNames = getLeagueNames();

        System.out.println(BRIGHT_YELLOW + "Choose League:" + RESET);

        for (int i = 0; i < leagueNames.size(); i++) {
            String league = leagueNames.get(i);
            System.out.println("[" + (i + 1) + "] " + league + " (" + teamsByLeague.get(league).size() + " teams)");
        }

        int choice = validateInput("Choose league: ", 1, leagueNames.size());
        return leagueNames.get(choice - 1);
    }

    private Team chooseTeamByLeague(Team excludedTeam) {
        Team selectedTeam;

        do {
            String selectedLeague = chooseLeague();
            ArrayList<Team> leagueTeams = teamsByLeague.get(selectedLeague);

            System.out.println();
            System.out.println(BRIGHT_YELLOW + "Choose Team from " + selectedLeague + ":" + RESET);

            for (int i = 0; i < leagueTeams.size(); i++) {
                Team team = leagueTeams.get(i);
                String unavailable = team == excludedTeam ? " " + BRIGHT_BLACK + "(already selected)" + RESET : "";
                System.out.printf("[%d] %-24s Overall: %d%s%n", i + 1, team.getName(), roundVal(team.getAverageOverallRating()), unavailable);
            }

            int teamChoice = validateInput("Choose team: ", 1, leagueTeams.size());
            selectedTeam = leagueTeams.get(teamChoice - 1);

            if (selectedTeam == excludedTeam) {
                System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Choose a different team.");
                System.out.println();
            }
        } while (selectedTeam == excludedTeam);

        return selectedTeam;
    }

    private ArrayList<Team> chooseMultipleTeamsByLeague(int numberOfTeams) {
        ArrayList<Team> selectedTeams = new ArrayList<>();

        for (int i = 0; i < numberOfTeams; i++) {
            System.out.println(BRIGHT_YELLOW + "Choose team " + (i + 1) + " of " + numberOfTeams + ":" + RESET);

            Team selectedTeam = chooseTeamByLeagueExcludingList(selectedTeams);
            selectedTeams.add(selectedTeam);

            System.out.println(selectedTeam.getName() + " added.");
            System.out.println();
        }

        return selectedTeams;
    }

    private Team chooseTeamByLeagueExcludingList(ArrayList<Team> selectedTeams) {
        Team selectedTeam;

        do {
            selectedTeam = chooseTeamByLeague(null);

            if (selectedTeams.contains(selectedTeam)) {
                System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " This team has already been selected.");
                System.out.println();
            }
        } while (selectedTeams.contains(selectedTeam));

        return selectedTeam;
    }

    /* Helper methods */
    private ArrayList<Team> flattenTeams(LinkedHashMap<String, ArrayList<Team>> teamsByLeague) {
        ArrayList<Team> teams = new ArrayList<>();

        for (String league : teamsByLeague.keySet()) {
            teams.addAll(teamsByLeague.get(league));
        }

        return teams;
    }

    private void addCurrentMatchToHistory() {
        if (currentMatch != null && currentMatch.isFinished() && !matchHistory.getMatches().contains(currentMatch)) {
            matchHistory.addMatch(currentMatch);
        }
    }

    private boolean checkMatchExists() {
        if (this.currentMatch == null) {
            System.out.println(RESET + "No match has been created yet. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return false;
        }

        return true;
    }

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

    private int roundVal(double val) {
        return (int) Math.round(val);
    }

    private void pause() {
        System.out.println(BRIGHT_BLACK + "[ENTER]" + RESET);
        console.nextLine();
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }
}
