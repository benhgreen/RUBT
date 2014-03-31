package RUBTclient;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.*;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;

/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */
public class RUBTclient {
	
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
		//checks if destination file exists. If so, user auth is required
		File mp3 = new File(destination);
		if(mp3.exists()){
			System.out.println("Output target file already exists. Type 'overwrite' to overwrite.");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			String answer = null;
			try {
				answer = bufferedReader.readLine();
			} catch (IOException e) {
				System.err.println("Invalid input");
				e.printStackTrace();
				System.exit(0);
			}
			if(!answer.equals("overwrite")){
				System.out.println("overwrite denied. quitting program...");
				System.exit(0);
			}
		}
		
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
		//run thread
		
		
		//extracts tracker information from torrent info to build GetRequest
		String announce_url = torrentinfo.announce_url.toString(); 
		int port_num = 6881;
		int file_length = torrentinfo.file_length;
		ByteBuffer info_hash = torrentinfo.info_hash;
		
		Tracker trackerConnection = new Tracker();
	
		//build the get request url to send to the tracker
		trackerConnection.constructURL(announce_url, info_hash, port_num, file_length);
		byte[] response_string=null;
		
		//sends get request to tracker
		try{
			response_string = trackerConnection.requestPeerList();
		}catch(Exception e){
			System.out.println("exception thrown sending get request");
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
		String[] peer_info;
		if(peer_list==null){
			System.out.println("no peers");
			System.exit(0);
		}
		//extracts array of peer info from valid peer
		peer_info = peer_list.getValidPeer();
		Peer myPeer = null;
		DestFile myDest = new DestFile(args[1], torrentinfo);
		if(peer_info != null){
			//peer_inf[0] = peer_id, peer_info[1] is ip, peer_info[2] is port
			myPeer = new Peer(peer_info[1], peer_info[0], Integer.parseInt(peer_info[2]), myDest);
		}else{
			System.out.println("no valid peers");
			System.exit(0);
		}
		
		//establish connection to valid peer
		if(myPeer.connectToPeer()==0){
			System.out.println("failed connecting to peer");
			System.exit(0);
		}
		//contruct new message for building peers output messages
		Message myMessage = new Message();
		
		//send handshake
		byte[] handshake=myMessage.handShake(info_hash.array(), trackerConnection.getUser_id());
		System.out.println("sent handshake");
		int handshake_status = myPeer.handshakePeer(handshake,info_hash.array());
		if(handshake_status==0){
			System.err.println("failed sending handshake");
			System.exit(0);
		}else if (handshake_status==-1){
			System.err.println("info_hash from peer didn't match");
			System.exit(0);
		}

		byte[] interested = myMessage.getInterested();
		System.out.println("sent interested");
		int interested_status = myPeer.sendInterested(interested); 
		if(interested_status==0){
			System.out.println("failed sending interested");
			System.exit(0);
		}else if(interested_status==-1){
			
		}
		//send started event to tracker
		try{trackerConnection.sendEvent("started", myPeer.getDownloaded());
		}catch(Exception e)
		{
			System.err.println("send start event exception");
			e.printStackTrace();
			System.exit(0);
			}
		
		DestFile resultFile = myPeer.downloadPieces(torrentinfo.file_length, torrentinfo.piece_length,13);
		//data from peer failed to verify after hashing/corrupt download
		if(resultFile == null){
			System.out.println("corrupted download. quitting...");
			try{trackerConnection.sendEvent("stopped", myPeer.getDownloaded());}
			catch(Exception e){
				System.err.println("send stopped event exception");
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		//send completed event to tracker
		try{trackerConnection.sendEvent("completed", myPeer.getDownloaded());}
		catch(Exception e)
				{
				System.err.println("send completed event exception");
				e.printStackTrace();
				System.exit(0);
				}
		
		//close all sockets and streams to peer
		myPeer.closeConnections();
		//close file stream
		resultFile.close();
	}
	
	private final TorrentInfo torrentinfo;
	private final String destinationFile;
	
	
	
	
	
	
	
	
}