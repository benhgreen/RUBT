package RUBTclient;

import java.net.*;
import java.io.*;
import java.util.Arrays;


/** Peer object handles all communication between client and peer
 */
public class Peer {
	
	String 		ip;          	//ip address of peer
	String 		peer_id;		//identifying name of peer
	int 		port;			//port number to access the peer
	DestFile 	destfile;		//destfile object returned after download completes
	
	int 			downloaded;			//data downloaded from tracker not including header
	Socket 			peerConnection;		//socket connection to peer
	OutputStream	peerOutputStream;	//OuputStream to peer for sending messages
	InputStream 	peerInputStream;	//InputStream to peer for reading responses
	
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
		
	/**connectToPeer() sets socket connections and input/output streams to the peer
	 * @return int 1 if successful/0 if failed
	 */
	public int connectToPeer(){
		//open sockets and input/output streams
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
	
	/**handshakePeer() sends the handshake message
	 * @param handshake
	 * @return 1 if successful/0 if failed
	 */
	public int handshakePeer(byte[] handshake){
		
		byte[] response = new byte[68];		
		//sends handshake message and reads response
		try{
			peerOutputStream.write(handshake);
			peerOutputStream.flush();
			wait(1000);
			peerInputStream.read(response);
		}catch(IOException e){
			return 0;
		}
		System.out.println("handshake response: " + Arrays.toString(response));
		//verify info hash
		//if correct
		return 1;
		//else return 0
	}
	
	/**sendInterested() sends the interested message and reads the unchoke and bitfield response
	 * @param interested byte array from message object
	 * @return int 1 if success/0 if failure
	 */
	public int sendInterested(byte[] interested){
		
		byte[] bitfield = new byte[6];
		byte[] unchoke = new byte[6];
		
		//sends interested message and reads the bitfield and unchoke message
		try{
			peerOutputStream.write(interested);	
			wait(1000);
			peerInputStream.read(bitfield);
			peerOutputStream.flush();
			System.out.println("bitfield response: " + Arrays.toString(bitfield));
			
			wait(1000);
			peerInputStream.read(unchoke);
			peerOutputStream.flush();
			System.out.println("unchoke response:  " + Arrays.toString(unchoke));
			//check unchoked
		}catch(IOException e){
			return 0;
		}
		return 1;
	}
	
	
	/**getChunk() is a downloadPieces helper method that downloads an individual chunk
	 * @param request message contructed from message object
	 * @param size of chunk determined by downloadPieces
	 * @return byte[] of the request response
	 */
	public byte[] getChunk(byte[] request, int size){
		byte[] data_chunk = new byte[size];
		//sends request and reads response
		try{
			peerOutputStream.write(request);
			wait(1000);
			peerInputStream.read(data_chunk);
			peerOutputStream.flush();

		}catch(IOException e){
			return null;
		}
		downloaded = downloaded + (size-13);
		System.out.println("total downloaded: "+downloaded+" bytes");
		return data_chunk;
	}
	
	/**downloadPieces() downloads all of the pieces available from the peer
	 * @param file_size extracted from torrentinfo
	 * @param piece_size extracted from torrentinfo
	 * @param header_size extracted from message object
	 * @return destFile object with pieces added
	 */
	public DestFile downloadPieces(int file_size, int piece_size, int header_size){
		wait(1000);
		byte[] request, data_chunk;
		int index = 0;
		int chunk_size = piece_size/2;
		byte[] piece_filler1 = new byte[chunk_size];
		byte[] piece_filler2 = new byte[chunk_size];
		byte[] piece_filler_final = null;
		Piece piece = null;
		Message myMessage = null;
		
		while(this.downloaded < file_size){
			myMessage = new Message();
			//sends request for first chunk to peer
			request = myMessage.request(index,0, chunk_size);
			data_chunk = getChunk(request,chunk_size + header_size);
			
			//copies datachunk into placeholder w/o header for combining later
			System.arraycopy(data_chunk, header_size, piece_filler1, 0, chunk_size);
			
			//handles the case of the last smallest chunk size
			if((file_size - downloaded) < piece_size){
				chunk_size = file_size-downloaded;
				piece_filler2 = new byte[chunk_size];
			}	
			myMessage = new Message();
			//sends request for second chunk to peer
			request = myMessage.request(index,piece_size/2,chunk_size);
			data_chunk = getChunk(request,chunk_size+header_size);
			System.arraycopy(data_chunk, header_size, piece_filler2, 0, chunk_size);
			
			//combines 2 chunks into one byte[],verifies and adds to destFile
			piece_filler_final = new byte[piece_filler1.length + piece_filler2.length];
			System.arraycopy(piece_filler1, 0, piece_filler_final,0,piece_filler1.length);
			System.arraycopy(piece_filler2, 0, piece_filler_final, piece_filler1.length, piece_filler2.length);
			piece = new Piece(piece_filler_final,index,0);
			//verifies
			if(!destfile.verify(piece)){
				closeConnections();
				return null;
			}
			destfile.addPiece(piece);
			index++;
		}
		return destfile;
	}
	
	
	/** closes input/outputstreams and socket connections 
	 */
	public void closeConnections(){
		try{
			peerInputStream.close();
			peerOutputStream.close();
			peerConnection.close();
		}catch(IOException e){
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
}
