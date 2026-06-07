// Exporting as package
package simulation;

// Importing necessary classes
import gamemechanics.*;
import java.util.*;

public class SimulationEngine {
    // Constants and instance variables
    private static final int DEFAULT_ADVANCE_AMOUNT = 5;
    private final Random random;

    // Main constructor
    public SimulationEngine() {
        this.random = new Random();
    }

    /* Simulation Methods */
    public Match simulateMatch(Team homeTeam, Team awayTeam) {
        Match match = new Match(homeTeam, awayTeam);
        setRandomTactics(match, homeTeam);
        setRandomTactics(match, awayTeam);
        simulateMatch(match);
        return match;
    }

    public void simulateMatch(Match match) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");

        if (!match.hasStarted()) {
            setRandomTactics(match, match.getHomeTeam());
            setRandomTactics(match, match.getAwayTeam());
        }

        while (!match.isFinished()) {
            advanceMatch(match, 1, true);
        }
    }
    /* */

    /* Overloaded advanceMatch methods for different use cases */
    public ArrayList<Event> advanceMatch(Match match) {
        return advanceMatch(match, DEFAULT_ADVANCE_AMOUNT, true);
    }

    public ArrayList<Event> advanceMatch(Match match, int minuteAmount) {
        return advanceMatch(match, minuteAmount, true);
    }

    public ArrayList<Event> advanceMatch(Match match, int minuteAmount, boolean autoResolveChoices) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        if (minuteAmount <= 0) throw new IllegalArgumentException("Minute amount must be greater than 0.");

        ArrayList<Event> newEvents = new ArrayList<>();

        if (match.isFinished()) return newEvents;

        int eventCountBefore = match.getEvents().size();

        if (!match.hasStarted()) {
            match.startMatch();
        }

        for (int i = 0; i < minuteAmount && !match.isFinished(); i++) {
            if (match.getCurrentMinute() >= match.getMatchLength()) {
                match.endMatch();
                break;
            }

            match.advanceMinute(1);

            generateMinuteEvents(match, match.getCurrentMinute(), autoResolveChoices);

            if (match.getCurrentMinute() >= match.getMatchLength() && !match.isFinished()) {
                match.endMatch();
            }
        }

        ArrayList<Event> allEvents = match.getEvents();
        for (int i = eventCountBefore; i < allEvents.size(); i++) {
            newEvents.add(allEvents.get(i));
        }

        return newEvents;
    }
    /* */

    // Assigns random tactics to a team if the match is being fully simulated by the computer.
    public void setRandomTactics(Match match, Team team) {
        if (match == null || team == null) throw new IllegalArgumentException("Match and team cannot be null.");
        match.validateTeamInMatch(team);

        TacticalStyle[] styles = TacticalStyle.values();
        match.setTactics(team, new TeamTactics(styles[random.nextInt(styles.length)]));
    }

    // Event generation method for each minute
    private void generateMinuteEvents(Match match, int minute, boolean autoResolveChoices) {
        if (minute == 45) {
            match.addEvent(new Event(
                minute,
                EventType.HALF_TIME,
                null,
                null,
                "Half time: " + match.getScoreLine() + "."
            ));
            return;
        }

        Team attackingTeam = chooseAttackingTeam(match);
        Team defendingTeam = match.getOpponent(attackingTeam);

        double eventThreshold = calculateEventThreshold(match, attackingTeam, defendingTeam);
        int eventChance = random.nextInt(100) + 1;
        if (eventChance > eventThreshold) return;

        int eventRoll = random.nextInt(100) + 1;
        Event event;

        if (eventRoll <= 48) {
            createNormalShot(match, attackingTeam, minute);
        } else if (eventRoll <= 64) {
            createFoul(match, defendingTeam, minute);
        } else if (eventRoll <= 82) {
            event = createBigChance(match, attackingTeam, minute);
            if (autoResolveChoices) {
                resolveChanceAutomatically(match, event);
            }
        } else if (eventRoll <= 88) {
            event = createPenalty(match, attackingTeam, minute);
            if (autoResolveChoices) {
                resolveChanceAutomatically(match, event);
            }
        } else {
            match.addEvent(new Event(
                minute,
                EventType.COMMENTARY,
                null,
                null,
                createMomentumCommentary(match)
            ));
        }
    }

    private double calculateEventThreshold(Match match, Team attackingTeam, Team defendingTeam) {
        double threshold = 24;

        threshold += match.getTactics(attackingTeam).getEventChanceModifier() * 0.8;
        threshold -= match.getTactics(defendingTeam).getDefenceModifier() * 0.25;
        threshold += match.getMomentum(attackingTeam) * 0.08;
        threshold -= match.getMomentum(defendingTeam) * 0.05;

        return clamp(threshold, 10, 42);
    }

    private String createMomentumCommentary(Match match) {
        Team strongerMomentumTeam = match.getHomeMomentum() >= match.getAwayMomentum() ? match.getHomeTeam() : match.getAwayTeam();

        String[] comments = {
            strongerMomentumTeam.getName() + " are starting to control the rhythm of the match.",
            "Both managers are trying to influence the tempo from the touchline.",
            "The midfield battle is becoming more intense.",
            strongerMomentumTeam.getName() + " are growing in confidence."
        };

        return comments[random.nextInt(comments.length)];
    }

    /* Chance/Event Creation Methods */
    public Event createBigChance(Match match, Team attackingTeam, int minute) {
        validateMatchAndTeam(match, attackingTeam);

        Player player = getRandomPlayerByPositions(match, attackingTeam, new String[] {"ATK", "MID"});

        ArrayList<String> choices = new ArrayList<>();
        choices.add("Power Shot");
        choices.add("Finesse Shot");
        choices.add("Pass Across Goal");
        choices.add("Dribble Keeper");

        Event event = new Event(
            minute,
            EventType.BIG_CHANCE,
            attackingTeam,
            player,
            player.getName() + " has a big chance for " + attackingTeam.getName() + "!",
            true,
            choices
        );

        match.addEvent(event);
        return event;
    }

    public Event createPenalty(Match match, Team attackingTeam, int minute) {
        validateMatchAndTeam(match, attackingTeam);

        Player player = getRandomPlayerByPositions(match, attackingTeam, new String[] {"ATK", "MID"});

        ArrayList<String> choices = new ArrayList<>();
        choices.add("Shoot Left");
        choices.add("Shoot Middle");
        choices.add("Shoot Right");

        Event event = new Event(
            minute,
            EventType.PENALTY,
            attackingTeam,
            player,
            player.getName() + " steps up to take a penalty for " + attackingTeam.getName() + ".",
            true,
            choices
        );

        match.addEvent(event);
        return event;
    }

    public Event createFoul(Match match, Team committingTeam, int minute) {
        validateMatchAndTeam(match, committingTeam);

        Player player = getRandomPlayerByPositions(match, committingTeam, new String[] {"DEF", "MID"});
        Team opponent = match.getOpponent(committingTeam);

        Event foul = new Event(
            minute,
            EventType.FOUL,
            committingTeam,
            player,
            player.getName() + " committed a foul for " + committingTeam.getName() + "."
        );

        match.addEvent(foul);

        int cardChance = random.nextInt(100) + 1;
        int cardRisk = 30 + match.getTactics(committingTeam).getCardRiskModifier();

        if (cardChance <= Math.max(3, 5 + match.getTactics(committingTeam).getCardRiskModifier() / 2)) {
            match.addEvent(new Event(
                minute,
                EventType.RED_CARD,
                committingTeam,
                player,
                player.getName() + " received a red card!"
            ));
            match.recordRedCard(player);
            match.adjustMomentum(committingTeam, -16);
            match.adjustMomentum(opponent, 8);
        } else if (cardChance <= clamp(cardRisk, 12, 45)) {
            if (findPlayer(player, match.getYellowCardedPlayers())) {
                match.addEvent(new Event(
                    minute,
                    EventType.YELLOW_CARD,
                    committingTeam,
                    player,
                    player.getName() + " received a second yellow card."
                ));
                match.addEvent(new Event(
                    minute,
                    EventType.RED_CARD,
                    committingTeam,
                    player,
                    player.getName() + " received a red card!"
                ));
                match.recordRedCard(player);
                match.adjustMomentum(committingTeam, -16);
                match.adjustMomentum(opponent, 8);
            } else {
                match.addEvent(new Event(
                    minute,
                    EventType.YELLOW_CARD,
                    committingTeam,
                    player,
                    player.getName() + " received a yellow card."
                ));
                match.recordYellowCard(player);
                match.adjustMomentum(committingTeam, -4);
            }
        }

        return foul;
    }

    public Event createNormalShot(Match match, Team attackingTeam, int minute) {
        validateMatchAndTeam(match, attackingTeam);

        Team defendingTeam = match.getOpponent(attackingTeam);
        Player player = getRandomPlayerByPositions(match, attackingTeam, new String[] {"ATK", "MID"});

        String randomShotType = switch (random.nextInt(4)) {
            case 0 -> "Power Shot";
            case 1 -> "Finesse Shot";
            case 2 -> "Pass Across Goal";
            default -> "Dribble Keeper";
        };

        double conversionChance = calculateConversionChance(match, player, randomShotType, attackingTeam, defendingTeam) * 0.55;
        boolean scored = random.nextDouble() * 100 <= conversionChance;

        if (scored) {
            match.addGoal(attackingTeam);

            String shotTypeDescription = switch (randomShotType) {
                case "Power Shot" -> " with a powerful shot";
                case "Finesse Shot" -> " with a finesse shot";
                case "Pass Across Goal" -> " with a pass across the goal";
                default -> " after a dribble around the keeper";
            };

            Event goal = new Event(
                minute,
                EventType.GOAL,
                attackingTeam,
                player,
                player.getName() + " scored for " + attackingTeam.getName() + shotTypeDescription + "!"
            );

            match.addEvent(goal);
            return goal;
        }

        match.adjustMomentum(attackingTeam, -2);
        match.adjustMomentum(defendingTeam, 2);

        String shotTypeDescription = switch (randomShotType) {
            case "Power Shot" -> "a power shot";
            case "Finesse Shot" -> "a finesse shot";
            case "Pass Across Goal" -> "a pass across the goal";
            default -> "to dribble the ball around the keeper";
        };

        Event miss = new Event(
            minute,
            EventType.MISS,
            attackingTeam,
            player,
            player.getName() + " tried " + shotTypeDescription + " for " + attackingTeam.getName() + ", but they failed!"
        );

        match.addEvent(miss);
        return miss;
    }
    /* */

    /* Chance resolution methods */
    public ArrayList<Event> resolveChanceAutomatically(Match match, Event event) {
        return resolveChance(match, event, chooseRandomChoice(event));
    }

    public ArrayList<Event> resolveChance(Match match, Event event, String selectedChoice) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        match.validateMatchInProgress();

        if (event == null) throw new IllegalArgumentException("Event cannot be null.");
        if (!match.getEvents().contains(event)) throw new IllegalArgumentException("Event must already be part of the match.");
        if (!event.isBigChance()) throw new IllegalArgumentException("Only big chance events can be resolved with a choice.");
        if (event.isResolved()) throw new IllegalStateException("This event has already been resolved.");

        Team attackingTeam = event.getTeam();
        Team defendingTeam = match.getOpponent(attackingTeam);
        Player player = event.getPlayer();
        ArrayList<Event> resolutionEvents = new ArrayList<>();

        event.setSelectedChoice(selectedChoice);

        double conversionChance;

        if (event.isPenalty()) conversionChance = calculatePenaltyConversionChance(match, player, selectedChoice, attackingTeam, defendingTeam);
        else conversionChance = calculateConversionChance(match, player, selectedChoice, attackingTeam, defendingTeam);

        boolean scored = random.nextDouble() * 100 <= conversionChance;
        event.setSuccessful(scored);

        if (scored) {
            match.addGoal(attackingTeam);

            Event goal = new Event(
                event.getMinute(),
                EventType.GOAL,
                attackingTeam,
                player,
                player.getName() + " scored for " + attackingTeam.getName() + "!"
            );

            match.addEvent(goal);
            resolutionEvents.add(goal);
        } else {
            Player goalkeeper = defendingTeam.getGoalkeeper();
            match.adjustMomentum(attackingTeam, -5);
            match.adjustMomentum(defendingTeam, 5);

            Event save = new Event(
                event.getMinute(),
                EventType.SAVE,
                defendingTeam,
                goalkeeper,
                defendingTeam.getName() + " survived the chance from " + attackingTeam.getName() + "."
            );

            match.addEvent(save);
            resolutionEvents.add(save);
        }

        return resolutionEvents;
    }

    public double calculateConversionChance(Match match, Player player, String selectedChoice, Team attackingTeam, Team defendingTeam) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");

        match.validateTeamInMatch(attackingTeam);
        match.validateTeamInMatch(defendingTeam);

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) throw new IllegalArgumentException("Selected choice cannot be blank/empty.");

        selectedChoice = selectedChoice.trim();

        double goalkeeperRating = defendingTeam.getTeamGoalkeeperRating();

        double chance =
            player.getShooting() * 0.35
            + player.getDribbling() * 0.20
            + player.getPace() * 0.10
            + player.getPhysical() * 0.10
            + attackingTeam.getTeamAttackRating(match.getRedCardedPlayers()) * 0.15
            + attackingTeam.getTeamMidfieldRating(match.getRedCardedPlayers()) * 0.10
            - defendingTeam.getTeamDefenceRating(match.getRedCardedPlayers()) * 0.20
            - goalkeeperRating * 0.10;

        chance += match.getTactics(attackingTeam).getConversionModifier();
        chance += match.getTactics(attackingTeam).getAttackModifier() * 0.35;
        chance -= match.getTactics(defendingTeam).getDefenceModifier() * 0.30;
        chance += match.getMomentum(attackingTeam) * 0.12;
        chance -= match.getMomentum(defendingTeam) * 0.08;

        if (selectedChoice.equalsIgnoreCase("Power Shot")) {
            chance += (player.getShooting() - 50) * 0.08;
            chance += (player.getPhysical() - 50) * 0.04;
        } else if (selectedChoice.equalsIgnoreCase("Finesse Shot")) {
            chance += (player.getShooting() - 50) * 0.07;
            chance += (player.getDribbling() - 50) * 0.05;
        } else if (selectedChoice.equalsIgnoreCase("Pass Across Goal")) {
            chance += (player.getPassing() - 50) * 0.08;
            chance += (attackingTeam.getTeamAttackRating(match.getRedCardedPlayers()) - 50) * 0.05;
        } else if (selectedChoice.equalsIgnoreCase("Dribble Keeper")) {
            chance += (player.getDribbling() - 50) * 0.08;
            chance += (player.getPace() - 50) * 0.05;
        }

        return clamp(chance, 5, 95);
    }

    private double calculatePenaltyConversionChance(Match match, Player player, String selectedChoice, Team attackingTeam, Team defendingTeam) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");
        if (defendingTeam == null) throw new IllegalArgumentException("Defending team cannot be null.");

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) throw new IllegalArgumentException("Selected choice cannot be blank/empty.");

        selectedChoice = selectedChoice.trim().toUpperCase();

        if (!selectedChoice.equalsIgnoreCase("SHOOT LEFT") && !selectedChoice.equalsIgnoreCase("SHOOT MIDDLE") && !selectedChoice.equalsIgnoreCase("SHOOT RIGHT")) throw new IllegalArgumentException("Selected choice must be either 'Shoot Left', 'Shoot Middle', or 'Shoot Right'.");

        double keeperRating = defendingTeam.getTeamGoalkeeperRating();
        double chance = 65 + player.getShooting() * 0.25 - keeperRating * 0.15;

        chance += match.getTactics(attackingTeam).getConversionModifier() * 0.4;
        chance += match.getMomentum(attackingTeam) * 0.08;
        chance -= match.getMomentum(defendingTeam) * 0.05;

        chance += switch (selectedChoice) {
            case "SHOOT MIDDLE" -> 5;
            default -> (player.getShooting() - 65) * 0.15;
        };

        String keeperDive = switch (random.nextInt(3)) {
            case 0 -> "SHOOT LEFT";
            case 1 -> "SHOOT MIDDLE";
            case 2 -> "SHOOT RIGHT";
            default -> "SHOOT MIDDLE";
        };

        if (keeperDive.equalsIgnoreCase(selectedChoice)) chance -= 25 + (keeperRating - 50) * 0.25;
        else chance += 10;

        return clamp(chance, 10, 95);
    }
    /* */

    /* Helper Methods */
    private void validateMatchAndTeam(Match match, Team team) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        match.validateMatchInProgress();
        match.validateTeamInMatch(team);
    }

    private Team chooseAttackingTeam(Match match) {
        double homeStrength =
            match.getHomeTeam().getTeamAttackRating(match.getRedCardedPlayers())
            + match.getHomeTeam().getTeamMidfieldRating(match.getRedCardedPlayers())
            + match.getTactics(match.getHomeTeam()).getAttackModifier()
            + match.getTactics(match.getHomeTeam()).getMidfieldModifier()
            + match.getMomentum(match.getHomeTeam()) * 0.6;

        double awayStrength =
            match.getAwayTeam().getTeamAttackRating(match.getRedCardedPlayers())
            + match.getAwayTeam().getTeamMidfieldRating(match.getRedCardedPlayers())
            + match.getTactics(match.getAwayTeam()).getAttackModifier()
            + match.getTactics(match.getAwayTeam()).getMidfieldModifier()
            + match.getMomentum(match.getAwayTeam()) * 0.6;

        homeStrength = Math.max(5, homeStrength);
        awayStrength = Math.max(5, awayStrength);

        double totalStrength = homeStrength + awayStrength;

        if (totalStrength <= 0) {
            return random.nextBoolean() ? match.getHomeTeam() : match.getAwayTeam();
        }

        double roll = random.nextDouble() * totalStrength;
        return roll <= homeStrength ? match.getHomeTeam() : match.getAwayTeam();
    }

    private Player getRandomPlayerByPositions(Match match, Team team, String[] positions) {
        ArrayList<Player> players = team.getPlayers();

        if (players.isEmpty()) throw new IllegalStateException("Cannot select a player from an empty team.");

        ArrayList<Player> candidates = new ArrayList<>();

        for (Player player : players) {
            for (String position : positions) {
                if (player.getPosition().equalsIgnoreCase(position) && !findPlayer(player, match.getRedCardedPlayers())) {
                    candidates.add(player);
                }
            }
        }

        if (candidates.isEmpty()) {
            for (Player player : players) {
                if (!findPlayer(player, match.getRedCardedPlayers())) {
                    candidates.add(player);
                }
            }
        }

        if (candidates.isEmpty()) throw new IllegalStateException("No available players remaining for this team.");

        return candidates.get(random.nextInt(candidates.size()));
    }

    private String chooseRandomChoice(Event event) {
        ArrayList<String> choices = event.getChoices();

        if (choices.isEmpty()) throw new IllegalArgumentException("Event has no choices.");

        return choices.get(random.nextInt(choices.size()));
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
