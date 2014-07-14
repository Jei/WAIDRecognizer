package it.unibo.cs.jonus.waidrec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.res.AssetManager;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

public class ModelManager {

	public static final String TEMP_FILE_NAME = "temp.arff";
	public static final String MODEL_FILE_NAME = "randomforest.model";
	private static final String VEHICLES_FILE_NAME = "vehicles";
	private static final String DEFAULT_VEHICLES_FILE_NAME = "default_vehicles";

	private File filesDir;
	private FastVector fvWekaAttributes;
	private Instances writingSet;

	public ModelManager(File filesDir) {
		this.filesDir = filesDir;

		// Declare the numeric attributes
		Attribute Attribute1 = new Attribute("avga");
		Attribute Attribute2 = new Attribute("avgg");
		Attribute Attribute3 = new Attribute("maxa");
		Attribute Attribute4 = new Attribute("maxg");
		Attribute Attribute5 = new Attribute("mina");
		Attribute Attribute6 = new Attribute("ming");
		Attribute Attribute7 = new Attribute("stda");
		Attribute Attribute8 = new Attribute("stdg");

		// Declare the class attribute along with its values
		ArrayList<String> vehiclesList = getVehicles();
		FastVector fvClassVal = new FastVector(4);
		for (String vehicle : vehiclesList) {
			fvClassVal.addElement(vehicle);
		}
		Attribute ClassAttribute = new Attribute("theClass", fvClassVal);

		// Declare the feature vector
		fvWekaAttributes = new FastVector(9);
		fvWekaAttributes.addElement(Attribute1);
		fvWekaAttributes.addElement(Attribute2);
		fvWekaAttributes.addElement(Attribute3);
		fvWekaAttributes.addElement(Attribute4);
		fvWekaAttributes.addElement(Attribute5);
		fvWekaAttributes.addElement(Attribute6);
		fvWekaAttributes.addElement(Attribute7);
		fvWekaAttributes.addElement(Attribute8);
		fvWekaAttributes.addElement(ClassAttribute);

		writingSet = new Instances("Rel", fvWekaAttributes, 1);
		writingSet.setClassIndex(8);
	}

	public File getClassificationModel() {
		return new File(filesDir.getPath() + File.separator + MODEL_FILE_NAME);
	}

	public RandomForest getRandomForestClassifier() throws Exception {
		return (RandomForest) weka.core.SerializationHelper.read(filesDir
				.getPath() + File.separator + MODEL_FILE_NAME);
	}

	public void addVehicle(String vehicleName) {
		ArrayList<String> vehiclesList = getVehicles();

		// Check string and if already registered
		String vt = vehicleName.replaceAll("\\s+", "");
		if (!vehiclesList.contains(vehicleName) && !vt.equals("")) {
			// TODO Add the string to the header of the other vehicle files
		}
	}

	public void removeVehicle(String vehicleName) {
		// TODO rimuovi classe dalla lista delle classi di veicoli
	}

	private void copyAssetToFile(FileInputStream source, File dest)
			throws IOException {
		OutputStream os = null;
		try {
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = source.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			source.close();
			os.close();
		}
	}

	// Appends an instance to the temp file
	public void writeInstance(Instance newInstance) throws IOException {
		File vehicleFile = new File(filesDir.getPath() + File.separator
				+ TEMP_FILE_NAME);
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(vehicleFile, true));
			writer.write(newInstance.toString());
			writer.newLine();
		} finally {
			writer.close();
		}

	}

	// Appends a VehicleInstance to the temp file
	public void writeInstance(VehicleInstance newInstance) throws IOException {
		File vehicleFile = new File(filesDir.getPath() + File.separator
				+ TEMP_FILE_NAME);
		BufferedWriter writer = null;

		// Create the instance
		Instance inst = new Instance(9);
		for (int i = 0; i < 8; i++) {
			inst.setMissing((Attribute) fvWekaAttributes.elementAt(i));
		}

		String category = newInstance.getCategory();
		MagnitudeFeatures accelFeatures = newInstance.getAccelFeatures();
		MagnitudeFeatures gyroFeatures = newInstance.getGyroFeatures();

		inst.setValue((Attribute) fvWekaAttributes.elementAt(0),
				accelFeatures.getAverage());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(2),
				accelFeatures.getMaximum());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(4),
				accelFeatures.getMinimum());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(6),
				accelFeatures.getStandardDeviation());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(1),
				gyroFeatures.getAverage());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(3),
				gyroFeatures.getMaximum());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(5),
				gyroFeatures.getMinimum());
		inst.setValue((Attribute) fvWekaAttributes.elementAt(7),
				gyroFeatures.getStandardDeviation());

		inst.setValue((Attribute) fvWekaAttributes.elementAt(8), category);
		writingSet.add(inst);

		try {
			writer = new BufferedWriter(new FileWriter(vehicleFile, true));
			writer.write(writingSet.firstInstance().toString());
			writer.newLine();
		} finally {
			writer.close();
		}

		writingSet.delete();

	}

	// Erase the temp file
	public void resetTempFile() throws IOException {
		// Write the header of the instances to the temp file
		ArffSaver saver = new ArffSaver();
		writingSet.delete();
		saver.setInstances(writingSet);
		saver.setFile(new File(filesDir.getPath() + File.separator
				+ TEMP_FILE_NAME));
		saver.writeBatch();
	}

	public void overwriteVehicle(String vehicleName) throws IOException {
		File vehicleFile = new File(filesDir.getPath() + File.separator
				+ vehicleName + ".arff");
		File tempFile = new File(filesDir.getPath() + File.separator
				+ TEMP_FILE_NAME);
		ArrayList<String> vehiclesList = getVehicles();

		// Check if the vehicle is registered
		if (vehiclesList.contains(vehicleName)) {
			// Load the instances from the temp file
			ArffLoader arffLoader = new ArffLoader();
			arffLoader.setFile(tempFile);
			Instances vehicleInstances = arffLoader.getDataSet();
			// Save the instances to the vehicle file
			ArffSaver saver = new ArffSaver();
			saver.setFile(vehicleFile);
			saver.setInstances(vehicleInstances);
			saver.writeBatch();
		}

	}

	public void appendToVehicle(String vehicleName) throws Exception {
		File vehicleFile = new File(filesDir.getPath() + File.separator
				+ vehicleName + ".arff");
		File tempFile = new File(filesDir.getPath() + File.separator
				+ TEMP_FILE_NAME);
		ArrayList<String> vehiclesList = getVehicles();

		// Check if the vehicle is registered
		if (vehiclesList.contains(vehicleName)) {
			Instances appendedInstances;
			// Load the instances from the temp file
			ArffLoader tempLoader = new ArffLoader();
			tempLoader.setFile(tempFile);
			Instances tempInstances = tempLoader.getDataSet();
			// Load the instances from the vehicle file
			ArffLoader vehicleLoader = new ArffLoader();
			vehicleLoader.setFile(vehicleFile);
			Instances vehicleInstances = vehicleLoader.getDataSet();

			// Append the instances
			DataSource tempSource = new DataSource(tempInstances);
			DataSource vehicleSource = new DataSource(vehicleInstances);
			appendedInstances = tempSource.getStructure();
			while (vehicleSource.hasMoreElements(appendedInstances)) {
				appendedInstances.add(vehicleSource
						.nextElement(appendedInstances));
			}
			while (tempSource.hasMoreElements(appendedInstances)) {
				appendedInstances
						.add(tempSource.nextElement(appendedInstances));
			}

			// Save the instances to the vehicle file
			ArffSaver saver = new ArffSaver();
			saver.setFile(vehicleFile);
			saver.setInstances(appendedInstances);
			saver.writeBatch();
		}

	}

	// Copies all the arff files for the standard vehicles from assets/ to
	// files/
	// This will erase any custom arff file for those vehicles
	public void resetFromAssets(AssetManager assets) throws IOException {
		FileInputStream vehicleAsset;
		FileInputStream in;
		BufferedReader reader;
		ArrayList<String> vehiclesList;

		// Delete all the .arff files
		File[] arffFiles = filesDir.listFiles(new FilenameFilter() {
			File f;

			public boolean accept(File dir, String name) {
				if (name.endsWith(".arff")) {
					return true;
				}
				f = new File(dir.getAbsolutePath() + "/" + name);

				return f.isDirectory();
			}
		});
		for (File f : arffFiles) {
			f.delete();
		}

		// Copy default_vehicles
		in = assets.openFd(DEFAULT_VEHICLES_FILE_NAME).createInputStream();
		reader = new BufferedReader(new InputStreamReader(in));
		File newVehiclesFile = new File(filesDir.getPath() + "/"
				+ VEHICLES_FILE_NAME);
		copyAssetToFile(in, newVehiclesFile);
		reader.close();

		// Copy all the vehicle files specified in default_vehicles
		vehiclesList = getVehicles();
		for (String vehicle : vehiclesList) {
			vehicleAsset = assets.openFd(vehicle + "_1000lines.gif")
					.createInputStream();
			File newVehicleFile = new File(filesDir.getPath() + "/" + vehicle
					+ ".arff");
			copyAssetToFile(vehicleAsset, newVehicleFile);
		}
	}

	// Generates a new model using weka, appending the arff files of every
	// vehicle class
	public void generateModel() throws Exception {
		Instances vehicleInstances;
		DataSource vehicleSource;
		Instances appendedInstances;
		ArffLoader arffLoader = new ArffLoader();
		ArrayList<String> vehiclesList = getVehicles();

		Classifier classifier = new RandomForest();

		// Get the instances for all the files
		boolean first = true;
		appendedInstances = null;
		for (String vehicle : vehiclesList) {
			arffLoader.setSource(new File(filesDir.getPath() + File.separator
					+ vehicle + ".arff"));
			vehicleInstances = arffLoader.getDataSet();
			vehicleSource = new DataSource(vehicleInstances);

			// Get the structure of the instances
			if (first) {
				appendedInstances = vehicleSource.getStructure();
				first = false;
			}

			while (vehicleSource.hasMoreElements(appendedInstances)) {
				appendedInstances.add(vehicleSource
						.nextElement(appendedInstances));
			}
		}

		Log.v("ModelManager", "instances loaded");
		Log.v("ModelManager",
				"total instances:" + appendedInstances.numInstances());

		// Generate the model
		appendedInstances.setClassIndex(appendedInstances.numAttributes() - 1);

		// Generate model using a classifier
		classifier.buildClassifier(appendedInstances);
		Log.v("ModelManager", "model generated");
		// Write the new model to file
		ObjectOutputStream output = new ObjectOutputStream(
				new FileOutputStream(filesDir.getPath() + File.separator
						+ MODEL_FILE_NAME));
		output.writeObject(classifier);
		output.flush();
		output.close();
		Log.v("ModelManager", "model wrote to file");

	}

	/**
	 * Returns the ArrayList of the vehicles known by this ModelManager
	 * 
	 * @return the list of supported vehicles
	 */
	public ArrayList<String> getVehicles() {
		ArrayList<String> vehiclesList = new ArrayList<String>();

		try {
			FileInputStream in = new FileInputStream(new File(
					filesDir.getPath() + File.separator + VEHICLES_FILE_NAME));
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));

			// Read from the vehicles file
			String vehicle = reader.readLine();
			while (vehicle != null) {
				// Escape whitespace lines
				String vt = vehicle.replaceAll("\\s+", "");
				if (!vt.equals("")) {
					vehiclesList.add(vehicle);
				}
				vehicle = reader.readLine();
			}

			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return vehiclesList;
	}

}
