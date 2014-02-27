package RUBTclient;

import java.net.*;
import java.io.*;
//import java.nio.ByteBuffer;
import java.util.Arrays;

public class Peer {
	
	String ip;
	String peer_id;
	Integer port;
	
	public Peer(String ip, String peer_id, Integer port) {
		super();
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
	}
		
	public void connectToPeer(byte[] handshake, byte[] interested, byte[] request) throws IOException{
		
		Socket peerConnection = null;
		OutputStream peerOutputStream = null;
		InputStream peerInputStream = null;
		
		try{
			peerConnection = new Socket(ip, port);
			peerOutputStream = peerConnection.getOutputStream();
			peerInputStream = peerConnection.getInputStream();
		}catch(UnknownHostException e){
			System.out.println("UnknownHostException");
		}catch(IOException e){
			System.out.println("IOException");
		}
		//write Message object byte array and listen for response;
		peerOutputStream.write(handshake);
		byte[] response = new byte[68];
		peerInputStream.read(response);
		System.out.println("handshake response: " + Arrays.toString(response));
		
		
		//verify data
		
		peerOutputStream.flush();
		peerOutputStream.write(interested);		
		
		response = new byte[68];
		peerInputStream.read(response);
		System.out.println("interested response1: " + Arrays.toString(response));
		
		
		response = new byte[68];
		peerInputStream.read(response);
		System.out.println("interested response2:  " + Arrays.toString(response));
		
		//peerOutputStream.flush();
		//byte[] request2 = {0,0,1,3,6,0,0,5};
		//peerOutputStream.write(request2);
		
		response = new byte[68];
		peerInputStream.read(response);
		System.out.println("response3:  " + Arrays.toString(response));
		
		peerOutputStream.close();
		peerInputStream.close();
		peerConnection.close();
	}
}
