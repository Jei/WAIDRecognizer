/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author jei
 * 
 */
public class HistoryManager {

	private File currentHistoryFile;
	private JSONArray currentHistoryJSON;
	private File filesDir;

	public HistoryManager(File filesDir) {
		this.filesDir = filesDir;
	}

	/**
	 * Initializes a new history file
	 * 
	 * @throws IOException
	 */
	public void newSession() throws IOException {
		long currentTime = System.currentTimeMillis();
		String newFileName = "history" + currentTime + ".json";

		currentHistoryFile = new File(filesDir + File.separator + newFileName);
		currentHistoryFile.createNewFile();

		currentHistoryJSON = new JSONArray();
	}

	/**
	 * Write the current session to file
	 * 
	 * @throws IOException
	 */
	public void writeSession() throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(currentHistoryFile));
			writer.write(currentHistoryJSON.toString());
		} finally {
			writer.close();
		}
	}

	/**
	 * Writes a new history item in the history file
	 * 
	 * @param the
	 *            history item to write
	 * @throws JSONException
	 */
	public void writeHistoryItem(HistoryItem item) throws JSONException {
		// Create a new JSON object for this item
		JSONObject jsonItem = new JSONObject();

		jsonItem.put("timestamp", item.getTimestamp());
		jsonItem.put("category", item.getCategory());
		jsonItem.put("avga", item.getAccelFeatures().getAverage());
		jsonItem.put("avgg", item.getGyroFeatures().getAverage());
		jsonItem.put("mina", item.getAccelFeatures().getMinimum());
		jsonItem.put("ming", item.getGyroFeatures().getMinimum());
		jsonItem.put("maxa", item.getAccelFeatures().getMaximum());
		jsonItem.put("maxg", item.getGyroFeatures().getMaximum());
		jsonItem.put("stda", item.getAccelFeatures().getStandardDeviation());
		jsonItem.put("stdg", item.getGyroFeatures().getStandardDeviation());

		// Write the item in the current history array
		currentHistoryJSON.put(jsonItem);
	}

}
