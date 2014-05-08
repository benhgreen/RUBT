package RUBTClient;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
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
	
	private int port = 0;					//Port that the client will be listening for connections
	/**
	 * @field uploaded number of bytes written to other peers
	 */
	public int uploaded;					
	private int downloaded = 0;					//current sessions downloaded amount
	
	private int unchokeLimit = 3;
	final TorrentInfo torrentinfo;		//torrent object extracted by destfile
	public boolean keepRunning = true;			//event loop flag for main client thread
	public Tracker tracker;					//tracker object that manages communication with tracker
	public DestFile destfile;					//object in change of managing client bitfield and file I/O
	private final int max_request = 16384;		//maximum number of bytes allowed to be requested of a peer
	private volatile int   peers_unchoked = 0;
	private final LinkedBlockingQueue<MessageTask> tasks = new LinkedBlockingQueue<MessageTask>();   //MessageTask queue that client reads form in event loop
	final List<Peer> peers = Collections.synchronizedList(new LinkedList<Peer>());			 //List of peers currently connected to client
	final List<Peer> blocking_peers = Collections.synchronizedList(new LinkedList<Peer>());
	
	
	public ExecutorService workers = Executors.newCachedThreadPool();	//thread pool of worker threads that spawn to manage MessageTasks
	private final Timer trackerTimer = new Timer("trackerTimer",true);						//timertask object that handles timed tracker announcements
	private TrackerAnnounceTask trackerTask;

	private final Timer optimisticTimer = new Timer("optimisticTimer",true);
	private OptimisticChokeTask optimisticTask;
	
	protected ServerSocket serverSocket;
	protected Socket incomingSocket;
	protected DataInputStream listenInput;
	protected DataOutputStream listenOutput;
	protected ConnectionListener listener;
	
	private static boolean seeding;
	
	/**
	 * RUBTClient constructor
	 * @param destfile object manages file I/O and bitfield manipulation 
	 */
	public RUBTClient(DestFile destfile){
		this.destfile = destfile;
		this.torrentinfo = destfile.getTorrentinfo();
		this.tracker = new Tracker(this.torrentinfo.file_length);
	}
	
	/**
	 * @param args string array containing torrent file and name of the file we 
	 * download.
	 */
	public static void main(String[] args){
		
		//verifies command line arguments
		if(args.length != 2){
			System.err.println("Usage: java RUBT <torrent> <destination>");
			return;
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
		boolean file_complete=false;
		if(mp3.exists()){
			file_complete = destfile.checkExistingFile();
		}else{
			destfile.initializeRAF();
		}
		if(file_complete)
		{
			seeding=true;
		}
		//builds bitfield based off of local mp3 file
		
		destfile.renewBitfield(); 
		RUBTClient client = new RUBTClient(destfile); 
		//set client field of destfile to current client for later tracker util
		destfile.setClient(client);		
		System.out.println(torrentinfo.file_length);
		System.out.println(torrentinfo.piece_length);
		System.out.println(torrentinfo.file_length/torrentinfo.piece_length);
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
	
	private static class OptimisticChokeTask extends TimerTask{
		
		private final RUBTClient client;
		
		public OptimisticChokeTask(final RUBTClient client){
			this.client = client;
		}
		
		public void run(){
			
			List<Peer> choked_peers = new LinkedList<Peer>();
			
			double bytes_per_second = 0;
			double lowest_bps = Integer.MAX_VALUE;
			
			if(client.peers.size() < 1) return;
			
			Peer dropped_peer = client.peers.get(0);
			Peer picked_up_peer = null;
			Message message = new Message();
			
			boolean seeding = client.getSeeding();  //replace this with an actual call the the client field
			System.out.println("@@@@@@@@@@@@@@@@@@ Optomizely unchoking    @@@@@@@@@@@@@@@@@@@@@@@");
			System.out.println("seeding: " + seeding);
			for (Peer peer: client.peers){
				if (!peer.isChoking()){
					if(seeding){
						bytes_per_second = peer.sent_bps;
					}else{
						bytes_per_second = peer.recieved_bps;
					}
					
					System.out.println(peer.getPeer_id() + " performance: " + bytes_per_second + "bps");
					
					if(bytes_per_second <  lowest_bps){
						lowest_bps = bytes_per_second;
						dropped_peer = peer;
					}
				}
			}
			
			for (Peer peer: client.peers){
				if (peer.isChoking()){
					choked_peers.add(peer);
				}
			}
			
			dropped_peer.sendMessage(message.getChoke());
			dropped_peer.setChoking(true);
			client.decrementUnchoked();
			
			System.out.println("Peer: " + dropped_peer.getPeer_id() + " has been choked");
			
			Random randomGenerator = new Random();
			
			if(choked_peers.size() > 0){ 
				picked_up_peer = choked_peers.get(randomGenerator.nextInt(choked_peers.size()));
				picked_up_peer.sendMessage(message.getUnchoke());
				picked_up_peer.setChoking(false);
				client.incrementUnchoked();
				System.out.println("Peer: " + picked_up_peer.getPeer_id() + " has been unchoked");
			}
			System.out.println("downloaded "+ client.downloaded);
			System.out.println("@@@@@@@@@@@@@  Optimizely Time done  @@@@@@@@@@@@");
		}
	}
	
	public void run(){
		
		listener = new ConnectionListener(this);
		listener.start();
		
		//starts up listener for user quit input
		//handles unexpected quitting by ending threads,closing connections, sending stopped event to tracker

		ShutdownHook hook = new ShutdownHook(this);
		hook.attachShutdownHook();
		startInputListener();
		
		final Message message = new Message();
		//sends started event and then takes the list of valid peers
		//to add them to the current list of running peers
		
			
		//block until port is set by connection listener thread
		while(this.port == 0){
		}
		Response peer_list = contactTracker("started");
		//takes care of handshake verification and populates queue with initial tasks
		addPeers(peer_list.getValidPeers());
		{	
			//set tracer interval based on initial tracker response and set timer
			int interval = peer_list.interval;
			if(interval <= 0 || interval >= 180){
				interval = 120;
			}
			System.out.println("tracker announce interval: " + interval);
			this.tracker.setInterval(interval);
			trackerTask = new TrackerAnnounceTask(this);
			this.trackerTimer.schedule(trackerTask, interval * 1000);
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
						if (peer!= null && !peers.contains(peer)){
							return;
						}
						switch(msg[0]){  

							case Message.CHOKE:	//We were choked. Set peer status to choked
								peer.setChoked(true);
								clearProgress(peer);   //since we were choked, we clear all in progress downloads.
								break;
							case Message.UNCHOKE:  //We were unchoked. Set peer status to unchoked and find out what piece to request
								peer.setChoked(false);
								chooseAndRequestPiece(peer);
								break;			
							case Message.INTERESTED: //Peer is interested in our data. Unchoke them
								System.out.println("Peer " + peer.getPeer_id() + " sent interested");
								peer.setRemoteInterested(true);
								if (peers_unchoked < 3){ //if we have less then 3 peers unchoked, we unchoke another peer
									peer.sendMessage(message.getUnchoke());   
									peer.setChoking(false);
									incrementUnchoked();   //  increment the amount of peers we have unchoked
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
										peer.sendMessage(message.getInterested());
									}
									else{
										//destfile.myRarityMachine.updatePeer(peer.getPeer_id(),piece);
									}
								}
								break;
							case Message.BITFIELD:  //Peer sent bitfield. Update peers bitfield and disconnect if not sent at right time
								if (!peer.getFirstSent()){
									peer.setFirstSent(true);
									//destfile.myRarityMachine.addPeer(peer.getPeer_id(),peer.getBitfield());
								}else {
									peer.setConnected(false);
									removePeer(peer);
									return;
								}
								//check if they have a piece we want. If so, request it
								if (destfile.firstNewPiece(peer.getBitfield()) != -1){ 
									peer.setInterested(true);
									peer.sendMessage(message.getInterested());
								}
								break;
							case Message.REQUEST:	//Peer wants our piece. Check choked state and send chunk
								if(!isValidRequest(msg,peer)){  //if the request is not valid or we are currently choking the peer, we disconnect the peer
									if(!peer.isChoking()){
									peer.setConnected(false);
									System.out.println("REQUEST CLOSING CONNECTION");
									removePeer(peer);
									}
								}
								break;
							case Message.PIECE:		//check where we are in the piece, then request the next part i think.
								if (!peer.isInterested()){ //they sent us a piece when we werent interested in what they have
									peer.setConnected(false);
									removePeer(peer);
								}else {
									//increment recieved bytes
									peer.recieved_bytes += msg.length;
									getNextBlock(msg,peer);
								}
								break;
							case Message.QUIT:	 	//User has input quit command. Disconnect from all peers and set loop flag to false to exit
								endEventLoop();
								break;
						}
					}
				});
			}catch (InterruptedException ie){
				System.err.println("caught interrupt. continuing anyway");
			}
		}
		cleanUp();
		return;
	}
	
	/**
	 * addPeers takes a list of new peers to be added to the list of currently connected peers
	 * checks peers are already connected. If not, attempt to connect, verify handshake, and accept bitfield 
	 * @param newPeers List of Peers to be connected to
	 */
	public void addPeers(List<Peer> newPeers){
		
		for (Peer peer: newPeers){
			peer.setClient(this);
			peer.start();
		}
		optimisticTask = new OptimisticChokeTask(this);
		optimisticTimer.scheduleAtFixedRate(optimisticTask, 30 * 1000, 30 * 1000);
	}
	
	/**
	 * Checks if peer_id is in the list of currently connected peers
	 * @param peer_id that will be looked for in list of currently connected peers
	 * @return true if already connected, false if not
	 */
	public synchronized boolean alreadyConnected(byte[] peer_id){
		
		for (Peer peer: this.peers){
			if (Arrays.equals(peer.getPeer_id(),peer_id)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method checks the handshake check against the information that we know about the peer and the infohash
	 * @param peer_handshake the remote peers handshake
	 * @return returns the peer_id of this handshake
	 */
	public byte[] handshakeCheck(byte[] peer_handshake){	
		
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
			//current_piece = destfile.myRarityMachine.rarestPiece();
			if (current_piece == -1){
				peer.setInterested(false);
				return;
			}
			destfile.markInProgress(current_piece);  //marks this piece as in progress
			peer.setLastRequestedPiece(current_piece);
	 	   	offset_counter = destfile.pieces[current_piece].getOffset();
			if (offset_counter != -1){
				offset_counter += max_request;
			}else {
				offset_counter = 0;
			}
	   		request_message = current_message.request(current_piece, offset_counter, max_request);
	   		
	   		System.out.println("requesting piece "+current_piece);
			peer.sendMessage(request_message);
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
		
		//peer.recieved_bytes += block.length;

		
		addChunk(piece,offset,block);  //places the chunk of data into a piece
		if ((piece == torrentinfo.file_length / torrentinfo.piece_length) && (offset + 2 * max_request > torrentinfo.file_length % torrentinfo.piece_length)){//checks if we are at the last chunk of the last piece
			small_request = (torrentinfo.file_length%torrentinfo.piece_length)%max_request;
			
			if (small_request + offset == torrentinfo.file_length % torrentinfo.piece_length){//just got back the last chunk of the last piece
				if(destfile.addPiece(piece)){ //if our piece verifies, we send have messages to everyone
					this.downloaded += destfile.pieces[piece].data.length;
					System.out.println("Giving the last piece");
					
					System.out.println("Downloaded "+ downloaded);
					
					Peer[] array = peers.toArray(new Peer[peers.size()]);
					
					//for(Peer all_peer: this.peers){
						//System.out.println("Sending a have");
						//all_peer.sendMessage(message.getHaveMessage(piece_bytes));
					//}
					for(int i = 0; i < array.length; i++){
						array[i].sendMessage(message.getHaveMessage(piece_bytes));
					}
					
					chooseAndRequestPiece(peer);
				}
				else{
					removePeer(peer);
				}
			}else {
				small_request = (torrentinfo.file_length%torrentinfo.piece_length) % max_request;
				request = message.request(piece, offset + max_request, small_request);
				if (peer.isChoked()){			//TODO i don't know how to handle starting up again if we get choked mid piece request
	   				System.out.println("got choked out");
	   				return;
	   			}else {
					peer.sendMessage(request);
				}
			}
			
		}else if (offset + max_request == torrentinfo.piece_length){ 	//checks if we got the last chunk of a piece
			if (destfile.addPiece(piece)){
				this.downloaded += destfile.pieces[piece].data.length;
				for (Peer all_peer: this.peers){
					all_peer.sendMessage(message.getHaveMessage(piece_bytes));
				}
				chooseAndRequestPiece(peer); 		//figures out the next piece to request
			}
			else {
				removePeer(peer);
			}
		}else {
			if (peer.isChoked()){			
   				System.out.println("got choked out");
   				return;
   			}else {
				request = message.request(piece, offset + max_request, max_request);
				peer.sendMessage(request);
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
			System.err.println("exception thrown requesting peer list from tracker");
			e.printStackTrace();

			if (event == null || event.equals("stopped")  || event.equals("completed") ){
				System.err.println("RUBTClient contactTracker(): ");
			}else if (event.equals("started")){
				System.err.println("RUBTClient contactTracker(): failed to contact tracker on startup. quitting ...");
				quitClientLoop();
			}
		}
		
		if (response_string == null){
			System.err.println("RUBTClient contactTracker(): null response from tracker");
			quitClientLoop();
		}
		
		if(event != null && event.equals("completed")){
			System.out.println("\n  \n  ***************completed*************  \n \n ");
			System.out.println("incomplete: " + this.destfile.incomplete);
		}
		return (new Response(response_string));
	}
	
	/**
	 * Adds a peer to the client's peer list.
	 * @param peer peer to be added to our peer list
	 */
	
	public void printPeers(){
		System.out.println("Printing peers");
		for(Peer peer: this.peers){
			System.out.println("####  " + peer.getPeer_id() + " ###");
		}
	}
	public synchronized void addPeerToList(Peer peer){
		peers.add(peer);
	}
	          
	/**
	 * Remove peer takes in a peer and removes/disconnects it from the list of active peers
	 * @param peer peer to be removed
	 */
	public void removePeer(Peer peer){
		if (peers.contains(peer)){
			System.out.println("closing connections for peer " + peer.getPeer_id());
			clearProgress(peer);
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
		if((length > max_request || length <= 0)|| (index > destfile.pieces.length || index < 0) || (begin < 0 || begin > torrentinfo.piece_length)){
			//checks if any of the fields in the request method are invalid
			return false;
		}
		piece = piece_message.getPieceMessage(destfile, index_bytes, length, begin_bytes);  //gets a piece message
		peer.sent_bytes += piece.length;
		uploaded+=piece.length;
		peer.sendMessage(piece);  //sends it off to peer to be uploaded through the socket
		return true;
	}
	
	private void startInputListener(){
		this.workers.execute(new Runnable(){
			public void run(){
				System.out.println("Input listener " +  Thread.currentThread().getName() + " " + Thread.currentThread().getId());
				Scanner scanner = new Scanner(System.in);
				while(true){
					if(scanner.nextLine().equals("quit")){
						quitClientLoop();
						break;
					}else{
						System.out.println("incorrect input. try typing \"quit\"");
					}
				}
			}
		});
	}
	/**
	 *Disconnects all currently connected peers and listener socket
	 */
	public void closeAllConnections(){
		for(Peer peer: this.peers){
			peer.closeConnections();
		}
		for(Peer peer: this.blocking_peers){
			peer.closeConnections();
		}
		
		try {
			if(serverSocket != null) serverSocket.close();
			if(incomingSocket != null) incomingSocket.close();
			if(listenOutput != null) listenOutput.close();
			if(listenInput  != null) listenInput.close();
			
		} catch (IOException e) {
			System.err.println("RUBTClient.java closeAllConnections: error while shuting down listener port");
		}
		System.out.println("All connections closed");
	}
	
	public void quitClientLoop(){
		endEventLoop();
		Message quit_message = new Message();
		MessageTask quit_task = new MessageTask(null, quit_message.getQuitMessage());
		addMessageTask(quit_task);
	}
	
	public void cleanUp(){
		//keepRunning = false;
		closeAllConnections();
		contactTracker("stopped");
		
		optimisticTimer.cancel();
		optimisticTask.cancel();
		trackerTimer.cancel();
		trackerTask.cancel();
		
		this.workers.shutdownNow();
		System.out.println("Ending Client Program");
	}
	
	public byte[] getbitfield(){
		return this.destfile.getMybitfield();
	}
	
	public int getPort(){
		return this.port;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public boolean getSeeding(){
		return this.seeding;
	}
	
	public void setSeeding(){
		seeding = true;
	}
	
	/**
	 *sets flag so client thread can exit event loop 
	 */
	private synchronized void endEventLoop(){
		this.keepRunning = false;
	}
	
	private synchronized void incrementUnchoked(){
		peers_unchoked++;
	}
	
	private synchronized void decrementUnchoked(){
		peers_unchoked--;
	}
	
	private void clearProgress(Peer peer){
		destfile.clearProgress(peer.getLastRequestedPiece());
	}
}
