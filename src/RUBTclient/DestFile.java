package RUBTclient;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */
public class DestFile {
	
	private TorrentInfo torrentinfo;
	private RandomAccessFile dest;
	
	public DestFile(String name, TorrentInfo torrentinfo){
		this.torrentinfo = torrentinfo;
		try {
			//initialize RandomAccessFile and set length
			this.dest = new RandomAccessFile(name,"rw");
			dest.setLength(torrentinfo.file_length);
		} catch (IOException e) {
			System.err.println("Error while initializing RandomAccessFile");
		}
	}
	
	/**Takes in a Piece object and writes its data to the location specified by the piece length and offset.
	 * @param Piece object containing data to add to the target file 
	 */
	public void addPiece(Piece piece){
		try {
			//calculate location to write data in the file using piece length and offset if applicable
			long target = piece.getPiece()*torrentinfo.piece_length + piece.getOffset();
			dest.seek(target);
			dest.write(piece.getData());
		} catch (IOException e) {
			System.err.println("Error while writing to RandomAccessFile");
		}
	}
	
	/**
	 * Closes RandomAccessFile associated with this DestFile.
	 */
	public void close(){
		try {
			this.dest.close();
		} catch (IOException e) {
			System.err.println("Error closing RandomAccessFile");
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
			System.err.println("Error intitializing MessageDigest");
		}
		byte[] hash = md.digest(piece.getData());
		
		//iterate through torrentinfo piece hashes and look for a match
		for(int i = 0; i<this.torrentinfo.piece_hashes.length; i++){
			if(Arrays.equals(hash, this.torrentinfo.piece_hashes[i].array())){
				return true;
			}
		}
		return false;
	}
}
