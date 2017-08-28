import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MessageHandler implements Runnable {

	private Starter starter;
	private PeerHandle peerHandler;

	public BlockingQueue<MessageDetails> messageQ;

	public static MessageHandler createInstance(Starter starter, PeerHandle peerHandler) {

		if (starter == null || peerHandler == null) {
			return null;
		}

		MessageHandler messageHandler = new MessageHandler();
		messageHandler.messageQ = new ArrayBlockingQueue<>(MessageConstants.QUEUE_SIZE);

		messageHandler.starter = starter;
		messageHandler.peerHandler = peerHandler;

		return messageHandler;
	}

	public void run() {


		while (true) {
			
			try {

				MessageDetails message = messageQ.take();

				if (message.getMessageType() ==  MessageConstants.BITFIELD) {

					int missingPieceIndex = starter.dataHandler.getRandomPieceIndex(peerHandler.neighborDataHandler);

					if (missingPieceIndex == -1) {
						peerHandler.sendNotInterestedMessage();
					} else {
						peerHandler.sendInterestedMessage(missingPieceIndex);
					}
				}

				if (message.getMessageType() ==  MessageConstants.HAVE) {

					int pieceIndex = message.getPieceIndex();

					peerHandler.neighborDataHandler.setPieceIndex(pieceIndex, true);

					if (starter.dataHandler.checkIfPiecePresent(pieceIndex))
						peerHandler.sendNotInterestedMessage();

					else
						peerHandler.sendInterestedMessage(pieceIndex);
				}

				if (message.getMessageType() ==  MessageConstants.PIECE) {

					int missingPieceIndex = starter.dataHandler.getRandomPieceIndex(peerHandler.neighborDataHandler);

					if (missingPieceIndex != -1)
							peerHandler.sendInterestedMessage(missingPieceIndex);
				}

				if (message.getMessageType() ==  MessageConstants.UNCHOKE) {

					int missingPieceIndex = starter.dataHandler.getRandomPieceIndex(peerHandler.neighborDataHandler);

					if (missingPieceIndex != -1)
						peerHandler.sendInterestedMessage(missingPieceIndex);
				}

			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}

}
