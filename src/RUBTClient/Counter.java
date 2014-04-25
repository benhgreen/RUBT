package RUBTClient;

public class Counter {
	
	int identifier;
	int count;
	
	public Counter(int identifier){
		this.identifier = identifier;
		this.count = 0;
	}
	
	public void increment(){
		this.count++;
	}

	/**
	 * @return the identifier
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

}
