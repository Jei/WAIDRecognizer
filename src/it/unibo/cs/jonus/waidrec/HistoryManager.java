/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	@SuppressWarnings("unchecked")
	public void writeHistoryItem(HistoryItem item) {
		// Create a new JSON object for this item
		JSONObject jsonItem = new JSONObject();

		jsonItem.put("timestamp", item.getTimestamp());
		jsonItem.put("category", item.getCategory());
		if (item.getAccelFeatures() != null) {
			jsonItem.put("avga", item.getAccelFeatures().getAverage());
			jsonItem.put("mina", item.getAccelFeatures().getMinimum());
			jsonItem.put("maxa", item.getAccelFeatures().getMaximum());
			jsonItem.put("stda", item.getAccelFeatures().getStandardDeviation());
		} else {
			jsonItem.put("avga", null);
			jsonItem.put("mina", null);
			jsonItem.put("maxa", null);
			jsonItem.put("stda", null);
		}
		if (item.getGyroFeatures() != null) {
			jsonItem.put("avgg", item.getGyroFeatures().getAverage());
			jsonItem.put("ming", item.getGyroFeatures().getMinimum());
			jsonItem.put("maxg", item.getGyroFeatures().getMaximum());
			jsonItem.put("stdg", item.getGyroFeatures().getStandardDeviation());
		} else {
			jsonItem.put("avgg", null);
			jsonItem.put("ming", null);
			jsonItem.put("maxg", null);
			jsonItem.put("stdg", null);
		}

		// Write the item in the current history array
		currentHistoryJSON.add(jsonItem);
	}

	/**
	 * Returns a list of the JSON history files
	 * 
	 * @return an ArrayList of File objects
	 */
	public ArrayList<File> getFilesList() {
		File[] filesArray = filesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File pathName, String fileName) {
				return fileName.endsWith(".json");
			}
		});

		ArrayList<File> filesList = new ArrayList<File>(
				Arrays.asList(filesArray));

		return filesList;
	}

	/**
	 * Get the JSON array from the specified JSON file
	 * 
	 * @param the
	 *            File object to read
	 * @return a JSONArray of JSONObjects
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public JSONArray getJSONData(File JSONfile) throws FileNotFoundException,
			IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONArray array = (JSONArray) parser.parse(new FileReader(JSONfile));

		return array;
	}
	
	
	/**
	 * Delete an history file
	 * @param the File to delete
	 * @return true if the file is succesfully deleted, false otherwise
	 */
	public boolean deleteFile(File file) {
		return file.delete();
	}

}
