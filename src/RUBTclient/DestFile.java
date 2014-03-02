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
		this.name = name;
		this.torrentinfo = torrentinfo;
		try {
			this.dest = new RandomAccessFile(name,"rw");
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
	
	public void addPiece(Piece piece){
		try {
			long target = piece.piece*torrentinfo.piece_length + piece.offset;
			dest.seek(target);
			dest.write(piece.data);
			System.out.println("added part of piece " + piece.piece + "to pos " + target);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close(){
		try {
			this.dest.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
