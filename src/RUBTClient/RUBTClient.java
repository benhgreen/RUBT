package RUBTClient;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;



import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;

/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */
public class RUBTClient extends Thread{
	
	private int port = 6881;					//Port that the client will be listening for connections
	public int uploaded;						//number of bytes written to other 
	private final TorrentInfo torrentinfo;		//torrent object extracted by destfile
	private boolean keepRunning = true;			//event loop flag for main client thread
	private Tracker tracker;					//tracker object that manages communication with tracker
	private DestFile destfile;					//object in change of managing client bitfield and file I/O
	private final int max_request = 16384;		//maximum number of bytes allowed to be requested of a peer
	private volatile int   peers_unchoked = 0;
	private final LinkedBlockingQueue<MessageTask> tasks = new LinkedBlockingQueue<MessageTask>();   //MessageTask queue that client reads form in event loop
	private final List<Peer> peers = Collections.synchronizedList(new LinkedList<Peer>());			 //List of peers currently connected to client
	
	public ExecutorService workers = Executors.newCachedThreadPool();	//thread pool of worker threads that spawn to manage MessageTasks
	private final Timer trackerTimer = new Timer();						//timertask object that handles timed tracker announcements
	private final Timer optimisticTimer = new Timer();
	
	/**
	 * RUBTClient constructor
	 * @param destfile object manages file I/O and bitfield manipulation 
	 */
	public RUBTClient(DestFile destfile){
		this.destfile = destfile;
		this.torrentinfo = destfile.getTorrentinfo();
		this.tracker = new Tracker(this.torrentinfo.file_length);
	}
	
	public static void main(String[] args){
		
		//verifies command line arguments
		if(args.length != 2){
			System.err.println("Usage: java RUBT <torrent> <destination>");
			System.exit(0);
		}
		
		//get user input arguments
		String torrentname = args[0];
		String destination = args[1];
		//prepare file stream
		FileInputStream fileInputStream = null;
		File torrent = new File(torrentname);
		
		//extract torrent info from file specified in args
		TorrentInfo torrentinfo = null;
		
		byte[] torrentbytes = new byte[(int)torrent.length()];
		
		try{
			fileInputStream = new FileInputStream(torrent);
			fileInputStream.read(torrentbytes);
			fileInputStream.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try{
			torrentinfo = new TorrentInfo(torrentbytes);
		}catch (BencodingException e) {
			System.err.println("Beencoding Exception!");
			e.printStackTrace();
		}
		
		DestFile destfile = new DestFile(torrentinfo, destination);
		
		File mp3 = new File(destination);
		if(mp3.exists()){
			destfile.checkExistingFile();
		}else{
			destfile.initializeRAF();
		}
		//builds bitfield based off of local mp3 file
		destfile.renewBitfield(); 
		RUBTClient client = new RUBTClient(destfile); 
		//set client field of destfile to current client for later tracker util
		destfile.setClient(client);		
		
		//spawns main client thread
		client.start();			
	}
	
	
	/**
	 * TimerTask that handles the periodic tracker announcements on set intervals
	 */
	private static class TrackerAnnounceTask extends TimerTask {
		
		private final RUBTClient client; //client that the TimerTask belongs to which is used for access to tracker
		
		public TrackerAnnounceTask(final RUBTClient client){
			this.client = client;
		}
		
		public void run(){
			//get list of peers from periodic tracker announcement (null for no event)
			Response peer_list = this.client.contactTracker(null);
			List<Peer> newPeers = peer_list.getValidPeers();
			
			//add peers to list of connected client peers and resets timer for next announcement 
			this.client.addPeers(newPeers);  
			System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@    tracker timer     @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			System.out.println("new interval: " + this.client.tracker.getInterval());
			int interval = this.client.tracker.getInterval();
			if(interval > 180  || interval < 60){
				interval = 180;
			}
			this.client.trackerTimer.schedule(new TrackerAnnounceTask(this.client), interval * 1000);
		}
	}
	private static class OptimisticChokeTask extends TimerTask
	{
		private final RUBTClient client;
		public OptimisticChokeTask(final RUBTClient client)
		{
			this.client = client;
		}
		public void run()
		{
			for(Peer peer: client.peers)
			{
				if(peer.isChoking()==false)
				{
					System.out.println("Peer:"+ peer.getPeer_id()+ " is unchoked");
				}
				else
				{
					System.out.println("Peer:"+ peer.getPeer_id()+ " is choked");
				}
			}
			System.out.println("@@@@@@done@@@@@@");
		}
	}
	public void run(){
		
		System.out.println("incomplete: " + this.destfile.incomplete);
		
		//starts up listener for user quit input
		//handles unexpected System.exit(0) by ending threads/sending stopped event to tracker

		ShutdownHook sample = new ShutdownHook(this);
		sample.attachShutdownHook();
		startInputListener();
		
		final Message message = new Message();
		//sends started event and then takes the list of valid peers
		//to add them to the current list of running peers
		Response peer_list = contactTracker("started");
		//takes care of handshake verification and populates queue with initial tasks
		addPeers(peer_list.getValidPeers());
		{	
			//set tracer interval based on initial tracker response and set timer
			int interval = peer_list.interval;
			if(interval <= 0){
				interval = 60;
			}
			System.out.println("tracker announce interval: " + interval);
			this.tracker.setInterval(interval);
			this.trackerTimer.schedule(new TrackerAnnounceTask(this), 5 * 1000);
		}
		
		/**
		 * Main client thread event loop that runs until flag is set by user
		 * inputting "quit". Client reads from a queue of Message tasks and spawns
		 * threads to deal with each task based on the message id
		 */
		
		while(keepRunning){

			try{
				//pull task from the queue. block until MessageTask is recieved
				final MessageTask task = this.tasks.take();
				
				//new thread from ExecutorService. Spawns one for every tasks and gets recycled when done
				this.workers.execute(new Runnable() {
					public void run(){
						//extract message and peer from MessageTask wrapper
						byte[] msg = task.getMessage();
						Peer peer = task.getPeer();
						//catches the case of leftover task from disconnected peer
						if(peer!= null && !peers.contains(peer)){
							return;
						}
						switch(msg[0]){  

							case Message.CHOKE:	//We were choked. Set peer status to choked
								peer.setChoked(true);
								break;
							case Message.UNCHOKE:  //We were unchoked. Set peer status to unchoked and find out what piece to request
								peer.setChoked(false);
								chooseAndRequestPiece(peer);
								break;			
							case Message.INTERESTED: //Peer is interested in our data. Unchoke them
								System.out.println("Peer " + peer.getPeer_id() + " sent interested");
								peer.setRemoteInterested(true);
							try {
								if(peers_unchoked<3) //if we have less then 3 peers unchoked, we unchoke another peer
								{
								peer.sendMessage(message.getUnchoke());   
								peer.setChoking(false);
								incrementUnchoked();   //  increment the amount of peers we have unchoked
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							}
								break;
							case Message.HAVE:  //Peer has new piece. Update their bitfield and check conditions for requesting their piece
								if (peer.isChoked()){
									byte[] piece_bytes = new byte[4];
									System.arraycopy(msg, 1, piece_bytes, 0, 4); //gets the piece number bytes from the piece message
									int piece = ByteBuffer.wrap(piece_bytes).getInt();
		
									destfile.manualMod(peer.getBitfield(), piece, true);
									if(destfile.firstNewPiece(peer.getBitfield()) != -1 && !peer.getFirstSent()){
										peer.setInterested(true);
										peer.setFirstSent(true);
										try{
											peer.sendMessage(message.getInterested());
										}catch(IOException e){
											System.out.println("peer send error");
										}
									}
								}
								break;
							case Message.BITFIELD:  //Peer sent bitfield. Update peers bitfield and disconnect if not sent at right time
								if(!peer.getFirstSent()){
									peer.setFirstSent(true);
								}else{
									peer.setConnected(false);
									removePeer(peer);
									return;
								}
								//check if they have a piece we want. If so, request it
								if (destfile.firstNewPiece(peer.getBitfield()) != -1){ 
									peer.setInterested(true);
									try{
										peer.sendMessage(message.getInterested());
									}catch(IOException e){
										System.out.println("peer send error");
									}
								}
								break;
							case Message.REQUEST:	//Peer wants our piece. Check choked state and send chunk
								if(!isValidRequest(msg,peer)||peer.isChoking())  //if the request is not valid or we are currently choking the peer, we disconnect the peer
								{
									peer.setConnected(false);
									removePeer(peer);
								}
								break;
							case Message.PIECE:		//check where we are in the piece, then request the next part i think.
								if(!peer.isInterested()){ //they sent us a piece when we werent interested in what they have
									peer.setConnected(false);
									removePeer(peer);
								}else {
									getNextBlock(msg,peer);
								}
								break;
								
							case Message.QUIT:	 	//User has input quit command. Disconnect from all peers and set loop flag to false to exit
								quitClient();
								break;
						}
					}
				});
			}catch (InterruptedException ie){
				System.err.println("caught interrupt. continuing anyway");
			}
		}
		//Shutdown hook catches exit and cleans up threads/connecitons
		System.exit(0);
	}
	
	/**
	 * addPeers takes a list of new peers to be added to the list of currently connected peers
	 * checks peers are already connected. If not, attempt to connect, verify handshake, and accept bitfield 
	 * @param newPeers List of Peers to be connected to
	 */
	public void addPeers(List<Peer> newPeers){
		
		byte[] bitfield;
		byte[] handshake;
		for(Peer peer: newPeers){

			System.out.println("checking peer: " + peer.getPeer_id());
			if (alreadyConnected(peer.getPeer_id()) || !peer.connectToPeer()){
				continue;
			}
			Message current_message = new Message();
			try {
				peer.sendMessage(current_message.handShake(torrentinfo.info_hash.array(), tracker.getUser_id()));
				handshake = peer.handshake();
				if(!handshakeCheck(handshake,peer)){
					peer.closeConnections();
					continue;
				}
			}catch (IOException e) {
				System.err.println("random  IO ex");
				continue;
			}
			peer.setClient(this);
			bitfield = current_message.getBitFieldMessage(destfile.getMybitfield());

			try {
				peer.sendMessage(bitfield);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			peers.add(peer);
			peer.start();
		}
		optimisticTimer.scheduleAtFixedRate(new OptimisticChokeTask(this), 1000, 10*1000);
	}
	
	
	/**
	 * Checks if peer_id is in the list of currently connected peers
	 * @param peer_id that will be looked for in list of currently connected peers
	 * @return true if already connected, false if not
	 */
	public boolean alreadyConnected(String peer_id){
		
		for(Peer peer: this.peers){
			if(peer.getPeer_id().equals(peer_id)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * handshake compares the remote peers handshake to our infohash  and peer_id that we expect
	 * @param peer_handshake byte array of the peers handshake response
	 * @param Peer whose id will be tested with the contents of the handshake
	 */
	private boolean handshakeCheck(byte[] peer_handshake,Peer peer){	
		
		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.
		
//		if(!(Arrays.equals(peer.getPeer_id().getBytes(),peer_id))){  //fails if the id in the hash doenst match the expected id.
//				System.err.println("Peer id didnt match");
//				return false;
//		}
		if (Arrays.equals(peer_infohash, this.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return true;
		}else {
			return false;
		}
	}
	/**
	 * This method checks the handshake check against the information that we know about the peer and the infohash
	 * @param peer_handshake the remote peers handshake
	 * @return returns the peer_id of this handshake
	 */
	private byte[] handshakeCheck(byte[] peer_handshake){	
		
		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.
		
		if (Arrays.equals(peer_infohash, this.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return peer_id;
		}else {
			return null;
		}
	}
	
	/**
	 * This method is called by the peer to give messages to the client's queue
	 * @param task to be sent to the clients queue
	 */
	public synchronized  void addMessageTask(MessageTask task){
		tasks.add(task);
	}
	
	/**
	 * This method picks which piece to be requested from a remote peer
	 * @param peer Peer who we are attempting to request a piece from
	 */
	public synchronized void chooseAndRequestPiece(final Peer peer){
		int current_piece = 0;
	   	int offset_counter = 0;
	   	int pieces = torrentinfo.piece_length/max_request;
	   	Message current_message = new Message();
	   	byte[] request_message;
		if (!peer.isChoked() && peer.isInterested()){ //if our peer is unchoked and we are interested
			
			//returns -1 when peer has no piece that we need
			current_piece = destfile.firstNewPiece(peer.getBitfield());
			if (current_piece == -1){
				peer.setInterested(false);
				return;
			}
			
	 	   	offset_counter = destfile.pieces[current_piece].getOffset();
			if(offset_counter != -1){
				offset_counter += max_request;
			}else {
				offset_counter = 0;
			}
	   		request_message = current_message.request(current_piece, offset_counter, max_request);
	   		try {
	   			System.out.println("requesting piece "+current_piece);
				peer.sendMessage(request_message);
			}catch (IOException e) {
				System.err.println("Error sending message to peer");
				return;
			}
	   	}
	}
	
	/**
	 * Takes a piece message from a peer, saves it to our file and figures out what piece to request next
	 * @param block
	 * @param peer
	 */
	
	private synchronized void addChunk(int piece, int offset,byte[] data){
		byte[] chunk = new byte[data.length-9];
		System.arraycopy(data, 9, chunk, 0, data.length-9);
		destfile.pieces[piece].assemble(chunk,offset);
	}
	/**
	 * Gets the next block of a piece
	 * @param block block of data received
	 * @param peer peer received from 
	 */
	private void getNextBlock(byte[] block,Peer peer){
		Message message = new Message();
		byte[] request;
		byte[] piece_bytes = new byte[4];
		byte[] offset_bytes = new byte[4];
		int small_request;
		System.arraycopy(block, 5, offset_bytes, 0, 4); //gets the offset bytes from the piece message
		System.arraycopy(block, 1, piece_bytes, 0, 4); //gets the piece number bytes from the piece message
		int offset = ByteBuffer.wrap(offset_bytes).getInt();  //wraps the offset bytes in a buffer and converts them into an int
		int piece = ByteBuffer.wrap(piece_bytes).getInt();
		addChunk(piece,offset,block);  //places the chunk of data into a piece
		if ((piece == torrentinfo.file_length / torrentinfo.piece_length) && (offset + 2 * max_request > torrentinfo.file_length % torrentinfo.piece_length)){//checks if we are at the last chunk of the last piece
			small_request = (torrentinfo.file_length%torrentinfo.piece_length)%max_request;
			
			if (small_request + offset == torrentinfo.file_length % torrentinfo.piece_length){//just got back the last chunk of the last piece
				if(destfile.addPiece(piece)){ //if our piece verifies, we send have messages to everyone
					System.out.println("Sending to all peers");
					this.uploaded += destfile.pieces[piece].data.length;
					//TODO send a have message to everybody?
					for(Peer all_peer: this.peers){
						try {
							//System.out.println("Sending a have");
							all_peer.sendMessage(message.getHaveMessage(piece_bytes));
						} catch (IOException e) {
							//System.out.println("Oh boy");
							e.printStackTrace();
						}
					}
					chooseAndRequestPiece(peer);
				}
				else{
					removePeer(peer);
				}
			}
			else {
				small_request = (torrentinfo.file_length%torrentinfo.piece_length) % max_request;
				request = message.request(piece, offset + max_request, small_request);
				if (peer.isChoked()){			//TODO i don't know how to handle starting up again if we get choked mid piece request
	   				System.out.println("got choked out");
	   				return;
	   			}else {
					try {
						peer.sendMessage(request);
					}catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		else if (offset + max_request == torrentinfo.piece_length){ 	//checks if we got the last chunk of a piece{
			if (destfile.addPiece(piece)){
				this.uploaded += destfile.pieces[piece].data.length;
				for (Peer all_peer: this.peers){
					try {
						//System.out.println("Sending a have");
						all_peer.sendMessage(message.getHaveMessage(piece_bytes));
					} catch (IOException e) {
						System.err.println("Peer disconnected");
					}
				}
				
			}
			else {
				removePeer(peer);
			}
			chooseAndRequestPiece(peer); 		//figures out the next piece to request
		}else {
			if (peer.isChoked()){			//TODO i don't know how to handle starting up again if we get choked mid piece request
   				System.out.println("got choked out");
   				return;
   			}else {
				request = message.request(piece, offset + max_request, max_request);
				try {
					peer.sendMessage(request);
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * This method sends contacts tracker and sends it events
	 * @param event type of event to be sent to the tracker
	 * @return Response to be sent
	 */
	public Response contactTracker(String event){
		this.tracker.updateProgress(this.torrentinfo.file_length - this.destfile.incomplete, this.uploaded);
		this.tracker.constructURL(this.torrentinfo.announce_url.toString(), this.torrentinfo.info_hash, this.port);
		byte[] response_string = null;
		try{
			response_string = this.tracker.requestPeerList(event);
		}catch (Exception e){
			System.out.println("exception thrown requesting peer list from tracker");
			e.printStackTrace();

			if (event == null || event.equals("stopped")  || event.equals("completed") ){
				System.out.println("already downloading. Dont stop program");
			}else if (event.equals("started")){
				System.out.println("Havent started downloading yet so just quit");
				System.exit(0);
			}
		}
		
		if (response_string == null){
			System.out.println("null response");
			System.exit(0);
		}
		
		if(event != null && event.equals("completed")){
			System.out.println("\n  \n  ***************completed*************  \n \n ");
			System.out.println("incomplete: " + this.destfile.incomplete);
		}
		return (new Response(response_string));
	}
	
	/**
	 * Remove peer takes in a peer and removes/disconnects it from the list of active peers
	 * @param peer peer to be removed
	 */
	private void removePeer(Peer peer){
		if (peers.contains(peer)){
			System.out.println("closing connections for peer " + peer.getId());
			peer.closeConnections();
			peers.remove(peer);
		}
	}
	
	/**
	 * Check to see if we have been issued a valid request, if true composes and sends the piece data
	 * @param message request message in question
	 */
	private boolean isValidRequest(byte[] message,Peer peer){
		Message piece_message = new Message();
		byte[]	index_bytes= new byte[4];
		byte[]  begin_bytes = new byte[4];
		byte[]  length_bytes = new byte[4];
		byte[] piece;
		System.arraycopy(message, 1, index_bytes, 0, 4);
		System.arraycopy(message, 5, begin_bytes, 0, 4);
		System.arraycopy(message, 9, length_bytes, 0, 4);
		int index = ByteBuffer.wrap(index_bytes).getInt();  //wraps the offset bytes in a buffer and converts them into an int
		int begin = ByteBuffer.wrap(begin_bytes).getInt();
		int length = ByteBuffer.wrap(length_bytes).getInt();
		if((length>max_request||length<=0)||(index>destfile.pieces.length||index<0)||(begin<0||begin>torrentinfo.piece_length)){//checks if any of the fields in the request method are invalid
			return false;
		}
		piece = piece_message.getPieceMessage(destfile, index_bytes, length, begin_bytes);  //gets a piece message
		try {
			
			peer.sendMessage(piece);  //sends it off to peer to be uploaded through the socket
		} catch (IOException e) {
			System.out.println("Borked pipe");
			e.printStackTrace();
		}
		return true;
	}
	
	private void startInputListener(){
		this.workers.execute(new Runnable(){
			public void run(){
				Scanner scanner = new Scanner(System.in);
				while(true){
					if(scanner.nextLine().equals("quit")){
						Message quit_message = new Message();
						MessageTask quit_task = new MessageTask(null, quit_message.getQuitMessage());
						addMessageTask(quit_task);
						//System.out.println("sending quit message");
						break;
					}else{
						System.out.println("incorrect input. try typing \"quit\"");
					}
				}
			}
		});
	}
	/**
	 * Listens on a specific port for incoming connections and adds them to the list
	 * of currently connected peers
	 */
	/*
	private void startIncomingConnections(){
		final RUBTClient client = this;
		this.workers.execute(new Runnable(){
			public void run(){
				boolean validPort = false;
				ServerSocket listenSocket = null;
				
				while(port <= 6889 && !validPort){
					try {
						listenSocket = new ServerSocket(port);
						validPort = true;
					} catch (IOException e) {
						setPort(port+1);
					}
				}
				if(port >= 6890){
					System.exit(0);
				}
				while (true){
					try{
						if(listenSocket == null){
							System.exit(0);
						}
						Socket clientSocket = listenSocket.accept();
						DataInputStream input = new DataInputStream(clientSocket.getInputStream());
						DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
						
						Peer peer = new Peer(clientSocket, input, output);
						Message msg = new Message();
						byte[] handshake;
						byte[] peer_id;
						try {
							peer.sendMessage(msg.handShake(torrentinfo.info_hash.array(), tracker.getUser_id()));
							handshake = peer.handshake();
							if(handshake == null){
								continue;
							}
							peer_id = handshakeCheck(handshake);
							if(peer_id == null){
								peer.closeConnections();
								continue;
							}
							String peer_string = new String(peer_id);
							if(alreadyConnected(peer_string)){
								peer.closeConnections();
								continue;
							}else{
								peer.setClient(client);
								peers.add(peer);
								peer.start();
							}
						}catch (IOException e) {
							System.err.println("random  IO ex");
							continue;
						}
					}catch(IOException ioe){
						System.err.println("IOException while handling request");
					}
				}
			}
		});
		
	}
	*/
	/**
	 *Disconnects all currently connected peers
	 */
	public void closeAllConnections(){
		for(Peer peer: this.peers){
			peer.setConnected(false);
			peer.closeConnections();
		}
		System.out.println("All connections closed");
	}
	
	
	public byte[] getbitfield(){
		return this.destfile.getMybitfield();
	}
	
	
	public void setPort(int port){
		this.port = port;
	}
	
	
	/**
	 *sets flag so client thread can exit event loop 
	 */
	private synchronized void quitClient(){
		this.keepRunning = false;
	}
	
	private synchronized void incrementUnchoked()
	{
		peers_unchoked++;
	}
	
	
}
