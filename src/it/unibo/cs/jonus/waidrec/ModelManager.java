package it.unibo.cs.jonus.waidrec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;

public class ModelManager {
	
	public static final String TEMP_FILE_NAME = "temp.arff";
	public static final String HISTORY_FILE_NAME = "history.arff";

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

	public void addArff(String vehicleName) {
		// TODO aggiungi file arff per veicolo
	}

	public void removeArff(String vehicleName) {
		// TODO rimuovi file arff per veicolo
	}

	// Appends an instance to the temp file
	public void writeInstance(Instance newInstance, File filesDir)
			throws IOException {
		File vehicleFile = new File(filesDir.getPath() + "/temp.arff");
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(vehicleFile, true));
			writer.write(newInstance.toString());
			writer.newLine();
		} finally {
			writer.close();
		}

	}

	// Erase the temp file
	public void resetTempFile(File filesDir, boolean append) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filesDir + "/temp.arff",
					false));
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
	public void resetFromAssets(AssetManager assets, File filesDir)
			throws IOException {
		FileInputStream walkingAsset;
		FileInputStream carAsset;
		FileInputStream trainAsset;
		FileInputStream idleAsset;

		// Get application assets
		walkingAsset = assets.openFd("walking_1000lines.gif")
				.createInputStream();
		carAsset = assets.openFd("car_1000lines.gif").createInputStream();
		trainAsset = assets.openFd("train_1000lines.gif").createInputStream();
		idleAsset = assets.openFd("idle_1000lines.gif").createInputStream();
		// Copy the arff files for walking, car and train from assets to app
		// files directory
		File newWalkingFile = new File(filesDir.getPath() + "/walking.arff");
		File newCarFile = new File(filesDir.getPath() + "/car.arff");
		File newTrainFile = new File(filesDir.getPath() + "/train.arff");
		File newIdleFile = new File(filesDir.getPath() + "/idle.arff");
		copyAssetToFile(walkingAsset, newWalkingFile);
		copyAssetToFile(carAsset, newCarFile);
		copyAssetToFile(trainAsset, newTrainFile);
		copyAssetToFile(idleAsset, newIdleFile);

	}

	// Generates a new model using weka, appending the arff files of every
	// vehicle class
	@SuppressLint("SdCardPath")
	public void generateModel(File filesDir) throws Exception {
		Instances walkingInstances;
		Instances carInstances;
		Instances trainInstances;
		Instances idleInstances;
		DataSource walkingSource;
		DataSource carSource;
		DataSource trainSource;
		DataSource idleSource;
		Instances appendedInstances;
		ArffLoader arffLoader = new ArffLoader();

		Classifier randomForestClassifier = new RandomForest();

		// Get the instances for all the files
		arffLoader.setSource(new File(filesDir.getPath() + "/walking.arff"));
		walkingInstances = arffLoader.getDataSet();
		walkingSource = new DataSource(walkingInstances);
		arffLoader.setSource(new File(filesDir.getPath() + "/car.arff"));
		carInstances = arffLoader.getDataSet();
		carSource = new DataSource(carInstances);
		arffLoader.setSource(new File(filesDir.getPath() + "/train.arff"));
		trainInstances = arffLoader.getDataSet();
		trainSource = new DataSource(trainInstances);
		arffLoader.setSource(new File(filesDir.getPath() + "/idle.arff"));
		idleInstances = arffLoader.getDataSet();
		idleSource = new DataSource(idleInstances);
		Log.v("ModelManager", "instances loaded");

		// Append the instances
		appendedInstances = walkingSource.getStructure();
		while (walkingSource.hasMoreElements(appendedInstances)) {
			appendedInstances.add(walkingSource.nextElement(appendedInstances));
		}
		while (carSource.hasMoreElements(appendedInstances)) {
			appendedInstances.add(carSource.nextElement(appendedInstances));
		}
		while (trainSource.hasMoreElements(appendedInstances)) {
			appendedInstances.add(trainSource.nextElement(appendedInstances));
		}
		while (idleSource.hasMoreElements(appendedInstances)) {
			appendedInstances.add(idleSource.nextElement(appendedInstances));
		}
		Log.v("ModelManager", "instances appended");
		Log.v("ModelManager",
				"total instances:" + appendedInstances.numInstances());

		// Generate the model
		appendedInstances.setClassIndex(appendedInstances.numAttributes() - 1);

		// Generate model using a classifier
		randomForestClassifier.buildClassifier(appendedInstances);
		Log.v("ModelManager", "model generated");
		// Write the new model to file
		// FIXME do not use hardcoded path
		ObjectOutputStream output = new ObjectOutputStream(
				new FileOutputStream(
						"/data/data/it.unibo.cs.jonus.waidrec/files/randomforest.model"));
		/*ObjectOutputStream externalOutput = new ObjectOutputStream(
				new FileOutputStream(
						"/sdcard/waidrec/randomforest.model"));*/
		output.writeObject(randomForestClassifier);
		//externalOutput.writeObject(randomForestClassifier);
		output.flush();
		//externalOutput.flush();
		output.close();
		//externalOutput.close();
		Log.v("ModelManager", "model wrote to file");

	}

}
