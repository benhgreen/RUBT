package RUBTclient;

public class Message 

{
	 private byte[] handshake = new byte[68];
     private final byte[] handshake_consts = {0x13,0,0,0,0,0,0,0,0,'B','i','t','T','o','r','r','e','n','t',' ','p','r','o','t','c','o','l'};
	
	/**
	 *something will go here eventually. 
	 */
	public Message()
	{
				
	}

	public void handShake(byte[] info_hash)
	{
		int shake_loc = 0;  //location of the handshake array that we are populating.
		for(int i = 0; i<handshake_consts.length;i++)
		{
			handshake[shake_loc] = handshake_consts[i];
			shake_loc++;
		}
		System.out.println(handshake_consts.length);
		System.out.println(handshake[26]);
		for(int j = 0; j < info_hash.length;j++)
		{
			handshake[shake_loc] = info_hash[j];
			shake_loc++;
		}
		System.out.println(handshake[27]);
	}
}

