package RUBTClient;

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
	int totalsize;
	int incomplete;
	private String filename;
	private byte[] mybitfield;
	
	public DestFile(String name, TorrentInfo torrentinfo){
		this.setTorrentinfo(torrentinfo);
		try {
			//initialize RandomAccessFile and set length
			this.dest = new RandomAccessFile(name,"rw");
			dest.setLength(torrentinfo.file_length);
			this.totalsize = torrentinfo.file_length;
			this.incomplete = torrentinfo.file_length;
			
			int mod1;
			if((mod1 = torrentinfo.file_length % 8) == 0){
				this.mybitfield = new byte[mod1];
			}else{
				this.mybitfield = new byte[mod1 + 1];
			}
			
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
			long target = piece.getPiece()*getTorrentinfo().piece_length + piece.getOffset();
			dest.seek(target);
			dest.write(piece.getData());
			this.incomplete -= (piece.getData().length);
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
		for(int i = 0; i<this.getTorrentinfo().piece_hashes.length; i++){
			if(Arrays.equals(hash, this.getTorrentinfo().piece_hashes[i].array())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param input Integer representation of bitfield
	 * @return String representation of bitfield in binary form, two's complement
	 */
	public String generateBitField(Integer input){
		String bitfield = Integer.toBinaryString(input);
		if(bitfield.length()>8){
			return bitfield.substring(bitfield.length()-8);
		}else{
			return bitfield;
		}
	}
	
	public void checkExistingFile(){
		
		for(int i = 0; i < this.torrentinfo.piece_hashes.length; i++){
			//check each piece here
		}
		
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public TorrentInfo getTorrentinfo() {
		return torrentinfo;
	}

	public void setTorrentinfo(TorrentInfo torrentinfo) {
		this.torrentinfo = torrentinfo;
	}
}
