/**
 * Most of this class was designed and implemented by chess programming theorists 
 * http://chessprogramming.wikispaces.com/Simplified+evaluation+function
 */

public class Evaluation {
    static int pawnBoard[][]={
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}};
    static int rookBoard[][]={
        { 0,  0,  0,  0,  0,  0,  0,  0},
        { 5, 10, 10, 10, 10, 10, 10,  5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        { 0,  0,  0,  5,  5,  0,  0,  0}};
    static int knightBoard[][]={
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}};
    static int bishopBoard[][]={
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}};
    static int queenBoard[][]={
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}};
    static int kingMidBoard[][]={
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}};
    static int kingEndBoard[][]={
        {-50,-40,-30,-20,-20,-30,-40,-50},
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30},
        {-50,-30,-30,-30,-30,-30,-30,-50}};
	
	public static int evaluate(int listSize, int depth) {
		int score = 0;
		int material = evalMaterial();
		score += evalAttack();
		score += material;
		score += evalMobility(listSize, depth, material);
		score += evalPosition(material);

		// Opponent's score
		Engine.flipAndSwitchBoard();
		material = evalMaterial();
		score -= evalAttack();
		score -= material;
		score -= evalMobility(listSize, depth, material);
		score -= evalPosition(material);
		Engine.flipAndSwitchBoard();
		return -(score + depth * 50);
	}

	public static int evalAttack() {
		int score = 0;
		int tempKingPosition = Engine.kingPosU;

		for (int i = 0; i < 64; i++) {
			if (Character.isLowerCase(Engine.board[i / 8][i % 8]))
				continue;
			Engine.kingPosU = i;
			switch (Engine.board[i / 8][i % 8]) {
				case ' ':
					// TODO empty square under attack
					break;
				case 'P':
					if (Engine.underCheck())
						score -= 64;
					break;
				case 'R':
					if (Engine.underCheck())
						score -= 500;
					break;
				case 'N':
					if (Engine.underCheck())
						score -= 300;
					break;
				case 'B':
					if (Engine.underCheck())
						score -= 300;
					break;
				case 'Q':
					if (Engine.underCheck())
						score -= 900;
					break;
			}
		}
		
		Engine.kingPosU = tempKingPosition;
		if (Engine.underCheck())
			score -= 200;
		return score / 2;
	}

	public static int evalMaterial() {
		int score = 0, bishopCount = 0;
		for (int i = 0; i < 64; i++) {
			if (Engine.board[i / 8][i % 8] == ' ' || Character.isLowerCase(Engine.board[i / 8][i % 8]))
				continue;
			switch (Engine.board[i / 8][i % 8]) {
				case 'P':
					score += 100;
					break;
				case 'R':
					score += 500;
					break;
				case 'N':
					score += 300;
					break;
				case 'B':
					bishopCount++;
					break;
				case 'Q':
					score += 900;
					break;
			}
		}
		if (bishopCount >= 2)
			score += 350 * bishopCount;
		else if (bishopCount == 1)
			score += 325;
		return score;
	}

	public static int evalMobility(int numberOfPossibleMoves, int depth, int material) {
		int score = 0;
		score += numberOfPossibleMoves * 5;		// 5 points per valid move
		
		if (numberOfPossibleMoves == 0) {		// current side is in checkmate or stalemate
			if (Engine.underCheck())
				// if checkmate
				score += -200000 * depth;
			else
				// if stalemate
				score += -150000 * depth;
		}
		
		return score;
	}

	public static int evalPosition(int material) {
		int score = 0;
		for (int i = 0; i < 64; i++) {
			if (Engine.board[i / 8][i % 8] == ' ' || Character.isLowerCase(Engine.board[i / 8][i % 8]))
				continue;
			switch (Engine.board[i / 8][i % 8]) {
				case 'P':
					score += pawnBoard[i / 8][i % 8];
					break;
				case 'R':
					score += rookBoard[i / 8][i % 8];
					break;
				case 'N':
					score += knightBoard[i / 8][i % 8];
					break;
				case 'B':
					score += bishopBoard[i / 8][i % 8];
					break;
				case 'Q':
					score += queenBoard[i / 8][i % 8];
					break;
				case 'K':
					if (material >= 1750) {
						score += kingMidBoard[i / 8][i % 8];
						score += Engine.getKingMoves(Engine.kingPosU).size() * 50;
					} else {
						score += kingEndBoard[i / 8][i % 8];
						score += Engine.getKingMoves(Engine.kingPosU).size() * 150;
					}
					break;
			}
		}
		return score;
	}
}