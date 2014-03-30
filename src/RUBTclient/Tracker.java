package RUBTclient;


import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.nio.ByteBuffer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
/**
 * @author Manuel Lopez
 * @author Ben Green
 * @author Christopher Rios
 *
 */

/**GetRequest object that manages the connection to tracker and the url
 */
public class Tracker {

	private int 			port_num; 			//port number of tracker
	private int 			file_length;		//file length of target file held by peer
	private int 			downloaded;			//number of bytes downloaded from peer
	private int	 			uploaded;			//number of bytes uploaded to peer
	private String 			url; 				//url contructed by contruct url class to for get request to tracker
	private String			encodedInfoHash;	//escaped info hash of torrent info 
	private static String 	usrid;				//identifying peer id for client
	
	
	/**GetRequest contructor
	 */
	public Tracker(){
		downloaded = 0;
		uploaded = 0;
		randomID();
	}

	/** constructURL() builds the initial url to contact the tracker
	 * @param announce_url based url extracted from torrentinfo
	 * @param info_hash byte[] extracted from torrentinfo
	 * @param port_num  port number extracted from torreninfo
	 * @param file_length file length in bytes extracted from torrentinfo
	 */
	public void constructURL(String announce_url, ByteBuffer info_hash, int port_num, int file_length){   //construct url key/value pairs
		
		setPort_num(port_num);
		setFile_length(file_length);
		String info_hash_encoded = "?info_hash=" + encodeHash(info_hash);
		String peer_id = "&peer_id=" + usrid;
		String port = "&port=" + port_num;
		String download_field = "&downloaded=" + downloaded;
		String uploaded = "&uploaded=" + 0;
		String left =  "&left=" + file_length;
	
		setUrl(announce_url + info_hash_encoded + peer_id + port + download_field + uploaded+ left);
	}
	
	/**encodeHash() escapes the info hash for sending to the tracker 
	 * @param info_hash ByteBuffer extracted from torrentinfo
	 * @return String encodedHash with escaped hex characters
	 */
	public String encodeHash(ByteBuffer info_hash){
		String hash = "";
		for(int i =0; i < 20; i++){
			hash = hash + "%" + String.format("%02x", info_hash.get(i));
		}
		setEncodedInfoHash(hash);
		return hash;
	}
	
	/**sendEvent() sends an event message to the tracker
	 * @param event String of the event("started","stopped", "completed")
	 * @param current_downloaded indicates how many bytes have been succesfully downloaded from the peer
	 * @return 1 when successful and 0  when failed
	 * @throws Exception IOException when opening connection to tracker
	 */
	public int sendEvent(String event, int current_downloaded) throws Exception{
		
		setDownloaded(current_downloaded);
		URL obj;
		if(event.equals("started")){
			obj = new URL(getUrl() + "&event=" + event);
		}else if(event.equals("completed") || event.equals("stopped")){
			obj = new URL(getUrl().substring(0,getUrl().indexOf("downloaded")) + "downloaded=" + downloaded + "&uploaded=" + getUploaded() + "&left=" + (getFile_length()-getDownloaded()) + "&event="+event);
		}else{
			return 0;
		}
		
		URLConnection connection = obj.openConnection();
		System.out.println(event + " event sent to tracker");
		return 1;
	}
	
	/**sendGetRequest() takes the contructed URL, makes a URL object, and connects to the tracker for a response
	 * @return bencoded response of peer list
	 * @throws Exception IOException when opening connection to tracker
	 */
	public byte[] sendGetRequest() throws Exception{   
		
		URL obj = new URL(getUrl());
		URLConnection connection = obj.openConnection();

		int contentLength = connection.getContentLength();
		
		DataInputStream datastream = new DataInputStream(connection.getInputStream());
		ByteArrayOutputStream tracker = new ByteArrayOutputStream();
		int tracker_response = datastream.read();
		String bencoded_response = "";
		while(tracker_response!=-1)
		{
			tracker.write(tracker_response);
			tracker_response=datastream.read();
		}
		tracker.close();
		return tracker.toByteArray();
	}
	
	/**RandomID generates random alphanumeric peer_id for client and assigns to peer_id field
	 */
	public void randomID(){
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
	
	 /**@return GetRequest.userid
	 */
	public String getUser_id()
	{
		return usrid;
	}
	
	/** @return this.url
	 */
	public String getUrl() {
		return url;
	}

	/**@param url String to be set to this.url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**@return this.port_nuil
	 */
	public int getPort_num() {
		return port_num;
	}

	/**@param port_num int port number to be set to this.port_num
	 */
	public void setPort_num(int port_num) {
		this.port_num = port_num;
	}

	/**@return this.getEncodedHash
	 */
	public String getEncodedInfoHash() {
		return encodedInfoHash;
	}
	
	
	/** @param encodedInfoHash String encoded hash to be set to this.encodedInfoHash
	 */
	public void setEncodedInfoHash(String encodedInfoHash) {
		this.encodedInfoHash = encodedInfoHash;
	}

	/** @return this.getFile_length
	 */
	public int getFile_length() {
		return file_length;
	}

	/** @param file_length int of file length bytes to be set to this.file_length
	 */
	public void setFile_length(int file_length) {
		this.file_length = file_length;
	}

	/** @return this.getDownloaded
	 */
	public int getDownloaded() {
		return downloaded;
	}

	/** @param downloaded int of downloaded bytes to be set to this.downloaded
	 */
	public void setDownloaded(int downloaded) {
		this.downloaded = downloaded;
	}

	/** @return this.getUploaded
	 */
	public int getUploaded() {
		return uploaded;
	}

	/** @param uploaded int of uploaded bytes to be set to this.uploaded
	 */
	public void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}
}