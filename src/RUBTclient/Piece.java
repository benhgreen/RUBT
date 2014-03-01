package RUBTclient;

public class Piece {
	
	byte[] data;
	int piece;
	int offset;
	/**
	 * @return the data
	 */
	
	public Piece(byte[] data, int piece, int offset){
		this.data = data;
		this.piece = piece;
		this.offset = offset;
	}
	

}
