package RUBTClient;

public class ShutdownHook{
	
	public void attachShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				System.out.println("inside shutdown hook");
			}
		});
		System.out.println("showdown hook attached");
	}
}
