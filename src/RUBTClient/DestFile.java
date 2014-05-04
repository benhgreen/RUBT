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
	public rarityMachine myRarityMachine;
	
	//int array for pieces - 0 = not downloaded, 1 = in progress, 2 = downloaded and verified
	private int[] mypieces;
	
	private boolean initialized;
	public Piece[] pieces;
	private int expectedbytes;
	private RUBTClient client;
	
	public DestFile(TorrentInfo torrentinfo, String filename){
		
		//intialize some variables and setup torrent info
		this.initialized = false;
		this.setTorrentinfo(torrentinfo);
		this.totalsize = torrentinfo.file_length;
		this.incomplete = torrentinfo.file_length;
		this.filename = filename;
		
		this.myRarityMachine = new rarityMachine(torrentinfo.piece_hashes.length);
		
		//calculate sizes of arrays representing pieces, bitfields, etc
		mypieces = new int[torrentinfo.piece_hashes.length];
		pieces = new Piece[torrentinfo.piece_hashes.length];
		int mod1;
		if((mod1 = torrentinfo.piece_hashes.length % 8) == 0){
			mybitfield = new byte[torrentinfo.piece_hashes.length / 8];
			expectedbytes = torrentinfo.piece_hashes.length / 8;
			
		}else{
			mybitfield = new byte[((torrentinfo.piece_hashes.length - mod1) / 8) + 1];
			expectedbytes = ((torrentinfo.piece_hashes.length - mod1) / 8) + 1;
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
			pieces[torrentinfo.piece_hashes.length - 1] = new Piece(diff);
		}
	}

	/**
	 * Set up RAF associated with this DestFile
	 */
	public void initializeRAF(){
		try {
			dest = new RandomAccessFile(filename,"rw");
			dest.setLength(torrentinfo.file_length);
			initialized = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**Takes in a Piece object and writes its data to the location specified by the piece length and offset.
	 * @param id Piece object containing data to add to the target file 
	 * @return true if piece verifies
	 */
	public synchronized boolean addPiece(int id){
		if(verify(this.pieces[id].getData()) == id){
			try {
				//calculate location to write data in the file using piece length and offset if applicable
				long target = id*getTorrentinfo().piece_length;
				dest.seek(target);
				dest.write(this.pieces[id].getData());
				
				//set piece as 'verified' and refresh bitfield
				this.mypieces[id] = 2;
				this.renewBitfield();
				
				//update incomplete field
				this.incomplete -= (this.pieces[id].getData().length);
				if(this.incomplete <= 0){
					this.client.contactTracker("completed");
					this.client.setSeeding();
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
	 * @param piece with data to verify
	 * @return True if the piece's data hash was located in the TorrentInfo's data hash array, false if it was not found.
	 */
	public int verify(byte[] piece){
		
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
				return i;
			}
		}
		System.out.println("FAILED");
		return -1;
	}
	
	/**Alternate method for verifying, accepts a Piece object instead of the raw byte[]
	 * @param piece - Piece to verify
	 * @return Boolean representing success/fail of verification
	 */
	public int verify(Piece piece){
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
		
		for(int i = 0; i < torrentinfo.piece_hashes.length; i++){
			byte temp[] = null;
			
			//special case for last piece, calculate if a smaller byte array is needed
			if(i == torrentinfo.piece_hashes.length - 1){
				if(torrentinfo.file_length % torrentinfo.piece_length != 0){
					temp = new byte[torrentinfo.file_length % torrentinfo.piece_length];
				}
			}else{
				temp = new byte[torrentinfo.piece_length];
			}
			
			try {
				this.dest.seek(i * torrentinfo.piece_length);
				this.dest.read(temp);
				if(this.verify(temp) == i){
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
				this.mybitfield[currentbyte] |= (1 << 7-mod);
			}else{
				this.mybitfield[currentbyte] &= ~(1 << 7-mod);
			}
			
		}
		
		myRarityMachine.setMybitfield(this.mypieces);
		
		printBitfield();
	}
	/**
	 * Initializes all bits to 0 in local bitfield
	 */
	public void initializeBitfield(){
		
		for(int i = 0; i < mypieces.length; i++){
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			mybitfield[currentbyte] &= ~(1 << 7-mod);
			mypieces[i] = 0;
		}
	}
	
	/**
	 * @return Bitfield of this DestFile
	 */
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
			mybitfield[currentbyte] |= (1 << 7-mod);
		}else{
			mybitfield[currentbyte] &= ~(1 << 7-mod);
		}
	}
	
	/**
	 * @param array - Bitfield to modify
	 * @param i - Position (in bytes) to modify
	 * @param bool
	 * @return modified byte array
	 */
	public byte[] manualMod(byte[] array, int i, boolean bool){
		
		int mod = i%8;
		int currentbyte = (i-(mod)) / 8;
		if(bool){
			array[currentbyte] |= (1 << 7-mod);
		}else{
			array[currentbyte] &= ~(1 << 7-mod);
		}
		
		return array;
	}
	
	/**Returns first piece that we don't have that a particular peer does have
	 * @param input Other bitfield
	 * @return First bit where input is 1 and mybitfield is 0 - aka first piece we are interested in downloading
	 */
	public synchronized int firstNewPiece(byte[] input){
	
		for(int i = 0; i < mypieces.length; i++){
			
			int mod = i%8;
			int currentbyte = (i-(mod)) / 8;
			
			if((mybitfield[currentbyte] >> (7-mod) & 1) != 1){
				if((input[currentbyte] >> (7-mod) & 1) == 1){
					if(mypieces[i] == 0){
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	/**Returns a chunk of data from a piece, presumably for uploading
	 * 
	 * @param piece - which piece to select
	 * @param start - the offset within the piece to begin the data chunk
	 * @param amount - in bytes, how much data to return
	 * @return a byte[] with the data requested
	 */
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
	
	private void printBitfield(){
		System.out.print("Bitfield:");
		for(int i = 0; i<expectedbytes; i++){
			System.out.print(" " + String.format("%8s", Integer.toBinaryString(mybitfield[i] & 0xFF)).replace(' ', '0'));
		}
		System.out.print("\n");
	}
	
	/**Marks a piece as 'in progress' by setting its flag to 1
	 * 
	 * @param pos - which piece to mark 'in progress'
	 */
	public synchronized void markInProgress(int pos){
		mypieces[pos] = 1;
	}
	
	/**Clears a piece's 'in progress' status by setting its flag to 0
	 * 
	 * @param pos - which piece to mark 'in progress'
	 */
	public synchronized void clearProgress(int pos){
		if(mypieces[pos] != 2){
			mypieces[pos] = 0;
		}
	}
		
	//various getters and setters	
	/**
	 * Gets the name of the file
	 * @return name of the file
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Sets the name of the file to be downloaded
	 * @param filename name of the file to be downloaded 
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Gets torrent info
	 * @return TorrentInfo object 
	 */
	public TorrentInfo getTorrentinfo() {
		return torrentinfo;
	}

	/**
	 * Sets the torrent info
	 * @param torrentinfo info of the torrent
	 */
	public void setTorrentinfo(TorrentInfo torrentinfo) {
		this.torrentinfo = torrentinfo;
	}

	/**
	 * Sets the client for a given file.
	 * @param client client that is downloading the file
	 */
	public void setClient(RUBTClient client) {
		this.client = client;
	}
}
