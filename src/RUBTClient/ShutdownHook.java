package RUBTClient;

import java.util.Scanner;

public class ShutdownHook extends Thread{
	
	RUBTClient client;
	
	public ShutdownHook(RUBTClient client){
		this.client = client;
	}
	
	public void run(){
		System.out.println("started quit thread");
		Scanner scanner = new Scanner(System.in);
		while(true){
			if(scanner.nextLine().equals("quit")){
				System.out.println("quitting caught. ending thread");
				Message quit_message = new Message();
				MessageTask quit_task = new MessageTask(null, quit_message.getQuitMessage());
				System.out.println("sending quit message");
				client.addMessageTask(quit_task);
				break;
			}else{
				System.out.println("incorrect input. try typing \"quit\"");
			}
		}
	}
	
	public void attachShutdownHook(){
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				System.out.println("inside shutdown hook");
			}
		});
		System.out.println("showdown hook attached");
	}
}
