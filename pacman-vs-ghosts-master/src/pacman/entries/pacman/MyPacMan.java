package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.Constants.GHOST;

//AI diagnostic window import.
import pacman.AI.AIDebugWindow;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */

import java.util.EnumMap;
import java.util.Random;

public class MyPacMan extends Controller<MOVE> {

	private static final int LOOKAHEAD_DEPTH = 3;
	private final Random rnd = new Random();

	@Override
	public MOVE getMove(Game game, long timeDue) {
		AIDebugWindow.getInstance();
		return getBestMoveLookahead(game, LOOKAHEAD_DEPTH);
	}

	/**
	 * Strategy 1: Depth-limited lookahead (greedy score-based)
	 */
	private MOVE getBestMoveLookahead(Game game, int depth) {
		AIDebugWindow.getInstance().log("\n=== [" + System.currentTimeMillis() % 100000 + "] New decision round ===");
		MOVE[] possibleMoves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
		MOVE bestMove = MOVE.NEUTRAL;
		int bestScore = Integer.MIN_VALUE;

		for (MOVE move : possibleMoves) {
			Game copy = game.copy();
			copy.advanceGame(move, getGhostMoves(copy));
			int score = simulate(copy, depth - 1);
			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
			}
			AIDebugWindow.getInstance().log("Testing move: " + move + " → SimScore: " + score);
		}

		AIDebugWindow.getInstance().log("Chosen move: " + bestMove + " with score: " + bestScore);

		return bestMove;
	}

	/**
	 * Simulate depth-limited random path and return score
	 */
	private int simulate(Game game, int depth) {
		if (depth == 0 || game.gameOver()) {
			return game.getScore();
		}

		MOVE[] moves = game.getPossibleMoves(game.getPacmanCurrentNodeIndex());
		if (moves.length == 0)
			return game.getScore();

		MOVE randomMove = moves[rnd.nextInt(moves.length)];
		game.advanceGame(randomMove, getGhostMoves(game));
		return simulate(game, depth - 1);
	}

	/**
	 * Neutral ghost logic (replaces learning opponent)
	 */
	private EnumMap<GHOST, MOVE> getGhostMoves(Game game) {
		EnumMap<GHOST, MOVE> moves = new EnumMap<>(GHOST.class);
		for (GHOST ghost : GHOST.values()) {
			moves.put(ghost, game.getGhostLastMoveMade(ghost)); // passief gedrag
		}
		return moves;
	}
}