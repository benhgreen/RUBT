package RUBTclient;

import java.net.*;
import java.io.*;
//import java.nio.ByteBuffer;
import java.util.Arrays;

import edu.rutgers.cs.cs352.bt.TorrentInfo;

public class Peer {
	
	String ip;
	String peer_id;
	Integer port;
	DestFile destfile;
	
	int downloaded;
	Socket peerConnection;
	OutputStream peerOutputStream;
	InputStream peerInputStream;
	
	public Peer(String ip, String peer_id, Integer port,DestFile destfile) {
		super();
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.destfile = destfile;
		
		this.peerConnection = null;
		this.peerOutputStream = null;
		this.peerInputStream = null;
	}
		
	public int connectToPeer(){
		try{
			peerConnection = new Socket(ip, port);
			peerConnection.setSoTimeout(60*1000);
			peerOutputStream = peerConnection.getOutputStream();
			peerInputStream = peerConnection.getInputStream();
		}catch(UnknownHostException e){
			System.out.println("UnknownHostException");
			return 0;
		}catch(IOException e){
			System.out.println("IOException");
			return 0;
		}
		return 1;
	}
	
	public int handshakePeer(byte[] handshake){
		
		byte[] response = new byte[68];		
		try{
			peerOutputStream.write(handshake);
			peerOutputStream.flush();
			wait(1000);
			peerInputStream.read(response);
		}catch(IOException e){
			return 0;
		}
		System.out.println("handshake response: " + Arrays.toString(response));
		//verify data
		//if correct
		return 1;
		//else return 0
	}
	public int sendInterested(byte[] interested){
		
		byte[] response1 = new byte[6];
		byte[] response2 = new byte[6];
		//byte[] response3 = new byte[68];

		try{
			peerOutputStream.write(interested);	
			//peerOutputStream.flush();
			wait(1000);
			peerInputStream.read(response1);
			System.out.println("interested response1: " + Arrays.toString(response1));
			wait(1000);
			peerInputStream.read(response2);
			System.out.println("interested response2:  " + Arrays.toString(response2));
			//check unchoked
		}catch(IOException e){
			return 0;
		}
		return 1;
	}
	
	public byte[] getChunk(byte[] request, int size){
		byte[] data_chunk = new byte[size];
		try{
			peerOutputStream.write(request);
			wait(1000);
			peerInputStream.read(data_chunk);
			peerOutputStream.flush();

		}catch(IOException e){
			return null;
		}
		downloaded = downloaded + (size-13);
		//System.out.println("downloaded: "+downloaded+" bytes");
		//verify?
		return data_chunk;
	}
	
	public DestFile downloadPieces(int file_size){
		wait(1000);
		byte[] request, data_chunk;
		int index = 0;
		int chunk_size = 16384;
		byte[] piece_filler1 = new byte[16384];
		byte[] piece_filler2 = new byte[16384];
		byte[] piece_filler_final = null;
		Piece piece = null;
		Message myMessage = null;
		
		while(this.downloaded < file_size){
			myMessage = new Message();
			request = myMessage.request(index,0, chunk_size);
			data_chunk = getChunk(request,16397);
			System.arraycopy(data_chunk, 13, piece_filler1, 0, chunk_size);
			//piece = new Piece(piece_filler1,index,0);
			//destfile.addPiece(piece);
			//add chunk
			if((file_size - downloaded) < 16384){
				chunk_size = 677;
				piece_filler2 = new byte[677];
			}	
			myMessage = new Message();
			request = myMessage.request(index,16384,chunk_size);
			data_chunk = getChunk(request,chunk_size+13);
			//add chunk
			System.arraycopy(data_chunk, 13, piece_filler2, 0, chunk_size);
			
			//copy to final piece
			piece_filler_final = new byte[piece_filler1.length + piece_filler2.length];
			System.arraycopy(piece_filler1, 0, piece_filler_final,0,piece_filler1.length);
			System.arraycopy(piece_filler2, 0, piece_filler_final, piece_filler1.length, piece_filler2.length);
			piece = new Piece(piece_filler_final,index,0);
			destfile.verify(piece);
			destfile.addPiece(piece);
			index++;
		}
		return destfile;
	}
	
	public void closeConnections(){
		try{
			peerInputStream.close();
			peerOutputStream.close();
			peerConnection.close();
		}catch(IOException e){
			return;
		}
	}
	public void wait(int milliseconds){
		try{Thread.sleep(milliseconds);}
		catch(InterruptedException ex){Thread.currentThread().interrupt();}
	}
}
