/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Paint.Align;
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
	// TODO add charts for sensor data and chart selector

	private GraphicalView categoriesChart;
	private TimeSeries categoriesSeries = new TimeSeries("Category");
	private XYSeriesRenderer categoriesRenderer = new XYSeriesRenderer();
	private XYMultipleSeriesDataset chartDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer chartRenderer = new XYMultipleSeriesRenderer();

	public static final String ARG_HISTORY_ITEMS = "history_items";

	public HistoryGraphFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_history_graph,
				container, false);

		// Get the list of items from the arguments
		ArrayList<HistoryItem> historyList = getArguments()
				.getParcelableArrayList(ARG_HISTORY_ITEMS);
		// FIXME
		if (historyList == null) {
			historyList = new ArrayList<HistoryItem>();
		}

		// Store the categories in an array to generate the labels
		final ArrayList<String> labelsArray = new ArrayList<String>();
		for (int i = 0; i < historyList.size(); i++) {
			HistoryItem item = historyList.get(i);
			String category = item.getCategory();
			if (!labelsArray.contains(category)) {
				labelsArray.add(category);
			}
		}

		// Fill the dataset
		chartDataset.addSeries(categoriesSeries);
		for (int i = 0; i < historyList.size(); i++) {
			HistoryItem item = historyList.get(i);
			Long timestamp = item.getTimestamp();
			String category = item.getCategory();
			Double avga = item.getAccelFeatures().getAverage();
			Double avgg = item.getGyroFeatures().getAverage();
			Double maxa = item.getAccelFeatures().getMaximum();
			Double maxg = item.getGyroFeatures().getMaximum();
			Double mina = item.getAccelFeatures().getMinimum();
			Double ming = item.getGyroFeatures().getMinimum();
			Double stda = item.getAccelFeatures().getStandardDeviation();
			Double stdg = item.getGyroFeatures().getStandardDeviation();

			categoriesSeries.add(timestamp.doubleValue(),
					labelsArray.indexOf(category));

		}

		// Set the labels of the Y axis (categories)
		for (String category : labelsArray) {
			chartRenderer
					.addYTextLabel(labelsArray.indexOf(category), category);
		}

		// Style the chart
		// TODO better style
		float textSize = (float) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 12, getResources()
						.getDisplayMetrics());
		int marginSizeNormal = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 20, getResources()
						.getDisplayMetrics());
		int marginSizeLarge = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 50, getResources()
						.getDisplayMetrics());
		chartRenderer.setLabelsTextSize(textSize);
		chartRenderer.setYLabelsAngle(-30.0f);
		chartRenderer.setXLabelsAngle(-30.0f);
		chartRenderer.setXLabelsColor(Color.BLACK);
		chartRenderer.setYLabelsColor(0, Color.BLACK);
		chartRenderer.setXLabelsAlign(Align.RIGHT);
		chartRenderer.setYLabelsAlign(Align.RIGHT);
		chartRenderer.setLegendTextSize(textSize);
		chartRenderer.setFitLegend(true);
		chartRenderer.setMargins(new int[] { marginSizeNormal, marginSizeLarge,
				marginSizeLarge, marginSizeNormal });
		chartRenderer.setPanEnabled(true, false);
		chartRenderer.setZoomEnabled(true, false);
		chartRenderer.setZoomButtonsVisible(false);
		chartRenderer.setBackgroundColor(Color.WHITE);
		chartRenderer.setMarginsColor(Color.WHITE);
		chartRenderer.setGridColor(Color.BLACK);
		chartRenderer.setPointSize(8);
		chartRenderer.setShowGridY(true);
		chartRenderer.setAntialiasing(true);
		// chartRenderer.setPanLimits(new double[] { 0,
		// categoriesSeries.getMinX(), 0, categoriesSeries.getMaxX() });
		chartRenderer.setYLabels(labelsArray.size());
		chartRenderer.setClickEnabled(false);

		// Style the series
		categoriesRenderer.setPointStyle(PointStyle.DIAMOND);
		categoriesRenderer.setFillPoints(true);
		categoriesRenderer.setColor(Color.BLACK);

		// Generate the chart
		chartRenderer.addSeriesRenderer(categoriesRenderer);
		categoriesChart = ChartFactory.getTimeChartView(getActivity(),
				chartDataset, chartRenderer, "HH:mm:ss");

		// Add the graph to the layout
		LinearLayout layout = (LinearLayout) rootView
				.findViewById(R.id.graphLayout);
		layout.addView(categoriesChart);

		return rootView;
	}
}
