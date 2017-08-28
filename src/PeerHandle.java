import java.io.*;
import java.net.Socket;

public class PeerHandle extends Thread {


	private Socket neighborPeerSocket;
	private Starter starter;
	private int downloadedData;
	private long downloadStartTime;

	private ObjectInputStream in;
	private ObjectOutputStream out;

	public int peerId;
	public boolean isChoked;
	public MessageHandler messageHandler;
	public boolean isPeerDead;
	public boolean isHandshakeACKReceived;
	public NeighborDataHandler neighborDataHandler;
	private P2PLogger Log;


	public synchronized static PeerHandle createPeerConnection(Socket socket, Starter starter) {

		if (socket == null || starter == null)
			return null;

		PeerHandle peerHandle = new PeerHandle();

		peerHandle.neighborPeerSocket = socket;
		peerHandle.starter = starter;
		peerHandle.peerId = starter.neighborId;
		peerHandle.neighborDataHandler = starter.dataHandlers.get(peerHandle.peerId);

		try {

			peerHandle.out = new ObjectOutputStream(peerHandle.neighborPeerSocket.getOutputStream());
			peerHandle.out.flush();
			peerHandle.in = new ObjectInputStream(peerHandle.neighborPeerSocket.getInputStream());

		} catch (IOException ioException) {
			System.out.println("Disconnect with peer " + peerHandle.peerId);
		}

		peerHandle.messageHandler = MessageHandler.createInstance(starter, peerHandle);

		new Thread(peerHandle.messageHandler).start();

		peerHandle.Log = starter.getLogger();
		return peerHandle;
	}

	@Override
	public void run() {

		try {

			System.out.println("Establishing TCP connection between " + starter.peerId + " & " + peerId);

			sendHandShakeMessage();
			receiveHandShakeMessage((HandShakeMessage) in.readObject());


			while (true) {

				if (in == null) {

					isPeerDead = true;
					starter.peersList.add(peerId);
					return;
				}

				Message message = (Message) in.readObject();

				if (message == null)
					continue;

				switch (message.getMessageType()) {

					case MessageConstants.BITFIELD:
						receiveBitFieldMessage((MessageDetails) message);
						break;

					case MessageConstants.INTERESTED:
						receiveInterestedMessage((MessageDetails) message);
						break;

					case MessageConstants.NOTINTERESTED:
						receiveNotInterestedMessage((MessageDetails) message);
						break;

					case MessageConstants.REQUEST:
						receiveRequestMessage((MessageDetails) message);
						break;

					case MessageConstants.PIECE:
						receivePieceMessage((MessageDetails) message);
						break;

					case MessageConstants.HAVE:
						receiveHaveMessage((MessageDetails) message);
						break;

					case MessageConstants.CHOKE:
						if (!isChoked) {
							System.out.println("Peer " + starter.peerId + " is choked by " + peerId);
							Log.info("Peer [" + starter.peerId + "] is choked by [" + peerId + "]");
							isChoked = true;
						}
						break;

					case MessageConstants.UNCHOKE:
						if (isChoked) {
							System.out.println("Peer " + starter.peerId + " is unchoked by " + peerId);
							Log.info("Peer [" + starter.peerId + "] is unchoked by [" + peerId + "]");
							isChoked = false;
							try {
								messageHandler.messageQ.put((MessageDetails) message);
							} catch (InterruptedException ie) {
								ie.printStackTrace();
							}
						}
						break;

					case MessageConstants.SHUTDOWN:
						starter.peersList.add(peerId);
						break;
				}
			}

		} catch (IOException io) {

			//System.out.println("IO Exception in PeerHandle" );
			isPeerDead = true;
			starter.peersList.add(peerId);
		} catch (ClassNotFoundException cnf) {

			System.out.println("Class Not Found exception in PeerHandle");
			cnf.printStackTrace();
		}

	}

	private void receiveHandShakeMessage(HandShakeMessage message) {

		while (true) {

			try {
				if (isValidHandShakeMessage(message)) {

					isHandshakeACKReceived = true;
					Log.info("Peer " + starter.peerId + " is connected from Peer " + peerId + ".");
					System.out.println("Received Handshake message from client: " + peerId);
					sendBitFieldMessage();
					return;
				}

				message = (HandShakeMessage) in.readObject();

			} catch (Exception ex) {

				System.out.println("Error occurred while receiving the handshake");

				break;
			}

		}
	}

	private void receiveBitFieldMessage(MessageDetails bfMessage) {

		try {

			System.out.println("Received bit field message, payload is " + Integer.reverse(bfMessage.getPayLoad())
					+ " from client " + peerId);

			downloadedData = 0;
			downloadStartTime = System.currentTimeMillis();
			Thread.sleep(2000);
			messageHandler.messageQ.put(bfMessage);

		} catch (Exception ex) {
			System.out.println("Error occurred while receiving the bitfieldMessage");
		}
	}

	private void receiveInterestedMessage(MessageDetails interestedMessage) {

		if (!isChoked)
			Log.info("Peer [" + starter.peerId + "] received the 'interested' message from [" + peerId + "]");
	}

	private void receiveNotInterestedMessage(MessageDetails notInterestedMessage) {

		if (!isChoked)
			Log.info("Peer [" + starter.peerId + "] received the 'not interested' message from [" + peerId + "]");
	}

	private void receiveRequestMessage(MessageDetails requestMessage) {

		if (!isChoked)
			sendPieceMessage(requestMessage);
	}

	private void receivePieceMessage(MessageDetails pieceMessage) {

		if (!isChoked) {

			if (starter.dataHandler.checkIfPiecePresent(pieceMessage.getPieceIndex())) {

				// Ignoring the received piece message as it was already present
				return;
			}

			downloadedData += pieceMessage.getPayLoadBytes().length;

			starter.dataHandler.writeData(pieceMessage.getPieceIndex(), pieceMessage.getPayLoadBytes());

			Log.info("Peer [" + starter.peerId + "] has downloaded the piece [" + pieceMessage.getPieceIndex() +
					"] from [" + peerId + "]. Now the number of pieces it has is " +
					(starter.dataHandler.getDownloadedPiecesCount()));

			System.out.println("Received piece message for piece index [" + pieceMessage.getPieceIndex() +
					"] from client " + peerId);

			starter.sendHaveMessageToNeighbor(pieceMessage.getPieceIndex());

			try {
				messageHandler.messageQ.put(pieceMessage);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
	}

	private void receiveHaveMessage(MessageDetails haveMessage) {

		if (!isChoked) {

			Log.info("Peer [" + starter.peerId +
					"] received the 'have' message from [" + peerId +
					"] for the piece " + haveMessage.getPieceIndex());
			try {
				messageHandler.messageQ.put(haveMessage);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

	}

	private synchronized void sendHandShakeMessage() {

		HandShakeMessage hndMessage = new HandShakeMessage();
		hndMessage.setPeerId(starter.peerId);

		try {

			out.writeObject(hndMessage);
			out.flush();
			System.out.println("Send HandShake message: " + hndMessage.getHeader() + hndMessage.getZeros() + hndMessage.getPeerId() +
					" to peer " + peerId);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private synchronized void sendBitFieldMessage() {

		MessageDetails bfMessage = new MessageDetails();
		bfMessage.setPayLoad(starter.dataHandler.getBitFieldPayload());
		bfMessage.setMessageType(MessageConstants.BITFIELD);

		try {
			out.writeObject(bfMessage);
			out.flush();

		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

	}

	public synchronized void sendNotInterestedMessage() {

		if (!isChoked) {

			MessageDetails notInterestedMessage = new MessageDetails();
			notInterestedMessage.setMessageType(MessageConstants.NOTINTERESTED);
			notInterestedMessage.setmLength(1);

			try {

				out.writeObject(notInterestedMessage);
				out.flush();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public synchronized void sendInterestedMessage(int missingPieceIndex) {

		if (!isChoked) {

			MessageDetails interestedMessage = new MessageDetails();
			interestedMessage.setMessageType(MessageConstants.INTERESTED);
			interestedMessage.setmLength(1);

			try {

				out.writeObject(interestedMessage);
				out.flush();

				sendRequestMessage(missingPieceIndex);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public synchronized void sendRequestMessage(int pieceIndex) {


		MessageDetails requestMessage = new MessageDetails();
		requestMessage.setMessageType(MessageConstants.REQUEST);
		requestMessage.setPieceIndex(pieceIndex);

		try {

			out.writeObject(requestMessage);
			out.flush();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public synchronized void sendPieceMessage(MessageDetails requestMessage) {

		try {

			MessageDetails pieceMessage = new MessageDetails();
			pieceMessage.setMessageType(MessageConstants.PIECE);
			pieceMessage.setPieceIndex(requestMessage.getPieceIndex());
			pieceMessage.setPayLoadBytes(starter.dataHandler.getPieceData(requestMessage.getPieceIndex()));

			out.writeObject(pieceMessage);
			out.flush();
			Thread.sleep(1000);

		} catch (Exception ex) {

			ex.printStackTrace();
		}
	}

	public synchronized void sendHaveMessage(int pieceIndex) {

		try {

			MessageDetails haveMessage = new MessageDetails();
			haveMessage.setPieceIndex(pieceIndex);
			haveMessage.setMessageType(MessageConstants.HAVE);
			haveMessage.setmLength(5);

			out.writeObject(haveMessage);
			out.flush();

		} catch (IOException io) {
			io.printStackTrace();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public synchronized void sendChokeMessage() {

		try {

			if (!isChoked && !isPeerDead) {

				downloadStartTime = System.currentTimeMillis();
				downloadedData = 0;

				MessageDetails chokeMessage = new MessageDetails();
				chokeMessage.setMessageType(MessageConstants.CHOKE);

				out.writeObject(chokeMessage);
				out.flush();

				isChoked = true;
				//System.out.println("Choking peer " + peerId);

			}

		} catch (IOException io) {
			//io.printStackTrace();
			isPeerDead = true;
			starter.peersList.add(peerId);

		} catch (Exception ex) {
			//ex.printStackTrace();
			isPeerDead = true;
			starter.peersList.add(peerId);
		}

	}

	public synchronized void sendUnchokeMessage() {

		try {


			if (isChoked && !isPeerDead) {

				downloadStartTime = System.currentTimeMillis();
				downloadedData = 0;

				MessageDetails unchokeMessage = new MessageDetails();
				unchokeMessage.setMessageType(MessageConstants.UNCHOKE);

				out.writeObject(unchokeMessage);
				out.flush();

				isChoked = false;
			}

		} catch (IOException io) {
			//io.printStackTrace();

			isPeerDead = true;
			starter.peersList.add(peerId);
		} catch (Exception ex) {
			//ex.printStackTrace();

			isPeerDead = true;
			starter.peersList.add(peerId);
		}
	}

	public synchronized void sendShutdownMessage() {

		try {

			MessageDetails shutdownMessage = new MessageDetails();
			shutdownMessage.setMessageType(MessageConstants.SHUTDOWN);

			out.writeUnshared(shutdownMessage);
			out.flush();

		} catch (IOException io) {
			//io.printStackTrace();
			isPeerDead = true;
			starter.peersList.add(peerId);

		} catch (Exception ex) {
			//ex.printStackTrace();
			isPeerDead = true;
			starter.peersList.add(peerId);

		}

	}

	private boolean isValidHandShakeMessage(HandShakeMessage hndMessage) {

		if (hndMessage.getHeader().equals(MessageConstants.HANDSHAKE_HEADER) &&
				hndMessage.getZeros().equals(MessageConstants.HANDSHAKE_ZEROS)
				&& hndMessage.getPeerId() == peerId)
			return true;

		System.out.println("Invalid Handshake message is received: ");
		return false;
	}

	public double getDownloadSpeed() {

		long totalTime = System.currentTimeMillis() - downloadStartTime;
		return downloadedData / (totalTime * 1.0);
	}

}
