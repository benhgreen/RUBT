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
			//initialize RandomAccessFile and set length
			this.dest = new RandomAccessFile(name,"rw");
			dest.setLength(torrentinfo.file_length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Takes in a Piece object and writes its data to the location specified by the piece length and offset.
	 * @param Piece object containing data to add to the target file 
	 */
	public void addPiece(Piece piece){
		try {
			//calculate location to write data in the file using piece length and offset if applicable
			long target = piece.piece*torrentinfo.piece_length + piece.offset;
			dest.seek(target);
			dest.write(piece.data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes RandomAccessFile associated with this DestFile.
	 */
	public void close(){
		try {
			this.dest.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param Piece with data to verify
	 * @return True if the piece's data hash was located in the TorrentInfo's data hash array, false if it was not found.
	 */
	public boolean verify(Piece piece){
		
		//get data hash
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] hash = md.digest(piece.data);
		
		//iterate through torrentinfo piece hashes and look for a match
		for(int i = 0; i<this.torrentinfo.piece_hashes.length; i++){
			if(Arrays.equals(hash, this.torrentinfo.piece_hashes[i].array())){
				return true;
			}
		}
		return false;
	}
}
