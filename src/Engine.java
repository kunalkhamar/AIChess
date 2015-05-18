/**
 * This class contains the logic of the game and the move 
 * recommending agent.
 * Also shows the startup splash screens to set difficulty
 *   
 * ICS4U Mrs. Kapustina
 * @author Kunal
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Engine {

	static char[][] board = new char[8][8];		// Represents current state
	static int kingPosU, kingPosL;				// U/L - Upper/Lower case; king's position
	static long nodesExplored = 0;
	static double totalTime = 0;
	static int botMoves = 0;
	private static int maxDepth = 7, maxBreadth = 7;	// depth = # of plies, breadth = # moves explored per ply

	public static final Map<Character, Byte> PIECE_BYTE = new HashMap<>();
	/* static initializer */
	static {
		PIECE_BYTE.put('P', (byte) 1);
		PIECE_BYTE.put('N', (byte) 3);
		PIECE_BYTE.put('B', (byte) 4);
		PIECE_BYTE.put('R', (byte) 5);
		PIECE_BYTE.put('Q', (byte) 9);
		PIECE_BYTE.put('K', (byte) 10);
		PIECE_BYTE.put(' ', (byte) 0);
		PIECE_BYTE.put('p', (byte) -1);
		PIECE_BYTE.put('n', (byte) -3);
		PIECE_BYTE.put('b', (byte) -4);
		PIECE_BYTE.put('r', (byte) -5);
		PIECE_BYTE.put('q', (byte) -9);
		PIECE_BYTE.put('k', (byte) -10);
	}

	/**
	 * Constructor
	 */
	public Engine() {
		reset();

		// dispatch thread
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					ImageIcon chessThrones = window.scale(new ImageIcon(getClass().getResource("/res/chessthrones.jpg")), 500, -1);
					window.getFrame().setVisible(true);
					JOptionPane.showMessageDialog(window.getFrame(), null, "AI Chess", 0, chessThrones);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		/* Set difficulty */
//		String[] option = { "Medium", "Easy", "Easier" };
		String[] option = { "Hard", "Medium" };
		int easy = JOptionPane.showOptionDialog(null, "Select difficulty", "Difficulty Levels",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, option, option[1]);
		if (easy == -1) {	// this difficulty is disabled for now
			maxDepth = 8;
			maxBreadth = 9;
		} else if (easy == 0) {
			maxDepth = 7;
			maxBreadth = 8;
		} else if (easy == 1) {
			maxDepth = 5;
			maxBreadth = 12;
		}
	}

	/**
	 * Reset board to original state
	 */
	static void reset() {
		nodesExplored = 0;
		totalTime = 0;
		botMoves = 0;
		
		board = new char[8][8];
		String emptyRank = "        ";
		String blackPieces = "rnbqkbnr";				// rook,knight,bishop,queen,king
		String blackPawns = "pppppppp";
		String whitePieces = blackPieces.toUpperCase();	// uppercase letters are white pieces
		String whitePawns = blackPawns.toUpperCase();

		// Assign
		board[0] = blackPieces.toCharArray();
		board[1] = blackPawns.toCharArray();
		board[6] = whitePawns.toCharArray();
		board[7] = whitePieces.toCharArray();

		for (int i = 2; i <= 5; i++)
			board[i] = emptyRank.toCharArray();

		kingPosL = 4;
		kingPosU = 7 * 8 + 4;
	}

	/**
	 * Flip the board along diagonal from top left to bottom right.<br>
	 * Switch white pieces with black pieces
	 */
	static void flipAndSwitchBoard() {
		char t;
		for (int i = 0; i < 32; i++) {
			int r = i / 8, c = i % 8;

			if (Character.isUpperCase(board[r][c]))
				t = Character.toLowerCase(board[r][c]);
			else
				t = Character.toUpperCase(board[r][c]);
			if (Character.isUpperCase(board[7 - r][7 - c]))
				board[r][c] = Character.toLowerCase(board[7 - r][7 - c]);
			else
				board[r][c] = Character.toUpperCase(board[7 - r][7 - c]);
			board[7 - r][7 - c] = t;
		}

		int kingTemp = kingPosU;
		kingPosU = 63 - kingPosL;
		kingPosL = 63 - kingTemp;
	}

	/**
	 * Make move represented by the move object
	 * @param m
	 */
	static void makeMove(Move m) {
		if (m.getCapture() == 'P') {
			/* Pawn promotion */
			board[1][m.getY1()] = ' ';
			board[0][m.getX1()] = Move.BYTE_PIECE.get((byte) m.getX2());
		} else {
			/* Not pawn promotion */
			board[m.getY2()][m.getX2()] = board[m.getY1()][m.getX1()];
			board[m.getY1()][m.getX1()] = ' ';
			if (board[m.getY2()][m.getX2()] == 'K')	// update king position
				kingPosU = m.getY2() * 8 + m.getX2();
		}
	}

	/**
	 * Undo the move that was made in accord with the move object
	 * @param m
	 */
	static void undoMove(Move m) {
		if (m.getCapture() == 'P') {
			/* Pawn promotion */
			board[1][m.getY1()] = 'P';
			board[0][m.getX1()] = Move.BYTE_PIECE.get((byte) m.getY2());
		} else {
			/* Not pawn promotion */
			board[m.getY1()][m.getX1()] = board[m.getY2()][m.getX2()];
			board[m.getY2()][m.getX2()] = m.getCapture();
			if (board[m.getY1()][m.getX1()] == 'K')	// update king position
				kingPosU = m.getY1() * 8 + m.getX1();
		}
	}

	/**
	 * Get a set of all possible moves of 'uppercase side'
	 * @return A set of all possible moves of 'uppercase side'
	 */
	static Set<Move> getMoves() {
		Set<Move> set = new HashSet<>();

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board[i][j] == ' ' || Character.isLowerCase(board[i][j]))
					continue;
				int loc = (i << 3) + j;
				switch (board[i][j]) {
					case ' ':
						break;
					case 'P':
						set.addAll(getPawnMoves(loc));
						break;
					case 'R':
						set.addAll(getOrthogonalMoves(loc, 'R'));
						break;
					case 'N':
						set.addAll(getKnightMoves(loc));
						break;
					case 'B':
						set.addAll(getDiagonalMoves(loc, 'B'));
						break;
					case 'Q':
						set.addAll(getQueenMoves(loc));
						break;
					case 'K':
						set.addAll(getKingMoves(loc));
						break;
				}
			}
		}

		return set;
	}

	/*
	 * General notes:
	 * 
	 * 1. The try-catch blocks that throw 
	 * ArrayIndexOutOfBoundsException are required as
	 * the loops do not check if all indices are in bounds.
	 * 
	 * 2. All moves are returned for the side represented
	 * by uppercase pieces
	 * 
	 * 3. isValidMove() applies to every piece's get___Moves 
	 */

	/**
	 * Check if move is in bounds and does not leave king under check
	 * @param r
	 * @param c
	 * @param cr
	 * @param cc
	 * @param piece
	 * @return Validity of move
	 */
	private static boolean isValidMove(int r, int c, int cr, int cc, char piece) {
		boolean valid = false;
		try {
			char oldPiece = board[cr][cc];	// store piece previously occupying square
			board[r][c] = ' ';				// set moving piece's old position to empty
			board[cr][cc] = piece;			// move moving piece to destination
			if (!underCheck())				// can't make a move that leads to a check
				valid = true;
			board[r][c] = piece;			// restore
			board[cr][cc] = oldPiece;		// restore
		} catch (ArrayIndexOutOfBoundsException e) {
			// if some index is out of range, just ignore move and return false
		}
		return valid;
	}

	/**
	 * Get all possible king moves
	 * @param loc
	 * @return Possible king moves
	 */
	static Collection<Move> getKingMoves(int loc) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;

		/* Check all squares around king */
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0)
					continue;
				try {
					int cr = r + i, cc = c + j;
					char cur = board[cr][cc];
					if (cur == ' ' || Character.isLowerCase(cur)) {
						int kingTemp = kingPosU;
						kingPosU = cr * 8 + cc;
						if (isValidMove(r, c, cr, cc, 'K'))
							list.add(new Move(r, c, cr, cc, cur));
						kingPosU = kingTemp;
					}
				} catch (ArrayIndexOutOfBoundsException e) {}
			}
		}

		// TODO Implement castling
		return list;
	}

	/**
	 * Get all possible queen moves
	 * @param loc
	 * @return Possible queen moves
	 */
	private static Collection<Move> getQueenMoves(int loc) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;
		int dist = 1;

		/* Check all squares around queen then keep increasing 
		   distance by 1 to include every possible move */
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0)
					continue;
				try {
					int cr = r + dist * i;
					int cc = c + dist * j;
					/* Blank squares */
					while (board[cr][cc] == ' ') {
						if (isValidMove(r, c, cr, cc, 'Q'))
							list.add(new Move(r, c, cr, cc, ' '));
						dist++;
						cr = r + dist * i;
						cc = c + dist * j;
					}
					/* Capture */
					if (Character.isLowerCase(board[cr][cc])) {
						if (isValidMove(r, c, cr, cc, 'Q'))
							list.add(new Move(r, c, cr, cc, board[cr][cc]));
					}
				} catch (ArrayIndexOutOfBoundsException e) {}
				dist = 1;
			}
		}
		return list;
	}

	/**
	 * Get all possible bishop moves
	 * @param loc
	 * @param piece
	 *            In case a queen's moves are being assessed
	 * @return Possible bishop moves
	 */
	private static Collection<Move> getDiagonalMoves(int loc, char piece) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;
		int dist = 1;

		/* Similar to queen moves, without orthogonal moves
		   so, i = 0 and j = 0 are not considered */
		for (int i = -1; i <= 1; i += 2) {
			for (int j = -1; j <= 1; j += 2) {
				try {
					int cr = r + dist * i;
					int cc = c + dist * j;
					/* Blank squares */
					while (board[cr][cc] == ' ') {
						if (isValidMove(r, c, cr, cc, piece))
							list.add(new Move(r, c, cr, cc, ' '));
						dist++;
						cr = r + dist * i;
						cc = c + dist * j;
					}
					/* Capture */
					if (Character.isLowerCase(board[cr][cc]))
						if (isValidMove(r, c, cr, cc, piece))
							list.add(new Move(r, c, cr, cc, board[cr][cc]));
				} catch (ArrayIndexOutOfBoundsException e) {}
				dist = 1;
			}
		}
		return list;
	}

	/**
	 * Get all possible rook moves
	 * @param loc
	 * @param piece
	 *            In case a queen's moves are being assessed
	 * @return Possible rook moves
	 */
	private static Collection<Move> getOrthogonalMoves(int loc, char piece) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;
		int dist = 1;

		/* Check all four directions then keep increasing 
		   distance to include all possible moves*/
		for (int i = -1; i <= 1; i += 2) {
			try {
				int cr = r;
				int cc = c + dist * i;
				/* Blank squares */
				while (board[cr][cc] == ' ') {
					if (isValidMove(r, c, cr, cc, piece))
						list.add(new Move(r, c, cr, cc, ' '));
					dist++;
					cr = r;
					cc = c + dist * i;
				}
				/* Capture */
				if (Character.isLowerCase(board[cr][cc]))
					if (isValidMove(r, c, cr, cc, piece))
						list.add(new Move(r, c, cr, cc, board[cr][cc]));
			} catch (ArrayIndexOutOfBoundsException e) {}
			dist = 1;
			try {
				int cr = r + dist * i;
				int cc = c;
				/* Blank squares */
				while (board[cr][cc] == ' ') {
					if (isValidMove(r, c, cr, cc, piece))
						list.add(new Move(r, c, cr, cc, ' '));
					dist++;
					cr = r + dist * i;
					cc = c;
				}
				/* Capture */
				if (Character.isLowerCase(board[cr][cc]))
					if (isValidMove(r, c, cr, cc, piece))
						list.add(new Move(r, c, cr, cc, board[cr][cc]));
			} catch (ArrayIndexOutOfBoundsException e) {}
			dist = 1;
		}
		return list;
	}

	private static final int[][] KNIGHT_MOVES = { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, 
												{ 1, -2 }, { 1, 2 }, { 2, -1 }, { 2, 1 } };

	/**
	 * Get all possible knight moves
	 * @param loc
	 * @return Possible knight moves
	 */
	private static Collection<Move> getKnightMoves(int loc) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;

		for (int i = 0; i < KNIGHT_MOVES.length; i++) {
			try {
				int cr = r + KNIGHT_MOVES[i][0];
				int cc = c + KNIGHT_MOVES[i][1];
				char cur = board[cr][cc];

				if (cur == ' ' || Character.isLowerCase(cur))
					if (isValidMove(r, c, cr, cc, 'N'))
						list.add(new Move(r, c, cr, cc, cur));
			} catch (ArrayIndexOutOfBoundsException e) {}
		}
		return list;
	}

	/**
	 * Get all possible pawn moves
	 * Take into account pawn promotion
	 * @param loc
	 * @return Possible pawn moves
	 */
	private static Collection<Move> getPawnMoves(int loc) {
		List<Move> list = new ArrayList<>();
		int r = loc / 8, c = loc % 8;
		final char[] PROMOTION = { 'Q', 'N' };

		for (int j = -1; j <= 1; j += 2) {
			/* Capture */
			try {
				if (loc >= 16 && Character.isLowerCase(board[r - 1][c + j]))
					if (isValidMove(r, c, r - 1, c + j, 'P'))
						list.add(new Move(r, c, r - 1, c + j, board[r - 1][c + j]));
			} catch (ArrayIndexOutOfBoundsException e) {}

			/* Promotion and Capture */
			try {
				if (loc < 16 && Character.isLowerCase(board[r - 1][c + j]))
					for (int k = 0; k < 2; k++)
						if (isValidMove(r, c, r - 1, c + j, 'P'))
							// Notation: (column1,column2,captured-piece,new-piece,'P')
							list.add(new Move(c, c + j, (int) PIECE_BYTE.get(board[r - 1][c + j]), (int) PIECE_BYTE.get(PROMOTION[k]), 'P'));
			} catch (ArrayIndexOutOfBoundsException e) {}

			/* Move one up */
			if (loc >= 16 && board[r - 1][c] == ' ')
				if (isValidMove(r, c, r - 1, c, 'P'))
					list.add(new Move(r, c, r - 1, c, ' '));
			/* Promotion and No Capture */
			if (loc < 16 && board[r - 1][c] == ' ')
				for (int k = 0; k < 2; k++)
					if (isValidMove(r, c, r - 1, c, 'P'))
						// Notation: (column1,column2,captured-piece,new-piece,'P')
						list.add(new Move(c, c, (int) PIECE_BYTE.get(' '), (int) PIECE_BYTE.get(PROMOTION[k]), 'P'));
			/* Move two up */
			if (loc >= 48 && board[r - 1][c] == ' ' && board[r - 2][c] == ' ')
				if (isValidMove(r, c, r - 2, c, 'P'))
					list.add(new Move(r, c, r - 2, c, ' '));
		}
		// TODO Implement en passant
		return list;
	}

	/**
	 * Check if king is under check
	 * Do this by assuming one piece at a time that
	 * king can move like every other piece and see
	 * if the king attacks the piece it is mimicking
	 * @return
	 *         If king is threatened or not
	 */
	static boolean underCheck() {
		int r = kingPosU / 8;
		int c = kingPosU % 8;

		// Bishop/Queen
		int dist = 1;
		for (int i = -1; i <= 1; i += 2) {
			for (int j = -1; j <= 1; j += 2) {
				try {
					while (board[r + dist * i][c + dist * j] == ' ')
						dist++;
					if (board[r + dist * i][c + dist * j] == 'b' ||
							board[r + dist * i][c + dist * j] == 'q') {
						return true;
					}
				} catch (ArrayIndexOutOfBoundsException e) {}
				dist = 1;
			}
		}

		// Rook/Queen
		dist = 1;
		for (int i = -1; i <= 1; i += 2) {
			try {
				while (board[r][c + dist * i] == ' ')
					dist++;
				if (board[r][c + dist * i] == 'r' ||
						board[r][c + dist * i] == 'q')
					return true;
			} catch (ArrayIndexOutOfBoundsException e) {}
			dist = 1;
			try {
				while (board[r + dist * i][c] == ' ')
					dist++;
				if (board[r + dist * i][c] == 'r' ||
						board[r + dist * i][c] == 'q')
					return true;
			} catch (ArrayIndexOutOfBoundsException e) {}
			dist = 1;
		}

		// Knight
		for (int i = 0; i < 8; i++) {
			try {
				if (board[r + KNIGHT_MOVES[i][0]][c + KNIGHT_MOVES[i][1]] == 'n')
					return true;
			} catch (ArrayIndexOutOfBoundsException e) {}
		}

		// Pawn
		if (kingPosU >= 16) {
			try {
				if (board[r - 1][c - 1] == 'p')
					return true;
			} catch (ArrayIndexOutOfBoundsException e) {}
			try {
				if (board[r - 1][c + 1] == 'p')
					return true;
			} catch (ArrayIndexOutOfBoundsException e) {}
		}

		// The other king
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0)
					continue;
				try {
					if (board[r + i][c + j] == 'k')
						return true;
				} catch (ArrayIndexOutOfBoundsException e) {}
			}
		}

		// No attacks on the king
		return false;
	}

	/**
	 * Returns true if there is at least one possible legal move
	 * Helpful in determining checkmate and stalemate
	 * @return getMoves().size == 0
	 */
	static boolean canMove() {
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board[i][j] == ' ' || Character.isLowerCase(board[i][j]))
					continue;
				int loc = i * 8 + j;
				switch (board[i][j]) {
					case 'K':
						if (!getKingMoves(loc).isEmpty())
							return true;
						break;
					case 'N':
						if (!getKnightMoves(loc).isEmpty())
							return true;
						break;
					case 'Q':
						if (!getQueenMoves(loc).isEmpty())
							return true;
						break;
					case 'P':
						if (!getPawnMoves(loc).isEmpty())
							return true;
						break;
					case 'B':
						if (!getDiagonalMoves(loc, 'B').isEmpty())
							return true;
						break;
					case 'R':
						if (!getOrthogonalMoves(loc, 'R').isEmpty())
							return true;
						break;
				}
			}
		}
		return false;
	}

	/**
	 * This implementation uses flipAndSwitchBoard() (and properties
	 * of zero-sum games) to implement negamax version of the minimax algorithm.
	 * The pruning is done by the alpha-beta heuristic
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @param move
	 * @param player
	 * @return The best move reported by the search
	 * @throws InterruptedException 
	 */
	static Move negaMax(Move move, int depth, int alpha, int beta, int player) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		
		nodesExplored++;
		if (depth == 0) {
			move.setAlphaBetaScore(Evaluation.evaluate(0, depth) * (player * 2 - 1));
			return move;
		}

		Set<Move> possibleMoves = getMoves();					// Get possible moves
		Queue<Move> queue = sortAndFilterMoves(possibleMoves);	// Sort moves, discard some if too many

		if (queue.size() == 0) {
			move.setAlphaBetaScore(Evaluation.evaluate(0, depth) * (player * 2 - 1));
			return move;
		}

		player = 1 - player;	// either 1 or 0

		while (!queue.isEmpty()) {
			/* Backtracking best-first search (depth and breadth limited) */
			Move m = queue.poll();				// move to try next
			makeMove(m);
			flipAndSwitchBoard();
			Move ret = negaMax(m, depth - 1, alpha, beta, player);
			int score = ret.getAlphaBetaScore();		// child's (opponent's) score (accurate)
			flipAndSwitchBoard();
			undoMove(m);

			if (player == 0) {
				if (score < beta) {
					beta = score;
					if (depth == getMaxDepth())
						move = ret;
				}
			} else {
				if (score > alpha) {
					alpha = score;
					if (depth == getMaxDepth())
						move = ret;
				}
			}
			/* Early termination/pruning heuristic */
			if (alpha >= beta) {
				if (player == 0) {
					move.setAlphaBetaScore(beta);
					return move;
				} else {
					move.setAlphaBetaScore(alpha);
					return move;
				}
			}
		}

		if (player == 0) {
			move.setAlphaBetaScore(beta);
			return move;
		} else {
			move.setAlphaBetaScore(alpha);
			return move;
		}
	}

	/**
	 * Select top k moves
	 * Uses a max heap
	 * Time ~ O(n log k)
	 * @param dat
	 * @return The best few moves
	 */
	private static Queue<Move> sortAndFilterMoves(Collection<Move> dat) {
		PriorityQueue<Move> minPQ = new PriorityQueue<>(10);
		PriorityQueue<Move> maxPQ = new PriorityQueue<>(10, Collections.reverseOrder());	// Descending comparator

		for (Move m : dat) {
			makeMove(m);
			m.setEvalPlaceHolder(-Evaluation.evaluate(-1, 0));	// store eval's score
			undoMove(m);
			
			maxPQ.add(m);										// @see Move.compareTo() for priority
			minPQ.add(m);
			if (maxPQ.size() > maxBreadth)
				maxPQ.remove(minPQ.poll());
		}

		// Only consider the best maxBreadth amount of moves
		Queue<Move> ret = new ArrayDeque<>();
		for (int i = 0, t = Math.min(maxBreadth, dat.size()); i < t; i++)
			ret.offer(maxPQ.poll());
		return ret;
	}

	/**
	 * Print board to console
	 */
	@SuppressWarnings("unused")
	private static void printBoard() {
		for (int i = 0; i < 8; i++)
			System.out.println(Arrays.toString(board[i]));
	}

	public static int getMaxDepth() {
		return maxDepth;
	}

}
