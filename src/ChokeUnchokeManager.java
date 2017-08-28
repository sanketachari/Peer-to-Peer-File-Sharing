import java.util.*;
import java.util.concurrent.*;

public class ChokeUnchokeManager implements Runnable{

	private static ChokeUnchokeManager manager = null;

	public ScheduledFuture<?> task = null;

	private int numberOfPreferredNeighbors;

	private ScheduledExecutorService taskScheduler = null;

	private Starter starter = null;

	private P2PLogger Log;
	
	public void start(int startDelay, int intervalDelay) {

		task = taskScheduler.scheduleAtFixedRate(this, startDelay, intervalDelay, TimeUnit.SECONDS);
	}
	
	public static synchronized ChokeUnchokeManager createInstance(Starter starter) {

		if (manager == null) {
			if (starter != null)
			{
				manager = new ChokeUnchokeManager();
				manager.taskScheduler = Executors.newScheduledThreadPool(1);
				manager.numberOfPreferredNeighbors = starter.getNumberOfPreferredNeighbors();
				manager.starter = starter;

				manager.Log = starter.getLogger();
			}
			else
			{
				return null;
			}
			
		}

		return manager;

	}

	public void run() {


		HashMap<Integer, Double> neighborsDownloadSpeed = starter.getPeersDownloadSpeed();


		if (numberOfPreferredNeighbors < neighborsDownloadSpeed.size()) {

			ArrayList<Integer> unchokePeersList = new ArrayList<>();
			ArrayList<Integer> chokedPeerList = new ArrayList<>();
			int count = 0;

			Set<Map.Entry<Integer, Double>> entrySet = neighborsDownloadSpeed.entrySet();

			Map.Entry<Integer, Double>[] tempArray = new Map.Entry[neighborsDownloadSpeed.size()];
			tempArray = entrySet.toArray(tempArray);

			if (starter.isFileExists){

				HashSet<Integer> peersToUnchoke = new HashSet<>();
				Random random = new Random();

				while (count < numberOfPreferredNeighbors){

					if (peersToUnchoke.add(tempArray[(random.nextInt(neighborsDownloadSpeed.size()))].getKey()))
						count++;
				}

				for (int key: peersToUnchoke){
					unchokePeersList.add(key);
				}

				for (int key: neighborsDownloadSpeed.keySet()){

					if (!unchokePeersList.contains(key))
						chokedPeerList.add(key);
				}
			}
			else {

				int len = tempArray.length;
				for (int i = 0; i < len; i++) {

					for (int j = i + 1; j < len; j++) {

						if (tempArray[i].getValue().compareTo(tempArray[j].getValue()) == -1) {

							Map.Entry<Integer, Double> tempEntry = tempArray[i];
							tempArray[i] = tempArray[j];
							tempArray[j] = tempEntry;
						}
					}
				}


				LinkedHashMap<Integer, Double> sortedSpeedMap = new LinkedHashMap<>();

				for (int i = 0; i < len; i++)
					sortedSpeedMap.put(tempArray[i].getKey(), tempArray[i].getValue());

				for (Map.Entry<Integer, Double> entry : sortedSpeedMap.entrySet()) {

					unchokePeersList.add(entry.getKey());
					count++;

					if (count == numberOfPreferredNeighbors)
						break;
				}

				for (int peerId : unchokePeersList)
					sortedSpeedMap.remove(peerId);

				chokedPeerList.addAll(sortedSpeedMap.keySet());

			}


			String message = "Peer [" + starter.peerId + "] has the preferred neighbors [";

			int unchokePeerListSize = unchokePeersList.size();

			for (int i = 0; i < unchokePeerListSize; i++){

				if (i == unchokePeerListSize - 1)
					message += unchokePeersList.get(i);

				else
					message += unchokePeersList.get(i) + ", ";
			}

			message += "]";

			Log.info(message);

			starter.unchokePeers(unchokePeersList);
			starter.chokePeers(chokedPeerList);
		}
	}

	
}
