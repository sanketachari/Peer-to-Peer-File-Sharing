
public class MessageDetails implements Message {

	private int messageType;
	private int pieceIndex;
	private int mLength;
	private int payLoad;
	private byte[] payLoadBytes;

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public int getPieceIndex() {
		return pieceIndex;
	}

	public void setPieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	public void setmLength(int mLength) {
		this.mLength = mLength;
	}

	public int getPayLoad() {
		return payLoad;
	}

	public void setPayLoad(int payLoad) {
		this.payLoad = payLoad;
	}

	public byte[] getPayLoadBytes() {
		return payLoadBytes;
	}

	public void setPayLoadBytes(byte[] payLoadBytes) {
		this.payLoadBytes = payLoadBytes;
	}
}
