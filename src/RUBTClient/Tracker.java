package RUBTClient;

import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.ByteArrayOutputStream;


/**
 * @author Ben Green
 * @author Manuel Lopez
 * @author Christopher Rios
 */

/**Tracker object that manages the connection  and all communication to tracker
 */
public class Tracker {

	private int 			port; 				//port number of client is listening on
	private int 			file_length;		//file length of target file held by peer
	private int 			downloaded;			//number of bytes downloaded from peer
	private int	 			uploaded;			//number of bytes uploaded to peers
	private int 			interval;			//milliseconds expected between tracker announcements
	private String 			url; 				//url contructed for annoucning to the
	private String			encodedInfoHash;	//escaped info hash of torrent info 
	private byte[] 			usrid;				//identifying peer id for client
	
	
	/**Tracker constructor generates out client peer_id
	 * @param file_length Length of file specified by torrentinfo in bytes
	 */
	public Tracker(int file_length){
		this.downloaded = 0;
		this.uploaded = 0;
		this.file_length = file_length;
		randomID();
	}
	
	/**
	 * Updates downloaded and uploaded fields for tracker
	 * @param downloaded
	 * @param uploaded
	 */
	public void updateProgress(int downloaded, int uploaded){
		this.downloaded = downloaded;
		this.uploaded = uploaded;
	}

	/** constructURL() builds the initial url to contact the tracker
	 * @param announce_url based url extracted from torrentinfo
	 * @param info_hash byte[] extracted from torrentinfo
	 * @param port Port number that client is listening on for incoming connections
	 */
	public void constructURL(String announce_url, ByteBuffer info_hash, int port){   //construct url key/value pairs
		
		this.port = port;
		String info_hash_encoded = "?info_hash=" + encodeHash(info_hash);
		String peer_id = "&peer_id=" + usrid;
		String port_field = "&port=" + port;
		String download_field = "&downloaded=" + downloaded;
		String upload_field = "&uploaded=" + uploaded;
		String left =  "&left=" + (file_length - downloaded);
	
		//setUrl(announce_url + info_hash_encoded + peer_id + port + download_field + upload_field+ left);
		this.url = (announce_url + info_hash_encoded + peer_id + port_field + download_field + upload_field+ left);
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
	 * @param event name of event being passed to the tracker (start, stopped, completed, <blank>)
	 * @return bencoded response of peer list
	 * @throws Exception IOException when opening connection to tracker
	 */
	public byte[] requestPeerList(String event) throws Exception{   
		
		URL obj;
		if(event == null){
			obj = new URL(this.url); 
		}else{
			obj = new URL(this.url + "&event=" + event); 
		}
		URLConnection connection = obj.openConnection(); //sends request

		//int contentLength = connection.getContentLength();
		
		DataInputStream datastream = new DataInputStream(connection.getInputStream());
		ByteArrayOutputStream encoded_response = new ByteArrayOutputStream();
		int tracker_response = datastream.read();
		//String bencoded_response = "";
		while(tracker_response != -1){
			encoded_response.write(tracker_response);
			tracker_response=datastream.read();
		}
		encoded_response.close();
		return encoded_response.toByteArray();
	}
	
	/** Generates random alphanumeric peer_id  byte array for client and assigns to peer_id field
	 */
	public void randomID(){
		byte[] idHeader = {'G','R','O', 'U','P','0','4'};
		byte[] idTail = new byte[13];
		int randomKey;
		Random r = new Random();
		 
		for(int i = 0; i < 13; i++){
			randomKey = r.nextInt(36);
			if(randomKey < 26)
				idTail[i] = (byte)((char)(randomKey + 65));
			else
				idTail[i] = (byte)((char)(randomKey + 22));
		}
		byte id[] = new byte[20];
		System.arraycopy(idHeader, 0, id, 0, 7);
		System.arraycopy(idTail, 0, id, 7, 13);
		System.out.println(Response.asString(ByteBuffer.wrap(id)));
		this.usrid = id;
		
	}
	
	 /**@return user id of our client
	 */
	public byte[] getUser_id(){
		return usrid;
	}
	
	/** @return url used to contact tracker
	 */
	public String getUrl() {
		return url;
	}

	/**@param url URL used to contact tracker
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**@return port number kept by tracker
	 */
	public int getPort() {
		return this.port;
	}

	/**@param port Port number tracker will instruct peers to contact our client
	 */
	public void setPort_num(int port) {
		this.port = port;
	}

	/**@return encoded hash of torrent info
	 */
	public String getEncodedInfoHash() {
		return encodedInfoHash;
	}
	
	
	/** @param encodedInfoHash String encoded hash to be set to this.encodedInfoHash
	 */
	public void setEncodedInfoHash(String encodedInfoHash) {
		this.encodedInfoHash = encodedInfoHash;
	}

	/** @return length of file specified by torrent info
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

	/** @return uploaded Amount of bytes uploaded in current session
	 */
	public int getUploaded() {
		return uploaded;
	}

	/** @param uploaded Amount of bytes uploaded in current session
	 */
	public void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}
	
	/**
	 * @return interval Time until next tracker contact
	 */
	public int getInterval() {
		return this.interval;
	}	
	
	/**
	 * @param interval Time until next tracker contact
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}
}