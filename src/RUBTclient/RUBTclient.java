package RUBTclient;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

public class RUBTclient {
	
	public static void main(String[] args){
		
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
		//System.out.println(Integer.toBinaryString(-2));
		
		String announce_url = torrentinfo.announce_url.toString(); 
		int port_num = 6881;
		int file_length = torrentinfo.file_length;
		ByteBuffer info_hash = torrentinfo.info_hash;
		
		GetRequest myRequest = new GetRequest();
		
		myRequest.constructURL(announce_url, info_hash, port_num, file_length);
		String response_string = null;
		try{
			response_string = myRequest.sendGetRequest();
			//System.out.println("sent get request");
		}catch(Exception e){
			System.out.println("exception thrown sending get request");
			System.exit(0);
		};
		//Response response = new Response(response_string);
		//0 is peer_id, 1 is ip, 2 is port
		
		String[] peer_info = new Response(response_string).getValidPeer();
		Peer myPeer = null;
		DestFile myDest = new DestFile(args[1], torrentinfo);
		if(peer_info != null){
			myPeer = new Peer(peer_info[1], peer_info[0], Integer.parseInt(peer_info[2]), myDest);
		}else{
			System.out.println("no valid peers");
			System.exit(0);
		}
		
		if(myPeer.connectToPeer()==0){
			System.out.println("failed connecting to peer");
			System.exit(0);
		}
		Message myMessage = new Message();
		
		byte[] handshake=myMessage.handShake(info_hash.array());
		if(myPeer.handshakePeer(handshake)==0){
			System.out.println("failed sending handshake");
			System.exit(0);
		}
		System.out.println("sent handshake");

		byte[] interested = myMessage.getInterested();
		if(myPeer.sendInterested(interested)==0){
			System.out.println("failed sending interested");
			System.exit(0);
		}
		System.out.println("sent interested");
		
		//send started event to tracker
		try{myRequest.sendEvent("started", myPeer.downloaded);
		}catch(Exception e){System.out.println("send start event exception");}
		
		DestFile resultFile = myPeer.downloadPieces(torrentinfo.file_length);
		
		if(resultFile == null){
			System.out.println("corrupted download");
			try{myRequest.sendEvent("stopped", myPeer.downloaded);}
			catch(Exception e){System.out.println("send stopped event exception");}
			System.exit(0);
		}
		//send completed event to tracker
		try{
			myRequest.sendEvent("completed", myPeer.downloaded);
		}catch(Exception e){
			System.out.println("send completed event exception");
		}
		
		
		
		myPeer.closeConnections();
		
		
		System.out.println("file length: " + file_length);
		
		resultFile.close();
		
	}
}