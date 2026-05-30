package simulation;

import gamemechanics.Event;
import gamemechanics.EventType;
import gamemechanics.Match;
import gamemechanics.Player;
import gamemechanics.Team;
import java.util.ArrayList;
import java.util.Random;

public class SimulationEngine {
    private final Random random;

    public SimulationEngine() {
        this.random = new Random();
    }

    public SimulationEngine(long seed) {
        this.random = new Random(seed);
    }

    public Match simulateMatch(Team homeTeam, Team awayTeam) {
        Match match = new Match(homeTeam, awayTeam);
        simulateMatch(match);
        return match;
    }

    public void simulateMatch(Match match) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");

        if (!match.hasStarted()) {
            match.startMatch();
        }

        for (int minute = 5; minute <= 90; minute += 5) {
            int eventRoll = random.nextInt(100) + 1;
            Team attackingTeam = chooseAttackingTeam(match);
            Event event;

            if (eventRoll <= 55) {
                createNormalShot(match, attackingTeam, minute);
            } else if (eventRoll <= 70) {
                createFoul(match, match.getOpponent(attackingTeam), minute);
            } else if (eventRoll <= 85) {
                event = createBigChance(match, attackingTeam, minute);
                resolveChance(match, event, chooseRandomChoice(event));
            } else if (eventRoll <= 92) {
                event = createPenalty(match, attackingTeam, minute);
                resolveChance(match, event, chooseRandomChoice(event));
            } else {
                match.addEvent(new Event(
                    minute,
                    EventType.COMMENTARY,
                    null,
                    null,
                    "Both teams are battling for control in midfield."
                ));
            }
        }

        if (!match.isFinished()) {
            match.endMatch();
        }
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

        Event foul = new Event(
            minute,
            EventType.FOUL,
            committingTeam,
            player,
            player.getName() + " committed a foul for " + committingTeam.getName() + "."
        );

        match.addEvent(foul);

        // Small chance of a card after a foul
        int cardChance = random.nextInt(100) + 1;

        if (cardChance <= 5) {
            match.addEvent(new Event(
                minute,
                EventType.RED_CARD,
                committingTeam,
                player,
                player.getName() + " received a red card!"
            ));
            match.recordRedCard(player);
        } else if (cardChance <= 30) {
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
            } else {
                match.addEvent(new Event(
                    minute,
                    EventType.YELLOW_CARD,
                    committingTeam,
                    player,
                    player.getName() + " received a yellow card."
                ));
                match.recordYellowCard(player);
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

    /* Chance Resolution Methods */
    public void resolveChance(Match match, Event event, String selectedChoice) {
        if (match == null) throw new IllegalArgumentException("Match cannot be null.");
        match.validateMatchInProgress();

        if (event == null) throw new IllegalArgumentException("Event cannot be null.");
        if (!match.getEvents().contains(event)) throw new IllegalArgumentException("Event must already be part of the match.");
        if (!event.isBigChance()) throw new IllegalArgumentException("Only big chance events can be resolved with a choice.");
        if (event.isResolved()) throw new IllegalStateException("This event has already been resolved.");

        Team attackingTeam = event.getTeam();
        Team defendingTeam = match.getOpponent(attackingTeam);
        Player player = event.getPlayer();

        event.setSelectedChoice(selectedChoice);

        double conversionChance;

        if (event.isPenalty()) conversionChance = calculatePenaltyConversionChance(player, selectedChoice, defendingTeam);
        else conversionChance = calculateConversionChance(match, player, selectedChoice, attackingTeam, defendingTeam);

        boolean scored = random.nextDouble() * 100 <= conversionChance;
        event.setSuccessful(scored);

        if (scored) {
            match.addGoal(attackingTeam);

            match.addEvent(new Event(
                event.getMinute(),
                EventType.GOAL,
                attackingTeam,
                player,
                player.getName() + " scored for " + attackingTeam.getName() + "!"
            ));
        } else {
            Player goalkeeper = defendingTeam.getGoalkeeper();

            match.addEvent(new Event(
                event.getMinute(),
                EventType.SAVE,
                defendingTeam,
                goalkeeper,
                defendingTeam.getName() + " survived the chance from " + attackingTeam.getName() + "."
            ));
        }
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

    private double calculatePenaltyConversionChance(Player player, String selectedChoice, Team defendingTeam) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null.");
        if (defendingTeam == null) throw new IllegalArgumentException("Defending team cannot be null.");

        if (selectedChoice == null || selectedChoice.trim().isEmpty()) throw new IllegalArgumentException("Selected choice cannot be blank/empty.");

        selectedChoice = selectedChoice.trim().toUpperCase();

        if (!selectedChoice.equalsIgnoreCase("SHOOT LEFT") && !selectedChoice.equalsIgnoreCase("SHOOT MIDDLE") && !selectedChoice.equalsIgnoreCase("SHOOT RIGHT")) throw new IllegalArgumentException("Selected choice must be either 'Shoot Left', 'Shoot Middle', or 'Shoot Right'.");

        double keeperRating = defendingTeam.getTeamGoalkeeperRating();
        double chance = 65 + player.getShooting() * 0.25 - keeperRating * 0.15;

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
        double homeStrength = match.getHomeTeam().getTeamAttackRating(match.getRedCardedPlayers()) + match.getHomeTeam().getTeamMidfieldRating(match.getRedCardedPlayers());
        double awayStrength = match.getAwayTeam().getTeamAttackRating(match.getRedCardedPlayers()) + match.getAwayTeam().getTeamMidfieldRating(match.getRedCardedPlayers());
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
