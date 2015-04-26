/**
 * ICS4U Mrs. Kapustina
 * 
 * @author Kunal
 */

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class GUI implements ActionListener {

	JFrame frame;
	int W = 448 / 8;					// width of piece
	int H = W;							// height of piece

	JButton[] buttons = new JButton[64];// 'squares' of the board
	int prevLoc = -1, curLoc = -1;		// button presses
	
	Thread computationThread ;

	/* Images for pieces */
	ImageIcon K = scale(new ImageIcon(getClass().getResource("/res/White/wking.png")), W, H);
	ImageIcon Q = scale(new ImageIcon(getClass().getResource("/res/White/wqueen.png")), W, H);
	ImageIcon R = scale(new ImageIcon(getClass().getResource("/res/White/wrook.png")), W, H);
	ImageIcon B = scale(new ImageIcon(getClass().getResource("/res/White/wbishop.png")), W, H);
	ImageIcon N = scale(new ImageIcon(getClass().getResource("/res/White/wknight.png")), W, H);
	ImageIcon P = scale(new ImageIcon(getClass().getResource("/res/White/wpawn.png")), W, H);

	ImageIcon k = scale(new ImageIcon(getClass().getResource("/res/Black/bking.png")), W, H);
	ImageIcon q = scale(new ImageIcon(getClass().getResource("/res/Black/bqueen.png")), W, H);
	ImageIcon r = scale(new ImageIcon(getClass().getResource("/res/Black/brook.png")), W, H);
	ImageIcon b = scale(new ImageIcon(getClass().getResource("/res/Black/bbishop.png")), W, H);
	ImageIcon n = scale(new ImageIcon(getClass().getResource("/res/Black/bknight.png")), W, H);
	ImageIcon p = scale(new ImageIcon(getClass().getResource("/res/Black/bpawn.png")), W, H);

	final Map<Character, ImageIcon> CHAR_IMAGE = new HashMap<>();

	/**
	 * Constructor
	 */
	public GUI() {
		CHAR_IMAGE.put('k', k);
		CHAR_IMAGE.put('q', q);
		CHAR_IMAGE.put('r', r);
		CHAR_IMAGE.put('b', b);
		CHAR_IMAGE.put('n', n);
		CHAR_IMAGE.put('p', p);
		CHAR_IMAGE.put('K', K);
		CHAR_IMAGE.put('Q', Q);
		CHAR_IMAGE.put('R', R);
		CHAR_IMAGE.put('B', B);
		CHAR_IMAGE.put('N', N);
		CHAR_IMAGE.put('P', P);
		initialize();
		update();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		/* This enables native Java look-and-feel
		try {
		    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
		            e.printStackTrace();
		}
		*/
		
		frame = new JFrame("AI Chess");
		frame.setBackground(new Color(240, 240, 240));
		frame.setBounds(0, 0, 448, 20 + 448);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		JMenuItem rulesItem = new JMenuItem("Rules");
		rulesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					// Open rules in web browser
					Desktop.getDesktop().browse(new URI("https://www.fide.com/component/handbook/?id=171&view=article"));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		});
		fileMenu.add(rulesItem);

		JMenuItem resetItem = new JMenuItem("Reset");
		resetItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		fileMenu.add(resetItem);

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		
		JPanel boardButtons = new JPanel();
		boardButtons.setLayout(new GridLayout(8, 8));
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				int loc = i * 8 + j;
				buttons[loc] = new JButton();
				buttons[loc].setActionCommand(Integer.toString(loc));
				buttons[loc].addActionListener(this);
				setButtonColor(i, j);
				boardButtons.add(buttons[loc]);
			}
		}
		frame.getContentPane().add(boardButtons);
	}

	final Color CHESSBOARD_BLUE = new Color(0, 166, 172);

	/**
	 * Set button color - checkerboard
	 * 
	 * @param i
	 * @param j
	 */
	private void setButtonColor(int i, int j) {
		int loc = i * 8 + j;
		if (i % 2 == 0) {
			if (j % 2 == 0)
				buttons[loc].setBackground(Color.WHITE);
			else
				buttons[loc].setBackground(CHESSBOARD_BLUE);
		} else if (j % 2 == 0) {
			buttons[loc].setBackground(CHESSBOARD_BLUE);
		} else {
			buttons[loc].setBackground(Color.WHITE);
		}
		
		// Mac OS compatibility settings
		buttons[loc].setOpaque(true);
		buttons[loc].setBorderPainted(false);
	}

	/**
	 * Scale icon to provided dimensions
	 * 
	 * @param in
	 * @param W
	 * @param H
	 * @return
	 *         Resized icon
	 */
	public ImageIcon scale(ImageIcon in, int W, int H) {
		Image timg = in.getImage();
		Image nimg = timg.getScaledInstance(W, H, Image.SCALE_SMOOTH);
		ImageIcon impic = new ImageIcon(nimg);
		return impic;
	}

	Set<Move> userPossibilities;

	/**
	 * 1 Convert button presses to moves
	 * 2 Check if user's move is valid
	 * 3 Check for termination conditions
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		int from = Integer.parseInt(e.getActionCommand());
		// Invalid button press
		if (prevLoc == -1 && (Engine.board[from / 8][from % 8] == ' ' ||
				Character.isLowerCase(Engine.board[from / 8][from % 8]))) {
			prevLoc = curLoc = -1;
		} else if (prevLoc == -1 || Character.isUpperCase((Engine.board[from / 8][from % 8]))) {
			// first press
			prevLoc = from;
		} else {
			// second press
			curLoc = from;

			Move userMove;
			if (prevLoc < 16 && Engine.board[prevLoc / 8][prevLoc % 8] == 'P') {
				// Pawn promotion
				String[] options = { "Queen", "Knight" };
				int promotion = JOptionPane.showOptionDialog(null, "Select piece", "Pawn Promotion",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				char piece;
				if (promotion == 0)
					piece = 'Q';
				else
					piece = 'N';
				userMove = new Move(prevLoc % 8, curLoc % 8,
						Engine.PIECE_BYTE.get(Engine.board[curLoc / 8][curLoc % 8]),
						Engine.PIECE_BYTE.get(piece), 'P');
			} else {
				userMove = new Move(prevLoc, curLoc, Engine.board[curLoc / 8][curLoc % 8]);
			}

			if (userPossibilities == null)
				userPossibilities = Engine.getMoves();
			if (userPossibilities.contains(userMove)) {		// move is valid
//				try {
//					Engine.makeMove(userMove);
//				} catch (InterruptedException e1) {
//					e1.printStackTrace();
//				}
				Engine.makeMove(userMove);
				update();
				Engine.flipAndSwitchBoard();

				/* Check termination */
				boolean compGameOver = !Engine.canMove();
				if (compGameOver && Engine.underCheck())
					// Checkmate
					JOptionPane.showMessageDialog(frame, "You won!");
				else if (compGameOver)
					// Stalemate
					JOptionPane.showMessageDialog(frame, "Draw");
				if (compGameOver)
					System.exit(0);

				/* Computer move, dispatch on separate thread */
				computationThread = new Thread() {
					public void run() {
						final long startTime = System.currentTimeMillis();
						try {
							Engine.makeMove(Engine.negaMax(new Move(), Engine.maxDepth, -10000000, 10000000, 0));
							onFinishComputation(startTime, System.currentTimeMillis());
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				};
				computationThread.start();
			} else {
				prevLoc = curLoc = -1;
			}
		}
	}
	
	/**
	 * Callback for when the engine finishes computation of the computer's next move
	 * @param startTime
	 * @param endTime
	 */
	public void onFinishComputation(final long startTime, final long endTime) {
		Engine.flipAndSwitchBoard();
		update();
		double moveTime = (endTime - startTime) / 1000.0;
		Engine.botMoves++;
		Engine.totalTime += (endTime - startTime);
		System.out.printf("Nodes explored = %6d\t\t Time: %.3f s\t\t Avg: %.3f s%n", 
				Engine.nodesExplored, moveTime, Engine.totalTime / 1000.0 / Engine.botMoves);
		Engine.nodesExplored = 0;

		boolean humanGameOver = !Engine.canMove();
		if (humanGameOver && Engine.underCheck())
			// Checkmate
			JOptionPane.showMessageDialog(frame, "Computer won!");
		else if (humanGameOver)
			// Stalemate
			JOptionPane.showMessageDialog(frame, "Draw");
		if (humanGameOver)
			System.exit(0);
		userPossibilities = Engine.getMoves();
		prevLoc = curLoc = -1;
	}

	/**
	 * Reset and start new game
	 */
	public void reset() {
		if (computationThread.isAlive()) {
			try {
				computationThread.interrupt();
				computationThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Engine.reset();
		update();
	}

	/**
	 * Reset icon of all buttons
	 * according to board state
	 */
	public void update() {
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++) {
				buttons[i * 8 + j].setIcon(CHAR_IMAGE.get(Engine.board[i][j]));
				setButtonColor(i, j);
			}
	}

}
