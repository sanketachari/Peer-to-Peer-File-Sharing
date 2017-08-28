import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class DataHandler {

	private int pieceSize;
	private int fileSize;

	private int totalPieceIndices;
	private boolean isFileExists = false;
	private boolean indexVector[];

	private RandomAccessFile byteStream;

	public static DataHandler createInstance(CommonConfig cfg, PeerInfoConfig pCfg, int peerId) {

		DataHandler dataHandler = new DataHandler();

		dataHandler.fileSize = cfg.getFileSize();
		dataHandler.pieceSize = cfg.getPieceSize();

		if (!dataHandler.initializeDataHandler(cfg, pCfg, peerId))
			return null;

		return dataHandler;
	}

	public boolean initializeDataHandler(CommonConfig cfg, PeerInfoConfig pCfg, int peerId){

		totalPieceIndices =  (int) Math.ceil(fileSize / (1.0 * pieceSize));

		isFileExists = pCfg.peerConfigMap.get(String.valueOf(peerId)).fileExists();

		indexVector = new boolean[totalPieceIndices];

		if (isFileExists){

			for (int i = 0; i < totalPieceIndices; i++)
				indexVector[i] = true;
		}

		String directoryName = "peer_" + peerId;
		File directory = new File(directoryName);

		if (!isFileExists)
			directory.mkdir();


		File file = new File(directory.getAbsolutePath() + "/" + cfg.getFileName());

		try {

			byteStream = new RandomAccessFile(file, "rw");

		}catch (IOException io){

			System.out.println("Error occurred while creating file stream");
			return false;
		}

		return true;
	}

	public boolean writeData(int index, byte[] data){

		// File write logic

		if (!indexVector[index])

		try {

			byteStream.seek(index * pieceSize);
			byteStream.write(data);

			setPieceIndex(index, true);
			return true;


		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public void close() {

		try {

			if (byteStream != null)
				byteStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getRandomPieceIndex(NeighborDataHandler neighborDataHandler){

		boolean []neighborIndexVector = neighborDataHandler.indexVector;
		int noOfMissingPieces = 0;
		int missingPieces[] = new int[neighborIndexVector.length];

		for (int i = 0; i < indexVector.length && i < neighborIndexVector.length; i++){

			if (!indexVector[i] && neighborIndexVector[i]){
				missingPieces[noOfMissingPieces] = i;
				noOfMissingPieces++;
			}
		}

		if (noOfMissingPieces > 0){

			Random random = new Random();

			return missingPieces[random.nextInt(noOfMissingPieces)];
		}

		return -1;
	}

	public int getBitFieldPayload(){

		int payload = 0;

		for (int i = 0 ; i < indexVector.length; i++){

			if (indexVector[i])
				payload = payload | i;

		}
		return Integer.reverse(payload);
	}

	public byte[] getPieceData(int pieceIndex){

		if (indexVector[pieceIndex]){

			try {
				byte[] readData = new byte[pieceSize];

				byteStream.seek((pieceIndex) * pieceSize);

				int dataSize = byteStream.read(readData);

				if (dataSize > 0 && dataSize != pieceSize) {


					byte[] data = new byte[dataSize];

					for (int i = 0; i < dataSize; i++)
						data[i] = readData[i];

					return data;
				}

				return readData;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public boolean checkIfPiecePresent(int pieceIndex){

		return indexVector[pieceIndex];
	}

	public void setPieceIndex(int pieceIndex, boolean val){

		indexVector[pieceIndex] = val;
	}

	public boolean checkIfFileDownloadComplete(){

		if(indexVector == null || indexVector.length==0)
			return false;

		int pieceIndex = 0;

		while(pieceIndex < totalPieceIndices)
		{
			if(!indexVector[pieceIndex])
				return false;
			else
				pieceIndex++;
		}

		return true;
	}

	public int getDownloadedPiecesCount(){

		int counter = 0;
		for(int i = 0; i < indexVector.length; i++){
			if(indexVector[i])
				counter++;
		}
		return counter;
	}
}

class NeighborDataHandler{

	private int peerId;
	private int fileSize;
	private int pieceSize;

	private boolean isFileExists;

	public boolean indexVector[];

	public static NeighborDataHandler createInstance(CommonConfig cfg, PeerInfoConfig pCfg, int peerId) {

		NeighborDataHandler dataHandler = new NeighborDataHandler();

		dataHandler.fileSize = cfg.getFileSize();
		dataHandler.pieceSize = cfg.getPieceSize();
		dataHandler.peerId = peerId;
		dataHandler.isFileExists = pCfg.peerConfigMap.get(String.valueOf(peerId)).fileExists();


		dataHandler.initializeNeighborDataHandler();

		return dataHandler;
	}


	public void initializeNeighborDataHandler(){

		int totalPieceIndices =  (int) Math.ceil(fileSize / (1.0 * pieceSize));
		indexVector = new boolean[totalPieceIndices];

		if (isFileExists){

			for (int i = 0; i < totalPieceIndices; i++)
				indexVector[i] = true;
		}
	}

	public void setPieceIndex(int pieceIndex, boolean val){

		indexVector[pieceIndex] = val;
	}
}