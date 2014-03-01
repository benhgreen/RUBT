package RUBTclient;

public class Piece {
	
	byte[] data;
	int piece;
	int offset;
	
	public Piece(byte[] data, int piece, int offset)
	{
		this.data = data;
		this.piece = piece;
		this.offset = offset;
	}
	
	/**
	 * @return the piece
	 */
	public int getPiece() {
		return piece;
	}
	
	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}
	
	

}
