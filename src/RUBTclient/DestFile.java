package RUBTclient;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import edu.rutgers.cs.cs352.bt.TorrentInfo;

public class DestFile {
	
	byte[] data;
	String name;
	TorrentInfo torrentinfo;
	RandomAccessFile dest;
	
	public DestFile(String name, TorrentInfo torrentinfo){
		this.name = name;
		this.torrentinfo = torrentinfo;
		try {
			this.dest = new RandomAccessFile(name,"rw");
			//dest.setLength(torrentinfo.file_length);
			dest.setLength(100);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testWrite(String input, Integer offset){
		try {
			dest.seek(offset);
			dest.write(input.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addPiece(Piece piece){
		try {
			long target = piece.piece*torrentinfo.piece_length + piece.offset;
			dest.seek(target);
			dest.write(piece.data);
			System.out.println("added part of piece " + piece.piece + " to pos " + target);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			this.dest.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean verify(Piece piece){
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] hash = md.digest(piece.data);
		
		for(int i = 0; i<this.torrentinfo.piece_hashes.length; i++){
			if(Arrays.equals(hash, this.torrentinfo.piece_hashes[i].array())){
				return true;
			}
		}
		return false;
	}
}
