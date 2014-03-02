package RUBTclient;

import java.nio.ByteBuffer;
import java.io.*;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;

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
		File picture = new File(destination);
		if(picture.exists()){
			System.out.println("Output target file already exists. Type 'overwrite' to overwrite.");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			String answer = null;
			try {
				answer = bufferedReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//extracts tracker information from torrent info to build GetRequest
		String announce_url = torrentinfo.announce_url.toString(); 
		int port_num = 6881;
		int file_length = torrentinfo.file_length;
		ByteBuffer info_hash = torrentinfo.info_hash;
		
		GetRequest myRequest = new GetRequest();
		
		//build the get request url to send to the tracker
		myRequest.constructURL(announce_url, info_hash, port_num, file_length);
		String response_string = null;
		
		//sends get request to tracker
		try{
			response_string = myRequest.sendGetRequest();
		}catch(Exception e){
			System.out.println("exception thrown sending get request");
			System.exit(0);
		};
		
		
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
		byte[] handshake=myMessage.handShake(info_hash.array());
		System.out.println("sent handshake");
		if(myPeer.handshakePeer(handshake,info_hash.array())==0){
			System.out.println("failed sending handshake");
			System.exit(0);
		}

		byte[] interested = myMessage.getInterested();
		System.out.println("sent interested");
		if(myPeer.sendInterested(interested)==0){
			System.out.println("failed sending interested");
			System.exit(0);
		}
		//send started event to tracker
		try{myRequest.sendEvent("started", myPeer.downloaded);
		}catch(Exception e){System.out.println("send start event exception");}
		
		DestFile resultFile = myPeer.downloadPieces(torrentinfo.file_length, torrentinfo.piece_length,myMessage.getRequest_Prefix());
		//data from peer failed to verify after hashing/corrupt download
		if(resultFile == null){
			System.out.println("corrupted download. quitting...");
			try{myRequest.sendEvent("stopped", myPeer.downloaded);}
			catch(Exception e){System.out.println("send stopped event exception");}
			System.exit(0);
		}
		
		//send completed event to tracker
		try{myRequest.sendEvent("completed", myPeer.downloaded);}
		catch(Exception e){System.out.println("send completed event exception");}
		
		//close all sockets and streams to peer
		myPeer.closeConnections();
		//close file stream
		resultFile.close();
	}
}