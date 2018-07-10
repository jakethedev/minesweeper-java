import java.awt.Dimension;

import javax.swing.JFrame;

public class Minesweeper {

	@SuppressWarnings("serial")
	public static void main(String[] args) {
		final MinesweeperPanel gameBoard =  new MinesweeperPanel();

		//Custom dispose method to call gameBoard.quitGame()
		JFrame frame = new JFrame(){
			public void dispose() {
				gameBoard.quitGame();
			}
		};
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		frame.setTitle(" - Boom Shaka-Laka - ");
		frame.setJMenuBar(gameBoard.menuBar());
		frame.getContentPane().add(gameBoard);

		frame.pack();
		frame.setVisible(true);
		frame.setMinimumSize(new Dimension(400,425));
	}
}
