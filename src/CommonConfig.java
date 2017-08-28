import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CommonConfig {

	private String FileName;
	private int NumberOfPreferredNeighbors;
	private int UnchokingInterval;
	private int OptimisticUnchokingInterval;
	private int FileSize;
	private int PieceSize;


	public CommonConfig(int NumberOfPreferredNeighbors, int UnchokingInterval,
						  int OptimisticUnchokingInterval, String FileName, int FileSize, int PieceSize) {

		this.setNumberOfPreferredNeighbors(NumberOfPreferredNeighbors);
		this.setUnchokingInterval(UnchokingInterval);
		this.setOptimisticUnchokingInterval(OptimisticUnchokingInterval);
		this.setFileName(FileName);
		this.setFileSize(FileSize);
		this.setPieceSize(PieceSize);

	}

	static CommonConfig setCommonConfigurations() {

		CommonConfig commonConfig = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader("./Common.cfg"));
			String line;
			String[] config = new String[6];


			for (int i = 0; i < config.length; i++) {

				if ((line = br.readLine())!= null){

					config[i] = line.split(" ")[1];
				}
			}

			commonConfig = new CommonConfig(Integer.parseInt(config[0]), Integer.parseInt(config[1]),
					Integer.parseInt(config[2]), config[3],
					Integer.parseInt(config[4]), Integer.parseInt(config[5]));

		} catch (FileNotFoundException fnf) {

			System.out.println("Common.cfg is not found");
		} catch (IOException io) {
			System.out.println("IO exception occurred while setting Common.cfg");
		}

		System.out.println("Common read done\n");

		return commonConfig;
	}

	public int getNumberOfPreferredNeighbors() {
		return NumberOfPreferredNeighbors;
	}

	public void setNumberOfPreferredNeighbors(int numberOfPreferredNeighbors) {
		NumberOfPreferredNeighbors = numberOfPreferredNeighbors;
	}

	public int getUnchokingInterval() {
		return UnchokingInterval;
	}

	public void setUnchokingInterval(int unchokingInterval) {
		UnchokingInterval = unchokingInterval;
	}

	public int getOptimisticUnchokingInterval() {
		return OptimisticUnchokingInterval;
	}

	public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
		OptimisticUnchokingInterval = optimisticUnchokingInterval;
	}

	public String getFileName() {
		return FileName;
	}

	public void setFileName(String fileName) {
		FileName = fileName;
	}

	public int getFileSize() {
		return FileSize;
	}

	public void setFileSize(int fileSize) {
		FileSize = fileSize;
	}

	public int getPieceSize() {
		return PieceSize;
	}

	public void setPieceSize(int pieceSize) {
		PieceSize = pieceSize;
	}
}