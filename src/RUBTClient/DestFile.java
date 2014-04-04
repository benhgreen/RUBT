package RUBTClient;

import java.io.FileNotFoundException;
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
	private boolean[] mypieces;
	private boolean initialized;
	
	public DestFile(TorrentInfo torrentinfo){
		this.initialized = false;
		this.setTorrentinfo(torrentinfo);
		this.totalsize = torrentinfo.file_length;
		this.incomplete = torrentinfo.file_length;
		
		this.mypieces = new boolean[this.torrentinfo.piece_hashes.length];
		int mod1;
		if((mod1 = torrentinfo.piece_hashes.length % 8) == 0){
			this.mybitfield = new byte[mod1];
			
		}else{
			this.mybitfield = new byte[((torrentinfo.piece_hashes.length - mod1) / 8) + 1];
		}
		this.initializeBitfield();
		for(int i = 0; i<this.mypieces.length; i++){
			this.mypieces[i] = false;
		}
		System.out.println(this.mypieces.length + " pieces");
		System.out.println(this.mybitfield.length + " bytes in bitfield");
	}
	
	public void initializeRAF(){
		try {
			this.dest = new RandomAccessFile(this.torrentinfo.file_name,"rw");
			this.dest.setLength(torrentinfo.file_length);
			this.initialized = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
			long target = piece.getPiece()*getTorrentinfo().piece_length + piece.getOffset();
			dest.seek(target);
			dest.write(piece.getData());
			this.mypieces[piece.getPiece()] = true;
			this.renewBitfield();
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
	public boolean verify(byte[] piece){
		
		//get data hash
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error intitializing MessageDigest");
		}
		byte[] hash = md.digest(piece);
		
		//iterate through torrentinfo piece hashes and look for a match
		for(int i = 0; i<this.getTorrentinfo().piece_hashes.length; i++){
			if(Arrays.equals(hash, this.getTorrentinfo().piece_hashes[i].array())){
				return true;
			}
		}
		return false;
	}
	
	public boolean verify(Piece piece){
		return verify(piece.getData());
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
	
	/**
	 *  Checks through a (presumed to exist) file for valid pieces and updates mypieces accordingly.
	 */
	public void checkExistingFile(){
		
		if(!this.initialized){
			this.initializeRAF();
		}
		byte[] temp = new byte[this.torrentinfo.piece_length];
		for(int i = 0; i < this.torrentinfo.piece_hashes.length; i++){
			//check each piece here
			try {
				this.dest.seek(i * this.torrentinfo.piece_length);
				this.dest.read(temp);
				if(this.verify(temp)){
					System.out.println("Piece " + i + " is valid.");
					this.mypieces[i] = true;
				}else{
					System.out.println("Piece " + i + " is INvalid.");
					this.mypieces[i] = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 *  Refreshes bitfield based on the current status of mypieces.
	 */
	public void renewBitfield(){
		
		for(int i = 0; i < this.mypieces.length; i++){
			
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			
			if(this.mypieces[currentbyte]){
				this.mybitfield[currentbyte] |= (1 << mod);
			}else{
				this.mybitfield[currentbyte] &= ~(1 << mod);
			}
			
		}
	}
	/**
	 * Initializes all bits to 0
	 */
	public void initializeBitfield(){
		
		for(int i = 0; i < this.mypieces.length; i++){
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			this.mybitfield[currentbyte] &= ~(1 << mod);
		}
	}
	
	public byte[] getMybitfield(){
		return mybitfield;
	}
	
	/**
	 * Manually set bit in bitfield
	 * @param i Bit to set
	 * @param bool Value to set
	 */
	public void manualMod(int i, boolean bool){
		
		int mod = i%8;
		int currentbyte = (i-(mod)) / 8;
		if(bool){
			this.mybitfield[currentbyte] |= (1 << mod);
		}else{
			this.mybitfield[currentbyte] &= ~(1 << mod);
		}
	}
	
	public byte[] manualMod(byte[] array, int i, boolean bool){
		
		int mod = i%8;
		int currentbyte = (i-(mod)) / 8;
		if(bool){
			array[currentbyte] |= (1 << mod);
		}else{
			array[currentbyte] &= ~(1 << mod);
		}
		
		return array;
	}
	
	/**
	 * @param input Other bitfield
	 * @return First bit where input is 1 and mybitfield is 0
	 */
	public int firstNewPiece(byte[] input){
		
		for(int i = 0; i < this.mypieces.length; i++){
			
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			
			if((input[currentbyte] >> mod & 1) == 1){
				if((this.mybitfield[currentbyte] >> mod & 1) != 1){
					return i;
				}
			}
		}
		
		return -1;
		
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
