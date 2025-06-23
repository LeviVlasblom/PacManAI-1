package pacman.AI;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

import java.util.*;

public class UCSPacMan extends Controller<MOVE> {

    /**
     * Search Method: Uniform Cost Search (UCS)
     * UCS expands the least-cost node first, guaranteeing the shortest path in graphs with varying edge costs.
     * Here, UCS is used to find the safest and closest pill for PacMan, considering ghost proximity as extra cost.
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
            target = getClosestPillUCS(game, current, activePills); // <-- UCS search is used here to find the closest pill
        }

        if (target == -1) {
            return MOVE.NEUTRAL;
        }

        return game.getNextMoveTowardsTarget(current, target, pacman.game.Constants.DM.PATH);
    }

    /**
     * Finds the closest pill using Uniform Cost Search (UCS).
     *
     * @param game   The current game state.
     * @param start  The starting node (PacMan's current position).
     * @param pills  The indices of active pills.
     * @return The index of the closest pill, or -1 if no path is found.
     */
    private int getClosestPillUCS(Game game, int start, int[] pills) {
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        Map<Integer, Integer> cameFrom = new HashMap<>();

        queue.add(new Node(start, 0));
        visited.add(start);

        Set<Integer> pillSet = new HashSet<>();
        for (int pill : pills) pillSet.add(pill);

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            int current = currentNode.index;

            // Check if the current node is a pill
            if (pillSet.contains(current)) {
                List<Integer> path = reconstructPath(cameFrom, current);
                if (path.size() > 1) {
                    return path.get(1);
                } else {
                    return current;
                }
            }

            for (int neighbor : game.getNeighbouringNodes(current)) {
                if (!visited.contains(neighbor)) {
                    int penalty = getGhostProximityPenalty(game, neighbor);
                    int newCost = currentNode.cost + 1 + penalty;
                    queue.add(new Node(neighbor, newCost));
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }
        return -1;
    }

    /**
     * Adds a cost penalty for being near any non-edible, non-lair ghost.
     */
    private int getGhostProximityPenalty(Game game, int nodeIndex) {
        int penalty = 0;
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostLairTime(ghost) > 0) continue;
            if (game.isGhostEdible(ghost)) continue;
            int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
            int dist = game.getShortestPathDistance(nodeIndex, ghostIndex);
            if (dist <= 4) penalty += (5 - dist); // Strong penalty for being very close
        }
        return penalty;
    }

    private static class Node {
        int index;
        int cost;

        Node(int index, int cost) {
            this.index = index;
            this.cost = cost;
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

