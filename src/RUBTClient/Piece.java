package RUBTClient;

/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */

public class Piece {
	
	/**
	 * @field data data of the piece
	 */
	public byte[] data;
	private int offset;
	
	/**
	 * @param size size of the piece object
	 */
	public Piece(int size)
	{
		this.data = new byte[size];
		this.offset = -1;
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
	
	/**
	 * @param data  - fresh chunk of data
	 * @param offset - where in the piece to begin writing the data
	 */
	public void assemble(byte[] data, int offset){
		for(int i = 0; i < data.length; i++){
			this.data[offset+i] = data[i];
		}
		this.offset = offset;
	}
}
