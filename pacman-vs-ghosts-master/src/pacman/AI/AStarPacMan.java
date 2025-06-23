package pacman.AI;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.DM;

import java.util.*;

public class AStarPacMan extends Controller<MOVE> {

    /**
     * Search Method: A* Search
     * A* combines UCS and Greedy by expanding nodes with the lowest sum of path cost and heuristic estimate to the goal.
     * Here, A* is used to efficiently find the safest and shortest path to pills or ghosts for PacMan.
     */

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        pacman.AI.AIDebugWindow.getInstance().clear();

        Integer target = getEdibleGhostTarget(game, 30);

        if (target == null) {
            // Ghost avoidance logic
            if (isGhostThreatening(game, 25)) {
                pacman.AI.AIDebugWindow.getInstance().log("DANGER! Ghost nearby. Searching safe pill...");
                target = getSafestPill(game, current);
            } else {
                target = getClosestPill(game, current, game.getActivePillsIndices());
            }
        } else {
            pacman.AI.AIDebugWindow.getInstance().log("Chasing edible ghost at: " + target);
        }

        if (target != null && game.getShortestPathDistance(current, target) < 3 && isGhostThreatening(game, 20)) {
            pacman.AI.AIDebugWindow.getInstance().log("Aborting risky short path to " + target);
            return MOVE.NEUTRAL;
        }

        if (target == null || target == -1) {
            pacman.AI.AIDebugWindow.getInstance().log("No target found. Returning NEUTRAL.");
            return MOVE.NEUTRAL;
        }

        List<Integer> path = findPathAStar(game, current, target); // <-- A* search is used here to find the path

        pacman.AI.AIDebugWindow.getInstance().log("Start node: " + current);
        pacman.AI.AIDebugWindow.getInstance().log("Target node: " + target);

        if (path == null || path.size() < 2) {
            pacman.AI.AIDebugWindow.getInstance().log("No valid path found. Returning NEUTRAL.");
            return MOVE.NEUTRAL;
        }

        pacman.AI.AIDebugWindow.getInstance().log("Path length: " + path.size());
        pacman.AI.AIDebugWindow.getInstance().log("Next step: " + path.get(1));

        StringBuilder sb = new StringBuilder("Full path: ");
        for (int node : path) {
            sb.append(node).append(" -> ");
        }
        pacman.AI.AIDebugWindow.getInstance().log(sb.toString());

        return game.getNextMoveTowardsTarget(current, path.get(1), DM.PATH);
    }

    /**
     * Finds the closest pill to Pacman using the shortest path distance.
     */
    private int getClosestPill(Game game, int from, int[] pills) {
        int minDist = Integer.MAX_VALUE;
        int closest = -1;
        for (int pill : pills) {
            int dist = game.getShortestPathDistance(from, pill);
            if (dist < minDist) {
                minDist = dist;
                closest = pill;
            }
        }
        return closest;
    }

    /**
     * Finds the path from Pacman's current position to the target using A* pathfinding.
     */
    private List<Integer> findPathAStar(Game game, int start, int goal) {
        PriorityQueue<NodeRecord> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<Integer, Integer> gScore = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();

        gScore.put(start, 0);
        openSet.add(new NodeRecord(start, 0, getHeuristic(game, start, goal)));

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();
            if (current.id == goal) {
                return reconstructPath(cameFrom, current.id);
            }
            for (int neighbor : game.getNeighbouringNodes(current.id)) {
                int tentativeG = gScore.get(current.id) + 1;
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current.id);
                    gScore.put(neighbor, tentativeG);
                    int f = tentativeG + getHeuristic(game, neighbor, goal);
                    openSet.add(new NodeRecord(neighbor, tentativeG, f));
                }
            }
        }
        return null;
    }

    /**
     * Calculates an extra cost for being near any non-edible, moving ghost.
     */
    private int getGhostProximityPenalty(Game game, int nodeIndex) {
        final int DANGER_RADIUS = 10;
        final int PENALTY_SCALE = 20;
        int penalty = 0;
        for (GHOST ghost : GHOST.values()) {
            int ghostIndex = game.getGhostCurrentNodeIndex(ghost);
            if (ghostIndex == -1 || game.isGhostEdible(ghost) || game.getGhostLairTime(ghost) > 0)
                continue;
            int distance = game.getShortestPathDistance(nodeIndex, ghostIndex);
            if (distance >= 0 && distance <= DANGER_RADIUS) {
                penalty += (DANGER_RADIUS - distance) * PENALTY_SCALE;
            }
        }
        return penalty;
    }

    /**
     * Helper class for A* search.
     * Stores node id, path cost (g), and total estimated cost (f = g + heuristic).
     * Used in the priority queue to determine which node to expand next.
     */
    private static class NodeRecord {
        int id;
        int g;
        int f;
        NodeRecord(int id, int g, int f) {
            this.id = id;
            this.g = g;
            this.f = f;
        }
    }

    private int getHeuristic(Game game, int from, int to) {
        int distance = game.getShortestPathDistance(from, to);
        int ghostPenalty = getGhostProximityPenalty(game, from);
        return distance + ghostPenalty;
    }

    /**
     * Finds the safest pill to eat based on the distance to all ghosts.
     */
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

    /**
     * Checks if there are any ghosts that are too close to Pacman.
     */
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

    /**
     * Finds the closest edible ghost within a certain distance from Pacman.
     */
    private Integer getEdibleGhostTarget(Game game, int maxDistance) {
        int pacman = game.getPacmanCurrentNodeIndex();
        int closest = -1;
        int bestDist = Integer.MAX_VALUE;
        for (GHOST ghost : GHOST.values()) {
            if (game.isGhostEdible(ghost) && game.getGhostLairTime(ghost) == 0) {
                int ghostNode = game.getGhostCurrentNodeIndex(ghost);
                int dist = game.getShortestPathDistance(pacman, ghostNode);
                if (dist < bestDist && dist <= maxDistance) {
                    bestDist = dist;
                    closest = ghostNode;
                }
            }
        }
        return (closest != -1) ? closest : null;
    }

    /**
     * Reconstructs the path from the start to the target using the cameFrom map.
     */
    private List<Integer> reconstructPath(Map<Integer, Integer> cameFrom, int current) {
        List<Integer> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        return path;
    }
}

