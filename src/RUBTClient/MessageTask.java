package RUBTClient;

public class MessageTask {
	
	private final Peer peer;
	private final byte[] message;
	
	public MessageTask(final Peer peer, final byte[] message){
		this.peer = peer;
		this.message = message;
	}
	
	public Peer getPeer(){
		return this.peer;
	}
	
	public byte[] getMessage() {
		return this.message;
	}
}
