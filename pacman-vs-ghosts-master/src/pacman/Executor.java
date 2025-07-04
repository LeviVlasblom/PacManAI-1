package pacman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Random;
import pacman.controllers.Controller;
import pacman.controllers.HumanController;
import pacman.controllers.KeyBoardInput;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.Legacy2TheReckoning;
import pacman.controllers.examples.NearestPillPacMan;
import pacman.controllers.examples.NearestPillPacManVS;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomNonRevPacMan;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;

// Self operating AI Agents import
import pacman.entries.pacman.MyPacMan;
import pacman.entries.ghosts.MyGhosts;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;

// AI Pacman Imort
import pacman.AI.AIDebugWindow;
import pacman.AI.AStarPacMan;
import pacman.AI.RLPacMan;
import pacman.AI.RLPacManV2;
import pacman.AI.BFSPacMan;
import pacman.AI.DFSPacMan;
import pacman.AI.GreedyPacMan;
import pacman.AI.UCSPacMan;

import static pacman.game.Constants.*;

/**
 * This class may be used to execute the game in timed or un-timed modes, with
 * or without
 * visuals. Competitors should implement their controllers in
 * game.entries.ghosts and
 * game.entries.pacman respectively. The skeleton classes are already provided.
 * The package
 * structure should not be changed (although you may create sub-packages in
 * these packages).
 */
@SuppressWarnings("unused")
public class Executor {

	private int iteration = 0;

	/**
	 * The main method. Several options are listed - simply remove comments to use
	 * the option you want.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Executor exec = new Executor();

		int numTrials = 100; // or any number you want

		// exec.runExperiment(new pacman.AI.DFSPacMan(), new MyGhosts(), numTrials);
		// exec.runExperiment(new pacman.AI.BFSPacMan(), new MyGhosts(), numTrials);
		// exec.runExperiment(new pacman.AI.UCSPacMan(), new MyGhosts(), numTrials);
		// exec.runExperiment(new pacman.AI.GreedyPacMan(), new MyGhosts(), numTrials);
		// exec.runExperiment(new pacman.AI.AStarPacMan(), new MyGhosts(), numTrials);

		/*
		 * //run a game in synchronous mode: game waits until controllers respond.
		 * int delay=5;
		 * boolean visual=true;
		 * exec.runGame(new RandomPacMan(),new RandomGhosts(),visual,delay);
		 */

		/// *
		// run the game in asynchronous mode.
		boolean visual = true;

		// Pacman AI Runtime
		// exec.runGameTimed(new AStarPacMan(), new MyGhosts(), visual);

		// exec.runGameTimedRecorded(new DFSPacMan(), new MyGhosts(), visual,
		// "replay.txt");
		// exec.runGameTimedRecorded(new BFSPacMan(), new MyGhosts(), visual,
		// "replay.txt");
		// exec.runGameTimedRecorded(new UCSPacMan(), new MyGhosts(), visual,
		// "replay.txt");
		// exec.runGameTimedRecorded(new GreedyPacMan(), new MyGhosts(), visual,
		// "replay.txt");
		// exec.runGameTimedRecorded(new AStarPacMan(), new StarterGhosts(), visual,
		// "replay.txt");

		// --------------------------------------------------------
		// PacMan AI RL Runtime
		RLPacManV2 rlPacman = new RLPacManV2(); // Deel deze instantie!
		// exec.runExperiment(new AStarPacMan(), new MyGhosts(), 100); // increased
		exec.runExperimentRL(rlPacman, new MyGhosts(), 1000); // increased
		// exec.runGameTimed(rlPacman, new MyGhosts(), visual);

		rlPacman.saveQTable("qtable.csv");
		// training games

		// for (int i = 0; i < 500; i++) {
		// exec.runGameTimed(rlPacman, new MyGhosts(), visual);
		// rlPacman.saveQTable("qtable.csv"); // Save Q-table after each game
		// }
		// System.out.println("Finale Q-table opgeslagen.");
		// ---------------------------------------------------------
		// RunTime against other Ghosts
		// exec.runGameTimed(new MyPacMan(), new RandomGhosts(), visual); // tegen
		// Randome Gghosts
		// exec.runGameTimed(new MyPacMan(), new StarterGhosts(), visual); // tegen
		// basic-AI
		// exec.runGameTimed(new MyPacMan(), new AggressiveGhosts(), visual); // tegen
		// Agressive Ghosts

		// exec.runGameTimed(new NearestPillPacMan(), new AggressiveGhosts(), visual);
		// exec.runGameTimed(new StarterPacMan(), new StarterGhosts(), visual);
		// exec.runGameTimed(new HumanController(new KeyBoardInput()), new
		// StarterGhosts(), visual);
		// */

		/*
		 * //run the game in asynchronous mode but advance as soon as both controllers
		 * are ready - this is the mode of the competition.
		 * //time limit of DELAY ms still applies.
		 * boolean visual=true;
		 * boolean fixedTime=false;
		 * exec.runGameTimedSpeedOptimised(new RandomPacMan(),new
		 * RandomGhosts(),fixedTime,visual);
		 */

		/*
		 * //run game in asynchronous mode and record it to file for replay at a later
		 * stage.
		 * boolean visual=true;
		 * String fileName="replay.txt";
		 * exec.runGameTimedRecorded(new HumanController(new KeyBoardInput()),new
		 * RandomGhosts(),visual,fileName);
		 * //exec.replayGame(fileName,visual);
		 */
	}

	/**
	 * For running multiple games without visuals. This is useful to get a good idea
	 * of how well a controller plays
	 * against a chosen opponent: the random nature of the game means that
	 * performance can vary from game to game.
	 * Running many games and looking at the average score (and standard
	 * deviation/error) helps to get a better
	 * idea of how well the controller is likely to do in the competition.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController  The Ghosts controller
	 * @param trials           The number of trials to be executed
	 */
	public void runExperiment(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			int trials) {
		double avgScore = 0;
		Game game = new Game(0);
		int random = new Random().nextInt();

		for (int i = 0; i < trials; i++) {
			game = new Game(random);
			while (!game.gameOver()) {
				game.advanceGame(
						pacManController.getMove(game.copy(), System.currentTimeMillis() + DELAY),
						ghostController.getMove(game.copy(), System.currentTimeMillis() + DELAY));
			}

			avgScore += game.getScore();
			System.out.println("Trial " + (i + 1) + ": Score = " + game.getScore());

			onLevelCompleted(game, pacManController, ghostController); // Keep this

			System.out.println("Average score over " + trials + " trials: " + (avgScore / trials));

			// Correct: save Q-table once at the end
			if (pacManController instanceof RLPacManV2 rl) {
				rl.saveQTable("qtable.csv");
				System.out.println("Q-table saved to qtable.csv after all training.");
			}
		}

		onLevelCompleted(game, pacManController, ghostController);
	}

	public void runExperimentRL(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			int trials) {
		double avgScore = 0;
		Game game = new Game(0);
		int random = new Random().nextInt();

		for (int i = 0; i < trials; i++) {
			game = new Game(random);
			while (!game.gameOver()) {
				game.advanceGame(
						pacManController.getMove(game.copy(), System.currentTimeMillis() + DELAY),
						ghostController.getMove(game.copy(), System.currentTimeMillis() + DELAY));
			}

			avgScore += game.getScore();
			System.out.println("Trial " + (i + 1) + ": Score = " + game.getScore());

			onLevelCompletedRL(game, pacManController, ghostController); // Keep this

			System.out.println("Average score over " + trials + " trials: " + (avgScore / trials));

			// Correct: save Q-table once at the end
			if (pacManController instanceof RLPacManV2 rl) {
				rl.saveQTable("qtable.csv");
				System.out.println("Q-table saved to qtable.csv after all training.");
			}
		}

		onLevelCompletedRL(game, pacManController, ghostController);
	}

	/**
	 * Run a game in asynchronous mode: the game waits until a move is returned. In
	 * order to slow thing down in case
	 * the controllers return very quickly, a time limit can be used. If fasted
	 * gameplay is required, this delay
	 * should be put as 0.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController  The Ghosts controller
	 * @param visual           Indicates whether or not to use visuals
	 * @param delay            The delay between time-steps
	 */
	public void runGame(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			boolean visual, int delay) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		while (!game.gameOver()) {
			game.advanceGame(pacManController.getMove(game.copy(), -1), ghostController.getMove(game.copy(), -1));

			try {
				Thread.sleep(delay);
			} catch (Exception e) {
			}

			if (visual)
				gv.repaint();
		}
	}

	/**
	 * Run the game with time limit (asynchronous mode). This is how it will be done
	 * in the competition.
	 * Can be played with and without visual display of game states.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController  The Ghosts controller
	 * @param visual           Indicates whether or not to use visuals
	 */
	public void runGameTimed(Controller<MOVE> pacManController, Controller<EnumMap<GHOST, MOVE>> ghostController,
			boolean visual) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		if (pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			game.advanceGame(pacManController.getMove(), ghostController.getMove());

			if (visual)
				gv.repaint();
		}

		// Call onLevelCompleted after the game ends
		onLevelCompleted(game, pacManController, ghostController);

		pacManController.terminate();
		ghostController.terminate();
	}

	/**
	 * Run the game in asynchronous mode but proceed as soon as both controllers
	 * replied. The time limit still applies so
	 * so the game will proceed after 40ms regardless of whether the controllers
	 * managed to calculate a turn.
	 * 
	 * @param pacManController The Pac-Man controller
	 * @param ghostController  The Ghosts controller
	 * @param fixedTime        Whether or not to wait until 40ms are up even if both
	 *                         controllers already responded
	 * @param visual           Indicates whether or not to use visuals
	 */
	public void runGameTimedSpeedOptimised(Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController, boolean fixedTime, boolean visual) {
		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		if (pacManController instanceof HumanController)
			gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				int waited = DELAY / INTERVAL_WAIT;

				for (int j = 0; j < DELAY / INTERVAL_WAIT; j++) {
					Thread.sleep(INTERVAL_WAIT);

					if (pacManController.hasComputed() && ghostController.hasComputed()) {
						waited = j;
						break;
					}
				}

				if (fixedTime)
					Thread.sleep(((DELAY / INTERVAL_WAIT) - waited) * INTERVAL_WAIT);

				game.advanceGame(pacManController.getMove(), ghostController.getMove());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (visual)
				gv.repaint();
		}

		pacManController.terminate();
		ghostController.terminate();
	}

	/**
	 * Run a game in asynchronous mode and recorded.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController  The Ghosts controller
	 * @param visual           Whether to run the game with visuals
	 * @param fileName         The file name of the file that saves the replay
	 */
	public void runGameTimedRecorded(Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController, boolean visual, String fileName) {
		StringBuilder replay = new StringBuilder();
		Game game = new Game(0);
		GameView gv = null;

		if (visual) {
			gv = new GameView(game).showGame();
			if (pacManController instanceof HumanController)
				gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());
		}

		new Thread(pacManController).start();
		new Thread(ghostController).start();

		while (!game.gameOver()) {
			pacManController.update(game.copy(), System.currentTimeMillis() + DELAY);
			ghostController.update(game.copy(), System.currentTimeMillis() + DELAY);

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			game.advanceGame(pacManController.getMove(), ghostController.getMove());

			if (visual)
				gv.repaint();

			replay.append(game.getGameState() + "\n");
		}

		pacManController.terminate();
		ghostController.terminate();

		saveToFile(replay.toString(), fileName, false);

		// Call onLevelCompleted after the game ends
		onLevelCompleted(game, pacManController, ghostController);
	}

	/**
	 * Replay a previously saved game.
	 *
	 * @param fileName The file name of the game to be played
	 * @param visual   Indicates whether or not to use visuals
	 */
	public void replayGame(String fileName, boolean visual) {
		ArrayList<String> timeSteps = loadReplay(fileName);

		Game game = new Game(0);

		GameView gv = null;

		if (visual)
			gv = new GameView(game).showGame();

		for (int j = 0; j < timeSteps.size(); j++) {
			game.setGameState(timeSteps.get(j));

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (visual)
				gv.repaint();
		}
	}

	public static void saveToFile(String data, String name, boolean append) {
		try {
			FileOutputStream outS = new FileOutputStream(name, append);
			PrintWriter pw = new PrintWriter(outS);

			pw.println(data);
			pw.flush();
			outS.close();

		} catch (IOException e) {
			System.out.println("Could not save data!");
		}
	}

	private static ArrayList<String> loadReplay(String fileName) {
		ArrayList<String> replay = new ArrayList<String>();

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			String input = br.readLine();

			while (input != null) {
				if (!input.equals(""))
					replay.add(input);

				input = br.readLine();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return replay;
	}

	private void writeResultsToCSV(String aiMethod, String ghostMethod, double totalTime, int totalScore, int level) {
		String csvFile = "results.csv";
		try (FileWriter writer = new FileWriter(csvFile, true)) {
			writer.append(aiMethod)
					.append(',')
					.append(ghostMethod)
					.append(',')
					.append(String.valueOf(totalTime))
					.append(',')
					.append(String.valueOf(totalScore))
					.append(',')
					.append(String.valueOf(level))
					.append('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeResultsToCSVRL(int iteration, String aiMethod, String ghostMethod, double totalTime,
			int totalScore, int level) {
		String fileName = "RL_Results.csv";
		File file = new File(fileName);
		boolean fileExists = file.exists();
		iteration++; // Increment iteration for each run

		try (FileWriter writer = new FileWriter(fileName, true)) {
			if (!fileExists) {
				// Write header once
				writer.append("AI Method,Ghost Method,Time (s),Score,Level\n");
			}

			// Write result row
			writer.append(String.format(Locale.US, "%s,%s,%s,%.2f,%d,%d\n",
					iteration, aiMethod, ghostMethod, totalTime, totalScore, level));
			writer.flush();
		} catch (IOException e) {
			System.out.println("Could not write results to CSV!");
			e.printStackTrace();
		}
	}

	private void onLevelCompleted(Game game, Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController) {
		double totalTime = game.getTotalTime() / 60.0;
		int totalScore = game.getScore();
		int level = game.getCurrentLevel() + 1;
		String aiMethod = pacManController.getClass().getSimpleName();
		String ghostMethod = ghostController.getClass().getSimpleName();

		// Save results to CSV
		writeResultsToCSV(aiMethod, ghostMethod, totalTime, totalScore, level);

		System.out.println("Game Over!");
		System.out.println("AI Method: " + aiMethod);
		System.out.println("Ghost Method: " + ghostMethod);
		System.out.println("Time: " + totalTime);
		System.out.println("Score: " + totalScore);
		System.out.println("Level: " + level);
	}

	private void onLevelCompletedRL(Game game, Controller<MOVE> pacManController,
			Controller<EnumMap<GHOST, MOVE>> ghostController) {

		double totalTime = game.getTotalTime() / 60.0;
		int totalScore = game.getScore();
		int level = game.getCurrentLevel() + 1;
		String aiMethod = pacManController.getClass().getSimpleName();
		String ghostMethod = ghostController.getClass().getSimpleName();

		iteration++; // increment on every call

		// Save results to CSV
		writeResultsToCSVRL(iteration, aiMethod, ghostMethod, totalTime, totalScore, level);

		System.out.println("Level completed:");
		System.out.println("AI Method: " + aiMethod);
		System.out.println("Ghost Method: " + ghostMethod);
		System.out.println("Time (in seconds): " + totalTime);
		System.out.println("Score: " + totalScore);
		System.out.println("Level: " + level);
	}
}