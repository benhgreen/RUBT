package RUBTClient;

import java.util.Arrays;
import java.net.ServerSocket;
import java.io.EOFException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;


/**
 * @author Ben Green
 * @author Manuel Lopez
 * @author Christopher Rios
 */

/**
 *	Handles incoming connections from peers
 */
/**
 * @author admin
 *
 */
public class ConnectionListener extends Thread{
	private final RUBTClient client;
	
	/**
	 * @param client RUBTClient thread that initializes ConnectionListener
	 */
	public ConnectionListener(final RUBTClient client){
		this.client = client;
	}
	
	/** 
	 * ConnectionListener picks a valid ports to listen on and on accepting an incoming connection
	 * it makes a new peer object, verifies it with handshake, and adds in to the clients
	 * list of connected peers
	 */
	public void run(){
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
		}
		while (client.keepRunning){
			try{
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
				peer_id = handshakeCheck(handshake);
				if(peer_id == null){
					System.out.println("no peer id returned from handshake");
					peer.closeConnections();
					continue;
				}
				peer.setPeer_id(peer_id);
				System.out.println("incoming peer id " +  peer_id);
				peer.setClient(client);
				peer.setConnected(true);
				peer.start();
			}catch(EOFException e){
				System.err.println("RUBTClient startIncomingConnections: tracker contacted us. just ignore him");
			}catch(IOException ioe){
				System.out.println('\n' + "RUBTClient startIncomingConnections: IOException while handling request" + '\n');
			}catch(Exception e){
				System.err.println("RUBTClient startIncomingConnections: generic exception");
			}
		}
		System.out.println("Ending connection listener thread");
		return;
	}
	
	private byte[] handshakeCheck(byte[] peer_handshake){	

		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.

		if (Arrays.equals(peer_infohash, this.client.torrentinfo.info_hash.array())){ //returns true if the peer id matches and the info hash matches
			return peer_id;
		}else {
			return null;
		}
	}
}	
