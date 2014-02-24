package RUBTclient;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;


public class GetRequest {

	private int total_downloaded;
	private int total_uploaded;
	private int left;
	private int file_length;
	private String url;
	private String url_encoded;
	
	
	 GetRequest(){
		setTotal_downloaded(0);
	}
		
	private static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		
		String url ="http://www.mannyjl625.info/songfinder/getrequest.txt";
		byte[] byteArray = new byte[10];
		GetRequest myRequest = new GetRequest();
		
		//System.out.println(myRequest.sendGetRequest(url));
		//randomID();
	}
	
	private void constructURL(String announce_url, byte[] info_hash_byteArray, String port_num, int file_length){   //construct url key/value pairs
		
		setFile_length(file_length);
		
		String info_hash = new String(info_hash_byteArray);
		String peer_id = "&peer_id=" + randomID();
		String port = "&port=" + port_num;
		String downloaded = "&downloaded=" + getTotal_downloaded();
		String uploaded = "&uploaded=" + getTotal_uploaded();
		String left =  "&left=" + getLeft();
	
		setUrl(announce_url + info_hash + peer_id + port + downloaded + uploaded+ left);
	}
	
	private void encodeURL() throws Exception{
		setUrl_encoded(URLEncoder.encode(getUrl(), "UTF-8"));
		System.out.println(getUrl_encoded());
	}
	private String sendGetRequest() throws Exception{   
		
		URL obj = new URL(url);
		int contentLength = obj.openConnection().getContentLength();
		System.out.println("contentLength: " + contentLength);
		
		return "yo";
	}
	
	private static String randomID(){
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

	private int getTotal_downloaded() {
		return total_downloaded;
	}

	private void setTotal_downloaded(int total_downloaded) {
		this.total_downloaded = total_downloaded;
	}

	private int getFile_length() {
		return file_length;
	}

	private void setFile_length(int file_length) {
		this.file_length = file_length;
	}

	private String getUrl() {
		return url;
	}

	private void setUrl(String url) {
		this.url = url;
	}

	private String getUrl_encoded() {
		return url_encoded;
	}

	private void setUrl_encoded(String url_encoded) {
		this.url_encoded = url_encoded;
	}

	private int getTotal_uploaded() {
		return total_uploaded;
	}

	private void setTotal_uploaded(int totat_uploaded) {
		this.total_uploaded = totat_uploaded;
	}

	private int getLeft() {
		return left;
	}

	private void setLeft() {
		this.left = getFile_length() - getTotal_downloaded();
	}

}
