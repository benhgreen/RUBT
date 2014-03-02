package RUBTclient;


import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Random;
import java.nio.ByteBuffer;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.rutgers.cs.cs352.bt.TorrentInfo;

public class GetRequest {

	private int port_num;
	private String url;
	private static String usrid;
	private String encodedInfoHash;
	GetRequest(){
		randomID();
	}
		

	public void constructURL(String announce_url, ByteBuffer info_hash, int port_num, int file_length){   //construct url key/value pairs
		
		setPort_num(port_num);
		String info_hash_encoded = "?info_hash=" + encodeHash(info_hash);
		String peer_id = "&peer_id=" + usrid;
		String port = "&port=" + port_num;
		String downloaded = "&downloaded=" + 0;
		String uploaded = "&uploaded=" + 0;
		String left =  "&left=" + file_length;
	
		setUrl(announce_url + info_hash_encoded + peer_id + port + downloaded + uploaded+ left);
	}
	
	public String encodeHash(ByteBuffer info_hash){
		String hash = "";
		for(int i =0; i < 20; i++){
			hash = hash + "%" + String.format("%02x", info_hash.get(i));
		}
		return hash;
	}
	public String sendGetRequest() throws Exception{   
		
		URL obj = new URL(getUrl());
		URLConnection connection = obj.openConnection();

		int contentLength = connection.getContentLength();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		String inputLine ;
		String bencoded_response = "";
		while((inputLine = in.readLine()) != null){
			bencoded_response = bencoded_response + inputLine;
		}
		in.close();
		return bencoded_response;
	}
	
	public static void randomID(){
		String id = "GROUP4";
		String randomChar;
		int randomKey;
		Random r = new Random();
		
		while(id.length()<20){
			randomKey = r.nextInt(36);
			if(randomKey < 26)
				randomChar = String.valueOf((char)(randomKey + 65));
			else
				randomChar = String.valueOf((char)(randomKey + 22));
			id = id+randomChar;
		}
		usrid=id;
	}
	public String getUser_id()
	{
		return usrid;
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPort_num() {
		return port_num;
	}

	public void setPort_num(int port_num) {
		this.port_num = port_num;
	}

	public String getEncodedInfoHash() {
		return encodedInfoHash;
	}

	public void setEncodedInfoHash(String encodedInfoHash) {
		this.encodedInfoHash = encodedInfoHash;
	}
}