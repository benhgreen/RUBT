package RUBTclient;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;


public class GetRequest {

	private int total_downloaded;
	private int total_uploaded;
	private int left;
	private int file_length;
	private int port_num;
	private String url;
	private String url_encoded;
	
	
	
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
	
	public void constructURL(String announce_url, String info_hash_byteArray, int port_num, int file_length){   //construct url key/value pairs
		
		setFile_length(file_length);
		setPort_num(port_num);
		setLeft();
		
		String info_hash = "?info_hash="+info_hash_byteArray;
		String peer_id = "&peer_id=" + randomID();
		String port = "&port=" + port_num;
		String downloaded = "&downloaded=" + getTotal_downloaded();
		String uploaded = "&uploaded=" + getTotal_uploaded();
		String left =  "&left=" + getLeft();
	
		setUrl(announce_url + info_hash + peer_id + port + downloaded + uploaded+ left);
	}
	
	public void encodeURL() throws Exception{
		setUrl_encoded(URLEncoder.encode(getUrl(), "UTF-8"));
		System.out.println(getUrl_encoded());
	}
	public String sendGetRequest() throws Exception{   
		
		URL obj = new URL(url);
		int contentLength = obj.openConnection().getContentLength();
		System.out.println("contentLength: " + contentLength);
		
		return "yo";
	}
	
	public static String randomID(){
		String id = "GROUP4";
		String letter;
		Random r = new Random();
		
		while(id.length()<20){
			letter = String.valueOf((char)(r.nextInt(26) + 65));
			id = id+letter;
		}
		System.out.println(id);
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

	public String getUrl_encoded() {
		return url_encoded;
	}

	public void setUrl_encoded(String url_encoded) {
		this.url_encoded = url_encoded;
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
