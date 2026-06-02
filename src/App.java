import gamemechanics.Event;
import gamemechanics.Match;
import gamemechanics.Player;
import gamemechanics.Team;
import simulation.LiveMatchRunner;
import simulation.SimulationEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class App {
    private static final String RESET = "\033[0m";
    private static final String BRIGHT_BLACK = "\033[90m";
    private static final String BRIGHT_RED = "\033[91m";
    private static final String BRIGHT_GREEN = "\033[92m";

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        // === ARSENAL FC (Home Team) ===
        Player arsGk   = new Player("David Raya", "GK", 83, 40, 80, 84, 48, 77);
        Player arsDef1 = new Player("William Saliba", "DEF", 81, 40, 70, 73, 87, 83);
        Player arsDef2 = new Player("Gabriel Magalhães", "DEF", 69, 45, 62, 63, 86, 84);
        Player arsDef3 = new Player("Ben White", "DEF", 77, 54, 76, 77, 82, 76);
        Player arsDef4 = new Player("Jurriën Timber", "DEF", 79, 53, 71, 79, 80, 78);
        Player arsMid1 = new Player("Declan Rice", "MID", 75, 66, 77, 79, 83, 83);
        Player arsMid2 = new Player("Martin Odegaard", "MID", 76, 81, 89, 88, 58, 64);
        Player arsMid3 = new Player("Mikel Merino", "MID", 67, 75, 77, 80, 79, 81);
        Player arsAtk1 = new Player("Bukayo Saka", "ATK", 86, 81, 83, 87, 45, 69);
        Player arsAtk2 = new Player("Kai Havertz", "ATK", 80, 81, 79, 82, 47, 77);
        Player arsAtk3 = new Player("Leandro Trossard", "ATK", 78, 82, 80, 84, 34, 65);

        // === FC BARCELONA (Away Team) ===
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

        Match match = new Match(homeTeam, awayTeam);
        SimulationEngine engine = new SimulationEngine();

        System.out.println("\n");

        LiveMatchRunner liveRunner = new LiveMatchRunner(
            match,
            engine,
            1,
            500,
            (ArrayList<Event> newEvents) -> {
                clearAboveLines(2);
                for (Event event : newEvents) {
                    System.out.println(event);
                }
                System.out.println(BRIGHT_GREEN + "Score: " + match.getScoreLine() + RESET);
                System.out.println();
            },
            (Event event) -> getEventUserChoice(console, event)
        );

        Thread matchThread = new Thread(liveRunner);
        matchThread.start();

        try {
            matchThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("=== FINAL MATCH SUMMARY ===");
        System.out.println(match);
        System.out.println("Winner: " + match.getWinner());
        System.out.println();

        System.out.println("=== MATCH TIMELINE ===");
        System.out.println(match.getFormattedTimeline());

        console.close();
    }

    private static String getEventUserChoice(Scanner console, Event event) {
        ArrayList<String> choices = event.getChoices();

        System.out.println(RESET + "Choose how to handle this chance:");
        for (int i = 0; i < choices.size(); i++) {
            System.out.println("[" + (i + 1) + "] " + choices.get(i));
        }

        int choice = validateInput(console, "Your choice: ", 1, choices.size());
        System.out.println();

        clearAboveLines(7);

        return choices.get(choice - 1);
    }

    private static int validateInput(Scanner console, String prompt, int min, int max) {
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
                    System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Use a number between " + min + " and " + max + ". " + BRIGHT_BLACK + "[ENTER]" + RESET);
                    console.nextLine();
                }
            } catch (InputMismatchException e) {
                console.nextLine();
                System.out.println(BRIGHT_RED + "Invalid choice." + RESET + " Use a number between " + min + " and " + max + ". " + BRIGHT_BLACK + "[ENTER]" + RESET);
                console.nextLine();
            }
        } while (!validInput);

        return input;
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void clearAboveLines(int lines) {
        for (int i = 0; i < lines; i++) System.out.print("\033[1A\033[K");
        System.out.flush();
    }
}
