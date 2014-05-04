package RUBTClient;


/**
 * ShutdownHook is the thread that listens for user input to terminate 
 * the client program
 */
public class ShutdownHook{
	
	private RUBTClient client;   //access to client for contacting its tracker
	
	public ShutdownHook(RUBTClient client){
		this.client = client;
	}
	
	public void attachShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			//cleans up at end of program by closing all connections and all spawned threads
			//also sends stopped tracker event
			public void run(){
				System.out.println("000000000000 in shutdown hook   000000000000000");
				client.cleanUp();
			}
		});
	}
}
