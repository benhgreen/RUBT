package RUBTclient;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

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
		
	public void connectToPeer(byte[] handshake) throws IOException{
		
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
		
		byte[] response = new byte[100];
		
		peerInputStream.read(response);
		
		System.out.println(response);
		
		peerOutputStream.close();
		peerInputStream.close();
		peerConnection.close();
	}
}
