package pacman.AI;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Game;

public class RLPacManV2 extends Controller<MOVE> {
    private final double alpha = 0.5;
    private final double gamma = 0.95;
    private double epsilon = 1.0;
    private final double epsilonDecay = 0.998;
    private final double minEpsilon = 0.1;

    private final Map<String, EnumMap<MOVE, Double>> qTable = new HashMap<>();
    private final Random random = new Random();
    private final Set<String> stateHistory = new HashSet<>();

    private String lastState;
    private MOVE lastMove;
    private int lastPacmanIndex = -1;

    private final double PILL_REWARD = 10.0;
    private final double POWER_PILL_REWARD = 50.0;
    private final double EAT_GHOST_REWARD = 200.0;
    private final double DEATH_PENALTY = -150.0;
    private final double LOOP_PENALTY = -20.0;
    private final double NEUTRAL_MOVE_PENALTY = -5.0;
    private final double MOVE_REWARD = 1.1;

    private final Map<String, Integer> visitCounts = new HashMap<>();

    public RLPacManV2() {
        loadQTable("qtable.csv");
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        String state = encodeState(game);
        MOVE move = selectMove(state, game);

        if (move == MOVE.NEUTRAL) {
            System.out.println("[DEBUG] NEUTRAL move selected for state: " + state);
        }

        double reward = calculateReward(game);

        if (state.equals(lastState)) {
            reward += LOOP_PENALTY;
        }

        if (move == MOVE.NEUTRAL) {
            reward += NEUTRAL_MOVE_PENALTY;
        }

        // Reward for moving to a new node
        int currentIndex = game.getPacmanCurrentNodeIndex();
        if (lastPacmanIndex != -1 && currentIndex != lastPacmanIndex) {
            reward += MOVE_REWARD;
        }
        lastPacmanIndex = currentIndex;

        visitCounts.put(state, visitCounts.getOrDefault(state, 0) + 1);

        if (lastState != null) {
            updateQTable(lastState, lastMove, reward, state);
        }

        lastState = state;
        lastMove = move;

        epsilon = Math.max(minEpsilon, epsilon * epsilonDecay);

        return move;
    }

    private String encodeState(Game game) {
        int pacmanNode = game.getPacmanCurrentNodeIndex();
        int nearestPillDist = Integer.MAX_VALUE;
        int nearestPowerPillDist = Integer.MAX_VALUE;

        boolean ghostNear = false;
        boolean anyGhostEdible = false;
        String ghostQuadrant = "N"; // N = none/default

        for (int pillIndex : game.getActivePillsIndices()) {
            int dist = game.getShortestPathDistance(pacmanNode, pillIndex);
            nearestPillDist = Math.min(nearestPillDist, dist);
        }

        for (int ppIndex : game.getActivePowerPillsIndices()) {
            int dist = game.getShortestPathDistance(pacmanNode, ppIndex);
            nearestPowerPillDist = Math.min(nearestPowerPillDist, dist);
        }

        int minGhostDist = Integer.MAX_VALUE;
        GHOST closestGhost = null;
        for (GHOST ghost : GHOST.values()) {
            int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
            if (ghostIndex >= 0 && game.getGhostLairTime(ghost) == 0) {
                int dist = game.getShortestPathDistance(pacmanNode, ghostIndex);
                if (dist < minGhostDist) {
                    minGhostDist = dist;
                    closestGhost = ghost;
                }
                if (dist < 6)
                    ghostNear = true;
                if (game.isGhostEdible(ghost))
                    anyGhostEdible = true;
            }
        }

        // Richting bepalen (kwadrant)
        if (closestGhost != null) {
            MOVE dir = game.getApproximateNextMoveTowardsTarget(
                    pacmanNode,
                    game.getGhostCurrentNodeIndex(closestGhost),
                    game.getPacmanLastMoveMade(),
                    DM.PATH);

            if (dir != null) {
                ghostQuadrant = dir.toString().substring(0, 1); // U/D/L/R/N
            }
        }

        int pillBucket = nearestPillDist / 20;
        int ppBucket = nearestPowerPillDist / 20;

        return String.format("P%d_PP%d_GN%d_EG%d_GQ%s",
                pillBucket,
                ppBucket,
                ghostNear ? 1 : 0,
                anyGhostEdible ? 1 : 0,
                ghostQuadrant);
    }

    private MOVE selectMove(String state, Game game) {
        MOVE[] possibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
        List<MOVE> legalMoves = new ArrayList<>();

        for (MOVE move : possibleMoves) {
            if (move != MOVE.NEUTRAL) {
                legalMoves.add(move);
            }
        }

        if (legalMoves.isEmpty())
            return MOVE.NEUTRAL;

        if (random.nextDouble() < epsilon) {
            return legalMoves.get(random.nextInt(legalMoves.size()));
        }

        EnumMap<MOVE, Double> moveMap = qTable.getOrDefault(state, initMoveMap());
        MOVE bestMove = MOVE.NEUTRAL;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (MOVE move : legalMoves) {
            double value = moveMap.getOrDefault(move, 0.0);
            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private void updateQTable(String state, MOVE move, double reward, String nextState) {
        EnumMap<MOVE, Double> qValues = qTable.computeIfAbsent(state, s -> initMoveMap());
        EnumMap<MOVE, Double> nextQ = qTable.computeIfAbsent(nextState, s -> initMoveMap());

        double oldValue = qValues.get(move);
        double maxNext = nextQ.values().stream().max(Double::compare).orElse(0.0);
        double newValue = oldValue + alpha * (reward + gamma * maxNext - oldValue);

        qValues.put(move, newValue);

        // System.out.printf("UPDATE: state=%s, move=%s, old=%.2f, new=%.2f,
        // reward=%.2f\n",
        // state, move, oldValue, newValue, reward);
    }

    private EnumMap<MOVE, Double> initMoveMap() {
        EnumMap<MOVE, Double> map = new EnumMap<>(MOVE.class);
        for (MOVE move : MOVE.values()) {
            map.put(move, 0.0);
        }
        return map;
    }

    private double calculateReward(Game game) {
        int pacman = game.getPacmanCurrentNodeIndex();
        double reward = -1.0;

        // Dood = direct straf
        if (game.wasPacManEaten())
            return DEATH_PENALTY;

        // Beloningen voor events
        if (game.wasPowerPillEaten())
            reward += POWER_PILL_REWARD;
        if (game.wasPillEaten())
            reward += PILL_REWARD;

        // Ghost-interacties
        for (GHOST g : GHOST.values()) {
            int ghost = game.getGhostCurrentNodeIndex(g);
            int lairTime = game.getGhostLairTime(g);

            // Ghost niet actief of positie onbekend
            if (ghost == -1 || lairTime > 0)
                continue;

            // Alleen berekenen als pacman geldige index heeft
            if (pacman != -1) {
                int dist = game.getShortestPathDistance(pacman, ghost);

                if (game.isGhostEdible(g)) {
                    if (dist < 8)
                        reward += 20.0; // beloon jacht
                    if (dist < 4)
                        reward += 50.0;
                } else {
                    if (dist < 6)
                        reward += -10.0 * (6 - dist); // straf bij naderen

                    // Vluchtgedrag belonen
                    if (lastMove != null) {
                        int next = game.getNeighbour(pacman, lastMove);
                        if (next != -1) {
                            int newDist = game.getShortestPathDistance(next, ghost);
                            if (newDist > dist)
                                reward += 10.0;
                        }
                    }
                }
            }
        }

        return reward;
    }

    public void saveQTable(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            for (Map.Entry<String, EnumMap<MOVE, Double>> entry : qTable.entrySet()) {
                String state = entry.getKey();
                for (Map.Entry<MOVE, Double> moveEntry : entry.getValue().entrySet()) {
                    writer.write(state + "," + moveEntry.getKey() + "," + moveEntry.getValue() + "\n");
                }
            }
            System.out.println("Q-table saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadQTable(String filename) {
        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts.length == 3) {
                    String state = parts[0].trim();
                    MOVE move = MOVE.valueOf(parts[1].trim());
                    double value = Double.parseDouble(parts[2].trim());
                    qTable.computeIfAbsent(state, s -> initMoveMap()).put(move, value);
                }
            }
            System.out.println("Q-table loaded.");
        } catch (IOException e) {
            System.out.println("No Q-table found, starting fresh.");
        }
    }
}
