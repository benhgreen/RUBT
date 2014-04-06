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
	private int[] mypieces;
	private boolean initialized;
	public Piece[] pieces;
	
	public DestFile(TorrentInfo torrentinfo){
		this.initialized = false;
		this.setTorrentinfo(torrentinfo);
		this.totalsize = torrentinfo.file_length;
		this.incomplete = torrentinfo.file_length;
		
		//printHashes();
		
		mypieces = new int[torrentinfo.piece_hashes.length];
		pieces = new Piece[torrentinfo.piece_hashes.length];
		int mod1;
		if((mod1 = torrentinfo.piece_hashes.length % 8) == 0){
			mybitfield = new byte[mod1];
			
		}else{
			mybitfield = new byte[((torrentinfo.piece_hashes.length - mod1) / 8) + 1];
		}
		this.initializeBitfield();
		for(int i = 0; i<mypieces.length - 1; i++){
			pieces[i] = new Piece(torrentinfo.piece_length);
		}
		
		//deal with possibility of different last piece length
		int diff = torrentinfo.file_length % torrentinfo.piece_length;
		if(diff == 0){
			pieces[torrentinfo.piece_hashes.length - 1] = new Piece(torrentinfo.piece_length);
		}else{
			pieces[torrentinfo.piece_length] = new Piece(diff);
		}

		
		System.out.println(mypieces.length + " pieces");
		System.out.println(mybitfield.length + " bytes in bitfield");
	}
	
	private void printHashes() {
		for(int i = 0; i < torrentinfo.piece_hashes.length; i++){
			System.out.println("HASH " + i + ": " + Response.asString(torrentinfo.piece_hashes[i]).getBytes().toString());
		}
		
	}

	public void initializeRAF(){
		try {
			dest = new RandomAccessFile(torrentinfo.file_name,"rw");
			dest.setLength(torrentinfo.file_length);
			initialized = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**Takes in a Piece object and writes its data to the location specified by the piece length and offset.
	 * @param Piece object containing data to add to the target file 
	 */
	public synchronized boolean addPiece(int id){
		if(verify(this.pieces[id].getData())){
			try {
				//calculate location to write data in the file using piece length and offset if applicable
				long target = id*getTorrentinfo().piece_length;
				dest.seek(target);
				dest.write(this.pieces[id].getData());
				this.mypieces[id] = 2;
				this.renewBitfield();
				this.incomplete -= (this.pieces[id].getData().length);
				if(this.incomplete < 0){
					this.incomplete = 0;
				}
				return true;
			} catch (IOException e) {
				System.err.println("Error while writing to RandomAccessFile");
			}
		}
		return false;
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
		for(int i = 0; i < this.getTorrentinfo().piece_hashes.length; i++){
			if(Arrays.equals(hash, this.getTorrentinfo().piece_hashes[i].array())){
				System.out.println("PASSED at piece " + i);
				return true;
			}
		}
		System.out.println("FAILED");
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
		
		if(!initialized){
			this.initializeRAF();
		}
		byte[] temp = new byte[torrentinfo.piece_length];
		for(int i = 0; i < torrentinfo.piece_hashes.length; i++){
			//check each piece here
			try {
				this.dest.seek(i * torrentinfo.piece_length);
				this.dest.read(temp);
				if(this.verify(temp)){
					mypieces[i] = 2;
				}else{
					System.out.println("Piece " + i + " is INvalid.");
					mypieces[i] = 0;
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
			
			if(this.mypieces[i] == 2){
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
		
		for(int i = 0; i < mypieces.length; i++){
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			mybitfield[currentbyte] &= ~(1 << mod);
			mypieces[i] = 0;
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
			mybitfield[currentbyte] |= (1 << mod);
		}else{
			mybitfield[currentbyte] &= ~(1 << mod);
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
//	 * @param input Other bitfield
	 * @return First bit where input is 1 and mybitfield is 0
	 */
	public int firstNewPiece(byte[] input){
	
		for(int i = 0; i < mypieces.length; i++){
			
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			
			if((mybitfield[currentbyte] >> (mod) & 1) != 1){
				if((input[currentbyte] >> (8-mod) & 1) == 1){
					return i;
				}
			}
		}
		return -1;
	}
	
	public byte[] getPieceData(int piece, int start, int amount){
		byte[] ret  = new byte[amount];
		try {
			dest.seek(piece*torrentinfo.piece_length + start);
			dest.read(ret);
			return ret;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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
