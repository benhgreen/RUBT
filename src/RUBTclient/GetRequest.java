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

	private int total_downloaded;
	private int total_uploaded;
	private int left;
	private int file_length;
	private int port_num;
	private String url;
	//private String url_encoded;
	
	
	
	 GetRequest(){
		setTotal_downloaded(0);
		setTotal_uploaded(0);
	}
		
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		
		String url ="http://www.mannyjl625.info/songfinder/getrequest.txt";
		//byte[] byteArray = new byte[10];
		GetRequest myRequest = new GetRequest();
		
		//System.out.println(myRequest.sendGetRequest(url));
		//randomID();
	}
	
	public void constructURL(String announce_url, ByteBuffer info_hash, int port_num, int file_length){   //construct url key/value pairs
		
		setFile_length(file_length);
		setPort_num(port_num);
		setLeft();
		
		String info_hash_encoded = "?info_hash=" + encodeHash(info_hash);
		String peer_id = "&peer_id=" + randomID();
		String port = "&port=" + port_num;
		String downloaded = "&downloaded=" + getTotal_downloaded();
		String uploaded = "&uploaded=" + getTotal_uploaded();
		String left =  "&left=" + getLeft();
	
		setUrl(announce_url + info_hash_encoded + peer_id + port + downloaded + uploaded+ left);
	}
	
	public String encodeHash(ByteBuffer info_hash){
		//setUrl_encoded(URLEncoder.encode(getUrl(), "UTF-8"));
		//System.out.println(getUrl_encoded());
		String hash = "";
		for(int i =0; i < 20; i++){
			hash = hash + "%" + String.format("%02x", info_hash.get(i));
			
		}
		System.out.println("encoded hash:" + hash);
		
		return hash;
	}
	public String sendGetRequest() throws Exception{   
		
		//URL obj = new URL("http://www.mannyjl625.info/songfinder/getrequest.txt");
		URL obj = new URL(getUrl());
		System.out.println("URL :" + getUrl());
		URLConnection connection = obj.openConnection();
		int contentLength = connection.getContentLength();
		System.out.println("contentLength: " + contentLength);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine ;
		String bencoded_response = "";
		while((inputLine = in.readLine()) != null){
			//System.out.println(inputLine);
			bencoded_response = bencoded_response + inputLine;
		}
		in.close();
		return bencoded_response;
	}
	
	public static String randomID(){
		String id = "GROUP4";
		String letter;
		Random r = new Random();
		
		while(id.length()<20){
			letter = String.valueOf((char)(r.nextInt(26) + 65));
			id = id+letter;
		}
		//System.out.println(id);
		return id;
	}

	public int getTotal_downloaded() {
		return total_downloaded;
	}

	public void setTotal_downloaded(int total_downloaded) {
		this.total_downloaded = total_downloaded;
	}

	public int getFile_length() {
		return file_length;
	}

	public void setFile_length(int file_length) {
		this.file_length = file_length;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getTotal_uploaded() {
		return total_uploaded;
	}

	public void setTotal_uploaded(int totat_uploaded) {
		this.total_uploaded = totat_uploaded;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft() {
		this.left = getFile_length() - getTotal_downloaded();
	}

	public int getPort_num() {
		return port_num;
	}

	public void setPort_num(int port_num) {
		this.port_num = port_num;
	}

}
