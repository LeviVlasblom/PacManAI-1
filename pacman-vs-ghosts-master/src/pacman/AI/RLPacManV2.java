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

// Q-learning PacMan agent with enhanced state features and reward shaping
// Uses Q-table, state abstraction, and ghost-aware features for learning
public class RLPacManV2 extends Controller<MOVE> {
    // Learning parameters
    private final double alpha = 0.5; // Learning rate
    private final double gamma = 0.95; // Discount factor
    private double epsilon = 1.0; // Exploration rate
    private final double epsilonDecay = 0.998; // Epsilon decay per step
    private final double minEpsilon = 0.1; // Minimum exploration

    // Q-table: maps state string to a map of MOVE -> Q-value
    private final Map<String, EnumMap<MOVE, Double>> qTable = new HashMap<>();
    private final Random random = new Random();

    private String lastState;
    private MOVE lastMove;
    private int lastPacmanIndex = -1; // For movement reward

    // Reward values for different events
    private final double PILL_REWARD = 10.0;
    private final double POWER_PILL_REWARD = 50.0;
    private final double EAT_GHOST_REWARD = 200.0;
    private final double DEATH_PENALTY = -150.0;
    private final double LOOP_PENALTY = -20.0;
    private final double NEUTRAL_MOVE_PENALTY = -5.0;
    private final double MOVE_REWARD = 1.1; // Reward for moving to a new node

    private final Map<String, Integer> visitCounts = new HashMap<>(); // State visit counts

    public RLPacManV2() {
        loadQTable("qtable.csv"); // Load Q-table from file if it exists
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        // Encode the current state
        String state = encodeState(game);
        // Select a move using epsilon-greedy policy
        MOVE move = selectMove(state, game);

        if (move == MOVE.NEUTRAL) {
            System.out.println("[DEBUG] NEUTRAL move selected for state: " + state);
        }

        // Calculate reward for the last action
        double reward = calculateReward(game);

        // Penalize loops (revisiting the same state)
        if (state.equals(lastState)) {
            reward += LOOP_PENALTY;
        }

        // Penalize standing still
        if (move == MOVE.NEUTRAL) {
            reward += NEUTRAL_MOVE_PENALTY;
        }

        // Reward for moving to a new node
        int currentIndex = game.getPacmanCurrentNodeIndex();
        if (lastPacmanIndex != -1 && currentIndex != lastPacmanIndex) {
            reward += MOVE_REWARD;
        }
        lastPacmanIndex = currentIndex;

        // Track state visit counts
        visitCounts.put(state, visitCounts.getOrDefault(state, 0) + 1);

        // Q-learning update
        if (lastState != null) {
            updateQTable(lastState, lastMove, reward, state);
        }

        // Update last state and move
        lastState = state;
        lastMove = move;

        // Decay epsilon for less exploration over time
        epsilon = Math.max(minEpsilon, epsilon * epsilonDecay);

        return move;
    }

    /**
     * Encodes the current game state as a string for Q-table lookup.
     * Uses bucketed distances, ghost proximity, edible flag, and ghost direction.
     */
    private String encodeState(Game game) {
        int pacmanNode = game.getPacmanCurrentNodeIndex();
        int nearestPillDist = Integer.MAX_VALUE;
        int nearestPowerPillDist = Integer.MAX_VALUE;

        boolean ghostNear = false;
        boolean anyGhostEdible = false;
        String ghostQuadrant = "N"; // N = none/default

        // Find distance to nearest pill
        for (int pillIndex : game.getActivePillsIndices()) {
            int dist = game.getShortestPathDistance(pacmanNode, pillIndex);
            nearestPillDist = Math.min(nearestPillDist, dist);
        }

        // Find distance to nearest power pill
        for (int ppIndex : game.getActivePowerPillsIndices()) {
            int dist = game.getShortestPathDistance(pacmanNode, ppIndex);
            nearestPowerPillDist = Math.min(nearestPowerPillDist, dist);
        }

        // Find closest ghost and its properties
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

        // Determine direction/quadrant to closest ghost
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

        // Bucket distances for state abstraction
        int pillBucket = nearestPillDist / 20;
        int ppBucket = nearestPowerPillDist / 20;

        // State string: pill bucket, power pill bucket, ghost near, edible, quadrant
        return String.format("P%d_PP%d_GN%d_EG%d_GQ%s",
                pillBucket,
                ppBucket,
                ghostNear ? 1 : 0,
                anyGhostEdible ? 1 : 0,
                ghostQuadrant);
    }

    /**
     * Selects a move using epsilon-greedy policy and Q-table.
     * If all Q-values are equal, picks randomly among legal moves.
     */
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

        // Exploration: pick random legal move
        if (random.nextDouble() < epsilon) {
            return legalMoves.get(random.nextInt(legalMoves.size()));
        }

        // Exploitation: pick best Q-value move
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

    /**
     * Q-learning update rule for the Q-table.
     */
    private void updateQTable(String state, MOVE move, double reward, String nextState) {
        EnumMap<MOVE, Double> qValues = qTable.computeIfAbsent(state, s -> initMoveMap());
        EnumMap<MOVE, Double> nextQ = qTable.computeIfAbsent(nextState, s -> initMoveMap());

        double oldValue = qValues.get(move);
        double maxNext = nextQ.values().stream().max(Double::compare).orElse(0.0);
        double newValue = oldValue + alpha * (reward + gamma * maxNext - oldValue);

        qValues.put(move, newValue);

        // Uncomment for debug: print Q-table updates
        // System.out.printf("UPDATE: state=%s, move=%s, old=%.2f, new=%.2f,
        // reward=%.2f\n",
        // state, move, oldValue, newValue, reward);
    }

    /**
     * Initializes a move map with all Q-values set to 0.0.
     */
    private EnumMap<MOVE, Double> initMoveMap() {
        EnumMap<MOVE, Double> map = new EnumMap<>(MOVE.class);
        for (MOVE move : MOVE.values()) {
            map.put(move, 0.0);
        }
        return map;
    }

    /**
     * Calculates the reward for the last action based on game events and ghost
     * proximity.
     */
    private double calculateReward(Game game) {
        int pacman = game.getPacmanCurrentNodeIndex();
        double reward = -1.0; // Small penalty for each step

        // Death = immediate penalty
        if (game.wasPacManEaten())
            return DEATH_PENALTY;

        // Rewards for events
        if (game.wasPowerPillEaten())
            reward += POWER_PILL_REWARD;
        if (game.wasPillEaten())
            reward += PILL_REWARD;

        // Ghost interactions
        for (GHOST g : GHOST.values()) {
            int ghost = game.getGhostCurrentNodeIndex(g);
            int lairTime = game.getGhostLairTime(g);

            // Skip if ghost not active or unknown
            if (ghost == -1 || lairTime > 0)
                continue;

            // Only calculate if Pac-Man has a valid index
            if (pacman != -1) {
                int dist = game.getShortestPathDistance(pacman, ghost);

                if (game.isGhostEdible(g)) {
                    if (dist < 8)
                        reward += 20.0; // Reward for chasing edible ghost
                    if (dist < 4)
                        reward += 50.0;
                } else {
                    if (dist < 6)
                        reward += -10.0 * (6 - dist); // Penalty for approaching dangerous ghost

                    // Reward for moving away from ghost
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

    /**
     * Saves the Q-table to a file.
     */
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

    /**
     * Loads the Q-table from a file if it exists.
     */
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
