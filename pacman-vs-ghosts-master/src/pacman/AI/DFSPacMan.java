package pacman.AI;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

import java.util.*;

public class DFSPacMan extends Controller<MOVE> {

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        int[] activePills = game.getActivePillsIndices();

        // If no pills are available, return NEUTRAL
        if (activePills.length == 0) {
            return MOVE.NEUTRAL;
        }

        // Ghost avoidance logic
        int target;
        if (isGhostThreatening(game, 25)) {
            target = getSafestPill(game, current);
        } else {
            target = getClosestPillDFS(game, current, activePills);
        }

        // If no valid target is found, return NEUTRAL
        if (target == -1) {
            return MOVE.NEUTRAL;
        }

        // Return the move towards the target
        return game.getNextMoveTowardsTarget(current, target, pacman.game.Constants.DM.PATH);
    }

    /**
     * Finds the closest pill using Depth-First Search (DFS).
     *
     * @param game   The current game state.
     * @param start  The starting node (PacMan's current position).
     * @param pills  The indices of active pills.
     * @return The index of the closest pill, or -1 if no path is found.
     */
    private int getClosestPillDFS(Game game, int start, int[] pills) {
        Set<Integer> visited = new HashSet<>();
        Stack<Integer> stack = new Stack<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();

        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            int current = stack.pop();

            // Check if the current node is a pill
            if (Arrays.stream(pills).anyMatch(pill -> pill == current)) {
                return reconstructPath(cameFrom, current).get(1); // Return the next step towards the pill
            }

            // Explore neighbors
            for (int neighbor : game.getNeighbouringNodes(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    stack.push(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }

        // No path found
        return -1;
    }

    /**
     * Reconstructs the path from the start to the target using the cameFrom map.
     *
     * @param cameFrom The map of nodes and their predecessors.
     * @param target   The target node.
     * @return A list representing the path from start to target.
     */
    private List<Integer> reconstructPath(Map<Integer, Integer> cameFrom, int target) {
        List<Integer> path = new ArrayList<>();
        path.add(target);

        while (cameFrom.containsKey(target)) {
            target = cameFrom.get(target);
            path.add(0, target);
        }

        return path;
    }

    // --- Ghost logic helpers (copied/adapted from AStarPacMan) ---

    private boolean isGhostThreatening(Game game, int ghostDistanceThreshold) {
        int pacman = game.getPacmanCurrentNodeIndex();
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostLairTime(ghost) > 0)
                continue;
            if (!game.isGhostEdible(ghost)) {
                int ghostPos = game.getGhostCurrentNodeIndex(ghost);
                int dist = game.getShortestPathDistance(pacman, ghostPos);
                if (dist <= ghostDistanceThreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getSafestPill(Game game, int pacmanIndex) {
        int[] pills = game.getActivePillsIndices();
        if (pills.length == 0)
            return -1;

        int safestPill = -1;
        int maxTotalGhostDistance = Integer.MIN_VALUE;

        for (int pill : pills) {
            int totalDist = 0;
            for (GHOST ghost : GHOST.values()) {
                if (game.getGhostLairTime(ghost) > 0)
                    continue;
                if (game.isGhostEdible(ghost))
                    continue;
                int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
                int dist = game.getShortestPathDistance(pill, ghostIndex);
                totalDist += dist;
            }
            if (totalDist > maxTotalGhostDistance) {
                maxTotalGhostDistance = totalDist;
                safestPill = pill;
            }
        }
        return safestPill;
    }
}
