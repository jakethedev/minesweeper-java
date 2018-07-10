import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class MinesweeperPanel extends JPanel implements MouseListener {

	//Logical operation data
	//Most self-explanatory, remainingLocations represents number of
	//unclicked locations on board to allow accurate checking
	//of winning condition
	private int width, height, numMines, flags, time, remainingLocations;
	private enum Difficulty {EASY, INT, HARD, CUST};
	private Difficulty diffLevel;
	private Timer gameClock;
	//High score file location, file writer, list of player names,
	//	and list of scores corresponding with players
	private String scoreLocation = ".hiScores";
	private FileWriter scoreWriter;
	private String[] hsNames;
	private int[] hsScores;
	private final int NUM_SCORES = 20; 

	//Menu pieces
	private JMenuBar menuBar;
	private JMenuItem easyGame, midGame, hardGame, customize, restart,
	highScores, giveUp, quitGame;
	//Organization, interactive pieces, and UI configuration
	private JPanel board;
	public JLabel infoLabel; 
	private GameButton[][] grid;
	private GameButton[] mines;

	/** Basic constructor to init the layout, build a menu and timer, and start a game */
	public MinesweeperPanel() {
		setLayout(new BorderLayout());
		buildMenuBarAndInfo();
		readHighScores();
		gameClock = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				time++;
				updateStatus();
			}
		});
		newGame(Difficulty.EASY);
	}

	/*============================================*/
	/*============ GAME LOGIC METHODS ============*/
	/*============================================*/

	/**New game method that sets up the game based on a difficulty.
	 * For clean resetting, call this and pass in Difficulty.RESET
	 * 
	 * @param d Any value of the above Enum Difficulty
	 */
	private void newGame(Difficulty d){
		this.diffLevel = d;
		switch (this.diffLevel) {
		///For standard type difficulty, standard rebuild
		case EASY:
			width = height = 9;
			numMines = 10;
			//TODO Get resizing calibrated
			//resize(width, MENU_HEIGHT);
			break;
		case INT:
			width = height = 16;
			numMines = 40;
			//resize(width, MENU_HEIGHT);
			break;
		case HARD:
			width = 30;
			height = 16;
			numMines = 1;
			//resize(width, MENU_HEIGHT);
			break;
		case CUST:
			//Set's width/height/numMines to user's pref
			setupBoardByUser(); 
			break;
		}
		if(board != null)
			remove(board);
		//Rebuild and replace grid panel any time this happens.
		add(buildGamePanel(), BorderLayout.CENTER);
		setRandomizedMines();
		remainingLocations = width * height;
		time = flags = 0;
		updateStatus();
		gameClock.start();
	}

	/**
	 * Clean reset of the current board, just make it
	 * replayable. No new content.
	 */
	private void resetCurrentGame(){
		for(int i = 0; i < width; i++){
			for(GameButton gb:grid[i]){
				gb.setIcon(GameButton.BLANK);
				gb.activate();
			}
		}
		flags = time = 0;
		remainingLocations = width * height;
		updateStatus();
		gameClock.start();
	}

	/**
	 * @param gb GameButton clicked for current move
	 */
	private void makeMove(GameButton gb){
		reveal(gb);
		if(gb.isMine()){
			loseGame(gb);
		} 
		else if (remainingLocations == mines.length){
			winGame();
		}
	}

	/**
	 * Handles game winning - this is reached if all non-bomb
	 * items are clicked, so we reveal the mines and ensure that
	 * flagged mines are just deactivated, which allows them to
	 * keep the flag icon.
	 */
	private void winGame() {
		gameClock.stop();
		for(GameButton mine:mines){
			if(!mine.flagged) 
				reveal(mine);
			mine.deactivate();
		}
		enterHighScore();
		displayHighScores();
	}

	/**
	 * Caused by clicking a mine or giving up.
	 */
	private void loseGame(GameButton explosion){
		gameClock.stop();
		for(int i = 0; i < width; i++){
			for(GameButton gb:grid[i]){
				if(gb == explosion){
					//Cool guys don't look at exploBOOOOOM
					gb.setIcon(GameButton.BANG);
				}else if(gb.flagged && !gb.isMine()){
					gb.setIcon(GameButton.F_FLAG);
				}else if(!gb.flagged){
					reveal(gb);
				}
				gb.deactivate();
			}
		}
		JOptionPane.showMessageDialog(this, "Booo you died :(");
	}

	/**
	 * Confirm shutdown. Saves currently loaded scores to disk, pauses game
	 * clock when activated, and if cancelled, resumes the clock
	 * 
	 * Currently returns an int as a result of making this a valid
	 * parameter for the method JFrame.setDefaultCloseOperation,
	 * which didn't work
	 */
	public void quitGame(){
		gameClock.stop();
		writeHighScores();
		System.exit(0);
	}

	/**Updater for the info label */
	private void updateStatus() {
		infoLabel.setText("Mines Left: "+Math.max(numMines-flags, 0)+" Time: "+time);
	}

	/**This reveals a game button upon clicking.
	 * If it's flagged, then we assume the click was in error, and we don't
	 * proceed.
	 * Then, we set an index up to pass to the image method of the button.
	 * Qe loop all neighbors and
	 * increment index from 0 to find neighboring mines. 
	 * If the resulting index is zero, recurse on all adjacent
	 * and diagonal items to clear out empty lot.
	 * 
	 * @param gb Button to reveal, does nothing if it's flagged or a mine
	 */
	private void reveal(GameButton gb){
		if(gb.flagged || gb.isMine() || gb.revealed){
			if(gb.isMine())
				gb.setIcon(GameButton.BOMB);
			return;
		}
		gb.deactivate();
		int numMinesNearby = 0;
		remainingLocations --;
		numMinesNearby = 0;
		for (int dX = -1; dX <= 1; dX++) {
			for (int dY = -1; dY <= 1; dY++) {
				int pX = gb.x + dX, pY = gb.y + dY;
				if(validLocation(pX, pY) && grid[pX][pY].type == ButtonType.MINE){
					numMinesNearby++;
				}
			}
		}
		if (numMinesNearby==0){ 
			/* Algorithm to clean up any blank squares
			 * Recursively grab all neighbors and call reveal on 
			 * them if they're blank or adjacent to a blank. */
			for (int dX = -1; dX <= 1; dX++) {
				for (int dY = -1; dY <= 1; dY++) {
					int pX = gb.x + dX, pY = gb.y + dY;
					if (validLocation(pX, pY) && 
							grid[pX][pY].isEnabled() &&
							!grid[pX][pY].flagged){
						reveal(grid[pX][pY]);
					}
				}
			}
		}
		gb.setIcon(numMinesNearby);
	}

	/**Handles flagging/deflagging a button. Occurs
	 * on right click of a valid, unrevealed location
	 * 
	 * @param move Valid, unrevealed button clicked 
	 * on board to flag.
	 */
	private void flag(GameButton move){
		if(move.flagged){
			move.setIcon(GameButton.BLANK);
			move.flagged = false;
			flags--;
		} else{
			move.flagged = true;
			move.setIcon(GameButton.FLAG);
			flags++;
		}
		updateStatus();
	}

	/** A game logic method thats only used in reveal. 
	 * 
	 * @param pX potential x location to check for validity
	 * @param pY potential y location to check for validity
	 * 
	 * @return True if location is valid in the grid
	 */
	private boolean validLocation(int pX, int pY) {
		//If any of the conditions in parens are true, location is invalid
		return !(pX < 0 || pX >= grid.length || pY < 0 || pY >= grid[0].length);
	}
	
	/*================================================================*/
	/*============HIGH SCORE HANDLING, SAVING, AND LOADING============*/
	/*================================================================*/

	/**
	 * Display high scores after every game. Entering a high
	 * score is handled by winGame()
	 */
	private void displayHighScores() {
		//Prevent this from counting as time used to play
		gameClock.stop();
		//Build a table to use as the message for display on a popup
		StringBuilder scoreTable = new StringBuilder();
		scoreTable.append(String.format("%-25s%s\n","Player:","Score:"));
		int bestScoreIndex = this.diffLevel.ordinal()*5;
		int worstScoreIndex = this.diffLevel.ordinal()*5 + 4;
		for(int i = bestScoreIndex; i <= worstScoreIndex; i++){
			scoreTable.append(String.format("%-36s\n", hsNames[i]));
			scoreTable.append(String.format("%36d\n", hsScores[i]));
		}
		informUser("High Scores for "+this.diffLevel, scoreTable.toString());
		//If the game isn't over, resume clock
		if(remainingLocations > numMines)
			gameClock.start();
	}

	/**
	 * Allows entering of a name for high score table.
	 */
	private void enterHighScore() {
		if(this.time >= hsScores[this.diffLevel.ordinal()*5+4]){
			return; //Not a high score. Get outta here.
		}
		//Try twice to get a name, redundancy is redundantly safe
		String playerName = promptUser("New High Score on "+this.diffLevel+"!",
				"Well done! Would you kindly give us your name?");
		if(playerName == null || playerName.trim().length() < 1){
			playerName = promptUser("New High Score on "+this.diffLevel+"!",
					"Seriously though. Not even a nickname?");
			if(playerName == null || playerName.trim().length() < 1){
				informUser("Score Not Recorded", 
						"Well, you're lame. Check out everyone that did better than you!");
				return;
			}
		}
		int bestScoreForDifficultyIndex = this.diffLevel.ordinal()*5;
		int worstScoreForDifficultyIndex = this.diffLevel.ordinal()*5 + 4;
		//Find insertion point from the top down
		int insertionIdx = bestScoreForDifficultyIndex;
		while(this.time >= hsScores[insertionIdx])
			insertionIdx ++;
		//Make room for new score, overwrite lowest score
		for(int swap = worstScoreForDifficultyIndex - 1; 
				swap >= insertionIdx; swap--){
			hsScores[swap + 1] = hsScores[swap];
			hsNames[swap + 1] = hsNames[swap];
		}
		hsScores[insertionIdx] = time;
		hsNames[insertionIdx] = playerName;
	}

	/*===========================================================*/
	/*============ SCORE SAVE AND LOAD FUNCTIONALITY ============*/
	/*===========================================================*/
	
	/**
	 * Reads high scores from disk. Called on construction to read
	 * in data, at which point all scores are held in memory
	 * until valid closure of the game, which should always
	 * call writeHighScores
	 * 
	 * File structure, 20 lines of:
	 * [name,score\n]
	 * 
	 * Lines 1-5 are easy mode high scores, 6-10 are intermediate,
	 * 11-15 are for hard mode, and 16-20 for custom mode.
	 */
	private void readHighScores(){
		System.out.println("Reading from disk...");
		File scoreFile = new File(scoreLocation);
		try{
			if(!scoreFile.exists() || !(scoreFile.length() > 10)){
				//Below method prints a default set of data to
				// './.hiScores'
				initializeScoreFile(scoreFile);
				scoreFile = new File(scoreLocation);
			}
			//Read the scores file into local mem
			Scanner in = new Scanner(scoreFile);
			hsNames = new String[NUM_SCORES];
			hsScores = new int[NUM_SCORES];
			for (int i = 0; i < NUM_SCORES; i++) {
				String[] raw = in.nextLine().split(",");
				hsNames[i] = raw[0];
				hsScores[i] = Integer.parseInt(raw[1]);
				//				System.out.printf("Line #%d: %s [%s]\n", i+1, hsNames[i], hsScores[i]);
			}
			in.close();
			scoreWriter = new FileWriter(scoreFile);
		}catch(Exception e){
			System.err.println("Problem reading high scores. Blame Jake.");
			System.exit(0);
		}
	}

	/**Function to initialize a first-run high scores file. This should
	 * only be called on first run per build, or if the previous file
	 * was corrupted or removed somehow.
	 * 
	 * Tries to build (or rebuild) score file, fill it with default data, 
	 * then finalize it and close the writer object. Should save it properly 
	 * to disk.
	 * 
	 * @param scoreFile Problematic file location to set up as high score location
	 * @param fileWriter Writer attached to the file
	 * @return Valid file for scores
	 */
	private void initializeScoreFile(File scores) throws IOException{
		//Ensure we have a file to work with before building writer
		if(scores.exists())
			scores.delete();
		scores.createNewFile();
		FileWriter overWriter = new FileWriter(scores);
		//Default data to plug into file
		for (int i = 0; i < NUM_SCORES; i++) {
			overWriter.write(String.format("N/A,%d\n",(9985+i)));
		}
		System.out.println("Error at './.hiScores', scores file rewritten with 20 default values");
		overWriter.close();
	}

	/**
	 * Write current high score table to disk. It's held in memory
	 * until this step. This is called by quitGame on confirmation
	 * of closure, and quitGame should be called on any normal
	 * close option to ensure these get saved
	 */
	private void writeHighScores(){
		System.out.println("Saving to disk...");
		try{
			scoreWriter.flush();
			//Write scores and names to disk
			for (int i = 0; i < NUM_SCORES; i++) {
				String tableEntry = hsNames[i]+","+hsScores[i]+"\n";
				scoreWriter.write(tableEntry);
				//				System.out.printf("Line #%d: %s [%s]\n", i+1, hsNames[i], hsScores[i]);
			}
			scoreWriter.close();
		}catch(Exception e){
			System.err.println("Problem writing high scores... Blame Jake.");
		}
	}

	//================USER INTERACTION UTILITIES==============//

	/**Yes or no confirmation dialog box
	 * 
	 * @param title Title of the confirmation popup
	 * @param message Message of the popup
	 * @return True iff confirmed
	 */
//	private boolean userConfirms(String title, String message){
//		return JOptionPane.showConfirmDialog(this, message, title, 
//				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) 
//				== JOptionPane.YES_OPTION;
//	}

	/**Informational popup to user
	 * 
	 * @param title Title of the info popup
	 * @param message Message of the popup
	 */
	private void informUser(String title, String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.PLAIN_MESSAGE);
	}

	/**Prompt user for input as a string, used one string at a time
	 * 
	 * @param title Title of the info popup
	 * @param message Message of the popup
	 */
	private String promptUser(String title, String message){
		return JOptionPane.showInputDialog(this, message, title, JOptionPane.QUESTION_MESSAGE);
	}
	

	//==========================================================
	//============INTERFACE CONSTRUCTION METHODS================
	//==========================================================

	/**
	 * @return A panel with reset/timer/score and game buttons
	 */
	private JPanel buildGamePanel(){
		if(board != null){
			remove(board);
			revalidate();
		}
		board = new JPanel();
		board.setLayout(new GridLayout(width, height));
		grid = new GameButton[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				(grid[x][y] = new GameButton(x, y)).addMouseListener(this);
				board.add(grid[x][y]);
				grid[x][y].activate();
			}
		}
		return board;
	}

	/**
	 * Handle a custom game round by the user. Pop up a small form
	 * to ask for values in a certain range, and input those on
	 * confirmation. 
	 */
	private void setupBoardByUser(){
		
		//TODO Custom: Any values from 8 × 8 or 9 × 9 to 30 × 24 field, with 10 to 668 mines.
		/*
		 * A type of input panel class may be really handy here?
		 * Use sliders for all values, then button to fire setup event?
		 * Run input listener on text to give hints on input?
		width = parse(input(Ask for width));
		height = parse(input(Ask for height));
		numMines = parse(input(Ask for mine num));
		 */
		informUser("Customize", "This is not what you meant to click on.");
		newGame(Difficulty.EASY);
	}

	/**
	 * Builds the mine array, then sets the mines in the game board
	 */
	private void setRandomizedMines() {
		mines = new GameButton[numMines];
		for(int i = 0; i < numMines; ){
			int randX = (int)(Math.random() * width);
			int randY = (int)(Math.random() * height);
			//Control incrementing to avoid double-mining a spot
			if(grid[randX][randY].type != ButtonType.MINE){
				mines[i] = grid[randX][randY];
				mines[i].type = ButtonType.MINE;
				i++;
			}
		}
	}

	//============Game Menu Setup================

	/**
	 * Sets up the menu bar at the top of the window
	 */
	private void buildMenuBarAndInfo(){
		//Build up the full menu
		menuBar = new JMenuBar();
		menuBar.add(buildFileMenu());
		//Set up status
		infoLabel = new JLabel();
		flags = numMines = time = 0;
		add(infoLabel, BorderLayout.SOUTH);
		updateStatus();
	}

	/**
	 * @return Game options menu with Save/Load and other features
	 */
	private JMenu buildFileMenu() {
		JMenu gameMenu = new JMenu("   Menu   ");

		//Build new game sub-menu
		JMenu newGame = new JMenu("New Game");
		(easyGame = new JMenuItem("Easy")).addMouseListener(this);
		(midGame = new JMenuItem("Intermediate")).addMouseListener(this);
		(hardGame = new JMenuItem("Hard")).addMouseListener(this);
		(customize = new JMenuItem("Custom...")).addMouseListener(this);
		newGame.add(easyGame);
		newGame.add(midGame);
		newGame.add(hardGame);
		newGame.add(customize);

		//Build options sub-menu
		JMenu opt = new JMenu("Options");
		(highScores = new JMenuItem("High Scores")).addMouseListener(this);
		(restart = new JMenuItem("Restart Game")).addMouseListener(this);
		(giveUp = new JMenuItem("Give Up")).addMouseListener(this);
		opt.add(highScores);
		opt.addSeparator();
		opt.add(restart);
		opt.add(giveUp);

		//Finish up the file menu
		(quitGame = new JMenuItem("Quit")).addMouseListener(this);
		gameMenu.add(newGame);
		gameMenu.add(opt);
		gameMenu.addSeparator();
		gameMenu.add(quitGame);
		return gameMenu;
	}

	/**Allows access to the built menu bar for the top-level frame
	 * to set it's menu to the menu constructed here
	 * 
	 * @return The menu bar for this program. 
	 */
	public JMenuBar menuBar(){
		return this.menuBar;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		//All actions are mouse driven, no hotkeys implemented yet
		Object selection = e.getComponent();
		//New game actions
		if(selection == easyGame){
			newGame(Difficulty.EASY);
		}else if(selection == midGame){
			newGame(Difficulty.INT);
		}else if(selection == hardGame){
			newGame(Difficulty.HARD); 
		}else if(selection == customize){
			newGame(Difficulty.CUST); 
		}else if(selection == restart){
			resetCurrentGame();
		}
		//Giving up, only works if game clock is moving
		else if(selection == giveUp){
			if(gameClock.isRunning())
				loseGame(null); 
		} 
		//High score and quit actions
		else if(selection == highScores){
			displayHighScores(); 
		} else if(selection == quitGame){
			quitGame();
		} else {
			//This state is inductively reached only on
			//an arbitrary random game button
			GameButton move = (GameButton)selection;
			if(move.isEnabled()){
				if(SwingUtilities.isLeftMouseButton(e) &&
						!move.flagged)
					makeMove(move);
				if(SwingUtilities.isRightMouseButton(e) ){
					flag(move);
				}
			}
		}
	}

	/*============ UNIMPLEMENTED METHODS FROM MouseListener============*/
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
}