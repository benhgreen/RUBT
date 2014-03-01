package RUBTclient;

import java.io.IOException;
import java.io.RandomAccessFile;

import edu.rutgers.cs.cs352.bt.TorrentInfo;

public class DestFile {
	
	byte[] data;
	String name;
	TorrentInfo torrentinfo;
	RandomAccessFile dest;
	
	public DestFile(String name, TorrentInfo torrentinfo){
		this.name = "hello.txt";
		this.torrentinfo = torrentinfo;
		try {
			this.dest = new RandomAccessFile("hello.txt","rw");
			//dest.setLength(torrentinfo.file_length);
			dest.setLength(100);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void testWrite(String input, Integer offset){
		try {
			dest.seek(offset);
			dest.write(input.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
