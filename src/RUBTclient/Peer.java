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
	
	public Peer(String ip, String peer_id, Integer port,DestFile destfile) {
		super();
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.destfile = destfile;
	}
		
	public void connectToPeer(byte[] handshake, byte[] interested, byte[] request) throws IOException{
		
		Socket peerConnection = null;
		OutputStream peerOutputStream = null;
		InputStream peerInputStream = null;
		
		try{
			peerConnection = new Socket(ip, port);
			peerOutputStream = peerConnection.getOutputStream();
			peerInputStream = peerConnection.getInputStream();
		}catch(UnknownHostException e){
			System.out.println("UnknownHostException");
		}catch(IOException e){
			System.out.println("IOException");
		}
		//write Message object byte array and listen for response;
		peerOutputStream.write(handshake);
		byte[] response = new byte[68];
		peerInputStream.read(response);
		System.out.println("handshake response: " + Arrays.toString(response));
		
		
		//verify data
		
		peerOutputStream.flush();
		peerOutputStream.write(interested);		
		
		response = new byte[68];
		peerInputStream.read(response);
		System.out.println("interested response1: " + Arrays.toString(response));
		
		
		response = new byte[68];
		peerInputStream.read(response);
		System.out.println("interested response2:  " + Arrays.toString(response));
	
		//piece 0 part 1
		//*******************************
		
		peerOutputStream.flush();
		if(response[4]==1)
		{	
			System.out.println("got unchoked");
			peerOutputStream.write(request);
		}
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk1   = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk1);
		//System.out.println("Data Chunk1: " + Arrays.toString(data_chunk1));

		//******************************************
		//piece 0 part 2
		//******************************************
		
		Message message2 = new Message();
		byte request2[] = message2.request(0, 16384, 16384);

		peerOutputStream.write(request2);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk2   = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk2);
		//System.out.println("Data Chunk2: " + Arrays.toString(data_chunk2));

		//*******************************
		//piece 1 part 1
		//*******************************
		Message message3 = new Message();
		byte request3[] = message3.request(1, 0, 16384);

		peerOutputStream.write(request3);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk3   = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk3);
		//System.out.println("Data Chunk3: " + Arrays.toString(data_chunk3));
		
		//*******************************
		//piece 1 part 2
		//*******************************
		Message message4 = new Message();
		byte request4[] = message4.request(1, 16384, 16384);

		peerOutputStream.write(request4);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk4 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk4);
		//System.out.println("Data Chunk4: " + Arrays.toString(data_chunk4));
		
		//*******************************
		//piece 2 part 1
		//*******************************
		Message message5 = new Message();
		byte request5[] = message5.request(2, 0, 16384);

		peerOutputStream.write(request5);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk5   = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk5);
		//System.out.println("Data Chunk5: " + Arrays.toString(data_chunk5));

		
		//*******************************
		//piece 2 part 2
		//*******************************
		Message message6 = new Message();
		byte request6[] = message6.request(2, 16384, 16384);

		peerOutputStream.write(request6);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk6   = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk6);
		//System.out.println("Data Chunk6: " + Arrays.toString(data_chunk6));
		
		//*******************************
		//piece 3 part 1
		//*******************************
		Message message7 = new Message();
		byte request7[] = message7.request(3, 0, 16384);

		peerOutputStream.write(request7);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk7 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk7);
		//System.out.println("Data Chunk7: " + Arrays.toString(data_chunk7));
		
		//*******************************
		//piece 3 part 2
		//*******************************
		Message message8 = new Message();
		byte request8[] = message8.request(3, 16384, 16384);

		peerOutputStream.write(request8);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk8 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk8);
		//System.out.println("Data Chunk8: " + Arrays.toString(data_chunk8));
		
		//*******************************
		//piece 4 part 1
		//*******************************
		Message message9 = new Message();
		byte request9[] = message9.request(4, 0, 16384);

		peerOutputStream.write(request9);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk9 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk9);
		//System.out.println("Data Chunk9: " + Arrays.toString(data_chunk9));
		
		//*******************************
		//piece 4 part 2
		//*******************************
		Message message10 = new Message();
		byte request10[] = message10.request(4, 16384, 16384);

		peerOutputStream.write(request10);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk10 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk10);
		//System.out.println("Data Chunk10: " + Arrays.toString(data_chunk10));
		
		//*******************************
		//piece 5 part 1
		//*******************************
		Message message11 = new Message();
		byte request11[] = message11.request(5, 0, 16384);

		peerOutputStream.write(request11);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk11 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk11);
		//System.out.println("Data Chunk11: " + Arrays.toString(data_chunk11));
		
		//*******************************
		//piece 5 part 2
		//*******************************
		Message message12 = new Message();
		byte request12[] = message12.request(5,16384, 16384);

		peerOutputStream.write(request12);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk12 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk12);
		//System.out.println("Data Chunk12: " + Arrays.toString(data_chunk12));
		
		//*******************************
		//piece 6 part 1
		//*******************************
		Message message13 = new Message();
		byte request13[] = message13.request(6,0, 16384);

		peerOutputStream.write(request13);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk13 = new byte[16397]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk13);
		//System.out.println("Data Chunk13: " + Arrays.toString(data_chunk13));
		
		//*******************************
		//piece 6 part 2 (tiny
		//*******************************
		Message message14 = new Message();
		byte request14[] = message14.request(6,16384, 677);

		peerOutputStream.write(request14);
		peerOutputStream.flush();
		
		try{
			Thread.sleep(1000);
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		byte [] data_chunk14 = new byte[690]; //this is the byte array, the first 12 are not part of the torrent data.
		peerInputStream.read(data_chunk14);
		System.out.println("Data Chunk14: " + Arrays.toString(data_chunk14));
		byte [] piece_filler = new byte[16384];
		
		//adding piece 0
		System.arraycopy(data_chunk1, 13, piece_filler, 0, 16384);
		Piece zero_one = new Piece(piece_filler,0,0);
		destfile.addPiece(zero_one);
		System.arraycopy(data_chunk2, 13, piece_filler, 0, 16384);
		Piece zero_two = new Piece(piece_filler,0,16384);
		destfile.addPiece(zero_two);
		//adding piece one
		System.arraycopy(data_chunk3, 13, piece_filler, 0, 16384);
		Piece one_one = new Piece(piece_filler,1,0);
		destfile.addPiece(one_one);
		System.arraycopy(data_chunk4, 13, piece_filler, 0, 16384);
		Piece one_two = new Piece(piece_filler,1,16384);
		destfile.addPiece(one_two);
		//adding piece two 
		System.arraycopy(data_chunk5, 13, piece_filler, 0, 16384);
		Piece two_one = new Piece(piece_filler,2,0);
		destfile.addPiece(two_one);
		System.arraycopy(data_chunk6, 13, piece_filler, 0, 16384);
		Piece two_two = new Piece(piece_filler,2,16384);
		destfile.addPiece(two_two);
		//adding piece 3
		System.arraycopy(data_chunk7, 13, piece_filler, 0, 16384);
		Piece three_one = new Piece(piece_filler,3,0);
		destfile.addPiece(three_one);
		System.arraycopy(data_chunk8, 13, piece_filler, 0, 16384);
		Piece three_two = new Piece(piece_filler,3,16384);
		destfile.addPiece(three_two);
		//adding piece 4
		System.arraycopy(data_chunk9, 13, piece_filler, 0, 16384);
		Piece four_one = new Piece(piece_filler,4,0);
		destfile.addPiece(four_one);
		System.arraycopy(data_chunk10, 13, piece_filler, 0, 16384);
		Piece four_two = new Piece(piece_filler,4,16384);
		destfile.addPiece(four_two);
		//adding piece 5
		System.arraycopy(data_chunk11, 13, piece_filler, 0, 16384);
		Piece five_one = new Piece(piece_filler,5,0);
		destfile.addPiece(five_one);
		System.arraycopy(data_chunk12, 13, piece_filler, 0, 16384);
		Piece five_two = new Piece(piece_filler,5,16384);
		destfile.addPiece(five_two);
		//adding piece 6
		System.arraycopy(data_chunk13, 13, piece_filler, 0, 16384);
		Piece six_one = new Piece(piece_filler,6,0);
		destfile.addPiece(six_one);
		System.arraycopy(data_chunk14, 13, piece_filler, 0, 16384);
		Piece six_two = new Piece(piece_filler,6,16384);
		destfile.addPiece(six_two);
		//************************
		//end
		
		peerOutputStream.close();
		peerInputStream.close();
		peerConnection.close();
	}
}
