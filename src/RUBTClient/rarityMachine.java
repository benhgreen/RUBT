package RUBTClient;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class rarityMachine {
	
	private Hashtable<byte[], boolean[]> pieceset;
	private final int piececount;
	private final int expected;
	
	public rarityMachine(int capacity){
		this.pieceset = new Hashtable<byte[], boolean[]>(capacity);
		this.piececount = capacity;
		this.expected = calcExpected(capacity);
	}
	
	public void addPeer(byte[] peerid, byte[] bitfield){
		
		pieceset.put(peerid, boolfield(bitfield));
	}

	public void deletePeer(byte[] peerid){
		
		if(pieceset.containsKey(peerid)){
			pieceset.remove(peerid);
		}
	}
	
	public void updatePeer(byte[] peerid, int piece){
		
		if(pieceset.contains(peerid)){
			pieceset.get(peerid)[piece] = true;
		}
	}
	
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
	
	private int calcExpected(int input){
		
		int mod = input % 8;
		if(input == 0){
			return input/8;
		}else{
			return (input/8)+1;
		}
	}
	
	private int[] enumerate(){
		
		int[] count = new int[piececount];
		
		Iterator<boolean[]> values = pieceset.values().iterator();
		
		while(values.hasNext()){
			
			boolean[] temp = values.next();
			
			for(int i = 0; i < piececount; i++){
				if(temp[i]){
					count[i]++;
				}
			}
		}
		return count;
	}
	
	public int rarestPiece(){
		
		int[] values = enumerate();
		
		return 0;
	}
}
