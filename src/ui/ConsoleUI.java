// Exporting as package
package ui;

// Importing all necessary classes
import gamemechanics.Event;
import gamemechanics.Match;
import gamemechanics.MatchHistory;
import gamemechanics.Team;
import gamemechanics.TeamFactory;
import gamemechanics.Tournament;
import gamemechanics.TournamentFormat;
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
            mainMenuChoice = validateInput("Your choice: ", 1, 6);

            switch (mainMenuChoice) {
                case 1 -> runQuickMatch();
                case 2 -> runCustomMatch();
                case 3 -> runTournamentSetup();
                case 4 -> {
                    displayTeamListByLeague();
                    pause();
                }
                case 5 -> {
                    displayMatchHistory();
                    pause();
                }
                default -> {
                    clearConsole();
                    System.out.println(RESET + "See you soon!");
                }
            }
        } while (mainMenuChoice != 6);

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
        Team homeTeam = chooseTeamByLeague(new ArrayList<>());

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Choose the away team:" + RESET);
        ArrayList<Team> excludedTeams = new ArrayList<>();
        excludedTeams.add(homeTeam);
        Team awayTeam = chooseTeamByLeague(excludedTeams);

        this.currentMatch = new Match(homeTeam, awayTeam);
        runMatchCentre();
    }

    private void runTournamentSetup() {
        clearConsole();
        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        System.out.println("║          TOURNAMENT SETUP            ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();

        TournamentFormat format = chooseTournamentFormat();
        int requiredTeams = format.getDefaultTeamCount();

        if (availableTeams.size() < requiredTeams) {
            System.out.println(BRIGHT_RED + "Not enough teams available." + RESET);
            System.out.println("This format requires " + requiredTeams + " teams, but only " + availableTeams.size() + " are loaded.");
            pause();
            return;
        }

        Team userTeam = chooseOptionalUserTeam();
        ArrayList<Team> selectedTeams = buildTournamentTeamList(requiredTeams, userTeam);

        Tournament tournament = new Tournament(format.getDisplayName() + " Tournament", selectedTeams, engine, userTeam);

        clearConsole();
        System.out.println(BRIGHT_GREEN + "Tournament created!" + RESET);
        System.out.println("Format: " + format.getDisplayName());
        System.out.println("Teams: " + selectedTeams.size());
        if (userTeam != null) System.out.println("User Team: " + userTeam.getName());
        System.out.println();
        System.out.println("Simulating tournament...");
        System.out.println();

        Team champion;

        switch (format) {
            case KNOCKOUT_ONLY -> champion = tournament.simulateKnockoutTournament();
            case CLASSIC_GROUP_STAGE -> champion = tournament.simulateClassicGroupStage();
            case MODERN_LEAGUE_PHASE -> champion = tournament.simulateModernLeaguePhase();
            default -> champion = tournament.simulateKnockoutTournament();
        }

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
                        System.out.println(BRIGHT_BLACK + currentMatch.getCurrentMinute() + "' No major events. " + RESET + "\n" + "Score: " + currentMatch.getScoreLine());
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
        pause();
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

    /* Tournament setup methods */
    private TournamentFormat chooseTournamentFormat() {
        TournamentFormat[] formats = TournamentFormat.values();

        System.out.println(BRIGHT_YELLOW + "Choose tournament format:" + RESET);

        for (int i = 0; i < formats.length; i++) {
            System.out.println("[" + (i + 1) + "] " + formats[i].getDisplayName() + " (" + formats[i].getDefaultTeamCount() + " teams)");
        }

        int choice = validateInput("Choose format: ", 1, formats.length);
        clearPreviousLines(formats.length + 3);

        return formats[choice - 1];
    }

    private Team chooseOptionalUserTeam() {
        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Do you want to choose a team to play as?" + RESET);
        System.out.println("[1] Yes");
        System.out.println("[2] No");

        int choice = validateInput("Your choice: ", 1, 2);
        clearPreviousLines(5);

        if (choice == 2) return null;

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Choose your team:" + RESET);
        return chooseTeamByLeague(new ArrayList<>());
    }

    private ArrayList<Team> buildTournamentTeamList(int requiredTeams, Team userTeam) {
        ArrayList<Team> selectedTeams = new ArrayList<>();

        if (userTeam != null) {
            selectedTeams.add(userTeam);
        }

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "How should the other teams be selected?" + RESET);
        System.out.println("[1] Randomly fill all remaining teams");
        System.out.println("[2] Choose some teams manually, then randomly fill the rest");
        System.out.println("[3] Manually choose all remaining teams");

        int selectionMode = validateInput("Your choice: ", 1, 3);
        clearPreviousLines(6);

        switch (selectionMode) {
            case 1 -> fillRemainingTeamsRandomly(selectedTeams, requiredTeams);
            case 2 -> chooseSomeTeamsThenRandomFill(selectedTeams, requiredTeams);
            default -> chooseAllRemainingTeamsManually(selectedTeams, requiredTeams);
        }

        return selectedTeams;
    }

    private void chooseSomeTeamsThenRandomFill(ArrayList<Team> selectedTeams, int requiredTeams) {
        int remainingSlots = requiredTeams - selectedTeams.size();

        System.out.println();
        System.out.println("Remaining team slots: " + remainingSlots);
        System.out.println("How many additional teams do you want to choose manually?");
        System.out.println(BRIGHT_BLACK + "The rest will be selected randomly." + RESET);

        int manualCount = validateInput("Manual teams: ", 0, remainingSlots);
        clearPreviousLines(4);

        for (int i = 0; i < manualCount; i++) {
            System.out.println(BRIGHT_YELLOW + "Choose manual team " + (i + 1) + " of " + manualCount + ":" + RESET);
            Team team = chooseTeamByLeague(selectedTeams);
            selectedTeams.add(team);
            System.out.println(team.getName() + " added.");
        }

        fillRemainingTeamsRandomly(selectedTeams, requiredTeams);
    }

    private void chooseAllRemainingTeamsManually(ArrayList<Team> selectedTeams, int requiredTeams) {
        while (selectedTeams.size() < requiredTeams) {
            System.out.println();
            System.out.println(BRIGHT_YELLOW + "Choose team " + (selectedTeams.size() + 1) + " of " + requiredTeams + ":" + RESET);
            Team team = chooseTeamByLeague(selectedTeams);
            selectedTeams.add(team);
            System.out.println(team.getName() + " added.");
        }
    }

    private void fillRemainingTeamsRandomly(ArrayList<Team> selectedTeams, int requiredTeams) {
        ArrayList<Team> remainingTeams = new ArrayList<>();

        for (Team team : availableTeams) {
            if (!selectedTeams.contains(team)) {
                remainingTeams.add(team);
            }
        }

        if (remainingTeams.size() + selectedTeams.size() < requiredTeams) {
            throw new IllegalStateException("Not enough teams available to fill the tournament.");
        }

        while (selectedTeams.size() < requiredTeams) {
            Team randomTeam = remainingTeams.remove((int) (Math.random() * remainingTeams.size()));
            selectedTeams.add(randomTeam);
        }

        System.out.println();
        System.out.println("Randomly filled remaining teams.");
    }
    /* */

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
            + "\n" + " [3] Tournament Mode"
            + "\n" + " [4] View Teams"
            + "\n" + " [5] Match History"
            + "\n" + " [6] Quit"
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
        clearConsole();

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
        clearPreviousLines(leagueNames.size() + 2);

        return leagueNames.get(choice - 1);
    }

    private Team chooseTeamByLeague(ArrayList<Team> excludedTeams) {
        if (excludedTeams == null) excludedTeams = new ArrayList<>();

        Team selectedTeam;

        do {
            String selectedLeague = chooseLeague();
            ArrayList<Team> leagueTeams = teamsByLeague.get(selectedLeague);
            System.out.println(BRIGHT_YELLOW + "Choose Team from " + selectedLeague + ":" + RESET);

            for (int i = 0; i < leagueTeams.size(); i++) {
                Team team = leagueTeams.get(i);
                String unavailable = excludedTeams.contains(team) ? " " + BRIGHT_BLACK + "(already selected)" + RESET : "";
                System.out.printf("[%d] %-24s Overall: %d%s%n", i + 1, team.getName(), roundVal(team.getAverageOverallRating()), unavailable);
            }

            int teamChoice = validateInput("Choose team: ", 1, leagueTeams.size());
            selectedTeam = leagueTeams.get(teamChoice - 1);
            clearPreviousLines(leagueTeams.size() + 4);

            if (excludedTeams.contains(selectedTeam)) {
                System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Choose a different team.");
                System.out.println();
            }
        } while (excludedTeams.contains(selectedTeam));

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

    public static void clearPreviousLines(int lines) {
        for (int i = 0; i < lines; i++) {
            System.out.print("\033[1A"); // move cursor up 1 line
            System.out.print("\033[2K"); // clear the entire line
        }
        System.out.flush();
    }
}
