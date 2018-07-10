import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class GameButton extends JButton {

	/**
	 * For use, pass a number to setIcon(int). Use the public constants below
	 * for access to the non sequential icons, and otherwise, pass in the number
	 * of adjacent mines (0-8) and the button will display the proper numeric icon
	 */
	public static final int BLANK = 0, BANG = 9, BOMB = 10, FLAG = 11, F_FLAG = 12;
	private final Color disabledColor = Color.DARK_GRAY, enabledColor = Color.LIGHT_GRAY;
	private static String aDir = "assets/buttonIcons/";
	private ImageIcon displayIcon;
	private static ImageIcon[] buttonIcons = 
			//Numeric icons 0 is null, 1-8 are accurately named
		{null, new ImageIcon(aDir+"1.png"), new ImageIcon(aDir+"2.png"), 
			new ImageIcon(aDir+"3.png"), new ImageIcon(aDir+"4.png"), 
			new ImageIcon(aDir+"5.png"), new ImageIcon(aDir+"6.png"), 
			new ImageIcon(aDir+"7.png"), new ImageIcon(aDir+"8.png"),
			//Bomb-based icons and flags
			new ImageIcon(aDir+"explosion.png"), new ImageIcon(aDir+"bomb.png"), 
			new ImageIcon(aDir+"p_bomb.png"), new ImageIcon(aDir+"wrong.png")}; /*For flag, question, mine*/

	//Settings for button's type and location
	public ButtonType type;
	public int x, y;
	public boolean flagged, revealed;

	/**Extended JButton with coordinates.
	 * 
	 * @param x X coordinate in a grid
	 * @param y Y coordinate in a grid
	 */
	public GameButton(int x, int y){
		super();
		this.x = x;
		this.y = y;
		this.type = ButtonType.SAFE;
		this.setBackground(Color.LIGHT_GRAY);
		flagged = false;
		revealed = false;
	}

	/**
	 * Set the icon of the current button based on the passed index.
	 * Self-explanatory constants available for use include:
	 * 		GameButton.BLANK, GameButton.BANG, 
	 * 		GameButton.BOMB, GameButton.FLAG,
	 * 		and GameButton.F_FLAG
	 * Other than the above, pass the number of nearby mines
	 * into this method to set the icon of the button to an
	 * appropriate label
	 */
	public void setIcon(int index){
		if(index != BLANK){
			displayIcon = new ImageIcon(buttonIcons[index].getImage()
					.getScaledInstance(getWidth(), getHeight(), java.awt.Image.SCALE_SMOOTH));
		} else{
			displayIcon = buttonIcons[BLANK];
		}
		setIcon(displayIcon);
		setDisabledIcon(displayIcon);
		//Qmark and blank can be clicked. Otherwise, disabled color
		if(!flagged){
			deactivate();
		}
		revalidate();
	}

	/** @return True if this is a mine */
	public boolean isMine() {
		return this.type == ButtonType.MINE;
	}

	/** Sets this button as a known mine */
	public void plantMine(){
		this.type = ButtonType.MINE;
	}

	/** Disabler function */
	public void deactivate(){
		this.setEnabled(false);
		this.setBackground(disabledColor);
		revealed = true;
	}

	/** Enabler function */
	public void activate(){
		this.setEnabled(true);
		this.setBackground(enabledColor);
		revealed = flagged = false;
	}
}
