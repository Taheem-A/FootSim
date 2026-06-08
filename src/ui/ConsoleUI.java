// Exporting as package
package ui;

// Importing all necessary classes
import gamemechanics.core.Event;
import gamemechanics.tactics.ManagerDecision;
import gamemechanics.core.Match;
import gamemechanics.core.MatchHistory;
import gamemechanics.tactics.TacticalStyle;
import gamemechanics.core.Team;
import gamemechanics.factory.TeamFactory;
import gamemechanics.tactics.TeamTactics;
import gamemechanics.tactics.TeamTalk;
import gamemechanics.tournament.Tournament;
import gamemechanics.tournament.TournamentFormat;
import gamemechanics.tournament.TournamentStanding;
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

    // Instance fields
    private final Scanner console;
    private final SimulationEngine engine;
    private final MatchHistory matchHistory;
    private final LinkedHashMap<String, ArrayList<Team>> teamsByLeague;
    private final ArrayList<Team> availableTeams;
    private Match currentMatch;
    private Team controlledTeam;

    // Enum to manage how user-team matches are handled in tournaments
    private enum TournamentUserMatchMode {
        SIMULATE_ALL,
        ASK_BEFORE_EACH,
        PLAY_ALL
    }

    // Main constructor
    public ConsoleUI() {
        this.console = new Scanner(System.in);
        this.engine = new SimulationEngine();
        this.matchHistory = new MatchHistory();
        this.teamsByLeague = TeamFactory.createTeamsByLeague();
        this.availableTeams = flattenTeams(this.teamsByLeague);
        this.currentMatch = null;
        this.controlledTeam = null;
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

    /*
        Quick match: Match between two random teams
    */
    private void runQuickMatch() {
        Team homeTeam = TeamFactory.getRandomTeam(availableTeams, null);
        Team awayTeam = TeamFactory.getRandomTeam(availableTeams, homeTeam);

        this.currentMatch = new Match(homeTeam, awayTeam);
        this.controlledTeam = chooseControlledTeam(currentMatch);
        runMatchCentre();
    }

    /*
        Custom match: Match with user-selected teams
    */
    private void runCustomMatch() {
        clearConsole();

        printHeader("CUSTOM MATCH SETUP");

        System.out.println(BRIGHT_YELLOW + "Choose the home team:" + RESET);
        Team homeTeam = chooseTeamByLeague(new ArrayList<>());

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Choose the away team:" + RESET);
        ArrayList<Team> excludedTeams = new ArrayList<>();
        excludedTeams.add(homeTeam);
        Team awayTeam = chooseTeamByLeague(excludedTeams);

        this.currentMatch = new Match(homeTeam, awayTeam);
        this.controlledTeam = chooseControlledTeam(currentMatch);
        runMatchCentre();
    }

    /*
        Tournament setup: Configure and run a tournament
    */
    private void runTournamentSetup() {
        clearConsole();

        printHeader("TOURNAMENT SETUP");

        TournamentFormat format = chooseTournamentFormat();
        int requiredTeams = format.getDefaultTeamCount();

        Team userTeam = chooseOptionalUserTeam();
        ArrayList<Team> selectedTeams = buildTournamentTeamList(requiredTeams, userTeam);
        TournamentUserMatchMode tournamentUserMatchMode = chooseTournamentUserMatchMode(userTeam);

        Tournament tournament = new Tournament(format.getDisplayName() + " Tournament", selectedTeams, engine, userTeam);
        tournament.setMatchRunner((homeTeam, awayTeam, userTeamMatch) -> {
            if (!userTeamMatch) return engine.simulateMatch(homeTeam, awayTeam);

            if (tournamentUserMatchMode == TournamentUserMatchMode.SIMULATE_ALL) return simulateUserTournamentMatchWithResult(homeTeam, awayTeam, userTeam);

            if (tournamentUserMatchMode == TournamentUserMatchMode.PLAY_ALL) return playInteractiveTournamentMatch(homeTeam, awayTeam, userTeam);

            if (askPlayTournamentMatch(homeTeam, awayTeam, userTeam)) return playInteractiveTournamentMatch(homeTeam, awayTeam, userTeam);

            return simulateUserTournamentMatchWithResult(homeTeam, awayTeam, userTeam);
        });

        tournament.setProgressListener(new Tournament.TournamentProgressListener() {
            @Override
            public void onGroupCompleted(String groupName, ArrayList<TournamentStanding> standings) {
                if (userTeam != null && standingsContainTeam(standings, userTeam)) displayGroupStandingUpdate(groupName, standings, userTeam);
            }

            @Override
            public void onLeagueRoundCompleted(int roundNumber, ArrayList<TournamentStanding> standings) {
                if (userTeam != null) displayLeaguePhaseUpdate(roundNumber, standings, userTeam);
            }

            @Override
            public void onKnockoutRoundCompleted(String roundName, ArrayList<Match> roundMatches, ArrayList<Team> winners, ArrayList<String> penaltyNotes) {
                if (userTeam != null && roundIsRelevantToUser(roundMatches, winners, userTeam)) displayKnockoutBracketUpdate(roundName, roundMatches, winners, penaltyNotes, userTeam);
            }
        });

        tournament.setPenaltyShootoutRunner((teamA, teamB, userTeamShootout) -> {
            if (userTeamShootout && tournamentUserMatchMode != TournamentUserMatchMode.SIMULATE_ALL && askPlayPenaltyShootout(teamA, teamB, userTeam)) return playPenaltyShootout(teamA, teamB, userTeam);

            return simulatePenaltyShootout(teamA, teamB, userTeamShootout);
        });

        clearConsole();

        System.out.println(
            BRIGHT_GREEN + "Tournament created!" + RESET + "\n" +
            "Format: " + format.getDisplayName() + "\n" +
            "Teams: " + selectedTeams.size() + "\n"
        );

        if (userTeam != null) System.out.println("User Team: " + userTeam.getName());
        System.out.println("User-Team Match Mode: " + formatTournamentUserMatchMode(tournamentUserMatchMode) + "\n");
        pause();

        switch (format) {
            case KNOCKOUT_ONLY -> tournament.simulateKnockoutTournament();
            case CLASSIC_GROUP_STAGE -> tournament.simulateClassicGroupStage();
            case MODERN_LEAGUE_PHASE -> tournament.simulateModernLeaguePhase();
            default -> tournament.simulateKnockoutTournament();
        }

        for (Match match : tournament.getMatches()) {
            matchHistory.addMatch(match);
        }

        clearConsole();
        System.out.println(
            BRIGHT_GREEN + "Tournament complete!" + RESET + "\n" +
            tournament.getFormattedReport() + "\n"
        );
        pause();
    }
    /* */

    /* Match centre methods */
    private void runMatchCentre() {
        int matchMenuChoice;
        boolean returnToMainMenu = false;

        do {
            displayMatchMenu();
            matchMenuChoice = validateInput("Your choice: ", 1, 6);

            switch (matchMenuChoice) {
                case 1 -> playInteractiveMatch(currentMatch, controlledTeam, true);
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

    // Handles playing an interactive tournament match
    private Match playInteractiveTournamentMatch(Team homeTeam, Team awayTeam, Team userTeam) {
        Match match = new Match(homeTeam, awayTeam);
        this.currentMatch = match;
        this.controlledTeam = userTeam;

        clearConsole();

        printHeader("USER TEAM TOURNAMENT MATCH");
        System.out.println(homeTeam.getName() + " vs " + awayTeam.getName() + "\n" + "You are controlling: " + userTeam.getName() + "\n");

        pause();

        playInteractiveMatch(match, userTeam, false);
        return match;
    }

    // Simulates a tournament match
    private Match simulateUserTournamentMatchWithResult(Team homeTeam, Team awayTeam, Team userTeam) {
        Match match = engine.simulateMatch(homeTeam, awayTeam);

        clearConsole();

        printHeader("USER TEAM MATCH SIMULATED");
        System.out.println(
            homeTeam.getName() + " vs " + awayTeam.getName() + "\n" +
            "You are controlling: " + userTeam.getName() + "\n" +

            "\n" +

            BRIGHT_YELLOW + "Result:" + RESET + "\n" +
            match.getScoreLine() + "\n" +
            "Winner after 90': " + match.getWinner() + "\n"
        );

        if (match.getHomeScore() == match.getAwayScore()) System.out.println(BRIGHT_BLACK + "Note: If this is a knockout match, penalties will decide who advances in the bracket update." + RESET + "\n");

        pause();

        return match;
    }

    // Plays an interactive match
    private void playInteractiveMatch(Match match, Team userTeam, boolean saveToHistory) {
        clearConsole();

        if (match == null) {
            System.out.println("No match has been created yet.");
            pause();
            return;
        }

        if (match.isFinished()) {
            System.out.println("This match has already finished.");
            pause();
            return;
        }

        setupMatchTactics(match, userTeam);
        
        printHeader("FOOTSIM - INTERACTIVE MATCH");
        System.out.println("\n" + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName() + "\n");

        if (userTeam != null) System.out.println("You are controlling: " + userTeam.getName() + "\n");
        else System.out.println("No controlled team. Big chances will be auto-resolved.\n");

        while (!match.isFinished()) {
            ArrayList<Event> newEvents = engine.advanceMatch(match, 1, false);

            if (!newEvents.isEmpty()) {
                displayLiveEvents(newEvents);
                resolveChanceEvents(match, newEvents, userTeam);
            } else if (match.getCurrentMinute() > 0 && match.getCurrentMinute() % 15 == 0) {
                System.out.println(BRIGHT_BLACK + match.getCurrentMinute() + "' No major events." + RESET);
                System.out.println("Score: " + match.getScoreLine());
                System.out.println();
            }

            if (!match.isFinished() && shouldAskManagerDecision(match)) askManagerDecision(match, userTeam);

            if (!match.isFinished() && match.getCurrentMinute() == 45 && userTeam != null) askHalfTimeTeamTalk(match, userTeam);

            sleep(250);
        }

        if (saveToHistory) addCurrentMatchToHistory();

        System.out.println("\n" + BRIGHT_GREEN + "Match finished!" + RESET + "\n");
        pause();
        displayMatchSummary();
        pause();
    }

    // Determines if manager decision is required
    private boolean shouldAskManagerDecision(Match match) {
        int min = match.getCurrentMinute();
        return min == 15 || min == 30 || min == 60 || min == 75;
    }

    // Pre-match tactic setup
    private void setupMatchTactics(Match match, Team userTeam) {
        if (userTeam != null) {
            TacticalStyle userStyle = chooseTacticalStyle(userTeam);
            match.setTactics(userTeam, new TeamTactics(userStyle));
            engine.setRandomTactics(match, match.getOpponent(userTeam));
        } else {
            engine.setRandomTactics(match, match.getHomeTeam());
            engine.setRandomTactics(match, match.getAwayTeam());
        }
    }

    // Instantly simulates the current match
    private void simulateRestOfMatch() {
        clearConsole();

        if (!checkMatchExists()) return;

        if (this.currentMatch.isFinished()) {
            System.out.println(RESET + "This match has already finished. " + BRIGHT_BLACK + "[ENTER]" + RESET);
            console.nextLine();
            return;
        }

        if (!currentMatch.hasStarted()) {
            engine.setRandomTactics(currentMatch, currentMatch.getHomeTeam());
            engine.setRandomTactics(currentMatch, currentMatch.getAwayTeam());
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

    // Displays live events
    private void displayLiveEvents(ArrayList<Event> events) {
        for (Event event : events) {
            System.out.println(formatEvent(event));
        }

        System.out.println("Score: " + currentMatch.getScoreLine());
        System.out.println("Momentum: "
            + currentMatch.getHomeTeam().getName() + " (" + currentMatch.getMomentumDescription(currentMatch.getHomeTeam()) + ") | "
            + currentMatch.getAwayTeam().getName() + " (" + currentMatch.getMomentumDescription(currentMatch.getAwayTeam()) + ")"
        );
        System.out.println();
    }

    // Resolve chance events
    private void resolveChanceEvents(Match match, ArrayList<Event> events, Team userTeam) {
        for (Event event : events) {
            if (event.isBigChance() && !event.isResolved()) {
                ArrayList<Event> resultEvents;

                if (userTeam != null && event.getTeam() == userTeam) {
                    String selectedChoice = askForChanceChoice(event);
                    resultEvents = engine.resolveChance(match, event, selectedChoice);
                } else {
                    System.out.println(BRIGHT_BLACK + "Auto-resolving chance for " + event.getTeam().getName() + "..." + RESET);
                    resultEvents = engine.resolveChanceAutomatically(match, event);
                }

                System.out.println();
                System.out.println(BRIGHT_YELLOW + "Result:" + RESET);

                for (Event resultEvent : resultEvents) System.out.println(formatEvent(resultEvent));

                System.out.println("Score: " + match.getScoreLine() + "\n");
            }
        }
    }

    // Helper function to get user choice for chance events
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
    /* */

    /* Interactive manager methods */

    // Choose which team to control
    private Team chooseControlledTeam(Match match) {
        clearConsole();
        System.out.println(BRIGHT_YELLOW + "Do you want to control a team in this match?" + RESET);
        System.out.println("[1] " + match.getHomeTeam().getName());
        System.out.println("[2] " + match.getAwayTeam().getName());
        System.out.println("[3] No, just simulate");

        int choice = validateInput("Your choice: ", 1, 3);

        if (choice == 1) return match.getHomeTeam();
        if (choice == 2) return match.getAwayTeam();
        return null;
    }

    // Choose pre-match tactics
    private TacticalStyle chooseTacticalStyle(Team team) {
        clearConsole();

        TacticalStyle[] styles = TacticalStyle.values();

        printHeader("PRE-MATCH TACTICS");
        System.out.println("Choose tactical style for " + team.getName() + ":\n");

        for (int i = 0; i < styles.length; i++) System.out.println(" [" + (i + 1) + "] " + styles[i].getDisplayName() + "\n     " + styles[i].getDescription());

        int choice = validateInput("Your choice: ", 1, styles.length);
        clearConsole();

        return styles[choice - 1];
    }

    // Get user decision for manager decisions
    private void askManagerDecision(Match match, Team userTeam) {
        if (userTeam == null) return;

        printHeader("MANAGER DECISION");

        ManagerDecision[] decisions = ManagerDecision.values();

        System.out.println(match.getCurrentMinute() + "' Match Update:");
        System.out.println(match.getScoreLine());
        System.out.println();
        System.out.println("Tactics: " + match.getTactics(userTeam));
        System.out.println("Momentum: " + match.getMomentumDescription(userTeam));
        System.out.println();
        System.out.println(BRIGHT_YELLOW + "What do you want to do?" + RESET);

        for (int i = 0; i < decisions.length; i++) {
            System.out.println("[" + (i + 1) + "] " + decisions[i].getDisplayName());
            System.out.println("    " + decisions[i].getDescription());
        }

        int choice = validateInput("Your choice: ", 1, decisions.length);
        ManagerDecision decision = decisions[choice - 1];

        match.applyManagerDecision(userTeam, decision);

        System.out.println();
        System.out.println(BRIGHT_GREEN + "Decision applied: " + decision.getDisplayName() + RESET);
        System.out.println("The match continues...");
        System.out.println();
    }

    // Get user decision for helf-time team talk decision
    private void askHalfTimeTeamTalk(Match match, Team userTeam) {
        TeamTalk[] talks = TeamTalk.values();

        printHeader("HALF-TIME TALK");

        System.out.println("Half-Time: " + match.getScoreLine() + "\n");
        System.out.println(BRIGHT_YELLOW + "Choose your team talk:" + RESET);

        for (int i = 0; i < talks.length; i++) System.out.println(" [" + (i + 1) + "] " + talks[i].getDisplayName() + "\n     " + talks[i].getDescription());

        int choice = validateInput("Your choice: ", 1, talks.length);
        TeamTalk talk = talks[choice - 1];

        match.applyTeamTalk(userTeam, talk);

        System.out.println("\n" + BRIGHT_GREEN + "Team talk applied: " + talk.getDisplayName() + RESET + "\n" + "\n" + "Second half continues...\n");
    }

    // Display helper method to print headers
    private void printHeader(String title) {
        int totalWidth = 36;
        int titleLength = title.length();
        
        int leftPadding = (totalWidth - titleLength) / 2;
        int rightPadding = totalWidth - titleLength - leftPadding;

        System.out.println(BRIGHT_CYAN + "╔══════════════════════════════════════╗");
        
        System.out.print("║ ");
        printSpaces(leftPadding);
        
        System.out.print(title);
        
        printSpaces(rightPadding);
        System.out.println(" ║");
        
        System.out.println("╚══════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    // Helper method to print N spaces
    private void printSpaces(int count) {
        for (int i = 0; i < count; i++) System.out.print(" ");
    }
    /* */

    // Get user choice on playing interactive pen shootout
    private boolean askPlayPenaltyShootout(Team teamA, Team teamB, Team userTeam) {
        clearConsole();

        printHeader("PENALTY SHOOTOUT");
        System.out.println(
            teamA.getName() + " vs " + teamB.getName() + "\n" +
            "You are controlling: " + userTeam.getName() + "\n" +
            "\n" +
            "The match is tied after 90 minutes.\n" +
            "\n" +
            " [1] Play the penalty shootout\n" +
            " [2] Simulate the penalty shootout instantly"
        );

        int choice = validateInput("Your choice: ", 1, 2);
        return choice == 1;
    }

    
    /*
        Plays a user-controlled pen shootout
        Uses 'Tournament' inner class 'PenaltyShootoutResult'
    */
    private gamemechanics.tournament.Tournament.PenaltyShootoutResult playPenaltyShootout(Team teamA, Team teamB, Team userTeam) {
        int teamAScore = 0;
        int teamBScore = 0;
        String summary = "Penalty shootout:\n";

        clearConsole();

        printHeader("PENALTY SHOOTOUT");
        System.out.println(teamA.getName() + " vs " + teamB.getName());
        System.out.println("You are controlling: " + userTeam.getName() + "\n");

        for (int i = 0; i < 5; i++) {
            boolean teamAScored = takePenalty(teamA, teamB, userTeam, i + 1);
            if (teamAScored) teamAScore++;
            summary += "  " + teamA.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")\n";
            System.out.println(teamA.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")");

            boolean teamBScored = takePenalty(teamB, teamA, userTeam, i + 1);
            if (teamBScored) teamBScore++;
            summary += "  " + teamB.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n";
            System.out.println(teamB.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n");
        }

        int suddenDeathRound = 1;

        while (teamAScore == teamBScore) {
            System.out.println(BRIGHT_YELLOW + "Sudden Death Round " + suddenDeathRound + RESET);

            boolean teamAScored = takePenalty(teamA, teamB, userTeam, suddenDeathRound);
            if (teamAScored) teamAScore++;
            summary += "  Sudden Death " + suddenDeathRound + " - " + teamA.getName() + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")\n";
            System.out.println(teamA.getName() + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")");

            boolean teamBScored = takePenalty(teamB, teamA, userTeam, suddenDeathRound);
            if (teamBScored) teamBScore++;
            summary += "  Sudden Death " + suddenDeathRound + " - " + teamB.getName() + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n";
            System.out.println(teamB.getName() + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n");

            suddenDeathRound++;
        }

        Team winner;
        if (teamAScore > teamBScore) winner = teamA;
        else winner = teamB;
        summary += "Penalty shootout result: " + teamA.getName() + " " + teamAScore + " - " + teamBScore + " " + teamB.getName() + ". " + winner.getName() + " advances.";

        System.out.println(BRIGHT_GREEN + "Penalty shootout winner: " + winner.getName() + RESET);
        pause();

        return new Tournament.PenaltyShootoutResult(winner, summary);
    }

    /*
        Simulates pen shootout
        Optional display to user
        Uses 'Tournament' inner class 'PenaltyShootoutResult'
    */
    private gamemechanics.tournament.Tournament.PenaltyShootoutResult simulatePenaltyShootout(Team teamA, Team teamB, boolean showResult) {
        int teamAScore = 0;
        int teamBScore = 0;
        String summary = "Penalty shootout:\n";

        for (int i = 0; i < 5; i++) {
            boolean teamAScored = scorePenalty(teamA, teamB, randomPenaltyDirection());
            if (teamAScored) teamAScore++;
            summary += "  " + teamA.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")\n";

            boolean teamBScored = scorePenalty(teamB, teamA, randomPenaltyDirection());
            if (teamBScored) teamBScore++;
            summary += "  " + teamB.getName() + " penalty " + (i + 1) + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n";
        }

        int suddenDeathRound = 1;

        while (teamAScore == teamBScore) {
            boolean teamAScored = scorePenalty(teamA, teamB, randomPenaltyDirection());
            if (teamAScored) teamAScore++;
            summary += "  Sudden Death " + suddenDeathRound + " - " + teamA.getName() + ": " + formatPenaltyResult(teamAScored) + " (" + teamAScore + "-" + teamBScore + ")\n";

            boolean teamBScored = scorePenalty(teamB, teamA, randomPenaltyDirection());
            if (teamBScored) teamBScore++;
            summary += "  Sudden Death " + suddenDeathRound + " - " + teamB.getName() + ": " + formatPenaltyResult(teamBScored) + " (" + teamAScore + "-" + teamBScore + ")\n";

            suddenDeathRound++;
        }

        Team winner = teamAScore > teamBScore ? teamA : teamB;
        summary += "Penalty shootout result: " + teamA.getName() + " " + teamAScore + " - " + teamBScore + " " + teamB.getName() + ". " + winner.getName() + " advances.";

        if (showResult) {
            clearConsole();

            printHeader("PENALTY SHOOTOUT RESULT");
            System.out.println(summary + "\n");
            pause();
        }

        return new Tournament.PenaltyShootoutResult(winner, summary);
    }

    // Handles a pen kick
    private boolean takePenalty(Team shootingTeam, Team defendingTeam, Team userTeam, int kickNumber) {
        String direction;

        if (shootingTeam == userTeam) {
            System.out.println(BRIGHT_YELLOW + shootingTeam.getName() + " penalty " + kickNumber + RESET);
            System.out.println("[1] Shoot Left");
            System.out.println("[2] Shoot Middle");
            System.out.println("[3] Shoot Right");

            int choice = validateInput("Your choice: ", 1, 3);
            direction = switch (choice) {
                case 1 -> "Left";
                case 2 -> "Middle";
                default -> "Right";
            };
        } else direction = randomPenaltyDirection();

        return scorePenalty(shootingTeam, defendingTeam, direction);
    }

    // Evaluates pen result
    private boolean scorePenalty(Team shootingTeam, Team defendingTeam, String shotDirection) {
        String keeperDive = randomPenaltyDirection();

        double chance = 65
            + shootingTeam.getTeamAttackRating() * 0.20
            - defendingTeam.getTeamGoalkeeperRating() * 0.15;

        if (shotDirection.equals(keeperDive)) chance -= 25;
        else chance += 8;

        if (shotDirection.equals("Middle")) chance += 3;

        if (chance < 10) chance = 10;
        if (chance > 95) chance = 95;

        return Math.random() * 100 < chance;
    }

    // Generates random pen direction
    private String randomPenaltyDirection() {
        int direction = (int) (Math.random() * 3);

        return switch (direction) {
            case 0 -> "Left";
            case 1 -> "Middle";
            default -> "Right";
        };
    }

    // Helper method to format pen result
    private String formatPenaltyResult(boolean scored) {
        if (scored) return "Scored";
        return "Missed";
    }

    /* Tournament setup methods */

    // Get user choice on tournament format
    private TournamentFormat chooseTournamentFormat() {
        TournamentFormat[] formats = TournamentFormat.values();

        System.out.println(BRIGHT_YELLOW + "Choose tournament format:" + RESET);

        for (int i = 0; i < formats.length; i++) System.out.println("[" + (i + 1) + "] " + formats[i].getDisplayName() + " (" + formats[i].getDefaultTeamCount() + " teams)");

        int choice = validateInput("Choose format: ", 1, formats.length);
        clearPreviousLines(formats.length + 2);

        return formats[choice - 1];
    }

    // Get user choice on team control
    private Team chooseOptionalUserTeam() {
        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Do you want to choose a team to play as?" + RESET);
        System.out.println("[1] Yes");
        System.out.println("[2] No");

        int choice = validateInput("Your choice: ", 1, 2);
        clearPreviousLines(4);

        if (choice == 2) return null;

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "Choose your team:" + RESET);
        return chooseTeamByLeague(new ArrayList<>());
    }

    // Get user choice on user match mode
    private TournamentUserMatchMode chooseTournamentUserMatchMode(Team userTeam) {
        if (userTeam == null) return TournamentUserMatchMode.SIMULATE_ALL;

        System.out.println();
        System.out.println(BRIGHT_YELLOW + "How should your team's tournament matches be handled?" + RESET);
        System.out.println("[1] Simulate all tournament matches instantly");
        System.out.println("[2] Ask before each of my team's matches");
        System.out.println("[3] Play all of my team's matches interactively");

        int choice = validateInput("Your choice: ", 1, 3);
        clearPreviousLines(5);

        return switch (choice) {
            case 1 -> TournamentUserMatchMode.SIMULATE_ALL;
            case 2 -> TournamentUserMatchMode.ASK_BEFORE_EACH;
            default -> TournamentUserMatchMode.PLAY_ALL;
        };
    }

    // Get user choice on interactive or instant match simulation 
    private boolean askPlayTournamentMatch(Team homeTeam, Team awayTeam, Team userTeam) {
        clearConsole();

        printHeader("YOUR TOURNAMENT MATCH");
        System.out.println(
            homeTeam.getName() + " vs " + awayTeam.getName() + "\n" +
            "You are controlling: " + userTeam.getName() + "\n" +
            "\n" +
            "[1] Play this match interactively\n" +
            "[2] Simulate this match instantly"
        );

        int choice = validateInput("Your choice: ", 1, 2);
        return choice == 1;
    }

    // Formatter for tournament user match mode
    private String formatTournamentUserMatchMode(TournamentUserMatchMode mode) {
        return switch (mode) {
            case SIMULATE_ALL -> "Simulate all matches";
            case ASK_BEFORE_EACH -> "Ask before each user-team match";
            case PLAY_ALL -> "Play all user-team matches";
        };
    }

    // Build tournament teams based on user choice(s)
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
        clearPreviousLines(5);

        switch (selectionMode) {
            case 1 -> fillRemainingTeamsRandomly(selectedTeams, requiredTeams);
            case 2 -> chooseSomeTeamsThenRandomFill(selectedTeams, requiredTeams);
            default -> chooseAllRemainingTeamsManually(selectedTeams, requiredTeams);
        }

        return selectedTeams;
    }

    // Helper method: Get some teams from user; select rest randomly
    private void chooseSomeTeamsThenRandomFill(ArrayList<Team> selectedTeams, int requiredTeams) {
        int remainingSlots = requiredTeams - selectedTeams.size();

        System.out.println();
        System.out.println("Remaining team slots: " + remainingSlots);
        System.out.println("How many additional teams do you want to choose manually?");
        System.out.println(BRIGHT_BLACK + "The rest will be selected randomly." + RESET);

        int manualCount = validateInput("Manual teams: ", 0, remainingSlots);
        clearPreviousLines(5);

        for (int i = 0; i < manualCount; i++) {
            System.out.println();
            System.out.println(BRIGHT_YELLOW + "Choose manual team " + (i + 1) + " of " + manualCount + ":" + RESET);
            Team team = chooseTeamByLeague(selectedTeams);
            selectedTeams.add(team);
            System.out.println(team.getName() + " added.");
        }

        fillRemainingTeamsRandomly(selectedTeams, requiredTeams);
    }

    // Select teams manually
    private void chooseAllRemainingTeamsManually(ArrayList<Team> selectedTeams, int requiredTeams) {
        while (selectedTeams.size() < requiredTeams) {
            System.out.println();
            System.out.println(BRIGHT_YELLOW + "Choose team " + (selectedTeams.size() + 1) + " of " + requiredTeams + ":" + RESET);
            Team team = chooseTeamByLeague(selectedTeams);
            selectedTeams.add(team);
            System.out.println(team.getName() + " added.");
        }
    }

    // Select teams randomly
    private void fillRemainingTeamsRandomly(ArrayList<Team> selectedTeams, int requiredTeams) {
        ArrayList<Team> remainingTeams = new ArrayList<>();

        for (Team team : availableTeams) if (!selectedTeams.contains(team)) remainingTeams.add(team);

        if (remainingTeams.size() + selectedTeams.size() < requiredTeams) throw new IllegalStateException("Not enough teams available to fill the tournament.");

        while (selectedTeams.size() < requiredTeams) selectedTeams.add(remainingTeams.remove((int) (Math.random() * remainingTeams.size())));

        System.out.println("\nRandomly filled remaining teams.");
    }
    /* */

    /* Tournament display methods */
    // Helper method to check team within standings
    private boolean standingsContainTeam(ArrayList<TournamentStanding> standings, Team team) {
        for (TournamentStanding standing : standings) if (standing.getTeam() == team) return true;

        return false;
    }

    // Helper method to check team relevancy
    private boolean roundIsRelevantToUser(ArrayList<Match> matches, ArrayList<Team> winners, Team userTeam) {
        for (Match match : matches) {
            if (match.getHomeTeam() == userTeam || match.getAwayTeam() == userTeam) return true;
        }

        for (Team winner : winners) {
            if (winner == userTeam) return true;
        }

        return false;
    }

    private void displayGroupStandingUpdate(String groupName, ArrayList<TournamentStanding> standings, Team userTeam) {
        clearConsole();

        printHeader("GROUP STAGE UPDATE");
        System.out.println(BRIGHT_YELLOW + groupName + " Standings" + RESET + "\n");

        for (int i = 0; i < standings.size(); i++) {
            TournamentStanding standing = standings.get(i);
            String marker = standing.getTeam() == userTeam ? "  <== Your Team" : "";
            String status = i < 2 ? " [Qualifying]" : " [Eliminated]";
            System.out.println((i + 1) + ". " + standing.getFormattedStanding() + status + marker);
        }

        System.out.println();
        pause();
    }

    private void displayLeaguePhaseUpdate(int roundNumber, ArrayList<TournamentStanding> standings, Team userTeam) {
        clearConsole();

        printHeader("LEAGUE PHASE UPDATE");
        System.out.println("After League Phase Round " + roundNumber + "\n");

        int userRank = findTeamRank(standings, userTeam);
        System.out.println("Your current rank: " + userRank + getRankSuffix(userRank) + "\n");

        int displayLimit = Math.min(12, standings.size());
        for (int i = 0; i < displayLimit; i++) {
            TournamentStanding standing = standings.get(i);
            String marker = standing.getTeam() == userTeam ? BRIGHT_YELLOW + "  <== Your Team" + RESET : "";
            String status = getLeaguePhaseStatus(i + 1);
            System.out.println(String.format("%2d", (i + 1)) + ". " + standing.getFormattedStanding() + status + marker);
        }

        if (userRank > displayLimit) {
            TournamentStanding userStanding = standings.get(userRank - 1);
            System.out.println("...");
            System.out.println(String.format("%2d", userRank) + ". " + userStanding.getFormattedStanding() + getLeaguePhaseStatus(userRank) + BRIGHT_YELLOW + "  <== Your Team" + RESET);
        }

        System.out.println();
        pause();
    }

    private void displayKnockoutBracketUpdate(String roundName, ArrayList<Match> roundMatches, ArrayList<Team> winners, ArrayList<String> penaltyNotes, Team userTeam) {
        clearConsole();

        printHeader("KNOCKOUT BRACKET UPDATE");
        System.out.println(BRIGHT_YELLOW + roundName + " Results" + RESET + "\n");

        for (int i = 0; i < roundMatches.size(); i++) {
            Match match = roundMatches.get(i);
            Team winner = winners.get(i);
            String marker = (match.getHomeTeam() == userTeam || match.getAwayTeam() == userTeam) ? BRIGHT_YELLOW + "  <== Your Match" + RESET: "";

            System.out.println("[" + (i + 1) + "] " + match.getScoreLine() + " | Advances: " + winner.getName() + marker);

            if (i < penaltyNotes.size() && !penaltyNotes.get(i).isEmpty()) {
                String[] lines = penaltyNotes.get(i).split("\\n");
                for (String line : lines) System.out.println("    " + line);
            }
        }

        System.out.println();

        if (winners.size() > 1) {
            System.out.println(BRIGHT_YELLOW + "Next Round Matchups" + RESET);
            for (int i = 0; i < winners.size(); i += 2) {
                if (i + 1 < winners.size()) {
                    String marker = (winners.get(i) == userTeam || winners.get(i + 1) == userTeam) ? BRIGHT_YELLOW + "  <== Your Match" + RESET : "";
                    System.out.println("- " + winners.get(i).getName() + " vs " + winners.get(i + 1).getName() + marker);
                }
            }
        } else System.out.println(BRIGHT_GREEN + "Tournament Champion: " + winners.get(0).getName() + RESET);

        if (!winners.contains(userTeam)) System.out.println("\n" + BRIGHT_RED + "Your team has been eliminated." + RESET);

        System.out.println();
        pause();
    }

    // Helper method to retireve team standings rank
    private int findTeamRank(ArrayList<TournamentStanding> standings, Team team) {
        for (int i = 0; i < standings.size(); i++) if (standings.get(i).getTeam() == team) return i + 1;

        return -1;
    }

    // Helper method to get league phase progression status
    private String getLeaguePhaseStatus(int rank) {
        if (rank <= 8) return " [Round of 16]";
        if (rank <= 24) return " [Play-Offs]";
        return " [Eliminated]";
    }

    // Helper method for formatting
    private String getRankSuffix(int rank) {
        if (rank % 100 >= 11 && rank % 100 <= 13) return "th";
        return switch (rank % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
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
            + "\n" + "║ [1] Play Interactive Match           ║"
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
        
        printHeader("AVAILABLE TEAMS");

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

        printHeader("TEAM INFORMATION");

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

        printHeader("MATCH TIMELINE");
        System.out.println(currentMatch.getFormattedTimeline());
    }

    private void displayMatchSummary() {
        clearConsole();

        if (!checkMatchExists()) return;

        
        printHeader("MATCH SUMMARY");

        System.out.println(
            "Final Score: " + currentMatch.getScoreLine() + "\n" +
            "Winner: " + currentMatch.getWinner() + "\n" +
            "Yellow Cards: " + currentMatch.getYellowCardedPlayers().size() + "\n" +
            "Red Cards: " + currentMatch.getRedCardedPlayers().size() + "\n" +
            
            "\n" +

            "Home Tactics: " + currentMatch.getTactics(currentMatch.getHomeTeam()) + "\n" +
            "Away Tactics: " + currentMatch.getTactics(currentMatch.getAwayTeam()) + "\n" +
            "Home Momentum: " + currentMatch.getMomentumDescription(currentMatch.getHomeTeam()) + "\n" +
            "Away Momentum: " + currentMatch.getMomentumDescription(currentMatch.getAwayTeam()) + "\n" +
    
            "\n" +
            
            BRIGHT_YELLOW + "Goal Events:" + RESET
        );

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
        
        printHeader("MATCH HISTORY");

        System.out.println(matchHistory);
        System.out.println();
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

            System.out.println("\n" + BRIGHT_YELLOW + "Choose Team from " + selectedLeague + ":" + RESET);

            for (int i = 0; i < leagueTeams.size(); i++) {
                Team team = leagueTeams.get(i);
                String unavailable = excludedTeams.contains(team) ? " " + BRIGHT_BLACK + "(already selected)" + RESET : "";
                System.out.printf("[%d] %-24s Overall: %d%s%n", i + 1, team.getName(), roundVal(team.getAverageOverallRating()), unavailable);
            }

            int teamChoice = validateInput("Choose team: ", 1, leagueTeams.size());
            selectedTeam = leagueTeams.get(teamChoice - 1);
            clearPreviousLines(leagueTeams.size() + 3);

            if (excludedTeams.contains(selectedTeam)) System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Choose a different team.\n");
        } while (excludedTeams.contains(selectedTeam));

        return selectedTeam;
    }
    /* */

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

                if (input >= min && input <= max) validInput = true;
                else System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Use a number between " + min + " and " + max + ".");
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

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    public static void clearPreviousLines(int lines) {
        for (int i = 0; i < lines; i++) System.out.print("\033[1A\033[2K");
        System.out.flush();
    }
    /* */
}
