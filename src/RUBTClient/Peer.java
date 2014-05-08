package RUBTClient;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;

/**
 * @author Ben Green
 * @author Manuel Lopez
 * @author Christopher Rios
 */

/**
 *  Peer object handles all communication between client and peer
 */
public class Peer extends Thread {
	
	private String  			ip;
	private int 				port;				
	private byte[] 				peer_id;			
	
	private Socket 				peerSocket;			
	private DataInputStream 	peerInputStream;	
	private DataOutputStream	peerOutputStream;	
	
	private boolean 			choked; 			
	private boolean 			choking; 	
	private boolean 			incoming;
	private boolean			 	connected;			
	private boolean 			interested;
	private boolean 			remote_interested;
	
	private byte[] 				response;
	private byte[] 				bitfield;
	
	
	private Date 				last_sent;
	private boolean				first_sent;  
	private RUBTClient 			client;
	private MessageTask 		message;
	
	private int 				last_requested_piece; 
	
	protected double			sent_bps;
	protected double			sent_bytes;
	protected double			received_bps;
	protected double 			received_bytes;
	
	private Timer 				sendTimer; 		
	private SendTimerTask		sendTask;
	
	private Timer					performanceTimer;
	private PerformanceTimerTask	performanceTask;

	/**
	 * Usual constructor of Peer, when we create and connect to a peer first
	 * @param ip ip of the peer
	 * @param peer_id id of the peer
	 * @param port port the peer is on
	 */
	public Peer(String ip, byte[] peer_id, Integer port) {//TODO take out timers
		//super();
		this.ip = ip;
		this.port = port;
		this.peer_id = peer_id;

		sent_bytes = 0;
		sent_bytes = 0;
		received_bps = 0;
		received_bytes = 0;
		
		this.peerSocket = null;
		this.peerInputStream = null;
		this.peerOutputStream = null;
		
		this.choked = true;
		this.choking = true;
		this.incoming = false;
		this.connected = false;
		this.interested = false;
		this.first_sent = false;

		
		last_sent = new Date();

		sendTimer = new Timer("SEND2",true);
		performanceTimer = new Timer("PERF2",true);
	}
	
	/**
	 * Constructor of Peer when a remote peer connects to us first
	 * @param peerSocket  socket that the peer connected to
	 * @param peerInputStream Input stream for the socket
	 * @param peerOutputStream Output stream for the socket
	 */
	
	
	public Peer(Socket peerSocket, DataInputStream peerInputStream, DataOutputStream peerOutputStream) {
		this.peerSocket = peerSocket;
		try {
			this.peerSocket.setSoTimeout(125*1000);  // set a new timer for received message, 2 minutes 5 seconds
		} catch (SocketException e) {
			System.out.println("peer socket exception");
		}

		this.peerInputStream = peerInputStream;
		this.peerOutputStream = peerOutputStream;
		
		this.choked = true;
		this.choking = true;
		this.incoming = true;

		this.connected = false;
		this.interested = false;
		this.first_sent = false;

		last_sent = new Date();
		sendTimer = new Timer("SEND TIMER",true);
		performanceTimer = new Timer("PERF TIMER",true);
		
		sent_bps = 0;
		sent_bytes = 0;
		received_bps = 0;
		received_bytes = 0;
	}
	
	/**
	 * Timer task for sending info through a socket. If the timer runs out, the Peer sends a keep alive.
	 * 
	 */
	private static class SendTimerTask extends TimerTask{
		private Peer peer;
		public SendTimerTask(Peer peer){
			this.peer = peer;
		}
		
		public void run() {
			
			if(peer.connected&&(System.currentTimeMillis()-peer.getLastSent()>=(150*1000))){
				System.out.println("Sending a keep alive");
				byte[] keep_alive = {0,0,0,0};
				peer.sendMessage(keep_alive);
			}
		}
	}
	
	private static class PerformanceTimerTask extends TimerTask{
		
		private Peer peer;
		
		public PerformanceTimerTask(Peer peer){
			this.peer = peer;
		}
		
		public void run(){
			
			if (!peer.choked){
				peer.received_bps = ((0.65 * peer.received_bps) + (0.35 * peer.received_bytes))/2;
				if(peer.received_bps < 100) peer.received_bps = 0; 
				peer.received_bytes = 0;
			}
			if (!peer.choking){
				peer.sent_bps = ((0.65 * peer.sent_bps) + (0.35 * peer.sent_bytes))/2;
				if(peer.sent_bps < 100) peer.sent_bps = 0;
				peer.sent_bytes = 0;
			}
		}
	}
	
	/* 
	 * Overloaded run method for peer. Reads from the input stream until the connection is closed. Once it correctly reads
	 * a message, it sends that message up to the client to be processed. 
	 */
	public void run(){
		
		//System.out.println("peer id: " + peer_id + " Thread: "+ Thread.currentThread().getName());
		byte[] client_bitfield;
		byte[] handshake;
		
		System.out.println("checking peer: " + this.getPeer_id());
		if (this.client.alreadyConnected(this.peer_id)){
			System.out.println("Peer.java: error at already connected");
			//this.client.printPeers();
			if(incoming) this.closeConnections();
			return;
		}
		if(this.peerSocket == null  && !this.connectToPeer()){
			System.out.println("Peer.java error at connectToPeer");
			return;
		}
		Message current_message = new Message();

		if(!incoming){
			this.client.blocking_peers.add(this);
			this.sendMessage(current_message.handShake(this.client.torrentinfo.info_hash.array(), this.client.tracker.getUser_id()));
			handshake = this.handshake();
			if(handshake == null){
				return;
			}else if(!handshakeCheck(handshake)){
				this.closeConnections();
				return;
			}
			this.client.blocking_peers.remove(this);
		}
		client_bitfield = current_message.getBitFieldMessage(this.client.destfile.getMybitfield());

		this.sendMessage(client_bitfield);
		
		this.client.addPeerToList(this);
		System.out.println("Peer added: " + this.peer_id);

		performanceTask = new PerformanceTimerTask(this);
		this.performanceTimer.scheduleAtFixedRate(performanceTask, 2*1000 ,2 * 1000);
		
		while (connected){   //runs until we are no longer connected to the Peer
			try {
				Thread.sleep(1*50);
				try {
					if(peerInputStream.available() == 0){
						continue;     //means the peer hasn't written anything to the socket yet, I would like to find a better way to do this
					}
				}catch (IOException e){
					return;
				}
				
				int length_prefix = peerInputStream.readInt();
				if(length_prefix <=0){ //means this is a keep alive from the peer
					continue;
				}
				
				//System.out.println("length prefix " + length_prefix);
				response = new byte[length_prefix];
				peerInputStream.readFully(response);
				
				if(response[0] == Message.BITFIELD&&first_sent==false){ //if the id is a bitfield, set this peers bitfield to this byte array, as long as it is sent at the right time.
					System.out.println("setting the bitfield");
					bitfield = new byte[length_prefix-1];
					System.arraycopy(response,1,this.bitfield,0,bitfield.length);
				}
				message = new MessageTask(this, response);//makes the response into a  new message task, passes a peer as well
				client.addMessageTask(message); //puts the message in its clients  task queue and resets timers
			}catch (EOFException e) { 
					continue;
			}catch (Exception e){
				System.err.println("Peer.java run() : General Exception");
				e.printStackTrace();
			}
		}
		return;
	}
		
	/**connectToPeer() sets socket connections and input/output streams to the peer
	 * @return true if successful/false if failed
	 */
	public boolean connectToPeer(){
		//open sockets and input/output streams
		try {
			this.peerSocket = new Socket(ip, port);
			this.peerSocket.setSoTimeout(125*1000); //set the socket timeout for 2 minutes and 10 seconds
			this.peerOutputStream = new DataOutputStream(peerSocket.getOutputStream());  
			this.peerInputStream = new DataInputStream(peerSocket.getInputStream());
			connected = true;
		}catch (UnknownHostException e){
			System.err.println("Peer.java connectToPeer(): UnknownHostException");
			return false;
		}catch (IOException e){
			System.err.println("Peer.java connectToPeer(): IOException");
			return false;
		}
		sendTask = new SendTimerTask(this);
		sendTimer.scheduleAtFixedRate(sendTask, 0, 10*1000);
		last_sent.setTime(System.currentTimeMillis());
		
		return true;
	}
	
	/**
	 * Sends a message to the peer
	 * Source: Taken From Rob Moore's skeleton code in our Sakai Resources folder
	 * @param Message message to be sent by the peer
	 */
	public synchronized void sendMessage(byte[] Message){
		if (this.peerOutputStream == null){
			System.out.println("stream is null");
		}else {
			try {
				peerOutputStream.write(Message);
			} catch (IOException e) {
				System.err.println("Broken pipe, removing peer");
				client.removePeer(this);
			}
		}
		//TODO update our last sent field
		last_sent.setTime(System.currentTimeMillis());
	}
	
	/**
	 * Gets and passes on a remote peers handshake
	 * @return the remote peers handshake
	 */
	public byte[] handshake(){
		
		byte[] phandshake = new byte[68]; //Receives initial handshake
		try {
			peerInputStream.readFully(phandshake);
		}catch (EOFException e){  //Usually happens when the tracker is probing us
			closeConnections();
			return null;
		}catch (IOException e1){
			System.err.println("Peer.java Handshake Error. Disconnecting peer");  //there was an error reading the handshake, disconnects from the peer.
			this.client.blocking_peers.remove(this);
			closeConnections();
			return null;
		}
		return phandshake;
	}
	
	
	/**
	 * handshake compares the remote peers handshake to our infohash  and peer_id that we expect
	 * @param peer_handshake byte array of the peers handshake response
	 * @param Peer whose id will be tested with the contents of the handshake
	 */
	private boolean handshakeCheck(byte[] peer_handshake){	
		
		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.
		
		if (Arrays.equals(peer_infohash, this.client.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return true;
		}else {
			return false;
		}
	}
	
	/** closes input/outputstreams and socket connections 
	 */
	@SuppressWarnings("deprecation")
	public void closeConnections(){
		//close all streams
		try {
			if (peerInputStream != null)
				peerInputStream.close();
			if (peerOutputStream != null)
				peerOutputStream.close();
			if (peerSocket != null)
				peerSocket.close();
			
			connected = false;
			this.stop();
			cleanUp();
		}catch (IOException e){
			System.out.println("Peer.java: failed to close connections");
			e.printStackTrace();
			return;
		}
	}
	private void cleanUp(){
		
		if(sendTask != null) sendTask.cancel();
		if(sendTimer != null) sendTimer.cancel();
		if(performanceTimer != null) performanceTimer.cancel();
		if(performanceTask != null ) performanceTask.cancel();
	}
	
	/**wait() uses Thread.sleep to allow time for peer to respond to requests
	 * @param milliseconds interval before read
	 */
	
	/**
	 * @return the peers ip
	 */
	public String getIp() {
		return ip;
	}
	
	/**
	 * This method sets a peers connected field to true or false
	 * @param connected if the peer is connected or not
	 */
	public void setConnected(boolean connected){
		this.connected = connected;
	}


	/**
	 * @return the peer's peer_id
	 */
	public byte[] getPeer_id() {
		return peer_id;
	}


	/**
	 * @return port of the peer
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return returns if the peer is interested or not
	 */
	public boolean isInterested() {
		return interested;
	}
	
	/**
	 * @return if the peer is choked or not
	 */
	public boolean isChoked() {
		return choked;
	}

	/**
	 * @return if our client is choking the remote peer
	 */
	public boolean isChoking() {
		return choking;
	}

	/**
	 * This method sets if the peer is being choked or not
	 * @param state if the peer is choked or not
	 */
	public void setChoked(boolean state){
		this.choked = state;
	}
	
	/**
	 * @return the remote peers bitfield
	 */
	public byte[] getBitfield(){
		return this.bitfield;
	}
	
	/**
	 * This method sets the peer's client, and also initializes its bitfield to the correct length
	 * @param client client that is associated with this peer
	 */
	public void setClient(RUBTClient client){
		this.client = client;
		this.bitfield = new byte[client.getbitfield().length]; 
	}

	/**
	 * This method sets the local peers interested state
	 * @param state if the client as a whole is interested in the pieces a remote peer has
	 */
	public void setInterested(boolean state){
		interested = state;
	}
	
	/**
	 * Sets a peers id
	 * @param peer_id peer_id of this peer
	 */
	public void setPeer_id(byte[] peer_id){
		this.peer_id = peer_id;
	}
	
	
	/**
	 * @return if the remote peer has sent its first message after a handshake
	 */
	public boolean getFirstSent(){
		return first_sent;
	}
	boolean validPort = false;
	/**
	 * This method helps keep track of if a peer is sending a bitfield out of order
	 * @param first_sent if first message after handshake has been received yet
	 */
	public void setFirstSent(boolean first_sent){
		this.first_sent = first_sent;
	}
	
	/**
	 * Compares one peer to another by IP and ID
	 * @param peer peer to compare to current peer
	 * @return true if they have identical peer ids and IP addresses
	 */
	public boolean equals(Peer peer){
		return(this.ip == peer.getIp() && this.peer_id.equals(peer.getPeer_id()));
	}

	/**
	 * This method keeps track of the last time a message was sent
	 * by our peer.
	 * @return the time of the last sent message
	 */
	public long getLastSent(){
		return last_sent.getTime();
	}
	
	/**
	 * This method sets if we are choking the remote peer or not
	 * @param b if we are choking the remote peer
	 */
	public void setChoking(boolean choking) {
		this.choking = choking;
	}
	
	/**
	 * Records the last piece requested of the remote peer.
	 * @return piece index of the last requested piece
	 */
	public int getLastRequestedPiece(){
		return last_requested_piece;
	}
	
	/**
	 * Sets the last requested piece to a provided index.
	 * @param last last requested piece index
	 */
	public void setLastRequestedPiece(int last){
		this.last_requested_piece = last;
	}

	public void setRemoteInterested(boolean interested) {
		this.remote_interested = interested;
	}
}