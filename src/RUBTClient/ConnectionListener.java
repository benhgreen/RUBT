package RUBTClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ConnectionListener extends Thread{
	private final RUBTClient client;
	
	public ConnectionListener(final RUBTClient client){
		this.client = client;
	}
	
	public void run(){
		System.out.println("incoming connections listener " + Thread.currentThread().getName() + " " + Thread.currentThread().getId());
		boolean validPort = false;
		client.setPort(6881);
		client.serverSocket = null;
		
		while(client.getPort() <= 6889 && !validPort){
			try {
				client.serverSocket = new ServerSocket(client.getPort());
				validPort = true;
			} catch (IOException e) {
				client.setPort(client.getPort() + 1);
			}
		}
		if(client.getPort() >= 6890){
			System.err.println("RUBTClient startIncomingConnections(): all valid ports taken. quitting....");
			client.quitClientLoop();
			return;
			//replace with gracefull exit methodtra 
		}
		while (client.keepRunning){
			try{
				System.out.println( "### listen loop ###");
				if(client.serverSocket == null){
					System.err.println("RUBTClient startIncomingConnections: null listener Socket. quitting...");
					client.quitClientLoop();
				}
				if(Thread.currentThread().isInterrupted()){
					System.out.println("intererupted listener thread");
					break;
				}
				client.incomingSocket = client.serverSocket.accept();
				client.listenInput = new DataInputStream(client.incomingSocket.getInputStream());
				client.listenOutput = new DataOutputStream(client.incomingSocket.getOutputStream());
				Peer peer = new Peer(client.incomingSocket, client.listenInput, client.listenOutput);
				Message msg = new Message();
				byte[] handshake;
				byte[] peer_id;
				peer.sendMessage(msg.handShake(client.torrentinfo.info_hash.array(), client.tracker.getUser_id()));
				handshake = peer.handshake();
				if(handshake == null){
					continue;
				}
				peer_id = client.handshakeCheck(handshake);
				if(peer_id == null){
					System.out.println("no peer id returned from handshake");
					peer.closeConnections();
					continue;
				}
				//byte[] peer_byte_array = ByteBuffer.wrap(peer_id);
				peer.setPeer_id(peer_id);
				System.out.println("@@@@@@@@@@@@@@@@@@  incoming peer id " +  Arrays.toString(peer_id));
				peer.setClient(client);
				peer.setConnected(true);
				peer.start();
			}catch(EOFException e){
				System.err.println("RUBTClient startIncomingConnections: tracker contacted us. just ignore him");
			}catch(IOException ioe){
				System.out.println("");
				System.err.println("RUBTClient startIncomingConnections: IOException while handling request");
			}catch(Exception e){
				System.err.println("RUBTClient startIncomingConnections: generic exception");
			}
		}
		System.out.println("Ending connection listener thread");
		return;
	}
}	
