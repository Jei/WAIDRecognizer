/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.text.SimpleDateFormat;
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
		GraphView graphView = new LineGraphView(getActivity(),
				"HistoryGraphView");
		GraphViewData[] accelAvgData = new GraphViewData[data.size()];
		GraphViewData[] gyroAvgData = new GraphViewData[data.size()];
		GraphViewData[] accelMaxData = new GraphViewData[data.size()];
		GraphViewData[] gyroMaxData = new GraphViewData[data.size()];
		GraphViewData[] accelMinData = new GraphViewData[data.size()];
		GraphViewData[] gyroMinData = new GraphViewData[data.size()];
		GraphViewData[] accelStdData = new GraphViewData[data.size()];
		GraphViewData[] gyroStdData = new GraphViewData[data.size()];
		for (int i = 0; i < data.size(); i++) {
			JSONObject jsonObj = (JSONObject) data.get(i);
			Long timestamp = (Long) jsonObj.get("timestamp");
			Double avga = (Double) jsonObj.get("avga");
			Double avgg = (Double) jsonObj.get("avgg");
			Double maxa = (Double) jsonObj.get("maxa");
			Double maxg = (Double) jsonObj.get("maxg");
			Double mina = (Double) jsonObj.get("mina");
			Double ming = (Double) jsonObj.get("ming");
			Double stda = (Double) jsonObj.get("stda");
			Double stdg = (Double) jsonObj.get("stdg");
			accelAvgData[i] = new GraphViewData(timestamp.doubleValue(), avga);
			gyroAvgData[i] = new GraphViewData(timestamp.doubleValue(), avgg);
			accelMaxData[i] = new GraphViewData(timestamp.doubleValue(), maxa);
			gyroMaxData[i] = new GraphViewData(timestamp.doubleValue(), maxg);
			accelMinData[i] = new GraphViewData(timestamp.doubleValue(), mina);
			gyroMinData[i] = new GraphViewData(timestamp.doubleValue(), ming);
			accelStdData[i] = new GraphViewData(timestamp.doubleValue(), stda);
			gyroStdData[i] = new GraphViewData(timestamp.doubleValue(), stdg);
		}
		// Set custom formatter for x axis (timestamp)

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
				}
				return null; // let graphview generate Y-axis label for us
			}
		});

		GraphViewSeriesStyle accelAvgStyle = new GraphViewSeriesStyle(
				Color.rgb(180, 50, 00), 4);
		GraphViewSeriesStyle gyroAvgStyle = new GraphViewSeriesStyle(
				Color.rgb(90, 190, 00), 4);
		GraphViewSeriesStyle accelMaxStyle = new GraphViewSeriesStyle(
				Color.rgb(200, 50, 00), 4);
		GraphViewSeriesStyle gyroMaxStyle = new GraphViewSeriesStyle(
				Color.rgb(90, 210, 00), 4);
		GraphViewSeriesStyle accelMinStyle = new GraphViewSeriesStyle(
				Color.rgb(220, 50, 00), 4);
		GraphViewSeriesStyle gyroMinStyle = new GraphViewSeriesStyle(
				Color.rgb(90, 230, 00), 4);
		GraphViewSeriesStyle accelStdStyle = new GraphViewSeriesStyle(
				Color.rgb(240, 50, 00), 4);
		GraphViewSeriesStyle gyroStdStyle = new GraphViewSeriesStyle(
				Color.rgb(90, 250, 00), 4);
		graphView.addSeries(new GraphViewSeries("avga", accelAvgStyle,
				accelAvgData));
		graphView.addSeries(new GraphViewSeries("avgg", gyroAvgStyle,
				gyroAvgData));
		graphView.addSeries(new GraphViewSeries("maxa", accelMaxStyle,
				accelMaxData));
		graphView.addSeries(new GraphViewSeries("maxg", gyroMaxStyle,
				gyroMaxData));
		graphView.addSeries(new GraphViewSeries("mina", accelMinStyle,
				accelMinData));
		graphView.addSeries(new GraphViewSeries("ming", gyroMinStyle,
				gyroMinData));
		graphView.addSeries(new GraphViewSeries("stda", accelStdStyle,
				accelStdData));
		graphView.addSeries(new GraphViewSeries("stdg", gyroStdStyle,
				gyroStdData));
		graphView.setShowLegend(true);
		graphView.getGraphViewStyle().setTextSize(20);
		graphView.getGraphViewStyle().setNumVerticalLabels(10);
		graphView.getGraphViewStyle().setNumHorizontalLabels(5);
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
