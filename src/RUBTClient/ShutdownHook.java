package RUBTClient;


/**
 * ShutdownHook thread that listens for user input to terminate 
 * the client program
 */
public class ShutdownHook{
	
	private RUBTClient client;  
	
	/**
	 * @param client RUBTClient thread that spawns shutdown hook and whose cleanUp method is used
	 */
	public ShutdownHook(RUBTClient client){
		this.client = client;
	}
	
	/**
	 * Calls client's cleanup method if client is unexpectedly shutdown and client thread
	 * no longer alive 
	 */
	public void attachShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				if(client.isAlive()) 
					client.cleanUp();
			}
		});
	}
}