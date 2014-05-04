package RUBTClient;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;

/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */


/** Peer object handles all communication between client and peer
 */
public class Peer extends Thread {
	
	
	
	private int 				port;				//port number to access the peer
	private String  			ip;          		//ip address of peer
	private String 				peer_id;			//identifying name of peer
	Socket 						peerSocket;			//socket connection to peer
	private DataOutputStream	peerOutputStream;	//OuputStream to peer for sending messages
	private DataInputStream 	peerInputStream;	//InputStream to peer for reading responses
	private boolean 			choked; 			//checks if we are being choked
	private boolean 			choking; 			//checks if we are choking the connected peer
	private boolean			 	connected;			//checks if peer is disconnected
	private boolean 			interested;
	private boolean 			remote_interested;
	private boolean 			incoming;
	
	private byte[] 				response;
	private byte[] 				bitfield;
	private Timer 				send_timer; 		//timers for sends
	private RUBTClient 			client;
	private MessageTask 		message;
	private boolean				first_sent;  //flag check that the first message after the handshake was sent. is used to make sure bitfield isnt sent in the wrong order. 
	private Date 				last_sent;
	private Timer				performanceTimer;
	private int 				last_requested_piece; 
	protected double 			recieved_bytes;
	protected double			recieved_bps;
	
	protected double			sent_bytes;
	protected double			sent_bps;
	
	/**
	 * Usual constructor of Peer, when we create and connect to a peer first
	 * @param ip ip of the peer
	 * @param peer_id id of the peer
	 * @param port port the peer is on
	 */
	public Peer(String ip, String peer_id, Integer port) {//TODO take out timers
		super();
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.peerSocket = null;
		this.peerInputStream = null;
		this.peerOutputStream = null;
		this.first_sent = false;
		this.choked = true;
		this.choking = true;
		this.connected = false;
		this.interested = false;
		
		this.incoming = false;
		
		send_timer = new Timer();
		performanceTimer = new Timer();
		last_sent = new Date();
		
		
		recieved_bytes = 0;
		recieved_bps = 0;
		sent_bytes = 0;
		sent_bytes = 0;
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
		this.first_sent = false;
		this.choked = true;
		this.choking = true;
		this.connected = false;
		this.interested = false;
		
		this.incoming = true;
		
		send_timer = new Timer();
		performanceTimer = new Timer();
		last_sent = new Date();
		recieved_bytes = 0;
		recieved_bps = 0;
		sent_bytes = 0;
		sent_bytes = 0;
	}
	
	
	/**
	 * @author Manuel Lopez
	 * @author Ben Greencrystal castles baptism lyrics
	 * @author Christopher Rios
	 * Timer task for sending info through a socket. If the timer runs out, the Peer sends a keep alive.
	 * 
	 */
	private static class SendTimerTask extends TimerTask{
		private Peer peer;
		public SendTimerTask(Peer peer)
		{
			this.peer = peer;
		}
		public void run() {
			// TODO Do something when the timer is up
			//System.out.println("send timer is up");
			if(peer.connected&&(System.currentTimeMillis()-peer.getLastSent()>=(150*1000)))
			{
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
				peer.recieved_bps = ((0.65 * peer.recieved_bps) + (0.35 * peer.recieved_bytes))/2;
				if(peer.recieved_bps < 100) peer.recieved_bps = 0; 
				peer.recieved_bytes = 0;
			}
			if (!peer.choking){
				peer.sent_bps = ((0.65 * peer.sent_bps) + (0.35 * peer.sent_bytes))/2;
				if(peer.sent_bps < 100) peer.sent_bps = 0;
				peer.sent_bytes = 0;
			}
			//System.out.println(peer.peer_id + " recieved bps: " + peer.recieved_bps);
			//System.out.println(peer.peer_id + " sent bsp: " + peer.sent_bps);
		}
	}
	
	/* 
	 * Overloaded run method for peer. Reads from the input stream until the connection is closed. Once it correctly reads
	 * a message, it sends that message up to the client to be processed. 
	 */
	public void run(){
		
		
		byte[] client_bitfield;
		byte[] handshake;
		
		System.out.println("checking peer: " + this.getPeer_id());
		if (this.client.alreadyConnected(this.peer_id)){
			System.out.println("Peer.java: error at already connected");
			this.client.printPeers();
			return;
		}
		if(this.peerSocket == null  && !this.connectToPeer()){
			System.out.println("Peer.java error at connectToPeer");
			return;
		}
		Message current_message = new Message();

		if(!incoming){
			this.sendMessage(current_message.handShake(this.client.torrentinfo.info_hash.array(), this.client.tracker.getUser_id()));
			handshake = this.handshake();
			if(handshake == null){
				return;
			}else if(!handshakeCheck(handshake)){
				this.closeConnections();
				return;
			}
		}
		client_bitfield = current_message.getBitFieldMessage(this.client.destfile.getMybitfield());

		this.sendMessage(client_bitfield);
		
		this.client.addPeerToList(this);
		System.out.println("Peer added: " + this.peer_id);

		//PerformanceTimerTask performanceTimerTask = new PerformanceTimerTask()
		this.performanceTimer.scheduleAtFixedRate(new PerformanceTimerTask(this), 2*1000 ,2 * 1000);
		
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
				if(length_prefix==0){ //means this is a keep alive from the peer
					continue;
				}
				
				if(length_prefix<0){  //bad error, sometimes we would read a large negative number.
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
		performanceTimer.cancel();
		send_timer.cancel();
		System.out.println("ending peer: " + peer_id);
		return;
	}
		
	/**connectToPeer() sets socket connections and input/output streams to the peer
	 * @return true if successful/false if failed
	 */
	public boolean connectToPeer(){
		//open sockets and input/output streams
		try {
			this.peerSocket = new Socket(ip, port);
			//this.peerSocket.setSoTimeout(125*1000); //set the socket timeout for 2 minutes and 10 seconds			
			this.peerSocket.setSoTimeout(10*1000); //set the socket timeout for 2 minutes and 10 seconds
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
		send_timer.scheduleAtFixedRate(new SendTimerTask(this), 0, 10*1000);
		last_sent.setTime(System.currentTimeMillis());
		return true;
	}
	
	/**
	 * Sends a message to the peer
	 * Source: Taken From Rob Moore's skeleton code in our Sakai Resources folder
	 * @param Message message to be sent by the peer
	 * @throws IOException 
	 */
	public synchronized void sendMessage(byte[] Message){
		if (this.peerOutputStream == null){
			System.out.println("stream is null");
		}else {
			//System.out.println("sending a message");
			try {
				peerOutputStream.write(Message);
			} catch (IOException e) {
				System.err.println("Broken pipe, removing peer");
				client.removePeer(this);
			}
		}
		//TODO update out last sent field
		last_sent.setTime(System.currentTimeMillis());
		
	}
	
	/**
	 * This method Gets and passes on a remote peers handshake
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
		
//		if(!(Arrays.equals(peer.getPeer_id().getBytes(),peer_id))){  //fails if the id in the hash doenst match the expected id.
//				System.err.println("Peer id didnt match");
//				return false;
//		}
		if (Arrays.equals(peer_infohash, this.client.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return true;
		}else {
			return false;
		}
	}
	
	
	/** closes input/outputstreams and socket connections 
	 */
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
			//send_timer.cancel();  
		}catch (IOException e){
			System.out.println("Peer.java: failed to close connections");
			e.printStackTrace();
			return;
		}
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
	public String getPeer_id() {
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
	 * This method sets if the remote peer is interested in the pieces that our client has or not
	 * @param state if the remote peer is interested or not
	 */
	public void setRemoteInterested(boolean state){
		this.remote_interested = state;
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
	
	public void setPeer_id(String peer_id){
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
	 * @param peer peer to compare
	 * @return true if they are equal, false otherwise
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
	public void setChoking(boolean b) {
		this.choking = b;
	}
	/**
	 * Records the last piece requested of the remote peer.
	 * @return piece index of the last requested piece
	 */
	public int getLastRequestedPiece()
	{
		return last_requested_piece;
	}
	
	/**
	 * Sets the last requested piece to a provided index.
	 * @param last last requested piece index
	 */
	public void setLastRequestedPiece(int last)
	{
		this.last_requested_piece = last;
	}
}