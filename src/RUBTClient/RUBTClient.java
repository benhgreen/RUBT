package RUBTClient;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
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
	
	private final int portnum = 6881;
	
	private final TorrentInfo torrentinfo;
	
	private final String destinationFile;
	
	private boolean keepRunning = true;
	
	private Tracker tracker;

	private DestFile destfile;
	
	private final LinkedBlockingQueue<MessageTask> tasks = new LinkedBlockingQueue<MessageTask>();

	private final List<Peer> peers = Collections.synchronizedList(new LinkedList<Peer>());
	
	
	public RUBTClient(DestFile destfile){
		this.destfile = destfile;
		this.torrentinfo = destfile.getTorrentinfo();
		this.destinationFile = destfile.getFilename();
		this.tracker = new Tracker();
	}
	
	public static void main(String[] args){
		int max_request = 16384;
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
		
		try {
			torrentinfo = new TorrentInfo(torrentbytes);
		} catch (BencodingException e) {
			System.err.println("Beencoding Exception!");
			e.printStackTrace();
		}
		
		DestFile destfile = new DestFile(args[1], torrentinfo);
		
		//checks if destination file exists. If so, user auth is required
		File mp3 = new File(destination);
		if(mp3.exists()){
			destfile.checkExistingFile();
		}
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
	
	public void run(){
		
		this.tracker.constructURL(this.torrentinfo.announce_url.toString(), this.torrentinfo.info_hash, this.portnum, this.torrentinfo.file_length);
		
		byte[] response_string=null;
		
		try{
			response_string = this.tracker.requestPeerList();
		}catch(Exception e){
			System.out.println("exception thrown requesting peer list from tracker");
			e.printStackTrace();
			System.exit(0);
		};
		if(response_string==null){
			System.out.println("null response");
			System.exit(0);
		}
		//0 is peer_id, 1 is ip, 2 is port
		//checks the list of peers from tracker
		Response peer_list = new Response(response_string);
		//extracts array of peer info from valid peer
		
		
		addPeers(peer_list.getValidPeers());
		
		while(this.keepRunning){
			try{
				MessageTask task = this.tasks.take();
				byte[] msg = task.getMessage();
				Peer peer = task.getPeer();
				switch(msg[0]){
					case Message.UNCHOKE:
						System.out.println("Peer " +peer.getPeer_id() +"sent unchoked");
						break;			
					case Message.HAVE:
						System.out.println("Peer " + peer.getPeer_id() + "sent have message");
						break;
					case Message.BITFIELD:
						System.out.println("Peer " + peer.getPeer_id() + "sent bitfield");
						break;

				}
			}catch (InterruptedException ie){
				System.err.println("caught interrupt. continueing anyway");
			}
		
		}
		
		
		
		
		//peer_info = peer_list.getValidPeers();
		//System.out.println(peer_info[0]);
		//System.out.println(torrentinfo.file_length);
		//System.out.println(torrentinfo.piece_length);
		//Peer myPeer = null;
		
		
		/*
		if(peer_info != null){
			//peer_inf[0] = peer_id, peer_info[1] is ip, peer_info[2] is port
			myPeer = new Peer(peer_info[1], peer_info[0], Integer.parseInt(peer_info[2]));
		}else{
			System.out.println("no valid peers");
			System.exit(0);
		}
		Message current_message = new Message();
		myPeer.connectToPeer();
		try {
			myPeer.sendMessage(current_message.handShake(this.torrentinfo.info_hash.array(), tracker.getUser_id()));
			byte[] handshake = myPeer.handshake();
			if(this.handshakeCheck(handshake)==false);
			{
				myPeer.closeConnections();
				//System.err.println("Invalid info hash from peer:"+peer_info[0]);
			}
		} catch (IOException e) {
			System.err.println("prob bad
			");
			e.printStackTrace();
		}
		System.out.println("client thread: " + Thread.currentThread());
		myPeer.start();
		try {
			myPeer.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("The Bitfield"+Arrays.toString(myPeer.getbitfield()));
		*/
	
		
	}
	
	/**
	 * @param newPeers List of Peers to be connected to
	 */
	
	void addPeers(List<Peer> newPeers){
		for(Peer peer: newPeers){
			if(!peer.connectToPeer()){
				continue;
			}
			Message current_message = new Message();
			try {
				peer.sendMessage(current_message.handShake(this.torrentinfo.info_hash.array(), tracker.getUser_id()));
				byte[] handshake = peer.handshake();
				
				if(!this.handshakeCheck(handshake)){
					peer.closeConnections();
					System.err.println("Invalid info hash from peer:");
				}
			} catch (IOException e) {
				System.err.println("random  IO ex");
				continue;
			}
			peers.add(peer);
		}
	}
	
	
	
	
	
	/**
	 * This method compares the remote peers handshake to our infohash 
	 * @param phandshake peer handshake
	 */
	private boolean handshakeCheck(byte[] phandshake)//TODO check that the expected peer name is correct?
	{
		byte[] peer_infohash = new byte [20];
		System.arraycopy(phandshake,28,peer_infohash,0,20); //copys the peer's infohash
		if(Arrays.equals(peer_infohash, this.torrentinfo.info_hash.array()))
		{
			System.out.println("Valid info hash");
			return true;
		}
		else
		{
			System.err.println("Invalid info hash");
			return false;
		}
	}
	public synchronized  void addMessage(MessageTask task)
	{
		tasks.add(task);
	}
}