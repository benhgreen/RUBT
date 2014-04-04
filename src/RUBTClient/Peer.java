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
	private boolean 			connected;			//checks if peer is disconnected
	private boolean 			interested;
	private boolean 			remote_interested;
	private byte[] 				response;
	private byte[] 				bitfield;
	private Timer 				send_timer; 		//timers for sends
	private Timer 				receive_timer; 		//timers for receives
	private RUBTClient 			client;
	private MessageTask 		message;
	
	
	public Peer(String ip, String peer_id, Integer port) {
		super();
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.peerSocket = null;
		this.peerInputStream = null;
		this.peerOutputStream = null;
		
		this.choked = true;
		this.choking = true;
		this.connected = false;
		this.interested = false;
		
		send_timer = new Timer();
		receive_timer = new Timer();
	}
	
	private static class SendTimerTask extends TimerTask{
		@Override
		public void run() {
			// TODO Do something when the timer is up
			System.out.println("send timer is up");
		}
	}
	
	private static class ReceiveTimerTask extends TimerTask{
		@Override
		public void run() {
			// TODO Do something when timer is up
			System.out.println("recieve timer is up");
		}
	}
	
	public void run(){
		
		while(connected){
			try {
				Thread.sleep(1*1000);
				if(peerInputStream.available()==0){
					System.out.println("Nothing to read");
					continue;
				}
				
				int length_prefix = peerInputStream.readInt();
				if(length_prefix==0){
					System.out.println("keep alive");
					//TODO reset timers
					continue;
				}
				//System.out.println("length prefix"+length_prefix);
				response = new byte[length_prefix];
				//System.out.println("reponse length"+response.length);
			//	if(length_prefix==0){
				
			//	}
				
				peerInputStream.read(response,0,length_prefix);
				if(response[0] == Message.BITFIELD){      //if the id is a bitfield, set this peers bitfield to this byte array.
					System.out.println("setting the bitfield");
					bitfield = new byte[length_prefix-1];
					System.arraycopy(response,1,this.bitfield,0,bitfield.length);
				}
				message = new MessageTask(this, response);//makes the response into a  new message task, passes a peer as well
				client.addMessageTask(message); //puts the message in its clients  task queue
			
				receive_timer.cancel();  //cancels the current timer for messages
		        receive_timer = new Timer();
		        receive_timer.schedule(new ReceiveTimerTask(), 120*1000);  //resets it for 2 minutes from last sent				//TODO send message to RUBT client
			}catch (EOFException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.err.println("EOF");
				//closeConnections();
			}catch (Exception e){
				e.printStackTrace();
				System.err.println("Other Exception");
			}
		}
		closeConnections();
	}
		
	/**connectToPeer() sets socket connections and input/output streams to the peer
	 * @return true if successful/false if failed
	 */
	public boolean connectToPeer(){
		//open sockets and input/output streams
		try{
			this.peerSocket = new Socket(ip, port);
			this.peerSocket.setSoTimeout(125*1000);
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
		send_timer.schedule(new SendTimerTask(),120*1000 );
		receive_timer.schedule(new ReceiveTimerTask(),120*1000);
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
		this.remote_interested=state;
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
		send_timer.cancel();  //cancels the current timer for sent messages
        send_timer = new Timer();
        send_timer.schedule(new SendTimerTask(), 120*1000);  //resets it for 2 minutes from last sent
        //System.out.println("peer thread send message: " + Thread.currentThread());
		
	}
	
	public byte[] getBitfield(){
		return this.bitfield;
	}
	
	public synchronized byte[] handshake(){

		byte[] phandshake = new byte[68]; //Receives initial handshake
		try{
			peerInputStream.readFully(phandshake);
		}catch (IOException e1){
			System.err.println("Handshake Error");
			e1.printStackTrace();
			closeConnections();
		}
		return phandshake;
	}
	
	public void setClient(RUBTClient client){
		this.client=client;
	}

	public void setInterested(boolean state){
		interested = state;
	}
	
	/**handshakePeer() sends the handshake message and reads the peers handshake and bitfield
	 * @param handshake
	 * @return 1 if successful/0 if failed
	 */
//	public int handshakePeer(byte[] handshake,byte[] info_hash){
//		
//		byte[] response = new byte[68];
//		byte[] bitfield = new byte[6];
//		byte[] peer_infohash = new byte[20];
//		//sends handshake message and reads response
//		try{
//			peerOutputStream.write(handshake);
//			peerOutputStream.flush();
//			wait(1000);
//			peerInputStream.read(response);
//			
//			wait(1000);
//			peerInputStream.read(bitfield);
//			peerOutputStream.flush();
//		}catch(IOException e){
//			return 0;
//		}
//		//captures peer's infohash to check against the torrent file's
//		System.arraycopy(response,28,peer_infohash,0,20);
//		//verify info hash
//		if (Arrays.equals(info_hash , peer_infohash))
//		{
//			return 1;
//		}
//		else
//		{
//			return -1;
//		}
//	}
//	
//	/**sendInterested() sends the interested message and reads the unchoke response
//	 * @param interested byte array from message object
//	 * @return int 1 if success/0 if failure
//	 */
//	public int sendInterested(byte[] interested){
//		
//		byte[] unchoke = new byte[5];
//		
//		//sends interested message and reads the unchoke message
//		try{
//			peerOutputStream.write(interested);	
//			wait(1000);
//			peerInputStream.read(unchoke);
//			peerOutputStream.flush();
//			
//			if(checkUnchoked(unchoke)==0){
//				return -1;
//			}
//			
//		}catch(IOException e){
//			return 0;
//		}
//		return 1;
//	}
//	public int checkUnchoked(byte[] response){
//		for(int timer = 0; timer < 120; timer ++){
//			if(response[4]==1){
//				return 1;
//			}
//			
//			wait(1000);
//			try{peerInputStream.read(response);}
//			catch(IOException e){return 0;};
//		}
//		return 0;
//	}
//	
//	
//	/**getChunk() is a downloadPieces helper method that downloads an individual chunk
//	 * @param request message contructed from message object
//	 * @param size of chunk determined by downloadPieces
//	 * @return byte[] of the request response
//	 */
//	public byte[] getChunk(byte[] request, int size){
//		byte[] data_chunk = new byte[size];
//		//sends request and reads response
//		try{
//			peerOutputStream.write(request);
//			wait(1000);
//			peerInputStream.read(data_chunk);
//			peerOutputStream.flush();
//
//		}catch(IOException e){
//			return null;
//		}
//		if(data_chunk[4] == 0){
//			if(checkUnchoked(data_chunk) == 0){
//				System.out.println("got choked out");
//				return null;
//			}
//			getChunk(request, size);
//		}
//		downloaded = downloaded + (size-13);
//		System.out.println("total downloaded: "+downloaded+" bytes");
//		return data_chunk;
//	}
//	
//	/**downloadPieces() downloads all of the pieces available from the peer
//	 * @param file_size extracted from torrentinfo
//	 * @param piece_size extracted from torrentinfo
//	 * @param header_size extracted from message object
//	 * @return destFile object with pieces added
//	 */
//	public DestFile downloadPieces(int file_size, int piece_size, int header_size){
//		wait(1000);
//		byte[] request, data_chunk;
//		int index = 0;
//		int chunk_size = piece_size/2;
//		byte[] piece_filler1 = new byte[chunk_size];
//		byte[] piece_filler2 = new byte[chunk_size];
//		byte[] piece_filler_final = null;
//		Piece piece = null;
//		Message myMessage = null;
//		
//		while(this.downloaded < file_size){
//			myMessage = new Message();
//			//sends request for first chunk to peer
//			request = myMessage.request(index,0, chunk_size);
//			data_chunk = getChunk(request,chunk_size + header_size);
//			if(data_chunk == null){
//				return null;
//			}
//			//copies datachunk into placeholder w/o header for combining later
//			System.arraycopy(data_chunk, header_size, piece_filler1, 0, chunk_size);
//			
//			//handles the case of the last smallest chunk size
//			if((file_size - downloaded) < piece_size){
//				chunk_size = file_size-downloaded;
//				piece_filler2 = new byte[chunk_size];
//			}	
//			myMessage = new Message();
//			//sends request for second chunk to peer
//			request = myMessage.request(index,piece_size/2,chunk_size);
//			data_chunk = getChunk(request,chunk_size+header_size);
//			if(data_chunk==null){
//				return null;
//			}
//			System.arraycopy(data_chunk, header_size, piece_filler2, 0, chunk_size);
//			
//			//combines 2 chunks into one byte[],verifies and adds to destFile
//			piece_filler_final = new byte[piece_filler1.length + piece_filler2.length];
//			System.arraycopy(piece_filler1, 0, piece_filler_final,0,piece_filler1.length);
//			System.arraycopy(piece_filler2, 0, piece_filler_final, piece_filler1.length, piece_filler2.length);
//			piece = new Piece(piece_filler_final,index,0);
//			//verifies
//			if(!destfile.verify(piece)){
//				closeConnections();
//				return null;
//			}
//			destfile.addPiece(piece);
//			index++;
//		}
//		return destfile;
//	}
//	
}