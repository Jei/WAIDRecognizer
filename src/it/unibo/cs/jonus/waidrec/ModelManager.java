package it.unibo.cs.jonus.waidrec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;

public class ModelManager {

	private static final String MODEL_FILE_EXTENSION = ".model";

	private File mFilesDir;
	private Classifier mClassifier;
	private Instances mHeader;
	private File mModelFile;

	public <C extends Classifier> ModelManager(Context context,
			Class<C> classifierType) {
		mFilesDir = context.getFilesDir();
		try {
			mClassifier = classifierType.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			mClassifier = new RandomForest();
		}
		String modelName = mClassifier.getClass().getSimpleName();
		mModelFile = new File(mFilesDir.getPath() + File.separator + modelName
				+ MODEL_FILE_EXTENSION);

		Runnable deserializationRunnable = new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {
				try {
					Object[] mah = weka.core.SerializationHelper
							.readAll(mModelFile.getAbsolutePath());
					mClassifier = (C) mah[0];
					mHeader = (Instances) mah[1];
					mHeader.setClassIndex(8);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		Thread deserializationThread = new Thread(null,
				deserializationRunnable, "modelOpener", 204800);
		deserializationThread.start();
		// FIXME don't hang the UI thread
		try {
			deserializationThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ModelManager(Context context) {
		this(context, RandomForest.class);
	}

	/**
	 * Returns the type of classifier used by this ModelManager
	 * 
	 * @return the class of the classifier
	 */
	@SuppressWarnings("unchecked")
	public <C extends Classifier> C getClassifierType() {
		return (C) mClassifier;
	}

	/**
	 * Sets the type of classifier to use to generate the model
	 * 
	 * @param classifierType
	 *            the class of the classifier
	 */
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

	/**
	 * Returns the model currently used by this ModelManager
	 * 
	 * @return the model
	 * @throws Exception
	 *             if the model file does not exist
	 */
	@SuppressWarnings("unchecked")
	public <C extends Classifier> C getModel() throws Exception {
		return (C) weka.core.SerializationHelper.read(mModelFile
				.getAbsolutePath());
	}

	/**
	 * Trains a classification model using the given list of vehicle instances
	 * 
	 * @param vehicleInstances
	 *            the list of instances
	 * @throws Exception
	 */
	public void generateModel(ArrayList<VehicleInstance> vehicleInstances)
			throws Exception {
		Instances wekaInstances;
		FastVector attributes = getAttributes();

		// Get list of vehicles from the vehicle instances
		ArrayList<String> vehicles = new ArrayList<String>();
		for (VehicleInstance i : vehicleInstances) {
			String vehicle = i.getCategory();
			if (!vehicles.contains(vehicle)) {
				vehicles.add(vehicle);
			}
		}
		// Get class attribute from the list
		FastVector fvClassVal = new FastVector();
		for (String vehicle : vehicles) {
			fvClassVal.addElement(vehicle);
		}
		// XXX RandomForest does not accept datasets with just one class value
		fvClassVal.addElement(" padding ");
		Attribute newClassAttribute = new Attribute("theClass", fvClassVal);
		// Substitute the class attribute
		attributes.removeElementAt(8);
		attributes.addElement(newClassAttribute);

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
		// Write the new model to file with the header
		SerializationHelper.writeAll(mModelFile.getAbsolutePath(),
				new Object[] { mClassifier, wekaInstances });
		Log.v("ModelManager", "model wrote to file");

	}

	/**
	 * Returns the ArrayList of the vehicles known by this ModelManager
	 * 
	 * @return the list of supported vehicles
	 */
	public ArrayList<String> getVehicles() {
		ArrayList<String> vehiclesList = new ArrayList<String>();

		Attribute classAttribute = mHeader.attribute(8);
		for (int i = 0; i < classAttribute.numValues(); i++) {
			vehiclesList.add(classAttribute.value(i));
		}

		return vehiclesList;
	}

	/**
	 * Classifies the given instance
	 * 
	 * @param instance
	 *            the instance to classify
	 * @return the modified instance
	 */
	public VehicleInstance classifyInstance(VehicleInstance instance) {
		Instance inst = new Instance(9);
		FastVector attributes = getAttributes();
		MagnitudeFeatures accelFeatures = instance.getAccelFeatures();
		MagnitudeFeatures gyroFeatures = instance.getGyroFeatures();

		for (int i = 0; i < 8; i++) {
			inst.setMissing(i);
		}

		if (accelFeatures != null) {
			// FIXME attributes don't work here, why?
			inst.setValue(0, accelFeatures.getAverage());
			inst.setValue(2, accelFeatures.getMaximum());
			inst.setValue(4, accelFeatures.getMinimum());
			inst.setValue(6, accelFeatures.getStandardDeviation());
		}
		if (gyroFeatures != null) {
			inst.setValue(1, gyroFeatures.getAverage());
			inst.setValue(3, gyroFeatures.getMaximum());
			inst.setValue(5, gyroFeatures.getMinimum());
			inst.setValue(7, gyroFeatures.getStandardDeviation());
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

	/**
	 * Read the vehicle data from a .arff file and return it as a list of
	 * instances
	 * 
	 * @param file
	 *            a valid .arff file
	 * @return the list of instances
	 * @throws IOException
	 *             if the file is not a valid .arff file
	 */
	public static ArrayList<VehicleInstance> readArffFile(File file)
			throws IOException {
		ArrayList<VehicleInstance> instances = new ArrayList<VehicleInstance>();
		ArffLoader arffLoader = new ArffLoader();

		// Get the instances from the file
		arffLoader.setSource(file);
		Instances wekaInstances = arffLoader.getDataSet();
		wekaInstances.setClassIndex(wekaInstances.numAttributes() - 1);
		for (int i = 0; i < wekaInstances.numInstances(); i++) {
			// Convert the Instance in a VehicleInstance
			VehicleInstance vehicleInstance = new VehicleInstance();
			MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
			MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();
			Instance currentInstance = wekaInstances.instance(i);

			accelFeatures.setAverage(currentInstance.value(0));
			accelFeatures.setMaximum(currentInstance.value(2));
			accelFeatures.setMinimum(currentInstance.value(4));
			accelFeatures.setStandardDeviation(currentInstance.value(6));
			gyroFeatures.setAverage(currentInstance.value(1));
			gyroFeatures.setMaximum(currentInstance.value(3));
			gyroFeatures.setMinimum(currentInstance.value(5));
			gyroFeatures.setStandardDeviation(currentInstance.value(7));
			vehicleInstance.setAccelFeatures(accelFeatures);
			vehicleInstance.setGyroFeatures(gyroFeatures);
			vehicleInstance.setCategory(currentInstance.toString(8));

			// Add the instance to the list
			instances.add(vehicleInstance);
		}

		return instances;
	}

	/**
	 * Write the given instances as a .arff file
	 * 
	 * @param instances
	 *            the list of instances to write
	 * @param file
	 *            a valid file to write
	 */
	public static void writeArffFile(ArrayList<VehicleInstance> instances,
			File file) {
		// TODO not yet implemented
	}

	// Get the attributes for the weka instances
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
		Attribute classAttribute = null;
		if (mHeader != null) {
			classAttribute = mHeader.classAttribute();
		} else {
			classAttribute = new Attribute("theClass", new FastVector());
		}

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
