package RUBTclient;

import java.nio.ByteBuffer;
//import java.util.Arrays;
/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */
public class Message 

{
	private final int request_prefix = 0xD;	
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
	
	 //Message object constructor
	public Message()
	{
				
	}
	/**
	 * This method constructs a byte array that contains the handshake message
	 * @param info_hash takes the info hash given by the .torrent file
	 * @return returns a handshake message in the form of a byte array  
	 */
	public byte[] handShake( byte[] info_hash, String userid)
	{
		byte[] handshake = new byte[68];
		
		System.arraycopy(handshake_consts,0,handshake,0,28);// copies handshake constants
		System.arraycopy(info_hash, 0, handshake,28 , 20);
		System.arraycopy(userid.getBytes(), 0, handshake,48 , 20);
		
		return handshake;
	}
	
	/**
	 * This method takes all inputs and constants for a request message
	 * and generates returns a complete request message.
	 * @param index index of the piece wanted
	 * @param begin offset of the byte inside of the piece
	 * @param length length that we want to download
	 * @return returns a composed request byte[] message
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
	 * @return not interested array
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
	 * @return request message prefix
	 */
	public int getRequest_Prefix()
	{
		return request_prefix;
	}
}

