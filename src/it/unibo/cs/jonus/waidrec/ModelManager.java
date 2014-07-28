package it.unibo.cs.jonus.waidrec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class ModelManager {

	private static final String MODEL_FILE_EXTENSION = ".model";
	private static final String VEHICLES_FILE_NAME = "vehicles";

	private File mFilesDir;
	private Classifier mClassifier;
	private File mModelFile;
	private File mVehiclesFile;

	public <C extends Classifier> ModelManager(Context context,
			Class<C> classifierType) {
		mFilesDir = context.getFilesDir();
		mVehiclesFile = new File(mFilesDir.getPath() + File.separator
				+ VEHICLES_FILE_NAME);
		try {
			mClassifier = classifierType.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		}
		String modelName = mClassifier.getClass().toString();
		mModelFile = new File(mFilesDir.getPath() + File.separator + modelName
				+ MODEL_FILE_EXTENSION);

		Runnable deserializationRunnable = new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {
				try {
					mClassifier = (C) weka.core.SerializationHelper
							.read(mModelFile.getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		Thread deserializationThread = new Thread(null,
				deserializationRunnable, "modelOpener", 204800);
		deserializationThread.start();
	}

	public ModelManager(Context context) {
		this(context, RandomForest.class);
	}

	@SuppressWarnings("unchecked")
	public <C extends Classifier> C getClassifierType() {
		return (C) mClassifier;
	}

	public <C extends Classifier> void setClassifierType(Class<C> classifierType) {
		try {
			mClassifier = classifierType.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		}
		String modelName = mClassifier.getClass().toString();
		mModelFile = new File(mFilesDir.getPath() + File.separator + modelName
				+ MODEL_FILE_EXTENSION);
	}

	@SuppressWarnings("unchecked")
	public <C extends Classifier> C getModel() throws Exception {
		return (C) weka.core.SerializationHelper.read(mModelFile
				.getAbsolutePath());
	}

	public boolean addVehicle(String vehicleName) {
		ArrayList<String> vehiclesList = getVehicles();
		boolean added = false;

		// Check string and if already registered
		String vt = vehicleName.replaceAll("\\s+", "");
		if (!vehiclesList.contains(vehicleName) && !vt.equals("")) {
			// Add new line to the vehicles file
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(mVehiclesFile, true));
				writer.write(vehicleName);
				writer.newLine();
				writer.close();
				added = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return added;
	}

	public boolean removeVehicle(String vehicleName) {
		ArrayList<String> vehiclesList = getVehicles();
		boolean removed = false;

		// Check if string is already registered
		if (vehiclesList.contains(vehicleName)) {
			// Rewrite the vehicles file without this vehicle
			vehiclesList.remove(vehicleName);
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(
						new FileWriter(mVehiclesFile, false));
				for (String v : vehiclesList) {
					writer.write(v);
					writer.newLine();
				}
				writer.close();
				removed = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return removed;
	}

	// Generates a new model using weka, appending the arff files of every
	// vehicle class
	public void generateModel(ArrayList<VehicleInstance> vehicleInstances)
			throws Exception {
		Instances wekaInstances;
		FastVector attributes = getAttributes();
		ArrayList<String> vehicles = getVehicles();

		// Convert the list in Instances
		wekaInstances = new Instances("Rel", attributes, 0);
		wekaInstances.setClassIndex(8);
		for (VehicleInstance vi : vehicleInstances) {
			if (vehicles.contains(vi.getCategory())) {
				Instance inst = new Instance(9);
				for (int i = 0; i < 8; i++) {
					inst.setMissing((Attribute) attributes.elementAt(i));
				}

				String category = vi.getCategory();
				MagnitudeFeatures accelFeatures = vi.getAccelFeatures();
				MagnitudeFeatures gyroFeatures = vi.getGyroFeatures();

				inst.setValue((Attribute) attributes.elementAt(0),
						accelFeatures.getAverage());
				inst.setValue((Attribute) attributes.elementAt(2),
						accelFeatures.getMaximum());
				inst.setValue((Attribute) attributes.elementAt(4),
						accelFeatures.getMinimum());
				inst.setValue((Attribute) attributes.elementAt(6),
						accelFeatures.getStandardDeviation());
				inst.setValue((Attribute) attributes.elementAt(1),
						gyroFeatures.getAverage());
				inst.setValue((Attribute) attributes.elementAt(3),
						gyroFeatures.getMaximum());
				inst.setValue((Attribute) attributes.elementAt(5),
						gyroFeatures.getMinimum());
				inst.setValue((Attribute) attributes.elementAt(7),
						gyroFeatures.getStandardDeviation());

				inst.setValue((Attribute) attributes.elementAt(8), category);
				wekaInstances.add(inst);
			}
		}

		Log.v("ModelManager", "total instances:" + wekaInstances.numInstances());

		// Generate model using a classifier
		mClassifier.buildClassifier(wekaInstances);
		Log.v("ModelManager", "model generated");
		// Write the new model to file
		ObjectOutputStream output = new ObjectOutputStream(
				new FileOutputStream(mModelFile));
		output.writeObject(mClassifier);
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
			FileInputStream in = new FileInputStream(mVehiclesFile);
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

	public VehicleInstance classifyInstance(VehicleInstance instance) {
		Instance inst = new Instance(9);
		FastVector attributes = getAttributes();
		MagnitudeFeatures accelFeatures = instance.getAccelFeatures();
		MagnitudeFeatures gyroFeatures = instance.getGyroFeatures();

		for (int i = 0; i < 9; i++) {
			inst.setMissing((Attribute) attributes.elementAt(i));
		}

		if (accelFeatures != null) {
			inst.setValue((Attribute) attributes.elementAt(0),
					accelFeatures.getAverage());
			inst.setValue((Attribute) attributes.elementAt(2),
					accelFeatures.getMaximum());
			inst.setValue((Attribute) attributes.elementAt(4),
					accelFeatures.getMinimum());
			inst.setValue((Attribute) attributes.elementAt(6),
					accelFeatures.getStandardDeviation());
		}
		if (gyroFeatures != null) {
			inst.setValue((Attribute) attributes.elementAt(1),
					gyroFeatures.getAverage());
			inst.setValue((Attribute) attributes.elementAt(3),
					gyroFeatures.getMaximum());
			inst.setValue((Attribute) attributes.elementAt(5),
					gyroFeatures.getMinimum());
			inst.setValue((Attribute) attributes.elementAt(7),
					gyroFeatures.getStandardDeviation());
		}

		// add the instance
		Instances classificationSet = new Instances("Rel", attributes, 1);
		classificationSet.setClassIndex(8);
		classificationSet.add(inst);

		// do classification
		double clsLabel = 0;
		try {
			if (mClassifier != null) {
				clsLabel = mClassifier.classifyInstance(classificationSet
						.instance(0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		classificationSet.instance(0).setClassValue(clsLabel);

		instance.setCategory(classificationSet.instance(0).toString(8));

		return instance;
	}

	private FastVector getAttributes() {
		FastVector wekaAttributes = null;

		// Declare the numeric attributes
		Attribute attribute1 = new Attribute("avga");
		Attribute attribute2 = new Attribute("avgg");
		Attribute attribute3 = new Attribute("maxa");
		Attribute attribute4 = new Attribute("maxg");
		Attribute attribute5 = new Attribute("mina");
		Attribute attribute6 = new Attribute("ming");
		Attribute attribute7 = new Attribute("stda");
		Attribute attribute8 = new Attribute("stdg");

		// Declare the class attribute along with its values
		ArrayList<String> vehiclesList = getVehicles();
		FastVector fvClassVal = new FastVector();
		for (String vehicle : vehiclesList) {
			fvClassVal.addElement(vehicle);
		}
		Attribute classAttribute = new Attribute("theClass", fvClassVal);

		// Declare the feature vector
		wekaAttributes = new FastVector(9);
		wekaAttributes.addElement(attribute1);
		wekaAttributes.addElement(attribute2);
		wekaAttributes.addElement(attribute3);
		wekaAttributes.addElement(attribute4);
		wekaAttributes.addElement(attribute5);
		wekaAttributes.addElement(attribute6);
		wekaAttributes.addElement(attribute7);
		wekaAttributes.addElement(attribute8);
		wekaAttributes.addElement(classAttribute);

		return wekaAttributes;
	}

}
