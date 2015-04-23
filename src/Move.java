/**
 * ICS4U Mrs. Kapustina
 * 
 * @author Kunal
 */

import java.util.HashMap;
import java.util.Map;

public class Move implements Comparable<Move> {
	byte x1, y1;	// from location
	byte x2, y2;	// to location
	char capture;	// piece captured
	/* For pawn promotion notation, see Engine.getPawnMoves() */

	/* Storage for temporary scores associated with board states */
	int alphaBetaScore;
	int evalPlaceHolder;

	/**
	 * Map a byte to a piece
	 */
	public static final Map<Byte, Character> BYTE_PIECE = new HashMap<>();
	static {
		BYTE_PIECE.put((byte) 1, 'P');
		BYTE_PIECE.put((byte) 3, 'N');
		BYTE_PIECE.put((byte) 4, 'B');
		BYTE_PIECE.put((byte) 5, 'R');
		BYTE_PIECE.put((byte) 9, 'Q');
		BYTE_PIECE.put((byte) 10, 'K');
		BYTE_PIECE.put((byte) 0, ' ');
		BYTE_PIECE.put((byte) -1, 'p');
		BYTE_PIECE.put((byte) -3, 'n');
		BYTE_PIECE.put((byte) -4, 'b');
		BYTE_PIECE.put((byte) -5, 'r');
		BYTE_PIECE.put((byte) -9, 'q');
		BYTE_PIECE.put((byte) -10, 'k');
	}

	public Move() {}

	public Move(byte y1, byte x1, byte y2, byte x2, char capture) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.capture = capture;
	}

	public Move(int r, int c, int cr, int cc, char capture) {
		this((byte) r, (byte) c, (byte) cr, (byte) cc, capture);
	}

	public Move(int locFrom, int locTo, char capture) {
		this(locFrom / 8, locFrom % 8, locTo / 8, locTo % 8, capture);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this.getClass() != o.getClass())
			return false;
		if (this == o)		// Optimization: check if pointers point to same location
			return true;

		Move that = (Move) o;
		return this.equals(that);
	}

	public boolean equals(Move that) {
		if (this.x1 != that.x1 || this.y1 != that.y1 || this.x2 != that.x2 || this.y2 != that.y2)
			return false;
		if (this.capture != that.capture)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (capture == 'P')
			return ("" + y1 + x1 + BYTE_PIECE.get(y2) + BYTE_PIECE.get(x2) + capture);
		else
			return ("" + y1 + x1 + y2 + x2 + capture);
	}

	/**
	 * <h3>Hash function</h3><br>
	 * A working model, does not guarantee uniform hashing<br>
	 * 
	 * Collisions among symmetric pieces are avoided by
	 * multiplying 'to' coordinates by 2^4 in addition to incorporating 'from' coordinates
	 * 
	 * capture is multiplied by 2^24 so that differences in
	 * position do not fill up the gap between difference in
	 * captured pieces' int value, again, helping avoid collisions
	 */
	public int hashCode() {
		return (((y1 << 3) + x1) + (((y2 << 3) + x2) << 4) + (Engine.board[y1][x1] << 14) + (capture << 24));
//		return (((y1 << 3) + x1) + (((y2 << 3) + x2) << 3) + (Engine.board[y1][x1] << 6) + (capture << 9));
	}

	@Override
	public int compareTo(Move that) {
		return Integer.compare(this.evalPlaceHolder, that.evalPlaceHolder);
	}

}
