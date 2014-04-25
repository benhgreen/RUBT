package RUBTClient;

public class Counter implements Comparable<Counter>{
	
	private int identifier;
	private int count;
	
	/**Create a new Counter representing a piece and how many peers have this piece
	 * @param identifier
	 */
	public Counter(int identifier){
		this.identifier = identifier;
		this.count = 0;
	}
	
	/**
	 * increment this Counter by 1
	 */
	public void increment(){
		this.count++;
	}

	public int getCount(){
		return count;
	}
	
	public void setIdentifier(int identifier){
		this.identifier = identifier;
	}
	
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
