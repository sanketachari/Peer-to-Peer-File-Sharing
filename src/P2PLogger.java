
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class P2PLogger extends Logger {

	public static P2PLogger Log = null;
	private SimpleDateFormat formatter = null;

	private String logFileName;
	private FileHandler fileHandler;
	private String peerID;

	@Override
	public synchronized void log(Level level, String msg) {

		super.log(level, msg + "\n");
	}

	public P2PLogger(String peerId, String logFileName, String name) {

		super(name, null);
		this.peerID = peerId;
		this.logFileName = logFileName;
		this.setLevel(Level.FINEST);
	}

	public void warning(String msg) {

		Calendar c = Calendar.getInstance();
		String dateInStringFormat = formatter.format(c.getTime());

		this.log(Level.WARNING, "[" + dateInStringFormat + "]: Peer peer_ID " + peerID + " " + msg);
	}
	
	public void initialize() throws SecurityException, IOException {

		fileHandler = new FileHandler(logFileName);
		fileHandler.setFormatter(new SimpleFormatter());
		String dateFormat = "dd MMM yyyy hh:mm:ss:SSS a";
		formatter = new SimpleDateFormat(dateFormat);
		this.addHandler(fileHandler);
	}

	public synchronized void info(String msg) {

		Calendar c = Calendar.getInstance();
		String dateInStringFormat = formatter.format(c.getTime());
		String output = "[" + dateInStringFormat + "] : " + msg;
		this.log(Level.INFO, output);
	}

	
	public void close() {
		try {
			if (fileHandler != null) {
				fileHandler.close();
			}
		} catch (Exception e) {
			System.out.println("Unable to close Log.");
			e.printStackTrace();
		}
	}

	

	
	public static P2PLogger getLogger(String peerId) {

		if (Log == null) {

			Log = new P2PLogger(peerId, "Peer_" + peerId + ".log", "P2PLogger");
			try {
				Log.initialize();
			} catch (Exception e) {
				Log.close();
				Log = null;
				System.out.println("Unable to create or initialize Log");
				e.printStackTrace();
			}
		}
		return Log;
	}
}
