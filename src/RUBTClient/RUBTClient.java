package RUBTClient;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
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
	
	private final int port = 6881;
	
	private int downloaded;
	
	private int uploaded;
	
	private final TorrentInfo torrentinfo;
	
	private final String destinationFile;
	
	private boolean keepRunning = true;
	
	private Tracker tracker;

	private DestFile destfile;
	
	private final LinkedBlockingQueue<MessageTask> tasks = new LinkedBlockingQueue<MessageTask>();

	private final List<Peer> peers = Collections.synchronizedList(new LinkedList<Peer>());
	
	private final int max_request = 16384;
	
	//private ExecutorService workers = Executors.newFixedThreadPool(10);
	private ExecutorService workers = Executors.newCachedThreadPool();
	
	private final Timer trackerTimer = new Timer();
	
	

	public RUBTClient(DestFile destfile){
		this.destfile = destfile;
		this.torrentinfo = destfile.getTorrentinfo();
		this.destinationFile = destfile.getFilename();
		this.tracker = new Tracker(this.torrentinfo.file_length);
	}
	
	public static void main(String[] args){
		
		//verifies command line args
		if(args.length != 2){
			System.err.println("Usage: java RUBT <torrent> <destination>");
			System.exit(0);
		}
		
		//get args
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
		
		DestFile destfile = new DestFile(torrentinfo);
		
		//checks if destination file exists. If so, user auth is required
		File mp3 = new File(torrentinfo.file_name);
		if(mp3.exists()){
			destfile.checkExistingFile();
		}else{
			//System.out.println("no files to see here");
			destfile.initializeRAF();
		}
		
		destfile.renewBitfield();
		//run thread
		RUBTClient client = new RUBTClient(destfile);
		client.start();
		
//		
//		
//		
//		//extracts tracker information from torrent info to build GetRequest
//		String announce_url = torrentinfo.announce_url.toString(); 
//		int port_num = 6881;
//		int file_length = torrentinfo.file_length;
//		ByteBuffer info_hash = torrentinfo.info_hash;
//		
//		Tracker trackerConnection = new Tracker();
//	
//		//build the get request url to send to the tracker
//		trackerConnection.constructURL(announce_url, info_hash, port_num, file_length);
//		byte[] response_string=null;
//		
//		//sends get request to tracker
//		try{
//			response_string = trackerConnection.requestPeerList();
//		}catch(Exception e){
//			System.out.println("exception thrown sending get request");
//			e.printStackTrace();
//			System.exit(0);
//		};
//
//		if(response_string==null){
//			System.out.println("null response");
//			System.exit(0);
//		}
//		//0 is peer_id, 1 is ip, 2 is port
//		//checks the list of peers from tracker
//		Response peer_list = new Response(response_string);
//		String[] peer_info;
//		if(peer_list==null){
//			System.out.println("no peers");
//			System.exit(0);
//		}
//		//extracts array of peer info from valid peer
//		peer_info = peer_list.getValidPeer();
//		Peer myPeer = null;
//		DestFile myDest = new DestFile(args[1], torrentinfo);
//		if(peer_info != null){
//			//peer_inf[0] = peer_id, peer_info[1] is ip, peer_info[2] is port
//			myPeer = new Peer(peer_info[1], peer_info[0], Integer.parseInt(peer_info[2]), myDest);
//		}else{
//			System.out.println("no valid peers");
//			System.exit(0);
//		}
//		
//		//establish connection to valid peer
//		if(myPeer.connectToPeer()==0){
//			System.out.println("failed connecting to peer");
//			System.exit(0);
//		}
//		//contruct new message for building peers output messages
//		Message myMessage = new Message();
//		
//		//send handshake
//		byte[] handshake=myMessage.handShake(info_hash.array(), trackerConnection.getUser_id());
//		System.out.println("sent handshake");
//		int handshake_status = myPeer.handshakePeer(handshake,info_hash.array());
//		if(handshake_status==0){
//			System.err.println("failed sending handshake");
//			System.exit(0);
//		}else if (handshake_status==-1){
//			System.err.println("info_hash from peer didn't match");
//			System.exit(0);
//		}
//
//		byte[] interested = myMessage.getInterested();
//		System.out.println("sent interested");
//		int interested_status = myPeer.sendInterested(interested); 
//		if(interested_status==0){
//			System.out.println("failed sending interested");
//			System.exit(0);
//		}else if(interested_status==-1){
//			
//		}
//		//send started event to tracker
//		try{trackerConnection.sendEvent("started", myPeer.getDownloaded());
//		}catch(Exception e)
//		{
//			System.err.println("send start event exception");
//			e.printStackTrace();
//			System.exit(0);
//			}
//		
//		DestFile resultFile = myPeer.downloadPieces(torrentinfo.file_length, torrentinfo.piece_length,13);
//		//data from peer failed to verify after hashing/corrupt download
//		if(resultFile == null){
//			System.out.println("corrupted download. quitting...");
//			try{trackerConnection.sendEvent("stopped", myPeer.getDownloaded());}
//			catch(Exception e){
//				System.err.println("send stopped event exception");
//				e.printStackTrace();
//				System.exit(0);
//			}
//		}
//		
//		//send completed event to tracker
//		try{trackerConnection.sendEvent("completed", myPeer.getDownloaded());}
//		catch(Exception e)
//				{
//				System.err.println("send completed event exception");
//				e.printStackTrace();
//				System.exit(0);
//				}
//		
//		//close all sockets and streams to peer
//		myPeer.closeConnections();
//		//close file stream
//		resultFile.close();
	}
	private static class TrackerAnnounceTask extends TimerTask {
		private final RUBTClient client;
		
		public TrackerAnnounceTask(final RUBTClient client){
			this.client = client;
		}
		
		public void run(){
			//updateprogress, contruct url, request peer list with null args, addpeers
			System.out.println("tracker announcement");
			Response peer_list = this.client.contactTracker(null);
			List<Peer> newPeers = peer_list.getValidPeers();
			
			this.client.addPeers(newPeers);
			this.client.trackerTimer.schedule(new TrackerAnnounceTask(this.client), this.client.tracker.getInterval() * 1000);
		}
	}
	
	public void run(){
		final Message message = new Message();
		Response peer_list = contactTracker("started");
		addPeers(peer_list.getValidPeers());
		{
			int interval = peer_list.interval;
			if(interval <= 0){
				interval = 60;
			}
			interval = 10;
			System.out.println("tracker announce interval: " + interval);
			this.tracker.setInterval(interval);
			this.trackerTimer.schedule(new TrackerAnnounceTask(this), interval * 1000);
		}
		

		while(this.keepRunning){

			try{
				final MessageTask task = this.tasks.take();
				this.workers.execute(new Runnable() {
					public void run(){
						byte[] msg = task.getMessage();
						Peer peer = task.getPeer();
						if(!peers.contains(peer)){
							System.out.println("leftover task from a disconnected peer");
							return;
						}
						switch(msg[0]){  

							
							case Message.CHOKE:
								System.out.println("Peer " +peer.getPeer_id() + " sent choked");
								peer.setChoked(true);
								break;
							case Message.UNCHOKE:
								System.out.println("Peer " +peer.getPeer_id() + " sent unchoked");
								peer.setChoked(false);
								chooseAndRequestPiece(peer);
								break;			
							case Message.INTERESTED:
								System.out.println("Peer " + peer.getPeer_id() + " sent interested");
								peer.setRemoteInterested(true);
								break;
							case Message.HAVE:
								System.out.println("Peer " + peer.getPeer_id() + " sent have message");
								if (peer.isChoked()){
									//System.out.println("first have message");
									byte[] piece_bytes = new byte[4];
									System.arraycopy(msg, 1, piece_bytes, 0, 4); //gets the piece number bytes from the piece message
									int piece = ByteBuffer.wrap(piece_bytes).getInt();
		
									destfile.manualMod(peer.getBitfield(), piece, true);
									//System.out.println("updated bitfield: " + Arrays.toString(peer.getBitfield()));
									if(destfile.firstNewPiece(peer.getBitfield()) != -1){		//then we are interested in a piece
										peer.setInterested(true);
										try{
											peer.sendMessage(message.getInterested());
										}catch(IOException e){
											System.out.println("peer send error");
										}
									}
								}
								break;
							case Message.BITFIELD:
								System.out.println("Peer " + peer.getPeer_id() + " sent bitfield");
								System.out.println( peer.getPeer_id() + " bitfield: "+ Arrays.toString(peer.getBitfield()));
								if(!peer.getFirstSent()){
									peer.setFirstSent(true);
								}else{
									peer.setConnected(false);
									removePeer(peer);
									System.out.println("closing peer");
									return;
								}
								
								if (destfile.firstNewPiece(peer.getBitfield()) != -1){		//then we are interested in a piece
									peer.setInterested(true);
									try{
										peer.sendMessage(message.getInterested());
									}catch(IOException e){
										System.out.println("peer send error");
									}
								}
								break;
							case Message.REQUEST:
								System.out.println("Peer " + peer.getPeer_id() + " sent request");
								break;
							case Message.PIECE:		//check where we are in the piece, then request the next part i think.
								//System.out.println("Peer " + peer.getPeer_id()+ " sent chunk");
								//if(!peer.getRemoteInterested())
								getNextBlock(msg,peer);
								break;
						}
					}
				});
			}catch (InterruptedException ie){
				System.err.println("caught interrupt. continuing anyway");
			}
		}	
	}
	
	/**
	 * @param newPeers List of Peers to be connected to
	 */
	public void addPeers(List<Peer> newPeers){
		
		for(Peer peer: newPeers){
			if (alreadyConnected(peer.getPeer_id()) || !peer.connectToPeer()){
				continue;
			}
			Message current_message = new Message();
			try {
				peer.sendMessage(current_message.handShake(this.torrentinfo.info_hash.array(), tracker.getUser_id()));
				byte[] handshake = peer.handshake();
				
				if(!this.handshakeCheck(handshake,peer)){
					peer.closeConnections();
					System.err.println("Invalid info hash from peer:");
					continue;
				}
			}catch (IOException e) {
				System.err.println("random  IO ex");
				continue;
			}
			current_message.getBitFieldMessage(destfile.getMybitfield());
			peers.add(peer);
			System.out.println("added new peer: " + peer.getPeer_id());
			peer.setClient(this);
			peer.start();
			return; 		//here to only run first peer
		}
		System.out.println("finished adding peers");
	}
	public boolean alreadyConnected(String peer_id){
		
		for(Peer peer: this.peers){
			if(peer.getPeer_id().equals(peer_id)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method compares the remote peers handshake to our infohash 
	 * @param phandshake peer handshake
	 */
	private boolean handshakeCheck(byte[] peer_handshake,Peer peer){	
		//TODO check that the expected peer name is correct?
		
		byte[] peer_infohash = new byte [20];
		System.arraycopy(peer_handshake, 28, peer_infohash, 0, 20); //copies the peer's infohash
		byte[] peer_id = new byte[20];
		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.
		if(!(Arrays.equals(peer.getPeer_id().getBytes(),peer_id)))  //fails if the id in the hash doenst match the expected id.
		{
				System.out.println("Peer id didnt match");
				return false;
		}
		System.out.println("Peer id in the handshake"+Arrays.toString(peer_id));
		if (Arrays.equals(peer_infohash, this.torrentinfo.info_hash.array())){  //returns true if the peer id matches and the info hash matches
			return true;
		}else {
			return false;
		}
	}
	
	public synchronized  void addMessageTask(MessageTask task){
		tasks.add(task);
	}
	
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
	   		
	   		System.out.println("our bitfield: " + Arrays.toString(destfile.getMybitfield()));
			System.out.println("");
	   		System.out.println("requesting piece: " + current_piece);
	   		
	   		offset_counter = destfile.pieces[current_piece].getOffset();
			if(offset_counter != -1){
				offset_counter += max_request;
			}else {
				offset_counter = 0;
			}
	   		request_message = current_message.request(current_piece, offset_counter, max_request);
	   		try {
				peer.sendMessage(request_message);
			}catch (IOException e) {
				System.err.println("Error sending message to peer");
				e.printStackTrace();
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
		//checks if we got the last chunk of a piece
		addChunk(piece,offset,block);  //places the chunk of data into a piece
		//offset+max_request
		//torrentinfo.file_length%torrentinfo.piece_length
		System.out.println(offset);
		if((piece==torrentinfo.file_length/torrentinfo.piece_length)&&(offset+2*max_request>torrentinfo.file_length%torrentinfo.piece_length))//checks if we are at the last chunk of the last piece
		{
			small_request = (torrentinfo.file_length%torrentinfo.piece_length)%max_request;
			if(small_request+offset ==torrentinfo.file_length%torrentinfo.piece_length)//just got back the last chunk of the last piece
			{
				System.out.println("Getting this junk to the guy");
				destfile.addPiece(piece);
				chooseAndRequestPiece(peer);
			}
			else
			{
				System.out.println("sending the last chunk request");
				small_request = (torrentinfo.file_length%torrentinfo.piece_length)%max_request;
				request = message.request(piece, offset+max_request,small_request);
				if (peer.isChoked()){			//TODO i don't know how to handle starting up again if we get choked mid piece request
	   				System.out.println("got choked out");
	   				return;
	   			}
				else 
				{
					try {
						peer.sendMessage(request);
					}catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		else if (offset + max_request == torrentinfo.piece_length){ 	//checks if we got the last chunk of a piece{
			destfile.addPiece(piece);
			chooseAndRequestPiece(peer); 		//figures out the next piece to request
		}
		else 
		{
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
	
	public Response contactTracker(String event){
		this.tracker.updateProgress(this.torrentinfo.file_length - this.destfile.incomplete, this.uploaded);
		this.tracker.constructURL(this.torrentinfo.announce_url.toString(), this.torrentinfo.info_hash, this.port);
		byte[] response_string = null;
		try{
			response_string = this.tracker.requestPeerList(event);
		}catch (Exception e){
			System.out.println("exception thrown requesting peer list from tracker");
			e.printStackTrace();

			if(event == null || event.equals("stopped")  || event.equals("completed") ){
				System.out.println("already downloading. carry on");
			}else if(event.equals("started")){
				System.out.println("Havent start yet so just quit");
				System.exit(0);
			}
		}
		
		if (response_string == null){
			System.out.println("null response");
			System.exit(0);
		}
		return (new Response(response_string));
	}
	
	
	/**
	 * Remove peer takes in a peer and removes/disconnects it from the list of active peers
	 */
	private void removePeer(Peer peer){
		if (peers.contains(peer)){
			System.out.println("closing connections");
			peer.closeConnections();
			peers.remove(peer);
		}
	}
	
	public byte[] getbitfield(){
		return this.destfile.getMybitfield();
	}
}
