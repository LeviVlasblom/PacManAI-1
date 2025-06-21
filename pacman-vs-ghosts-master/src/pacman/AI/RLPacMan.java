package pacman.AI;

import java.util.*;
import pacman.controllers.Controller;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class RLPacMan extends Controller<MOVE> {
    private static final Random rnd = new Random();

    private double alpha = 0.1; // learning rate
    private double gamma = 0.95; // discount factor
    private double epsilon = 1.0; // exploration rate
    private final double minEpsilon = 0.05;
    private final double epsilonDecay = 0.999; // slower decay for more exploration

    private int previousScore = 0;
    private int previousPillCount = 0;

    private String lastState = null;
    private MOVE lastMove = MOVE.NEUTRAL;

    private int previousPowerPillCount = 0;
    private Map<GHOST, Integer> previousGhostEdibleTimes = new EnumMap<>(GHOST.class);

    private boolean initialized = false;
    private int previousLevel = 0;
    private final Map<String, EnumMap<MOVE, Double>> qTable = new HashMap<>();

    // Loop detection buffer
    private static final int LOOP_HISTORY_SIZE = 20; // increased history size
    private static final double LOOP_PENALTY = -5.0;
    private final Deque<String> recentStateActions = new LinkedList<>();

    public RLPacMan() {
        loadQTable(); // Load Q-table from file if it exists
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {

        if (!initialized) {
            loadQTable();
            initialized = true;
        }

        String state = encodeState(game);
        MOVE move = selectMove(state, game);
        double reward = computeReward(game);

        // Loop detection: penalize if this state-action is in recent history
        String stateAction = state + ":" + move;
        boolean looped = false;
        if (recentStateActions.contains(stateAction)) {
            reward += LOOP_PENALTY;
            looped = true;
        }
        recentStateActions.addLast(stateAction);
        if (recentStateActions.size() > LOOP_HISTORY_SIZE) {
            recentStateActions.removeFirst();
        }

        if (lastState != null) {
            updateQTable(lastState, lastMove, reward, state);
        }

        epsilon = Math.max(minEpsilon, epsilon * epsilonDecay);

        lastState = state;
        lastMove = move;

        previousScore = game.getScore();
        previousPillCount = game.getActivePillsIndices().length;
        previousLevel = game.getCurrentLevel();

        // Debugging output to CSV (add looped column)
        // try (FileWriter debugWriter = new FileWriter("RL_Debug.csv", true)) {
        // debugWriter.append(String.format(Locale.US,
        // "%s,%s,%.2f,%.3f,%.2f,%d,%s\n",
        // state, move, reward, epsilon, qTable.get(state).get(move), qTable.size(),
        // looped ? "LOOP" : ""));
        // } catch (IOException e) {
        // // Ignore debug write errors
        // }

        return move;
    }

    private String encodeState(Game game) {
        int current = game.getPacmanCurrentNodeIndex();
        int level = game.getCurrentLevel();

        // Distance to nearest ghost
        int nearestGhostDist = Arrays.stream(GHOST.values())
                .filter(g -> game.getGhostLairTime(g) <= 0)
                .mapToInt(g -> game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(g)))
                .min().orElse(999);

        // Distance to nearest edible ghost
        int nearestEdibleGhostDist = Arrays.stream(GHOST.values())
                .filter(g -> game.isGhostEdible(g) && game.getGhostLairTime(g) == 0)
                .mapToInt(g -> game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(g)))
                .min().orElse(999);

        // Distance to nearest pill
        int[] pills = game.getActivePillsIndices();
        int minPillDist = Integer.MAX_VALUE;
        for (int pill : pills) {
            int dist = game.getShortestPathDistance(current, pill);
            if (dist >= 0 && dist < minPillDist)
                minPillDist = dist;
        }

        // Number of remaining pills
        int numPills = pills.length;

        // Number of remaining power pills
        int numPowerPills = game.getActivePowerPillsIndices().length;

        // Number of edible ghosts
        int edibleGhosts = (int) Arrays.stream(GHOST.values())
                .filter(g -> game.isGhostEdible(g) && game.getGhostLairTime(g) == 0)
                .count();

        // Discretize distances for state abstraction
        int ghostDistBucket = nearestGhostDist / 5; // e.g., 0-4, 5-9, ...
        int edibleGhostDistBucket = nearestEdibleGhostDist / 5;
        int pillDistBucket = minPillDist / 5;

        // Compose state string with level and last move
        return String.format("LVL%d_G%d_E%d_P%d_NP%d_NPP%d_EG%d_L%s",
                level, ghostDistBucket, edibleGhostDistBucket, pillDistBucket, numPills, numPowerPills, edibleGhosts,
                lastMove);
    }

    private MOVE selectMove(String state, Game game) {
        EnumMap<MOVE, Double> moves = qTable.computeIfAbsent(state, s -> initMoveMap());

        MOVE[] legal = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());

        if (rnd.nextDouble() < epsilon) {
            return legal[rnd.nextInt(legal.length)];
        }

        return moves.entrySet().stream()
                .filter(entry -> Arrays.asList(legal).contains(entry.getKey()))
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(legal[0]); // fallback to a legal move
    }

    private double computeReward(Game game) {
        double reward = 0;

        // Stronger penalty for being eaten
        if (game.wasPacManEaten()) {
            return -1000;
        }

        // Larger reward for finishing the game
        if (game.gameOver()) {
            reward += 2000;
        }

        if (game.getCurrentLevel() > previousLevel) {
            reward += 3000; // bonus for level complete
        }

        // Larger reward for score increase
        int currentScore = game.getScore();
        if (currentScore > previousScore) {
            reward += (currentScore - previousScore) * 0.2;
        }

        // Larger reward for eating pills
        int currentPillCount = game.getActivePillsIndices().length;
        if (currentPillCount < previousPillCount) {
            reward += 20;
        }

        // Larger reward for eating power pills
        int[] powerPills = game.getActivePowerPillsIndices();
        if (previousPowerPillCount > powerPills.length) {
            reward += 100;
        }

        // Larger reward for eating ghosts
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) < previousGhostEdibleTimes.getOrDefault(ghost, 0)) {
                reward += 200;
            }
        }

        // Stronger penalty for being near dangerous ghosts
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        for (GHOST ghost : GHOST.values()) {
            if (!game.isGhostEdible(ghost) && game.getGhostLairTime(ghost) == 0) {
                int dist = game.getShortestPathDistance(pacmanIndex, game.getGhostCurrentNodeIndex(ghost));
                if (dist >= 0 && dist <= 5) {
                    reward -= 10;
                }
            }
        }

        // Penalty for standing still
        if (lastMove == MOVE.NEUTRAL) {
            reward -= 2;
        }

        reward -= 0.2; // slightly larger penalty for each step

        // Update state trackers
        previousScore = currentScore;
        previousPillCount = currentPillCount;
        previousPowerPillCount = powerPills.length;
        for (GHOST ghost : GHOST.values()) {
            previousGhostEdibleTimes.put(ghost, game.getGhostEdibleTime(ghost));
        }

        return reward;
    }

    // Pad naar Q-table bestand
    private static final String Q_TABLE_FILE = "qtable.csv";

    // Removed terminate() override because it is final in Controller

    public void saveQTable() {
        try (FileWriter writer = new FileWriter(Q_TABLE_FILE)) {
            for (Map.Entry<String, EnumMap<MOVE, Double>> entry : qTable.entrySet()) {
                String state = entry.getKey();
                EnumMap<MOVE, Double> moves = entry.getValue();
                for (MOVE m : MOVE.values()) {
                    writer.write(state + "," + m + "," + moves.getOrDefault(m, 0.0) + "\n");
                }
            }
            writer.flush();
            System.out.println("Q-table saved successfully.");
        } catch (IOException e) {
            System.out.println("Failed to save Q-table.");
            e.printStackTrace();
        }
    }

    // Q-table laden bij opstarten
    public void loadQTable() {
        File file = new File(Q_TABLE_FILE);
        if (!file.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3)
                    continue;

                String state = parts[0];
                MOVE move = MOVE.valueOf(parts[1]);
                double value = Double.parseDouble(parts[2]);

                qTable.computeIfAbsent(state, k -> initMoveMap()).put(move, value);
            }
            System.out.println("[RLPacMan] Q-table loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateQTable(String state, MOVE move, double reward, String nextState) {
        EnumMap<MOVE, Double> qValues = qTable.computeIfAbsent(state, s -> initMoveMap());
        EnumMap<MOVE, Double> nextQ = qTable.computeIfAbsent(nextState, s -> initMoveMap());

        double oldValue = qValues.get(move);
        double maxNext = nextQ.values().stream().max(Double::compare).orElse(0.0);
        double newValue = oldValue + alpha * (reward + gamma * maxNext - oldValue);

        qValues.put(move, newValue);
    }

    private EnumMap<MOVE, Double> initMoveMap() {
        EnumMap<MOVE, Double> map = new EnumMap<>(MOVE.class);
        for (MOVE m : MOVE.values()) {
            map.put(m, 0.0);
        }
        return map;
    }

}
