import java.io.*;
import java.util.LinkedHashMap;

public class PeerInfoConfig{

	public static LinkedHashMap<String, PeerSettings> peerConfigMap = new LinkedHashMap<>();

	static PeerInfoConfig config;

	public static PeerInfoConfig createInstance() {

		if (config == null) {

			config = new PeerInfoConfig();
			setPeerInfoConfigurations();
		}

		return config;
	}

	private static void setPeerInfoConfigurations() {

		try {

			BufferedReader br = new BufferedReader(new FileReader("./PeerInfo.cfg"));

			String line;

			while ((line = br.readLine()) != null) {

				String[] info = line.split(" ");

				peerConfigMap.put(info[0], new PeerSettings(info[1], info[2], info[3]));

				//System.out.println(line);
			}
		} catch (FileNotFoundException fnf) {

			System.out.println("PeerInfo.cfg is not found");
		} catch (IOException io) {
			System.out.println("IO exception occurred while setting Common.cfg");
		}

		System.out.println("\nPeerInfoConfig read done");
	}

}

class PeerSettings{

	private String hostName;
	private int port;
	private String fileExists;


	public PeerSettings(String hostName, String port, String fileExists) {

		this.setHostName(hostName);
		this.setPort(port);
		this.setFileExists(fileExists);
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}

	public boolean fileExists(){

		return fileExists.equals("1")? true : false;
	}

	public void setFileExists(String fileExists) {
		this.fileExists = fileExists;
	}
}



