package RUBTclient;

import java.util.Arrays;

public class Message 

{
	 private byte[] handshake = new byte[68];
     private final byte[] handshake_consts = {0x13,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
	
	/**
	 *something will go here eventually. 
	 */
	public Message()
	{
				
	}
	public byte[] handShake( byte[] info_hash)
	{
		char[] userid;
		int shake_loc = 0;  //location of the handshake array that we are populating.
		for(int i = 0; i<handshake_consts.length;i++)
		{
			handshake[shake_loc] = handshake_consts[i];
			shake_loc++;
		}
		for(int s = 0; s < info_hash.length;s++)
		{
			handshake[shake_loc] = info_hash[s];
			shake_loc++;
		}
		GetRequest poop = new GetRequest();
		userid=poop.getUser_id().toCharArray();
		for(int i = 0;i<userid.length;i++ )
		{
			handshake[shake_loc] = (byte)userid[i];
			shake_loc++;
		}
		return handshake;
	}
}

