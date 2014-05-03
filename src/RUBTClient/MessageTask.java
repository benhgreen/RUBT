package RUBTClient;

public class MessageTask {
	
	private final Peer peer;
	private final byte[] message;
	
	/**
	 * @param peer Peer who sent the message
	 * @param message message that was sent by that peer
	 */
	public MessageTask(final Peer peer, final byte[] message){
		this.peer = peer;
		this.message = message;
	}
	
	/**
	 * @return returns the peer of the MessageTask
	 */
	public Peer getPeer(){
		return this.peer;
	}
	
	/**
	 * @return message of the MessageTask
	 */
	public byte[] getMessage() {
		return this.message;
	}
}
