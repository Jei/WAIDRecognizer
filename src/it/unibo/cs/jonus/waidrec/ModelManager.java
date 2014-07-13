package it.unibo.cs.jonus.waidrec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.content.res.AssetManager;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
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
		FastVector fvClassVal = new FastVector(4);
		fvClassVal.addElement("walking");
		fvClassVal.addElement("car");
		fvClassVal.addElement("train");
		fvClassVal.addElement("idle");
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

	public void addClass(String className) {
		// TODO aggiungi classe alla lista delle classi di veicoli
	}

	public void removeClass(String className) {
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

	private void addArff(String vehicleName) {
		// TODO aggiungi file arff per veicolo
	}

	private void removeArff(String vehicleName) {
		// TODO rimuovi file arff per veicolo
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
	public void resetTempFile(boolean append) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filesDir.getPath()
					+ File.separator + TEMP_FILE_NAME, false));
			if (append) {
				writer.write("");
			} else {
				writer.write("@RELATION sensor");
				writer.newLine();
				writer.newLine();
				writer.write("@ATTRIBUTE avga NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE avgg NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE maxa NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE maxg NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE mina NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE ming NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE stda NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE stdg NUMERIC");
				writer.newLine();
				writer.write("@ATTRIBUTE class        {walking,car,train,idle}");
				writer.newLine();
				writer.newLine();
				writer.write("@DATA");
				writer.newLine();
			}
		} finally {
			writer.close();
		}
	}

	public void overwriteArffFile(File tempFile, File vehicleFile)
			throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			reader = new BufferedReader(new FileReader(tempFile));
			// Create FileWriter in overwrite mode
			writer = new BufferedWriter(new FileWriter(vehicleFile, false));
			String line;

			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}

		} finally {
			reader.close();
			writer.close();
		}

	}

	public void appendToArffFile(File tempFile, File vehicleFile)
			throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			reader = new BufferedReader(new FileReader(tempFile));
			// Create FileWriter in append mode
			writer = new BufferedWriter(new FileWriter(vehicleFile, true));
			String line;

			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}

		} finally {
			reader.close();
			writer.close();
		}

	}

	// Copies all the arff files for the standard vehicles from assets/ to
	// files/
	// This will erase any custom arff file for those vehicles
	public void resetFromAssets(AssetManager assets) throws IOException {
		FileInputStream vehicleAsset;
		FileInputStream in = assets.openFd(DEFAULT_VEHICLES_FILE_NAME)
				.createInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		// Copy default_vehicles
		File newVehiclesFile = new File(filesDir.getPath() + "/"
				+ VEHICLES_FILE_NAME);
		copyAssetToFile(in, newVehiclesFile);

		// Copy all the vehicle files specified in default_vehicles
		String vehicle = reader.readLine();
		while (vehicle != null) {
			vehicleAsset = assets.openFd(vehicle + "_1000lines.gif")
					.createInputStream();
			File newVehicleFile = new File(filesDir.getPath() + "/" + vehicle
					+ ".arff");
			copyAssetToFile(vehicleAsset, newVehicleFile);

			vehicle = reader.readLine();
		}
		reader.close();
	}

	// Generates a new model using weka, appending the arff files of every
	// vehicle class
	public void generateModel() throws Exception {
		Instances vehicleInstances;
		DataSource vehicleSource;
		Instances appendedInstances;
		ArffLoader arffLoader = new ArffLoader();

		Classifier classifier = new RandomForest();

		FileInputStream in = new FileInputStream(new File(filesDir.getPath()
				+ File.separator + VEHICLES_FILE_NAME));
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		// Get the instances for all the files
		boolean first = true;
		appendedInstances = null;
		String vehicle = reader.readLine();
		while (vehicle != null) {
			arffLoader.setSource(new File(filesDir.getPath() + File.separator
					+ vehicle + ".arff"));
			vehicleInstances = arffLoader.getDataSet();
			vehicleSource = new DataSource(vehicleInstances);

			// Get the structure of the instances
			if (first) {
				appendedInstances = vehicleSource.getStructure();
				first = false;
			}

			int i = 0;
			while (vehicleSource.hasMoreElements(appendedInstances)) {
				appendedInstances.add(vehicleSource
						.nextElement(appendedInstances));
				i++;
			}

			vehicle = reader.readLine();
		}
		reader.close();

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

}
