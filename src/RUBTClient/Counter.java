package RUBTClient;

/**
 * @author Ben Green
 *This class creates an object that keeps a counter of how many times a particular piece appears
 */
public class Counter implements Comparable<Counter>{
	
	private int identifier;
	private int count;
	
	/**Create a new Counter representing a piece and how many peers have this piece
	 * @param identifier piece index identifier
	 */
	public Counter(int identifier){
		this.identifier = identifier;
		this.count = 0;
	}
	
	/**
	 * increment this Counter by 1
	 */
	public void increment(){
		//System.out.println("INCREMENTING PIECE " + identifier + " TO " + count+1);
		this.count++;
	}

	/**
	 * Gets the count of a piece
	 * @return the count for a particular piece
	 */
	public int getCount(){
		return count;
	}
	
	/**
	 * Sets the identifier for a counter
	 * @param identifier index of a piece for this counter
	 */
	public void setIdentifier(int identifier){
		this.identifier = identifier;
	}
	
	/**
	 * Gets the identifier for a piece
	 * @return identifier of a counter
	 */
	public int getIdentifier(){
		return this.identifier;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Counter o) {
		return (this.count - o.getCount());
	}

}
