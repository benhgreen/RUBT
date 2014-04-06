package RUBTClient;

public class ShutdownHook{
	
	private RUBTClient client;
	
	public ShutdownHook(RUBTClient client){
		this.client = client;
	}
	
	public void attachShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				//System.out.println("inside shutdown hook");
				client.closeAllConnections();
				
				client.contactTracker("stopped");
				client.workers.shutdown();
				while(!client.workers.isTerminated()){
					//System.out.println("shutting down threads");
				}
				System.out.println("FINITO");
			}
		});
		System.out.println("showdown hook attached");
	}
}
