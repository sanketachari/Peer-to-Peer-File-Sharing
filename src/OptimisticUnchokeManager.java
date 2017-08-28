import java.util.*;
import java.util.concurrent.*;

public class OptimisticUnchokeManager implements Runnable {

	public ScheduledFuture<?> task = null;
	public ScheduledExecutorService task_scheduler = null;
	public boolean isInitialized = true;
	private static OptimisticUnchokeManager manager = null;
	private Starter starter = null;

	private boolean initialize() {

		task_scheduler = null;
		task_scheduler = Executors.newScheduledThreadPool(1);

		return true;
	}

	public static synchronized OptimisticUnchokeManager createInstance(Starter starter) {

		boolean managerExists = false;

		if (!managerExists) {

			if (manager == null) {

				if (starter == null)
					return null;

				manager = new OptimisticUnchokeManager();

				if (!manager.initialize()) {

					manager.task.cancel(true);
					manager = null;
					return null;
				}

				manager.starter = starter;
			}
		}

		return manager;
	}

	public void run() {

		ArrayList<Integer> chokedPeerList = null;
		chokedPeerList = starter.chokedPeerList;



		if (chokedPeerList.size() > 0) {

			Random random = new Random();
			starter.optimisticUnchokeOnePeer(chokedPeerList.get(random.nextInt(chokedPeerList.size())));
		}

		starter.checkPeersFileDownloadComplete();

		if (starter.dataHandler.checkIfFileDownloadComplete())
			starter.sendShutdownMessage();

	}
}
