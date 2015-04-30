/**
 * ICS4U Mrs. Kapustina
 * 
 * @author Kunal
 */

import java.util.HashMap;
import java.util.Map;

public class Move implements Comparable<Move> {
	private byte x1, y1;	// from location
	private byte x2, y2;	// to location
	private char capture;	// piece captured
	// Pawn promotion has the following notation:
	// (x1,y1,x2,y2,capture) ->
	//    (column1,column2,captured-piece,new-piece,'P')

	/* Storage for temporary scores associated with board states */
	private int alphaBetaScore;
	private int evalPlaceHolder;

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
		this.setX1(x1);
		this.setY1(y1);
		this.setX2(x2);
		this.setY2(y2);
		this.setCapture(capture);
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
		if (this == o)		// check if pointers point to same location
			return true;

		Move that = (Move) o;
		return this.equals(that);
	}

	public boolean equals(Move that) {
		if (this.getX1() != that.getX1() || this.getY1() != that.getY1() || 
			this.getX2() != that.getX2() || this.getY2() != that.getY2())
			return false;
		if (this.getCapture() != that.getCapture())
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
	 */
	public int hashCode() {
		return (((y1 << 3) + x1) + (((y2 << 3) + x2) << 3) + (Engine.board[y1][x1] << 6) + (capture << 9));
	}

	@Override
	public int compareTo(Move that) {
		return Integer.compare(this.getEvalPlaceHolder(), that.getEvalPlaceHolder());
	}

	
	/* Getters and setters */
	
	public char getCapture() {
		return capture;
	}

	public void setCapture(char capture) {
		this.capture = capture;
	}

	public byte getY1() {
		return y1;
	}

	public void setY1(byte y1) {
		this.y1 = y1;
	}

	public byte getX1() {
		return x1;
	}

	public void setX1(byte x1) {
		this.x1 = x1;
	}

	public byte getY2() {
		return y2;
	}

	public void setY2(byte y2) {
		this.y2 = y2;
	}

	public byte getX2() {
		return x2;
	}

	public void setX2(byte x2) {
		this.x2 = x2;
	}

	public int getAlphaBetaScore() {
		return alphaBetaScore;
	}

	public void setAlphaBetaScore(int alphaBetaScore) {
		this.alphaBetaScore = alphaBetaScore;
	}

	public int getEvalPlaceHolder() {
		return evalPlaceHolder;
	}

	public void setEvalPlaceHolder(int evalPlaceHolder) {
		this.evalPlaceHolder = evalPlaceHolder;
	}

}
