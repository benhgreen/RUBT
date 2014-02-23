package RUBTclient.java;
import java.net.URL;
import java.io.*;

public class RUBTclient {
	
	public static void main(String[] args){
		
		if(args.length != 2){
			System.err.println("Usage: java RUBT <torrent> <destination>");
			System.exit(0);
		}

		String torrentname = args[0];
		String destination = args[1];
		
		System.out.println("hello "+ torrentname + " " + destination);
		
	}

}
