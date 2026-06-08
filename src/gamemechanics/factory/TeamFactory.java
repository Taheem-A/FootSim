// Exporting as package
package gamemechanics.factory;

// Importing necessary classes
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Random;

import gamemechanics.core.Player;
import gamemechanics.core.Team;

public class TeamFactory {
    private static final String RESOURCE_DATA_FILE = "/data/fc26_player_data.csv";
    private static final String FALLBACK_DATA_FILE = "src/data/fc26_player_data.csv";
    private static final int PLAYERS_PER_TEAM = 11;
    private static final Random random = new Random();

    // Private constructor prevents this utility class from being instantiated.
    private TeamFactory() { }

    // Creates every team stored in the CSV file.
    public static ArrayList<Team> createAllTeams() {
        LinkedHashMap<String, ArrayList<PlayerRecord>> groupedPlayers = loadPlayerRecordsByTeam();
        ArrayList<Team> teams = new ArrayList<>();

        for (String teamName : groupedPlayers.keySet()) teams.add(buildTeam(teamName, groupedPlayers.get(teamName)));

        return teams;
    }

    // Creates every team grouped under its league.
    public static LinkedHashMap<String, ArrayList<Team>> createTeamsByLeague() {
        LinkedHashMap<String, ArrayList<PlayerRecord>> groupedPlayers = loadPlayerRecordsByLeagueAndTeam();
        LinkedHashMap<String, ArrayList<Team>> teamsByLeague = new LinkedHashMap<>();

        for (String key : groupedPlayers.keySet()) {
            ArrayList<PlayerRecord> records = groupedPlayers.get(key);

            if (!records.isEmpty()) {
                String league = records.get(0).league;
                String teamName = records.get(0).teamName;

                teamsByLeague
                    .computeIfAbsent(league, value -> new ArrayList<>())
                    .add(buildTeam(teamName, records));
            }
        }

        return teamsByLeague;
    }

    // Returns the names of all leagues in the data file.
    public static ArrayList<String> getLeagueNames() {
        return new ArrayList<>(createTeamsByLeague().keySet());
    }

    // Selects a random team while optionally excluding one team.
    public static Team getRandomTeam(ArrayList<Team> teams, Team excludedTeam) {
        if (teams == null || teams.isEmpty()) throw new IllegalArgumentException("Team list cannot be empty.");

        ArrayList<Team> availableTeams = new ArrayList<>();

        for (Team team : teams) if (team != excludedTeam) availableTeams.add(team);

        if (availableTeams.isEmpty()) throw new IllegalArgumentException("No available teams to choose from.");

        return availableTeams.get(random.nextInt(availableTeams.size()));
    }

    // Loads all player records from the CSV file and groups them by team.
    private static LinkedHashMap<String, ArrayList<PlayerRecord>> loadPlayerRecordsByTeam() {
        LinkedHashMap<String, ArrayList<PlayerRecord>> groupedPlayers = new LinkedHashMap<>();

        for (PlayerRecord record : loadPlayerRecords()) {
            groupedPlayers
                .computeIfAbsent(record.teamName, key -> new ArrayList<>())
                .add(record);
        }

        return groupedPlayers;
    }

    // Loads all player records from the CSV file and groups them by league + team.
    private static LinkedHashMap<String, ArrayList<PlayerRecord>> loadPlayerRecordsByLeagueAndTeam() {
        LinkedHashMap<String, ArrayList<PlayerRecord>> groupedPlayers = new LinkedHashMap<>();

        for (PlayerRecord record : loadPlayerRecords()) {
            String key = record.league + " - " + record.teamName;

            groupedPlayers
                .computeIfAbsent(key, value -> new ArrayList<>())
                .add(record);
        }

        return groupedPlayers;
    }

    // Loads all player records from the CSV file.
    private static ArrayList<PlayerRecord> loadPlayerRecords() {
        ArrayList<PlayerRecord> records = new ArrayList<>();
        InputStream stream = openDataFile();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (firstLine) firstLine = false;
                else if (!line.trim().isEmpty()) records.add(parsePlayerRecord(line, rowNumber));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error loading team data: " + e.getMessage());
        }

        return records;
    }

    // Opens the data file from the default location first, then the src folder as a fallback.
    private static InputStream openDataFile() {
        InputStream stream = TeamFactory.class.getResourceAsStream(RESOURCE_DATA_FILE);

        if (stream != null) return stream;

        try {
            return new FileInputStream(FALLBACK_DATA_FILE);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Could not find data file. Both " + RESOURCE_DATA_FILE + " and " + FALLBACK_DATA_FILE + " failed.");
        }
    }

    // Converts one CSV line into a PlayerRecord.
    private static PlayerRecord parsePlayerRecord(String line, int rowNumber) {
        String[] parts = line.split(",", -1);

        if (parts.length != 11) throw new IllegalArgumentException("Invalid CSV format on row " + rowNumber + ": " + line);

        String league = parts[0].trim();
        String teamName = parts[1].trim();
        String playerName = parts[2].trim();
        String fcPosition = parts[3].trim();

        int overall = parseInt(parts[4], rowNumber, "OVR");
        int pace = parseInt(parts[5], rowNumber, "PAC");
        int shooting = parseInt(parts[6], rowNumber, "SHO");
        int passing = parseInt(parts[7], rowNumber, "PAS");
        int dribbling = parseInt(parts[8], rowNumber, "DRI");
        int defence = parseInt(parts[9], rowNumber, "DEF");
        int physical = parseInt(parts[10], rowNumber, "PHY");

        return new PlayerRecord(
            league,
            teamName,
            playerName,
            convertPosition(fcPosition),
            overall,
            pace,
            shooting,
            passing,
            dribbling,
            defence,
            physical
        );
    }

    /**
        Builds a Team object using the best available players.
        Attempts to create a balanced team first, then fills any missing spots with the best remaining players.
     */
    private static Team buildTeam(String teamName, ArrayList<PlayerRecord> records) {
        ArrayList<PlayerRecord> selectedRecords = new ArrayList<>();

        // Preferred formation: 1 GK, 4 DEF, 3 MID, 3 ATK
        addBestPlayersByPosition(records, selectedRecords, "GK", 1);
        addBestPlayersByPosition(records, selectedRecords, "DEF", 4);
        addBestPlayersByPosition(records, selectedRecords, "MID", 3);
        addBestPlayersByPosition(records, selectedRecords, "ATK", 3);

        // If the team does not have enough players in one category,
        // fill the remaining spots with the best players who have not already been selected.
        fillRemainingPlayers(records, selectedRecords);

        if (selectedRecords.size() < PLAYERS_PER_TEAM) throw new IllegalArgumentException(teamName + " does not have enough total players for a full starting 11.");

        ArrayList<Player> players = new ArrayList<>();

        for (PlayerRecord record : selectedRecords) players.add(createPlayerFromRecord(record));

        return new Team(teamName, players);
    }

    // Adds the best players from one simplified position group.
    private static void addBestPlayersByPosition(
        ArrayList<PlayerRecord> records,
        ArrayList<PlayerRecord> selectedRecords,
        String position,
        int amount
    ) {
        ArrayList<PlayerRecord> matchingRecords = new ArrayList<>();

        for (PlayerRecord record : records) if (record.position.equals(position) && !selectedRecords.contains(record)) matchingRecords.add(record);

        sortByOverall(matchingRecords);

        for (int i = 0; i < matchingRecords.size() && i < amount; i++) selectedRecords.add(matchingRecords.get(i));
    }

    // Fills any empty squad spots with the best remaining players, regardless of position.
    private static void fillRemainingPlayers(
        ArrayList<PlayerRecord> records,
        ArrayList<PlayerRecord> selectedRecords
    ) {
        ArrayList<PlayerRecord> remainingRecords = new ArrayList<>();

        for (PlayerRecord record : records) if (!selectedRecords.contains(record)) remainingRecords.add(record);

        sortByOverall(remainingRecords);

        for (int i = 0; i < remainingRecords.size() && selectedRecords.size() < PLAYERS_PER_TEAM; i++) selectedRecords.add(remainingRecords.get(i));
    }

    // Sorts players from highest overall to lowest overall.
    private static void sortByOverall(ArrayList<PlayerRecord> records) {
        Collections.sort(records, (PlayerRecord a, PlayerRecord b) -> Integer.compare(b.overall, a.overall));
    }

    // Converts a PlayerRecord into the actual Player object used by the game.
    private static Player createPlayerFromRecord(PlayerRecord record) {
        return new Player(
            record.playerName,
            record.position,
            record.overall,
            record.pace,
            record.shooting,
            record.passing,
            record.dribbling,
            record.defence,
            record.physical
        );
    }

    // Converts official FC 26 positions into your simplified project positions.
    private static String convertPosition(String fcPosition) {
        String position = fcPosition.toUpperCase().trim();

        if (position.equals("GK")) return "GK";

        if (
            position.equals("CB") ||
            position.equals("LB") ||
            position.equals("RB") ||
            position.equals("LWB") ||
            position.equals("RWB")
        ) {
            return "DEF";
        }

        if (
            position.equals("CDM") ||
            position.equals("CM") ||
            position.equals("CAM")
        ) {
            return "MID";
        }

        if (
            position.equals("ST") ||
            position.equals("CF") ||
            position.equals("LW") ||
            position.equals("RW") ||
            position.equals("LM") ||
            position.equals("RM") ||
            position.equals("LF") ||
            position.equals("RF")
        ) {
            return "ATK";
        }

        return "MID";
    }

    // Parses integer values safely.
    private static int parseInt(String value, int rowNumber, String columnName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + columnName + " value on row " + rowNumber + ": " + value);
        }
    }

    // Stores official FC 26 player data before turning it into your Player object.
    private static class PlayerRecord {
        private String league;
        private String teamName;
        private String playerName;
        private String position;
        private int overall;
        private int pace;
        private int shooting;
        private int passing;
        private int dribbling;
        private int defence;
        private int physical;

        private PlayerRecord(
            String league,
            String teamName,
            String playerName,
            String position,
            int overall,
            int pace,
            int shooting,
            int passing,
            int dribbling,
            int defence,
            int physical
        ) {
            this.league = league;
            this.teamName = teamName;
            this.playerName = playerName;
            this.position = position;
            this.overall = overall;
            this.pace = pace;
            this.shooting = shooting;
            this.passing = passing;
            this.dribbling = dribbling;
            this.defence = defence;
            this.physical = physical;
        }
    }
}
