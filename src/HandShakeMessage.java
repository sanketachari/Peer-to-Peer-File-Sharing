
public class HandShakeMessage implements Message{

	private String header = "P2PFILESHARINGPROJ";
	private String zeros =  "0000000000";
	private int peerId;

	public int getPeerId() {
		return peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public String getHeader() {
		return header;
	}

	public String getZeros() {
		return zeros;
	}

	public int getMessageType(){

		return MessageConstants.HANDSHAKE;
	}
}
