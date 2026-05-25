// Exporting as package
package gamemechanics;

// Importing necessary classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Match {
    // Instance fields
    private final Team homeTeam;
    private final Team awayTeam;
    private int homeScore;
    private int awayScore;
    private final ArrayList<Event> events;
    private final ArrayList<Player> yellowCardedPlayers;
    private final ArrayList<Player> redCardedPlayers;
    private boolean started;
    private boolean finished;
    private final Random random;

    // Constructor
    public Match(Team homeTeam, Team awayTeam) {
        validateTeams(homeTeam, awayTeam);

        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeScore = 0;
        this.awayScore = 0;
        this.events = new ArrayList<>();
        this.yellowCardedPlayers = new ArrayList<>();
        this.redCardedPlayers = new ArrayList<>();
        this.started = false;
        this.finished = false;
        this.random = new Random();
    }

    /* Getters */
    public Team getHomeTeam() {
        return this.homeTeam;
    }

    public Team getAwayTeam() {
        return this.awayTeam;
    }

    public int getHomeScore() {
        return this.homeScore;
    }

    public int getAwayScore() {
        return this.awayScore;
    }

    public ArrayList<Event> getEvents() {
        return new ArrayList<>(this.events);
    }

    public ArrayList<Player> getYellowCardedPlayers() {
        return new ArrayList<>(this.yellowCardedPlayers);
    }

    public ArrayList<Player> getRedCardedPlayers() {
        return new ArrayList<>(this.redCardedPlayers);
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isFinished() {
        return this.finished;
    }
    /* */

    /* Main Match Methods */
    public void startMatch() {
        if (this.started) throw new IllegalStateException("Match has already started.");

        this.started = true;

        addEvent(new Event(
            0,
            "KICKOFF",
            null,
            null,
            "Kickoff: " + this.homeTeam.getName() + " vs " + this.awayTeam.getName() + "."
        ));
    }

    public void endMatch() {
        if (!this.started) throw new IllegalStateException("Match cannot end before it starts.");

        if (this.finished) throw new IllegalStateException("Match has already finished.");

        this.finished = true;

        addEvent(new Event(
            90,
            "FULL_TIME",
            null,
            null,
            "Full time: " + getScoreLine() + "."
        ));
    }

    public void addEvent(Event event) {
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");

        this.events.add(event);
    }

    public void addGoal(Team scoringTeam) {
        if (!isTeamInMatch(scoringTeam)) throw new IllegalArgumentException("Scoring team must be part of the match.");

        if (isHomeTeam(scoringTeam)) this.homeScore++;
        else this.awayScore++;
    }
    /* */

    /* Chance/Event Creation Methods */
    public Event createBigChance(Team attackingTeam, int minute) {
        validateMatchInProgress();
        validateTeamInMatch(attackingTeam);

        Player player = getRandomPlayerByPositions(attackingTeam, new String[] {"ATK", "MID"});

        ArrayList<String> choices = new ArrayList<>();
        choices.add("Power Shot");
        choices.add("Finesse Shot");
        choices.add("Pass Across Goal");
        choices.add("Dribble Keeper");

        Event event = new Event(
            minute,
            "BIG_CHANCE",
            attackingTeam,
            player,
            player.getName() + " has a big chance for " + attackingTeam.getName() + "!",
            true,
            choices
        );

        addEvent(event);
        return event;
    }

    public Event createPenalty(Team attackingTeam, int minute) {
        validateMatchInProgress();
        validateTeamInMatch(attackingTeam);

        Player player = getRandomPlayerByPositions(attackingTeam, new String[] {"ATK", "MID"});

        ArrayList<String> choices = new ArrayList<>();
        choices.add("Shoot Left");
        choices.add("Shoot Middle");
        choices.add("Shoot Right");

        Event event = new Event(
            minute,
            "PENALTY",
            attackingTeam,
            player,
            player.getName() + " steps up to take a penalty for " + attackingTeam.getName() + ".",
            true,
            choices
        );

        addEvent(event);
        return event;
    }

    public Event createFoul(Team committingTeam, int minute) {
        validateMatchInProgress();
        validateTeamInMatch(committingTeam);

        Player player = getRandomPlayerByPositions(committingTeam, new String[] {"DEF", "MID"});

        Event foul = new Event(
            minute,
            "FOUL",
            committingTeam,
            player,
            player.getName() + " committed a foul for " + committingTeam.getName() + "."
        );

        addEvent(foul);

        // Small chance of a card after a foul
        int cardChance = random.nextInt(100) + 1;

        if (cardChance <= 5) {
            addEvent(new Event(
                minute,
                "RED_CARD",
                committingTeam,
                player,
                player.getName() + " received a red card!"
            ));
            this.redCardedPlayers.add(player);
            this.yellowCardedPlayers.remove(player);
        } else if (cardChance <= 30) {
            if (findPlayer(player, yellowCardedPlayers)) {
                addEvent(new Event(
                    minute,
                    "YELLOW_CARD",
                    committingTeam,
                    player,
                    player.getName() + " received a second yellow card."
                ));
                addEvent(new Event(
                    minute,
                    "RED_CARD",
                    committingTeam,
                    player,
                    player.getName() + " received a red card!"
                ));
                this.redCardedPlayers.add(player);
                this.yellowCardedPlayers.remove(player);
            } else {
                addEvent(new Event(
                    minute,
                    "YELLOW_CARD",
                    committingTeam,
                    player,
                    player.getName() + " received a yellow card."
                ));
                this.yellowCardedPlayers.add(player);
            }
        }

        return foul;
    }

    public Event createNormalShot(Team attackingTeam, int minute) {
        validateMatchInProgress();
        validateTeamInMatch(attackingTeam);

        Team defendingTeam = getOpponent(attackingTeam);
        Player player = getRandomPlayerByPositions(attackingTeam, new String[] {"ATK", "MID"});

        int shotType = random.nextInt(4);
        String randomShotType = switch (shotType) {
            case 0 -> "Power Shot";
            case 1 -> "Finesse Shot";
            case 2 -> "Pass Across Goal";
            default -> "Dribble Keeper";
        };

        double conversionChance = calculateConversionChance(player, randomShotType, attackingTeam, defendingTeam) * 0.55;
        boolean scored = random.nextDouble() * 100 <= conversionChance;

        if (scored) {
            addGoal(attackingTeam);

            String shotTypeDescription = switch (randomShotType) {
                case "Power Shot" -> " with a powerful shot";
                case "Finesse Shot" -> " with a finesse shot";
                case "Pass Across Goal" -> " with a pass across the goal";
                default -> " after a dribble around the keeper";
            };

            Event goal = new Event(
                minute,
                "GOAL",
                attackingTeam,
                player,
                player.getName() + " scored for " + attackingTeam.getName() + shotTypeDescription + "!"
            );

            addEvent(goal);
            return goal;
        }

        String shotTypeDescription = switch (randomShotType) {
            case "Power Shot" -> "a power shot";
            case "Finesse Shot" -> "a finesse shot";
            case "Pass Across Goal" -> "a pass across the goal";
            default -> "to dribble the ball around the keeper";
        };

        Event miss = new Event(
            minute,
            "MISS",
            attackingTeam,
            player,
            player.getName() + " tried " + shotTypeDescription + " for " + attackingTeam.getName() + ", but they failed!"
        );

        addEvent(miss);
        return miss;
    }
    /* */

    /* Chance Resolution Methods */
    public void resolveChance(Event event, String selectedChoice) {
        validateMatchInProgress();

        if (event == null) throw new IllegalArgumentException("Event cannot be null.");

        if (!this.events.contains(event)) throw new IllegalArgumentException("Event must already be part of the match.");

        if (!event.isBigChance()) throw new IllegalArgumentException("Only big chance events can be resolved with a choice.");

        if (event.isResolved()) throw new IllegalStateException("This event has already been resolved.");

        Team attackingTeam = event.getTeam();
        Team defendingTeam = getOpponent(attackingTeam);
        Player player = event.getPlayer();

        event.setSelectedChoice(selectedChoice);

        double conversionChance;

        if (event.isPenalty()) conversionChance = calculatePenaltyConversionChance(player, selectedChoice);
        else conversionChance = calculateConversionChance(player, selectedChoice, attackingTeam, defendingTeam);

        boolean scored = random.nextDouble() * 100 <= conversionChance;
        event.setSuccessful(scored);

        if (scored) {
            addGoal(attackingTeam);

            addEvent(new Event(
                event.getMinute(),
                "GOAL",
                attackingTeam,
                player,
                player.getName() + " scored for " + attackingTeam.getName() + "!"
            ));
        } else {
            addEvent(new Event(
                event.getMinute(),
                "SAVE",
                defendingTeam,
                null,
                defendingTeam.getName() + " survived the chance from " + attackingTeam.getName() + "."
            ));
        }
    }

    public double calculateConversionChance(Player player, String selectedChoice, Team attackingTeam, Team defendingTeam) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");

        validateTeamInMatch(attackingTeam);
        validateTeamInMatch(defendingTeam);

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) throw new IllegalArgumentException("Selected choice cannot be blank/empty.");

        selectedChoice = selectedChoice.trim();

        double chance =
            player.getShooting() * 0.35
            + player.getDribbling() * 0.20
            + player.getPace() * 0.10
            + player.getPhysical() * 0.10
            + attackingTeam.getTeamAttackRating(redCardedPlayers) * 0.15
            + attackingTeam.getTeamMidfieldRating(redCardedPlayers) * 0.10
            - defendingTeam.getTeamDefenceRating(redCardedPlayers) * 0.25;

        if (selectedChoice.equalsIgnoreCase("Power Shot")) {
            chance += (player.getShooting() - 50) * 0.08;
            chance += (player.getPhysical() - 50) * 0.04;
        } else if (selectedChoice.equalsIgnoreCase("Finesse Shot")) {
            chance += (player.getShooting() - 50) * 0.07;
            chance += (player.getDribbling() - 50) * 0.05;
        } else if (selectedChoice.equalsIgnoreCase("Pass Across Goal")) {
            chance += (player.getPassing() - 50) * 0.08;
            chance += (attackingTeam.getTeamAttackRating(redCardedPlayers) - 50) * 0.05;
        } else if (selectedChoice.equalsIgnoreCase("Dribble Keeper")) {
            chance += (player.getDribbling() - 50) * 0.08;
            chance += (player.getPace() - 50) * 0.05;
        }

        return clamp(chance, 5, 95);
    }

    private double calculatePenaltyConversionChance(Player player, String selectedChoice) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) throw new IllegalArgumentException("Selected choice cannot be blank/empty.");
            
        selectedChoice = selectedChoice.trim().toUpperCase();
        
        if (!selectedChoice.equalsIgnoreCase("SHOOT LEFT") && !selectedChoice.equalsIgnoreCase("SHOOT MIDDLE") && !selectedChoice.equalsIgnoreCase("SHOOT RIGHT")) throw new IllegalArgumentException("Selected choice must be either 'Shoot Left', 'Shoot Middle', or 'Shoot Right'.");

        double chance = 65 + player.getShooting() * 0.30;

        chance += switch (selectedChoice) {
            case "SHOOT MIDDLE" -> 5;
            default -> (player.getShooting() - 65) * 0.15;
        };

        String keeperDive = switch (random.nextInt(3)) {
            case 0 -> "Shoot Left";
            case 1 -> "Shoot Middle";
            case 2 -> "Shoot Right";
            default -> "Shoot Middle";
        };

        if (keeperDive.equalsIgnoreCase(selectedChoice)) chance -= 45;
        else chance += 15;

        return clamp(chance, 10, 95);
    }
    /* */

    /* Display Methods */
    public String getScoreLine() {
        return this.homeTeam.getName() + " " + this.homeScore + " - " + this.awayScore + " " + this.awayTeam.getName();
    }

    public String getWinner() {
        if (this.homeScore > this.awayScore) return this.homeTeam.getName();
        else if (this.awayScore > this.homeScore) return this.awayTeam.getName();

        return "Draw";
    }

    public String getFormattedTimeline() {
        if (this.events.isEmpty()) return "No match events recorded.";

        ArrayList<Event> sortedEvents = new ArrayList<>(this.events);

        // Collections.sort(sortedEvents, new Comparator<Event>() {
        //     @Override
        //     public int compare(Event event1, Event event2) {
        //         return event1.getMinute() - event2.getMinute();
        //     }
        // });

        Collections.sort(sortedEvents, (Event event1, Event event2) -> event1.getMinute() - event2.getMinute());

        String result = "Match Timeline:\n";

        for (Event event : sortedEvents) result += "- " + event + "\n";

        return result;
    }

    @Override
    public String toString() {
        String status;
        if (this.finished) status = "Finished";
        else if (this.started) status = "In Progress";
        else status = "Not Started";
        String result = String.format("""
            Match Summary
            Home Team: %s
            Away Team: %s
            Score: %s
            Status: %s
            """
            , this.homeTeam.getName(), this.awayTeam.getName(), getScoreLine(), status);

        return result;
    }
    /* */

    /* Helper Methods */
    private void validateTeams(Team homeTeam, Team awayTeam) {
        if (homeTeam == null || awayTeam == null) throw new IllegalArgumentException("Both teams must exist.");

        if (homeTeam == awayTeam) throw new IllegalArgumentException("A team cannot play against itself.");
    }

    private void validateMatchInProgress() {
        if (!this.started) throw new IllegalStateException("Match has not started yet.");

        if (this.finished) throw new IllegalStateException("Match has already finished.");
    }

    private void validateTeamInMatch(Team team) {
        if (!isTeamInMatch(team)) throw new IllegalArgumentException("Team must be part of this match.");
    }

    private boolean isTeamInMatch(Team team) {
        return team != null && (team == this.homeTeam || team == this.awayTeam);
    }

    private boolean isHomeTeam(Team team) {
        return team == this.homeTeam;
    }

    private Team getOpponent(Team team) {
        validateTeamInMatch(team);

        if (isHomeTeam(team)) return this.awayTeam;

        return this.homeTeam;
    }

    private Player getRandomPlayerByPositions(Team team, String[] positions) {
        ArrayList<Player> players = team.getPlayers();

        if (players.isEmpty()) throw new IllegalStateException("Cannot select a player from an empty team.");

        ArrayList<Player> candidates = new ArrayList<>();

        for (Player player : players) for (String position : positions) if (player.getPosition().equalsIgnoreCase(position) && !findPlayer(player, redCardedPlayers)) candidates.add(player);

        if (candidates.isEmpty()) for (Player player : players) if (!findPlayer(player, redCardedPlayers)) candidates.add(player);

        return candidates.get(random.nextInt(candidates.size()));
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private boolean findPlayer(Player player, ArrayList<Player> playerList) {
        if (player == null || playerList == null) {
            throw new IllegalArgumentException("Player and player list cannot be null.");
        }
        for (Player p : playerList) {
            if (p.getName().equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }
    /* */
}