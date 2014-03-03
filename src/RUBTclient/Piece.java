package RUBTclient;

public class Piece {
	
	private byte[] data;
	private int piece;
	private int offset;
	
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
	
	/**
	 * @return the data byte[]
	 */
	public byte[] getData()
	{
		return data;
	}
}
