package RUBTClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 *This method is responsible for all composing and identification of messages sent between peers
 *
 */
public class Message 

{
	
	private final int request_prefix = 0xD;
	//message identifiers
	
	/**
	 * @field CHOKE Value of the choke identifier
	 */
	public static final  byte CHOKE = 0;
	//
	/**
	 * @field UNCHOKE Value of the unchoke identifier
	 */
	public static final byte UNCHOKE = 1;
	/**
	 * @field INTERESTED Value of the interested identifier 
	 */
	public static final byte INTERESTED = 2;
	/**
	 * @field HAVE Value of the have identifier
	 */
	public static final byte HAVE = 4;
	/**
	 *  @field BITFIELD Value of the bitfield identifier
	 */
	public static final byte BITFIELD = 5;
	/**
	 * @field REQUEST Value of the request identifier
	 */
	public static final byte REQUEST = 6;
	/**
	 * @field PIECE Value of the piece identifier
	 */
	public static final byte PIECE = 7;
	/**
	 * @field QUIT Value of the quit identifier
	 */
	public static final byte QUIT = 25;
	 //message headers
	private final byte[] handshake_consts = {0x13,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
	private final byte[] have_consts = {0,0,0,5,4};
	private final byte[] request_consts = {0,0,0,0xD,6};
	//all non-payload messages
	private final byte[] choke = { 0,0,0,1,0};
	private final byte[] unchoke = {0,0,0,1,1};
	private final byte[] interested = {0,0,0,1,2};
	private final byte[] not_interested = {0,0,0,1,3};
	private final byte[] keep_alive = {0,0,0,0};
	/**
	 * This method constructs a byte array that contains the handshake message
	 * @param info_hash takes the info hash given by the .torrent file
	 * @param userid our user id
	 * @return returns a handshake message in the form of a byte array  
	 */
	public byte[] handShake( byte[] info_hash, byte[] userid)
	{
		byte[] handshake = new byte[68];
		
		System.arraycopy(handshake_consts,0,handshake,0,28);// copies handshake constants
		System.arraycopy(info_hash, 0, handshake,28 , 20);
		System.arraycopy(userid, 0, handshake,48 , 20);
		
		return handshake;
	}
	
	/**
	 * This method takes all inputs and constants for a request message
	 * and generates returns a complete request message.
	 * @param index index of the piece wanted
	 * @param begin offset of the byte inside of the piece
	 * @param length length that we want to download
	 * @return returns a composed request message
	 */
	public byte[] request(int index, int begin, int length)
	{
		ByteBuffer request = ByteBuffer.allocate(17);//allocates a byte buffer to compose our request in
		request.put(request_consts);
		request.putInt(index);
		request.putInt(begin);
		request.putInt(length);
		return request.array();//returns the buffer as a byte array
	}
	
	/**
	 * @return choke message
	 */
	public byte[] getChoke() 
	{
		return choke;
	}
	/**
	 * @return unchoke message
	 */
	public byte[] getUnchoke() 
	{
		return unchoke;
	}
	
	/**
	 * @return Interested message
	 */
	public byte[] getInterested() 
	{
		return interested;
	}
	
	/**
	 * @return not interested message
	 */
	public byte[] getNot_interested() 
	{
		return not_interested;
	}
	
	/**
	 * @return keep alive message
	 */
	public byte[] getKeep_alive() 
	{
		return keep_alive;
	}
	/**
	 * 
	 * @return request message prefix
	 */
	public int getRequest_Prefix()
	{
		return request_prefix;
	}
	
	/**
	 * Generates a bitfield array to send to a newly connected peer
	 * @param mybitfield the clients bitfield
	 * @return returns our bitfield
	 */
	public byte[] getBitFieldMessage(byte[] mybitfield) 
	{
		int field_length = mybitfield.length+1;
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(field_length);
		byte[] bitfield = new byte[5+field_length-1];  //makes a bitfield message
		System.arraycopy(buffer.array(), 0, bitfield, 0, 4);
		bitfield[4]=BITFIELD;
		System.arraycopy(mybitfield,0,bitfield,5,mybitfield.length);
		return bitfield;
	}
	
	/**
	 * Generates a piece message to send to a requesting peer
	 * @param file file the data will come from
	 * @param req_index piece index requested
	 * @param req_length requested length
	 * @param req_begin offset of the request
	 * @param offset index that we will start the piece at
	 * @return constructed piece message
	 */
	public byte[] getPieceMessage(DestFile file,byte[] req_index,int req_length,byte[] req_begin)
	{
		byte[] index_bytes = req_index;  //index bytes
		byte[] begin_bytes = req_begin;
		byte[] piece_message = new byte[req_length+13]; //size of piece is length of request, plus 13 bytes for header info
		byte[] block = new byte[req_length];
		int length = req_length+9;            
		int index = ByteBuffer.wrap(index_bytes).getInt();  //wraps the offset bytes in a buffer and converts them into an int
		int begin = ByteBuffer.wrap(begin_bytes).getInt();
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(length);
		block = file.getPieceData(index, begin, length); //gets the requested chunk from the file
		System.arraycopy(buffer.array(), 0, piece_message, 0, 4);    //copy length
		piece_message[4]=PIECE;
		System.arraycopy(index_bytes, 0, piece_message, 5, 4);   //copy index
		System.arraycopy(begin_bytes,0,piece_message,9,4);       //copy begin
		System.arraycopy(block, 0, piece_message, 13, req_length);
		return piece_message;
	}
	
	/**
	 * Generates a quit message to signal our client
	 * @return quit message
	 */
	public byte[] getQuitMessage()
	{
		byte[] quit_message = {QUIT};
		return quit_message;
	}
	/**
	 * Generates a have message to send to our peers
	 * @param index of the piece that we have
	 * @return a constructed have message
	 */
	public byte[] getHaveMessage(byte[] index) 
	{
		byte[] have = new byte[9]; 
		System.arraycopy(have_consts, 0, have, 0, 5);
		System.arraycopy(index,0,have,5,4);
		return have;
	}
	
	/**
	 * Passes a Data Input Stream that the message object interprets and sets its fields to
	 * @param in peer's data input stream
	 * @return a new message object
	 */
	public static Message read(DataInputStream in)
	{
		return null;
	}
	
	/**
	 * Writes a message through an output Stream
	 * @param out data output stream to write to
	 */
	public void write(DataOutputStream out)
	{
		
	}
}

