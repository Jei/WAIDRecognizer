/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * @author jei
 * 
 */
public class HistoryGraphFragment extends Fragment {

	public static final String ARG_JSON_DATA = "json_data";

	public HistoryGraphFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_history_graph,
				container, false);

		// Create a new graph with the JSON data
		JSONArray data = (JSONArray) getArguments().getSerializable(
				ARG_JSON_DATA);
		// FIXME
		if (data == null) {
			data = new JSONArray();
		}

		GraphView graphView = new LineGraphView(getActivity(), "History");
		ArrayList<GraphViewData> categoryData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> accelAvgData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> gyroAvgData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> accelMaxData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> gyroMaxData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> accelMinData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> gyroMinData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> accelStdData = new ArrayList<GraphViewData>();
		ArrayList<GraphViewData> gyroStdData = new ArrayList<GraphViewData>();

		// Store the categories in an array to generate the labels
		final ArrayList<String> labelsArray = new ArrayList<String>();
		for (int i = 0; i < data.size(); i++) {
			JSONObject jsonObj = (JSONObject) data.get(i);
			String category = (String) jsonObj.get("category");
			if (!labelsArray.contains(category)) {
				labelsArray.add(category);
			}
		}

		for (int i = 0; i < data.size(); i++) {
			// FIXME
			JSONObject jsonObj = (JSONObject) data.get(i);
			Long timestamp = (Long) jsonObj.get("timestamp");
			String category = (String) jsonObj.get("category");
			Double avga = (Double) jsonObj.get("avga");
			Double avgg = (Double) jsonObj.get("avgg");
			Double maxa = (Double) jsonObj.get("maxa");
			Double maxg = (Double) jsonObj.get("maxg");
			Double mina = (Double) jsonObj.get("mina");
			Double ming = (Double) jsonObj.get("ming");
			Double stda = (Double) jsonObj.get("stda");
			Double stdg = (Double) jsonObj.get("stdg");
			categoryData.add(new GraphViewData(timestamp.doubleValue(),
					labelsArray.indexOf(category)));
			if (avga != null) {
				accelAvgData.add(new GraphViewData(timestamp.doubleValue(),
						avga));
			}
			if (avgg != null) {
				gyroAvgData
						.add(new GraphViewData(timestamp.doubleValue(), avgg));
			}
			if (maxa != null) {
				accelMaxData.add(new GraphViewData(timestamp.doubleValue(),
						maxa));
			}
			if (maxg != null) {
				gyroMaxData
						.add(new GraphViewData(timestamp.doubleValue(), maxg));
			}
			if (mina != null) {
				accelMinData.add(new GraphViewData(timestamp.doubleValue(),
						mina));
			}
			if (ming != null) {
				gyroMinData
						.add(new GraphViewData(timestamp.doubleValue(), ming));
			}
			if (stda != null) {
				accelStdData.add(new GraphViewData(timestamp.doubleValue(),
						stda));
			}
			if (stdg != null) {
				gyroStdData
						.add(new GraphViewData(timestamp.doubleValue(), stdg));
			}
		}

		// Set custom formatter for x and y axis (timestamp and category)
		graphView.setCustomLabelFormatter(new CustomLabelFormatter() {

			@Override
			public String formatLabel(double value, boolean isValueX) {
				if (isValueX) {
					// Return human time
					SimpleDateFormat sdfDateTime = new SimpleDateFormat(
							"HH:mm:ss", Locale.getDefault());
					String humanDate = sdfDateTime
							.format(new Date((long) value));
					return humanDate;
				} else {
					if (value >= labelsArray.size() || value < 0) {
						return new String("");
					} else {
						return labelsArray.get((int) value);
					}
				}
			}
		});

		// FIXME
		GraphViewSeriesStyle categoryStyle = new GraphViewSeriesStyle(
				Color.rgb(100, 00, 100), 6);
		graphView.addSeries(new GraphViewSeries("Category", categoryStyle,
				categoryData.toArray(new GraphViewData[categoryData.size()])));

		// TODO different graph for magnitude features
		/*
		 * GraphViewSeriesStyle accelAvgStyle = new GraphViewSeriesStyle(
		 * Color.rgb(180, 50, 00), 4); GraphViewSeriesStyle gyroAvgStyle = new
		 * GraphViewSeriesStyle(Color.rgb( 90, 190, 00), 4);
		 * GraphViewSeriesStyle accelMaxStyle = new GraphViewSeriesStyle(
		 * Color.rgb(200, 50, 00), 4); GraphViewSeriesStyle gyroMaxStyle = new
		 * GraphViewSeriesStyle(Color.rgb( 90, 210, 00), 4);
		 * GraphViewSeriesStyle accelMinStyle = new GraphViewSeriesStyle(
		 * Color.rgb(220, 50, 00), 4); GraphViewSeriesStyle gyroMinStyle = new
		 * GraphViewSeriesStyle(Color.rgb( 90, 230, 00), 4);
		 * GraphViewSeriesStyle accelStdStyle = new GraphViewSeriesStyle(
		 * Color.rgb(240, 50, 00), 4); GraphViewSeriesStyle gyroStdStyle = new
		 * GraphViewSeriesStyle(Color.rgb( 90, 250, 00), 4);
		 * graphView.addSeries(new GraphViewSeries("avga", accelAvgStyle,
		 * accelAvgData.toArray(new GraphViewData[accelAvgData.size()])));
		 * graphView.addSeries(new GraphViewSeries("avgg", gyroAvgStyle,
		 * gyroAvgData.toArray(new GraphViewData[gyroAvgData.size()])));
		 * graphView.addSeries(new GraphViewSeries("maxa", accelMaxStyle,
		 * accelMaxData.toArray(new GraphViewData[accelMaxData.size()])));
		 * graphView.addSeries(new GraphViewSeries("maxg", gyroMaxStyle,
		 * gyroMaxData.toArray(new GraphViewData[gyroMaxData.size()])));
		 * graphView.addSeries(new GraphViewSeries("mina", accelMinStyle,
		 * accelMinData.toArray(new GraphViewData[accelMinData.size()])));
		 * graphView.addSeries(new GraphViewSeries("ming", gyroMinStyle,
		 * gyroMinData.toArray(new GraphViewData[gyroMinData.size()])));
		 * graphView.addSeries(new GraphViewSeries("stda", accelStdStyle,
		 * accelStdData.toArray(new GraphViewData[accelStdData.size()])));
		 * graphView.addSeries(new GraphViewSeries("stdg", gyroStdStyle,
		 * gyroStdData.toArray(new GraphViewData[gyroStdData.size()])));
		 */

		graphView.setShowLegend(true);
		// Convert standard text sp size to px
		float textSize = (float) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 14, getResources()
						.getDisplayMetrics());
		graphView.getGraphViewStyle().setTextSize(textSize);
		graphView.getGraphViewStyle().setNumHorizontalLabels(5);
		if (labelsArray.size() > 1) {
			graphView.getGraphViewStyle().setNumVerticalLabels(
					labelsArray.size());
		} else {
			graphView.getGraphViewStyle().setNumVerticalLabels(3);
		}
		graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
		graphView.setScrollable(true);
		graphView.setScalable(true);

		// Add the graph to the layout
		LinearLayout layout = (LinearLayout) rootView
				.findViewById(R.id.graphLayout);
		layout.addView(graphView);

		return rootView;
	}
}
