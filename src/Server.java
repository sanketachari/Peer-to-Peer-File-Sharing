import java.net.*;
import java.io.*;
import java.util.List;

public class Server implements Runnable {

	private static Server server;
	private int peerId;
	private int peerPort;
	private Starter starter;

	public boolean isServerCompleted;

	public static synchronized Server createInstance(int peerPortNo, Starter starter) {

		if (server == null) {

			System.out.println("Initializing Server");
			server = new Server();
			server.starter = starter;
			server.peerId = starter.peerId;
			server.peerPort = peerPortNo;
		}

		return server;
	}

	@Override
	public void run() {

		try {

			ServerSocket listener = new ServerSocket(peerPort);
			System.out.println("Server " + peerId + " is running ");

			List<Integer> neighbors = starter.getPotentialPeers();

			try {

				for (int neighbor : neighbors) {

					Socket socket = listener.accept();
					starter.neighborId = neighbor;
					PeerHandle peerHandler = PeerHandle.createPeerConnection(socket, starter);

					if (peerHandler != null) {

						new Thread(peerHandler).start();
						starter.peerHandleForNeighbors.add(peerHandler);
					}

					System.out.println("Client is connected: " + neighbor);
				}
				isServerCompleted = true;

			} finally {

				listener.close();
			}

		} catch (IOException io) {
			io.printStackTrace();
			System.out.println("Error Occurred while initializing server " + peerId);
		}
	}
}
