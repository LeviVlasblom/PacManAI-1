package pacman.AI;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

import java.util.*;

public class GreedyPacMan extends Controller<MOVE> {

    /**
     * Search Method: Greedy Search
     * Greedy search always selects the next node that appears closest to the goal (pill), using a heuristic.
     * This approach is fast but can miss optimal paths. Here, Greedy is used to find the closest pill for PacMan.
     */

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        int[] activePills = game.getActivePillsIndices();

        if (activePills.length == 0) {
            return MOVE.NEUTRAL;
        }

        // Ghost avoidance logic
        int target;
        if (isGhostThreatening(game, 25)) {
            target = getSafestPill(game, current);
        } else {
            target = getClosestPillGreedy(game, current, activePills); // <-- Greedy search is used here to find the closest pill
        }

        if (target == -1) {
            return MOVE.NEUTRAL;
        }

        return game.getNextMoveTowardsTarget(current, target, pacman.game.Constants.DM.PATH);
    }

    /**
     * Finds the closest pill using Greedy Search.
     *
     * @param game   The current game state.
     * @param start  The starting node (PacMan's current position).
     * @param pills  The indices of active pills.
     * @return The index of the closest pill, or -1 if no path is found.
     */
    private int getClosestPillGreedy(Game game, int start, int[] pills) {
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.heuristic));
        Map<Integer, Integer> cameFrom = new HashMap<>();

        queue.add(new Node(start, getHeuristic(game, start, pills)));
        visited.add(start);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            int current = currentNode.index;

            // Check if the current node is a pill
            if (Arrays.stream(pills).anyMatch(pill -> pill == current)) {
                List<Integer> path = reconstructPath(cameFrom, current);
                if (path.size() > 1) {
                    return path.get(1); // Return the next step towards the pill
                } else {
                    return current;
                }
            }

            // Explore neighbors, always picking the one with the lowest heuristic
            for (int neighbor : game.getNeighbouringNodes(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(new Node(neighbor, getHeuristic(game, neighbor, pills)));
                    cameFrom.put(neighbor, current);
                }
            }
        }

        // No path found
        return -1;
    }

    private int getHeuristic(Game game, int from, int[] pills) {
        // Heuristic: minimum path distance to any pill
        int minDist = Integer.MAX_VALUE;
        for (int pill : pills) {
            int dist = game.getShortestPathDistance(from, pill);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private static class Node {
        int index;
        int heuristic;

        Node(int index, int heuristic) {
            this.index = index;
            this.heuristic = heuristic;
        }
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

    // Ghost logic helpers

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

