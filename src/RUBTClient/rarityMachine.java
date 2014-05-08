package RUBTClient;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

public class rarityMachine {
	
	private Hashtable<byte[], boolean[]> pieceset;
	private final int piececount;
	private final int expected;
	private int[] mybitfield;
	private DestFile destfile;
	
	/**
	 * @param mybitfield to update this bitfield with. Uses the same format as mypieces in Destfile
	 */
	public void setMybitfield(int[] mybitfield){
		this.mybitfield = mybitfield;
	}
	
	public rarityMachine(int capacity, DestFile destfile){
		this.pieceset = new Hashtable<byte[], boolean[]>(capacity);
		this.piececount = capacity;
		this.expected = calcExpected(capacity);
		this.destfile = destfile;
	}
	
	/**Add peer to table with initial bitfield
	 * @param peerid of peer
	 * @param bitfield of peer
	 */
	public synchronized void addPeer(byte[] peerid, byte[] bitfield){
		
		//System.out.println("ADDING BITFIELD FOR " + peerid);
		//DestFile.printBitfield(bitfield, expected);
		pieceset.put(peerid, boolfield(bitfield));
	}

	/**
	 * @param peerid peer to remove from table
	 */
	public void deletePeer(byte[] peerid){
		
		if(pieceset.containsKey(peerid)){
			pieceset.remove(peerid);
		}
	}
	
	/**Update a specific peer based on a new have message
	 * @param peerid to update
	 * @param piece that should be updated
	 */
	public void updatePeer(byte[] peerid, int piece){
		
		//System.out.println("UPDATING PEER" + peerid);
		if(pieceset.contains(peerid)){
			pieceset.get(peerid)[piece] = true;
		}
	}
	
	/**
	 * @param bitfield to convert
	 * @return boolean array representing bitfield
	 */
	private boolean[] boolfield(byte[] bitfield) {
		
		boolean[] ret = new boolean[piececount];
		
		if(bitfield.length != expected){
			System.err.println("Invalid bitfield being entered!");
			return null;
		}else{
			for(int i = 0; i < piececount; i++){
				
				int mod = i%8;
				int currentbyte = (i-(mod)) / 8;
				
				if(((bitfield[currentbyte] >> (7-mod) & 1) == 1)){
					ret[i] = true;
				}else{
					ret[i] = false;
				}
			}
		}
		return ret;
	}
	
	/**
	 * @param input piece count
	 * @return number of expected digits in bitfield
	 */
	private int calcExpected(int input){
		
		int mod = input % 8;
		if(mod == 0){
			return input/8;
		}else{
			return (input/8)+1;
		}
	}
	
	/**
	 * @return Counter array, each one representing a piece and how many peers have it
	 */
	private Counter[] enumerate(){
		
		Counter[] count = new Counter[piececount];
		for(int i = 0; i < piececount; i++){
			count[i] = new Counter(i);
		}
		
		Iterator<boolean[]> values = pieceset.values().iterator();
		
		while(values.hasNext()){
			
			boolean[] temp = values.next();
			
			for(int i = 0; i < piececount; i++){
				if(temp[i]){
					count[i].increment();
					//System.out.println("Piece " + i + " incremented to " + count[i].getCount());
				}else{
					//System.out.println("PIECE " + i + " NOT INCREMENTED");
				}
			}
		}
		return count;
	}
	
	/**
	 * @return identifier number of the rarest piece
	 */
	public synchronized int rarestPiece(byte[] bitfield){
		
		Counter[] values = enumerate();
		Arrays.sort(values);
		
		Counter rarest = identifyRarest(values, bitfield);
		
//		System.out.println("PRINTING RARITIES");
//		System.out.println("--------------------");
//		for(int i = 0; i<piececount; i++){
//			System.out.println("Piece " + values[i].getIdentifier() + ": " + values[i].getCount());
//		}
//		
//		System.out.println("RAREST PIECE IS: " + rarest.getIdentifier());
		
		return rarest.getIdentifier();
	}

	/**
	 * @param values, hopefully already sorted in ascending order of popularity
	 * @return Smallest piece. TODO implement randomness
	 */
	private Counter identifyRarest(Counter[] values, byte[] bitfield) {
		
//		int benchmark = values[0].getCount();
//		int cutoff = 0;
//		
//		for(int i = 0; i < values.length; i++){
//			
//			if(values[i].getCount() > benchmark){
//				
//				cutoff = i + 1;
//				break;
//			}
//		}
//		
//		Random generator = new Random();
//		int select = generator.;
//		
//		while(mybitfield[select = generator.nextInt()] == 2){
//			select = generator.nextInt();
//		}
		
		boolean[] bools = boolfield(bitfield);
		
		for(int i = 0; i<piececount; i++){
			if(mybitfield[i] == 0 && bools[i]){
				destfile.markInProgress(i);
				return values[i];
			}
		}
		
		return new Counter(-1);
	}
}
