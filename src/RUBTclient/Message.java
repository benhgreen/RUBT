package RUBTclient;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message 

{
	 private final byte[] handshake_consts = {0x13,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
	 private final byte[] have_consts = {0,0,0,5,4};
	 private final byte[] request_consts = {0,0,0,0xD,6};
	//all non-payload messages
	 private final byte[] choke = { 0,0,0,1,0};
	 private final byte[] unchoke = {0,0,0,1,1};
	 private final byte[] interested = {0,0,0,1,2};
	 private final byte[] not_interested = {0,0,0,1,3};
	 private final byte[] keep_alive = {0,0,0,0};
	
	public Message()
	{
				
	}
	/**
	 * This method constructs a byte array that contains the handshake message
	 * @param info_hash takes the info hash given by the .torrent file
	 * @return returns a handshake in the form of a byte array  
	 */
	public byte[] handShake( byte[] info_hash)
	{
		byte[] handshake = new byte[68];
		byte[] userid;
		System.arraycopy(handshake_consts,0,handshake,0,28);// copies handshake constants
		System.arraycopy(info_hash, 0, handshake,28 , 20);
		GetRequest our_id = new GetRequest();
		userid=our_id.getUser_id().getBytes();    //gets our user id, and then puts it into our handshake
		System.arraycopy(userid, 0, handshake,48 , 20);
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
		ByteBuffer request = ByteBuffer.allocate(17);//need a better way, maybe bytearray output stream?
		request.put(request_consts);
		request.putInt(index);
		request.putInt(begin);
		request.putInt(length);
		return request.array();
	}
	
	public byte[] getChoke() 
	{
		return choke;
	}
	public byte[] getUnchoke() 
	{
		return unchoke;
	}
	public byte[] getInterested() 
	{
		return interested;
	}
	public byte[] getNot_interested() 
	{
		return not_interested;
	}
	public byte[] getKeep_alive() 
	{
		return keep_alive;
	}
}

