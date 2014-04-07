package RUBTClient;

import java.net.*;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.Arrays;
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
	private Socket 				peerSocket;			//socket connection to peer
	private DataOutputStream	peerOutputStream;	//OuputStream to peer for sending messages
	private DataInputStream 	peerInputStream;	//InputStream to peer for reading responses
	private boolean 			choked; 			//checks if we are being choked
	private boolean 			choking; 			//checks if we are choking the connected peer
	private boolean			 	connected;			//checks if peer is disconnected
	private boolean 			interested;
	private boolean 			remote_interested;
	private byte[] 				response;
	private byte[] 				bitfield;
	private Timer 				send_timer; 		//timers for sends
	private Timer 				receive_timer; 		//timers for receives
	private RUBTClient 			client;
	private MessageTask 		message;
	private boolean				first_sent;  //flag check that the first message after the handshake was sent. is used to make sure bitfield isnt sent in the wrong order. 
	
	public Peer(String ip, String peer_id, Integer port) {
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
		
		send_timer = new Timer();
		receive_timer = new Timer();
	}
	
	public Peer(Socket peerSocket, DataInputStream peerInputStream, DataOutputStream peerOutputStream) {
		this.peerSocket = peerSocket;
		try {
			this.peerSocket.setSoTimeout(130*1000);
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
		
		send_timer = new Timer();
		receive_timer = new Timer();
		
		send_timer.schedule(new SendTimerTask(this),115*1000 ); //set a new timer for sent messages, set for 1 minute 55 seconds.
		receive_timer.schedule(new ReceiveTimerTask(this),125*1000);
	}
	
	
	private static class SendTimerTask extends TimerTask{
		private Peer peer;
		private Message message;
		private byte[] keep_alive;
		public SendTimerTask(Peer peer)
		{
			this.peer = peer;
			Message message = new Message();
			keep_alive = new byte[4];
		}
		public void run() {
			// TODO Do something when the timer is up
			System.out.println("send timer is up");
			System.out.println(peer.connected);
			if(peer.connected)
			{
			message.getKeep_alive();
			keep_alive = message.getKeep_alive();
			System.out.println(Arrays.toString(keep_alive));
			try {
				System.out.println("sending a keep alive");
				peer.sendMessage(keep_alive);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			
		}
	}
	
	private static class ReceiveTimerTask extends TimerTask{
		private Peer peer;
		public ReceiveTimerTask(Peer peer)
		{
			this.peer = peer;
		}
		public void run() 
		{
			
			System.out.println("recieve timer is up,disconnecting peer");
			peer.closeConnections();//disconnects from peer when timer is up.
		}
	}
	
	public void run(){
		
		while(connected){
			try {
				Thread.sleep(1*100);
				try{
					if(peerInputStream.available()==0){
						//System.out.println("nothing yet");
						continue;     //means the peer hasn't written anything to the socket yet, I would like to find a better way to do this
					}
				}catch(IOException e){
					return;
				}
				
				int length_prefix = peerInputStream.readInt();
				if(length_prefix==0){ //means this is a keep alive from the peer
					receive_timer.cancel();  //cancels the current timer for messages
			        receive_timer = new Timer();
			        receive_timer.schedule(new ReceiveTimerTask(this), 125*1000);  //resets it for 2 minutes and 5 seconds from last sent, extra 5 to be generous	
					continue;
				}
				if(length_prefix<0)
				{
					continue;     //oh what has happened here
				}
				response = new byte[length_prefix];
				peerInputStream.read(response,0,length_prefix);
				if(response[0] == Message.BITFIELD){      //if the id is a bitfield, set this peers bitfield to this byte array.
					System.out.println("setting the bitfield");
					bitfield = new byte[length_prefix-1];
					System.arraycopy(response,1,this.bitfield,0,bitfield.length);
				}
				message = new MessageTask(this, response);//makes the response into a  new message task, passes a peer as well
				client.addMessageTask(message); //puts the message in its clients  task queue
				/*
				if(first_sent == false)//checks if first sent has been recorded, if not sets it to true.
				{
					first_sent = true;
				}
				*/
				receive_timer.cancel();
		        receive_timer = new Timer();
		        receive_timer.schedule(new ReceiveTimerTask(this), 125*1000);
			}catch (EOFException e) {
					continue;
			}catch (Exception e){
				System.err.println("Other Exception");
			}
		}
	}
		
	/**connectToPeer() sets socket connections and input/output streams to the peer
	 * @return true if successful/false if failed
	 */
	public boolean connectToPeer(){
		//open sockets and input/output streams
		try{
			this.peerSocket = new Socket(ip, port);
			this.peerSocket.setSoTimeout(130*1000); //set the socket timeout for 2 minutes and 30 seconds
			this.peerOutputStream = new DataOutputStream(peerSocket.getOutputStream());
			this.peerInputStream = new DataInputStream(peerSocket.getInputStream());
			connected = true;
		}catch (UnknownHostException e){
			System.err.println("UnknownHostException");
			return false;
		}catch (IOException e){
			System.err.println("IOException");
			return false;
		}
		send_timer.schedule(new SendTimerTask(this),115*1000 ); //set a new timer for sent messages, set for 1 minute 55 seconds.
		receive_timer.schedule(new ReceiveTimerTask(this),125*1000);
		return true;
	}
	
	
	/** closes input/outputstreams and socket connections 
	 */
	public void closeConnections(){
		try{
			peerInputStream.close();
			peerOutputStream.close();
			peerSocket.close();
			connected = false;
		}catch (IOException e){
			return;
		}
	}
	
	/**wait() uses Thread.sleep to allow time for peer to respond to requests
	 * @param milliseconds interval before read
	 */
	public void wait(int milliseconds){
		try{Thread.sleep(milliseconds);}
		catch(InterruptedException ex){Thread.currentThread().interrupt();}
	}
	public Socket getSocket(){
		return this.peerSocket;
	}
	public String getIp() {
		return ip;
	}
	
	public void setConnected(boolean connected){
		this.connected = connected;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPeer_id() {
		return peer_id;
	}

	public void setPeer_id(String peer_id) {
		this.peer_id = peer_id;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isInterested() {
		return interested;
	}
	
	public boolean isRemoteInterested() {
		return remote_interested;
	}
	
	public boolean isChoked() {
		return choked;
	}

	public boolean isChoking() {
		return choking;
	}

	public InputStream getPeerInputStream() {
		return peerInputStream;
	}

	public void setPeerInputStream(DataInputStream peerInputStream) {
		this.peerInputStream = peerInputStream;
	}
	public void setChoked(boolean state){
		this.choked = state;
	}
	
	public void setRemoteInterested(boolean state){
		this.remote_interested = state;
	}
	/**
	 * Sends a message to the peer
	 * Source: Taken From Rob Moore's skeleton code in our Sakai Resources folder
	 * @param Message message to be sent
	 * @throws IOException 
	 */
	public synchronized void sendMessage(byte[] Message) throws IOException {
		if(this.peerOutputStream == null){
			System.out.println("stream is null");
		}else {
			peerOutputStream.write(Message);
		}
		send_timer.cancel();  //cancels the current timer for sent messages, starts a new one
        send_timer = new Timer();
        send_timer.schedule(new SendTimerTask(this), 115*1000);
		
	}
	
	public byte[] getBitfield(){
		return this.bitfield;
	}
	
	public byte[] handshake(){
		
		System.out.println("waiting for handshake...");
		byte[] phandshake = new byte[68]; //Receives initial handshake
		try{
			peerInputStream.readFully(phandshake);
			System.out.println("handshake caught");
		}catch (EOFException e){
			System.out.println(("Tracker testing us....return null"));
			return null;
		}catch (IOException e1){
			System.err.println("Handshake Error");  //there was an error reading the handshake, disconnects from the peer.
			e1.printStackTrace();
			closeConnections();
		}
		return phandshake;
	}
	
	public void setClient(RUBTClient client){
		this.client = client;
		this.bitfield = new byte[client.getbitfield().length]; 
	}

	public void setInterested(boolean state){
		interested = state;
	}
	
	public boolean getFirstSent(){
		return first_sent;
	}
	
	public void setFirstSent(boolean first_sent){
		this.first_sent = first_sent;
	}
	
	public boolean equals(Peer peer){
		return(this.ip == peer.getIp() && this.peer_id.equals(peer.getPeer_id()));
	}

	public void setChoking(boolean b) {
		
		this.choking=b;
		
	}
}