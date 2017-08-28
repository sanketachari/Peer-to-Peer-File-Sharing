import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Starter {

	int peerId;
	int neighborId;
	private boolean allPeersConnected;
	private static PeerInfoConfig peerConfig;
	static CommonConfig commonConfig;

	private static Starter starter;
	private static Server server;
	private static ChokeUnchokeManager chokeUnchokeManager;
	private static OptimisticUnchokeManager optimisticUnchokeManager;

	public int pieceSize;
	public int fileSize;
	public boolean isFileExists;
	public HashSet<Integer> peersList = new HashSet<>();
	public ArrayList<Integer> chokedPeerList = new ArrayList<>();
	public HashSet<PeerHandle> peerHandleForNeighbors = new HashSet<>();
	public HashMap<Integer, NeighborDataHandler> dataHandlers = new HashMap<>();
	public DataHandler dataHandler;
	private int optimisticallyUnchoked = -1;
	private P2PLogger Log;

	public static synchronized Starter createInstance(String peerID) {

		if (starter == null) {

			starter = new Starter();
			starter.peerId = Integer.parseInt(peerID);
			peerConfig = PeerInfoConfig.createInstance();
			commonConfig = CommonConfig.setCommonConfigurations();
			server = Server.createInstance(PeerInfoConfig.peerConfigMap.get(peerID).getPort(), starter);

			starter.fileSize = commonConfig.getFileSize();
			starter.pieceSize = commonConfig.getPieceSize();
			starter.isFileExists = PeerInfoConfig.peerConfigMap.get(peerID).fileExists();
			starter.dataHandler = DataHandler.createInstance(commonConfig, peerConfig, starter.peerId);
			starter.initializeDataHandlers();

			starter.Log = P2PLogger.getLogger(peerID);
		}

		return starter;
	}

	public void run() {

		new Thread(server).start();
		connectOtherPeers();

		chokeUnchokeManager = ChokeUnchokeManager.createInstance(starter);

		int chokeUnchokeInterval = commonConfig.getUnchokingInterval();
		chokeUnchokeManager.start(0, chokeUnchokeInterval);

		optimisticUnchokeManager = OptimisticUnchokeManager.createInstance(starter);
		int optimisticUnchokeInterval = commonConfig.getOptimisticUnchokingInterval();

		optimisticUnchokeManager.task = optimisticUnchokeManager.
				task_scheduler.
				scheduleAtFixedRate(optimisticUnchokeManager,
						10, optimisticUnchokeInterval, TimeUnit.SECONDS);
	}

	private void connectOtherPeers() {

		HashMap<String, PeerSettings> neighborPeerMap = null;
		neighborPeerMap = PeerInfoConfig.peerConfigMap;
		Set<String> peerIdList = neighborPeerMap.keySet();
		int peerSize = peerIdList.size();
		for (String neighborPeerId : peerIdList) {

			if (Integer.parseInt(neighborPeerId) < peerId) {

				neighborId = Integer.parseInt(neighborPeerId);

				PeerSettings peerInfo = neighborPeerMap.get(neighborPeerId);
				int neighborPortNumber = peerInfo.getPort();

				String neighborPeerHost = peerInfo.getHostName();

				try {

					Socket neighborPeerSocket = new Socket(neighborPeerHost, neighborPortNumber);
					PeerHandle neighborPeerHandler = PeerHandle.createPeerConnection(neighborPeerSocket, starter);

					if (neighborPeerHandler != null) {
						peerHandleForNeighbors.add(neighborPeerHandler);
						new Thread(neighborPeerHandler).start();
					}

				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				Log.info("Peer " + peerId + " makes a connection to Peer " + neighborPeerId);
			}
		}

		starter.allPeersConnected = true;
	}

	public List<Integer> getPotentialPeers() {

		HashMap<String, PeerSettings> neighborPeerMap = PeerInfoConfig.peerConfigMap;
		Set<String> peerIDList = neighborPeerMap.keySet();

		List<Integer> peerIds = new ArrayList<>();

		for (String neighborPeerID : peerIDList) {

			if (Integer.parseInt(neighborPeerID) > peerId) {
				peerIds.add(Integer.parseInt(neighborPeerID));
			}
		}

		return peerIds;
	}

	public synchronized void sendHaveMessageToNeighbor(int pieceIndex) {

		Set<PeerHandle> syncNeighborsHandle = Collections.synchronizedSet(peerHandleForNeighbors);

		for (PeerHandle peerHandle : syncNeighborsHandle) {

			if (!peerHandle.isPeerDead)
				peerHandle.sendHaveMessage(pieceIndex);
		}
	}

	public void initializeDataHandlers() {

		for (String neighbor : peerConfig.peerConfigMap.keySet()) {

			if (Integer.valueOf(neighbor) != starter.peerId)
				starter.dataHandlers.put(Integer.valueOf(neighbor),
						NeighborDataHandler.createInstance(commonConfig, peerConfig, Integer.valueOf(neighbor)));
		}
	}

	public int getNumberOfPreferredNeighbors() {

		return commonConfig.getNumberOfPreferredNeighbors();
	}

	public void unchokePeers(ArrayList<Integer> peerList) {

		for (int peerToBeUnChoked : peerList) {

			for (PeerHandle peerHandler : peerHandleForNeighbors) {

				if (peerHandler.peerId == peerToBeUnChoked && !peersList.contains(peerHandler.peerId)) {

					peerHandler.sendUnchokeMessage();
					break;
				}
			}
		}
	}

	public void chokePeers(ArrayList<Integer> peerList) {

		chokedPeerList = peerList;

		for (int peerToBeChoked : peerList) {

			for (PeerHandle peerHandler : peerHandleForNeighbors) {

				if (peerHandler.peerId == peerToBeChoked && !peersList.contains(peerHandler.peerId)) {
					//&& optimisticallyUnchoked != peerToBeChoked) {

					peerHandler.sendChokeMessage();
					break;
				}
			}
		}
	}

	public HashMap<Integer, Double> getPeersDownloadSpeed() {

		HashMap<Integer, Double> peersDownloadSpeed = new HashMap<>();

		for (PeerHandle peerHandler : peerHandleForNeighbors) {

			peersDownloadSpeed.put(peerHandler.peerId, peerHandler.getDownloadSpeed());
		}

		return peersDownloadSpeed;
	}

	public void optimisticUnchokeOnePeer(int peerToBeUnChoked) {

		for (PeerHandle peerHandler : peerHandleForNeighbors) {

			if (peerHandler.peerId == peerToBeUnChoked && !peersList.contains(peerHandler.peerId)) {

				System.out.println("Peer " + peerId + " has the optimistically unchoked neighbor " + peerToBeUnChoked);

				Log.info("Peer [" + peerId + "] has the optimistically unchoked neighbor [" + peerToBeUnChoked + "]");
				peerHandler.sendUnchokeMessage();
				optimisticallyUnchoked = peerToBeUnChoked;
				break;
			}
		}
	}

	public void checkPeersFileDownloadComplete() {

		System.out.println("check all peers file download " + peerId + " ,  " + peersList);

		if (server.isServerCompleted && allPeersConnected &&
				PeerInfoConfig.peerConfigMap.size() == peersList.size()) {

			System.out.println(peersList);

			chokeUnchokeManager.task.cancel(true);
			optimisticUnchokeManager.task.cancel(true);

			System.out.println("EXIT");
			Log.close();
			dataHandler.close();


			try {
				System.exit(0);
			} catch (Exception e) {
				System.out.println("ERROR");
			}
		}
	}

	public void sendShutdownMessage() {

		Log.info("Peer [" + peerId + "] has downloaded the complete file.");

		if (server.isServerCompleted && allPeersConnected) {

			peersList.add(peerId);

			for (PeerHandle peerHandle : peerHandleForNeighbors) {

				if (!peerHandle.isPeerDead)
					peerHandle.sendShutdownMessage();
				else
					peersList.add(peerHandle.peerId);
			}
		}
	}

	public synchronized P2PLogger getLogger() {
		return Log;
	}

}
