package RUBTclient;

import java.nio.ByteBuffer;
import java.util.*;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

public class Response {
	
	String message;
	ArrayList<Peer> peers = new ArrayList<Peer>();
	List peerdict;
	Integer downloaded;
	Integer complete;
	Integer min_interval;
	Integer interval;
	
	public Response(String getrequest) {
		super();
		
		Map peerdict = null;
		
		try {
			peerdict = (Map) Bencoder2.decode(getrequest.getBytes());
//			System.out.println("printing");
//			ToolKit.printMap(peerdict, 0);
		} catch (BencodingException e) {
			System.err.println("Error decoding response to GET request.");
			e.printStackTrace();
		}
		
		final Iterator i = peerdict.keySet().iterator();
        Object key = null;
        while (i.hasNext() && (key = i.next()) != null)
        {
        	String string_key = asString((ByteBuffer) key);
            if (string_key.equals("peers")){
            	
            	ArrayList peerlist = (ArrayList) peerdict.get(key);
            	
            	Iterator iter = peerlist.iterator();
            	
            	while(iter.hasNext()){
            		HashMap peer = (HashMap) iter.next();
            		Iterator j = peer.keySet().iterator();
            		
            		String temp_peer_id = null;
        			String temp_ip = null;
        			Integer temp_port = null;
            		
            		while(j.hasNext()){
            			
            			Object next = j.next();
            			
            			String argh = asString((ByteBuffer) next);
            			
            			if(argh.equals("peer id")){
            				temp_peer_id = asString((ByteBuffer) peer.get(next));
            			}else if(argh.equals("port")){
            				temp_port = (Integer) peer.get(next);
            			}else if(argh.equals("ip")){
            				temp_ip = asString((ByteBuffer) peer.get(next));
            			}
            			
            		}
        			
        			Peer new_peer = new Peer(temp_ip, temp_peer_id, temp_port);
        			
        			this.peers.add(new_peer);
            	}
            	
            }else if(string_key.equals("interval")){
            	this.interval = (Integer) peerdict.get(key);
            }else if(string_key.equals("min interval")){
            	this.min_interval = (Integer) peerdict.get(key);
            }
        }
	}
	
	private static String asString(ByteBuffer buff){
		  StringBuilder sb = new StringBuilder();
		  byte[] b = buff.array();
		  for(int i = 0; i < b.length; ++i){
		    if(b[i] > 31 || b[i] < 127){
		      sb.append((char)b[i]);
		    }else {
		      sb.append(String.format("%02x",b[i]));
		    }
		  }
		  return sb.toString();
		}

	public void printPeers() {
		Iterator<Peer> iter = this.peers.iterator();
		
		while(iter.hasNext()){
			Peer temp = iter.next();
			
			System.out.println("IP: " + temp.ip);
			System.out.println("Port: " + temp.port);
			System.out.println("Peer ID: " + temp.peer_id);
		}
		
	}

}
