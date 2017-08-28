
public class MessageConstants {

	public static final int CHOKE = 0;
	public static final int UNCHOKE = 1;
	public static final int INTERESTED = 2;
	public static final int NOTINTERESTED = 3;
	public static final int HAVE = 4;
	public static final int BITFIELD = 5;
	public static final int REQUEST = 6;
	public static final int PIECE = 7;
	public static final int SHUTDOWN = 8;
	public static final int HANDSHAKE = 9;

	public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
	public static final String HANDSHAKE_ZEROS = "0000000000";

	public static final int QUEUE_SIZE = 500;

	public static synchronized MessageConstants createInstance(){

		MessageConstants messageConstants = new MessageConstants();
		return messageConstants;
	}
}
