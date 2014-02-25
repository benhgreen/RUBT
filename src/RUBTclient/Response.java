package RUBTclient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

public class Response {
	
	String message;
	Peer[] peers;
	List peerdict;
	Integer downloaded;
	Integer complete;
	Integer min_interval;
	Integer interval;
	
	public Response(String getrequest) {
		super();
		this.message = message;
		this.peers = peers;
		this.peerdict = peerdict;
		this.downloaded = downloaded;
		this.complete = complete;
		this.min_interval = min_interval;
		this.interval = interval;
		
		
		
		Map peerdict = null;
		
		
		
		try {
			peerdict = (Map) Bencoder2.decode(getrequest.getBytes());
			System.out.println("printing");
			ToolKit.printMap(peerdict, 0);
		} catch (BencodingException e) {
			System.err.println("Error decoding response to GET request.");
			e.printStackTrace();
		}
		
		int depth = 0;
		
		
		final Iterator i = peerdict.keySet().iterator();
        Object key = null;
        for (int k = 0; k < depth; k++)
            System.out.print("  ");
        System.out.println("Dictionary:");
        while (i.hasNext() && (key = i.next()) != null)
        {
        	System.out.println("key class " + key.getClass().getName());
            System.out.println("object class " + peerdict.get(key).getClass().getName());
        }
	}

}
