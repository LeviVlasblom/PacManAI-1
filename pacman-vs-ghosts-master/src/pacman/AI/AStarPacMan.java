package pacman.AI;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;

import java.io.Console;
import java.util.*;

public class AStarPacMan extends Controller<MOVE> {

    /*
     * use getMove() to get the next move.
     * This method should return the next move for Pacman based on the current game
     * state.
     * It uses A* pathfinding to find the closest pill and returns the move towards
     * it.
     */
    // @Override
    // public MOVE getMove(Game game, long timeDue) {
    // // game.getPacmanCurrentNodeIndex() returns the current position of Pacman.
    // int current = game.getPacmanCurrentNodeIndex();
    // // game.getActivePillsIndices() returns the indices of all active pills in
    // the
    // // game. these are the points!
    // int[] activePills = game.getActivePillsIndices();

    // // If there are no active pills, return neutral move (do nothing).
    // if (activePills.length == 0)
    // return MOVE.NEUTRAL;

    // // Find the closest pill to Pacman using A* pathfinding.
    // int target = getClosestPill(game, current, activePills);
    // List<Integer> path = findPathAStar(game, current, target);

    // AIDebugWindow.getInstance().clear();
    // AIDebugWindow.getInstance().log("Start node: " + current);
    // AIDebugWindow.getInstance().log("Target node: " + target);

    // // Path not found or too short
    // if (path == null || path.size() < 2) {
    // AIDebugWindow.getInstance().log("No valid path found. Returning NEUTRAL.");
    // return MOVE.NEUTRAL;
    // }

    // // Log valid path
    // AIDebugWindow.getInstance().log("Path length: " + path.size());
    // AIDebugWindow.getInstance().log("Next step: " + path.get(1));

    // StringBuilder sb = new StringBuilder("Full path: ");
    // for (int node : path) {
    // sb.append(node).append(" -> ");
    // }
    // AIDebugWindow.getInstance().log(sb.toString());

    // // Return move towards second node in the path
    // return game.getNextMoveTowardsTarget(current, path.get(1), DM.PATH);
    // }

    /*
     * use getMove() to get the next move.
     * This method should return the next move for Pacman based on the current game
     * state.
     * It uses A* pathfinding to find the closest pill and returns the move towards
     * it.
     */
    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        AIDebugWindow.getInstance().clear();

        // get a edible ghost target if available with a maximum distance of 30 in nodes
        Integer target = getEdibleGhostTarget(game, 30);

        // If no edible ghost is found, we will look for the closest pill.
        if (target == null) {
            // Check if there are any ghosts that are too close to Pacman with max distance
            // of 25 in nodes.
            if (isGhostThreatening(game, 25)) {
                AIDebugWindow.getInstance().log("DANGER! Ghost nearby. Searching safe pill...");
                target = getSafestPill(game, current); // You can implement this as needed
            } else {
                // If no ghosts are threatening, find the closest pill
                target = getClosestPill(game, current, game.getActivePillsIndices());
            }
        } else {
            AIDebugWindow.getInstance().log("Chasing edible ghost at: " + target);
        }

        // If no target available (e.g. no pills or ghosts), return NEUTRAL
        if (target == null) {
            AIDebugWindow.getInstance().log("No target found. Returning NEUTRAL.");
            return MOVE.NEUTRAL;
        }

        // Calculate path to target using A*
        List<Integer> path = findPathAStar(game, current, target);

        AIDebugWindow.getInstance().log("Start node: " + current);
        AIDebugWindow.getInstance().log("Target node: " + target);

        // Check if valid path was found
        if (path == null || path.size() < 2) {
            AIDebugWindow.getInstance().log("No valid path found. Returning NEUTRAL.");
            return MOVE.NEUTRAL;
        }

        // Log and return move
        AIDebugWindow.getInstance().log("Path length: " + path.size());
        AIDebugWindow.getInstance().log("Next step: " + path.get(1));

        StringBuilder sb = new StringBuilder("Full path: ");
        for (int node : path) {
            sb.append(node).append(" -> ");
        }
        AIDebugWindow.getInstance().log(sb.toString());

        // Return move towards second node in the path
        return game.getNextMoveTowardsTarget(current, path.get(1), DM.PATH);
    }

    /*
     * Finds the closest pill to Pacman using the shortest path distance.
     */
    private int getClosestPill(Game game, int from, int[] pills) {
        int minDist = Integer.MAX_VALUE;
        int closest = -1;

        // Iterate through all pills and find the one with the shortest path distance
        // formula =
        for (int pill : pills) {
            int dist = game.getShortestPathDistance(from, pill);
            if (dist < minDist) {
                minDist = dist;
                closest = pill;
            }
        }

        return closest;
    }

    /*
     * Finds the path from Pacman's current position to the target pill using A*
     * pathfinding.
     */
    private List<Integer> findPathAStar(Game game, int start, int goal) {
        // Priority queue for nodes to explore, ordered by total estimated cost f(n) = g
        // + h
        PriorityQueue<NodeRecord> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));

        // Map to store the cost from start to each node (g-score)
        Map<Integer, Integer> gScore = new HashMap<>();

        // Map to reconstruct the final path (cameFrom[node] = previousNode)
        Map<Integer, Integer> cameFrom = new HashMap<>();

        // Initialize start node
        gScore.put(start, 0);
        openSet.add(new NodeRecord(start, 0, getHeuristic(game, start, goal)));

        // Main A* loop
        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll(); // Node with lowest f-score

            // If goal is reached, reconstruct and return path
            if (current.id == goal) {
                return reconstructPath(cameFrom, current.id);
            }

            // Explore neighboring nodes
            for (int neighbor : game.getNeighbouringNodes(current.id)) {
                int tentativeG = gScore.get(current.id) + 1; // assume uniform cost of 1 per move

                // If this path to neighbor is better than any previous one
                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current.id);
                    gScore.put(neighbor, tentativeG);

                    int f = tentativeG + getHeuristic(game, neighbor, goal);

                    System.out.println("Adding neighbor: " + neighbor + " with g: " + tentativeG + " and f: " + f);
                    openSet.add(new NodeRecord(neighbor, tentativeG, f));
                }
            }
        }

        // If goal was never reached, return null
        return null;
    }

    // Represents a node in the A* search with cost-so-far (g) and total cost
    // estimate (f = g + h)
    private static class NodeRecord {
        int id; // node index
        int g; // cost from start to this node
        int f; // total estimated cost to goal

        NodeRecord(int id, int g, int f) {
            this.id = id;
            this.g = g;
            this.f = f;
        }
    }

    // Returns the heuristic cost from node 'from' to node 'to' using built-in path
    // distance
    private int getHeuristic(Game game, int from, int to) {
        return game.getShortestPathDistance(from, to);
    }

    /*
     * Finds the safest pill to eat based on the distance to all ghosts.
     * The safest pill is the one that maximizes the total distance to all ghosts.
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

    /*
     * Checks if there are any ghosts that are too close to Pacman, which would make
     * the pathfinding dangerous.
     */
    private boolean isGhostThreatening(Game game, int ghostDistanceThreshold) {
        int pacman = game.getPacmanCurrentNodeIndex();

        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostLairTime(ghost) > 0)
                continue; // ignore ghosts in lair

            if (!game.isGhostEdible(ghost)) {
                int ghostPos = game.getGhostCurrentNodeIndex(ghost);
                int dist = game.getShortestPathDistance(pacman, ghostPos);
                if (dist <= ghostDistanceThreshold) {
                    return true; // ghost nearby
                }
            }
        }

        return false;
    }

    /*
     * Finds the closest edible ghost within a certain distance from Pacman.
     * Returns the index of the ghost node if found, or null if no edible ghost is
     * close enough.
     */
    private Integer getEdibleGhostTarget(Game game, int maxDistance) {
        int pacman = game.getPacmanCurrentNodeIndex();
        int closest = -1;
        int bestDist = Integer.MAX_VALUE;

        // Iterate through all ghosts and find the closest edible one
        // that is not in the lair
        for (GHOST ghost : GHOST.values()) {
            if (game.isGhostEdible(ghost) && game.getGhostLairTime(ghost) == 0) {
                int ghostNode = game.getGhostCurrentNodeIndex(ghost);
                int dist = game.getShortestPathDistance(pacman, ghostNode);
                // Only consider ghosts within the specified max distance
                if (dist < bestDist && dist <= maxDistance) {
                    bestDist = dist;
                    closest = ghostNode;
                }
            }
        }

        // Return the closest edible ghost node index, or null if none found
        return (closest != -1) ? closest : null;
    }

    // Reconstructs the path from goal back to start using the cameFrom map
    private List<Integer> reconstructPath(Map<Integer, Integer> cameFrom, int current) {
        List<Integer> path = new ArrayList<>();
        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current); // prepend to build path from start to goal
        }

        return path;
    }
}
