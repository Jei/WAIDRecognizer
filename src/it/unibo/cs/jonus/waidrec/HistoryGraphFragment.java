/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.Arrays;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * @author jei
 * 
 */
public class HistoryGraphFragment extends Fragment {

	private GraphicalView mChart;
	private LinearLayout graphLayout;
	private Spinner historyDataSpinner;
	private static final String[] spinnerChoices = { "Vehicles",
			"Accelerometer", "Gyroscope" };
	public static final String ARG_HISTORY_ITEMS = "history_items";
	private static final int CHART_CATEGORIES = 0;
	private static final int CHART_ACCELEROMETER = 1;
	private static final int CHART_GYROSCOPE = 2;
	ArrayList<HistoryItem> currentHistoryList;

	public HistoryGraphFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_history_graph,
				container, false);

		// Populate the spinner for data selection
		historyDataSpinner = (Spinner) rootView
				.findViewById(R.id.historyDataSpinner);
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
				this.getActivity(), android.R.layout.simple_spinner_item,
				new ArrayList<String>(Arrays.asList(spinnerChoices)));
		spinnerArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		historyDataSpinner.setAdapter(spinnerArrayAdapter);
		historyDataSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parentView,
							View selected, int position, long id) {
						mChart = getChart(position);
						graphLayout.removeAllViews();
						graphLayout.addView(mChart);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {

					}
				});

		// Get the list of items from the arguments
		currentHistoryList = getArguments().getParcelableArrayList(
				ARG_HISTORY_ITEMS);
		if (currentHistoryList == null) {
			currentHistoryList = new ArrayList<HistoryItem>();
		}

		// Generate the chart
		mChart = getChart(CHART_CATEGORIES);

		// Add the graph to the layout
		graphLayout = (LinearLayout) rootView.findViewById(R.id.graphLayout);
		graphLayout.addView(mChart);

		return rootView;
	}

	public void setHistoryList(Bundle bundle) {
		// Get the list of items from the bundle
		currentHistoryList = bundle.getParcelableArrayList(ARG_HISTORY_ITEMS);

		if (currentHistoryList != null) {
			int currentSelection = historyDataSpinner.getSelectedItemPosition();
			mChart = getChart(currentSelection);
			graphLayout.removeAllViews();
			graphLayout.addView(mChart);
		}
	}

	private GraphicalView getChart(int type) {
		// Get the chartRenderer
		XYMultipleSeriesRenderer chartRenderer = getChartRenderer();

		XYMultipleSeriesDataset chartDataset = null;
		double[] limits = { 0, 0, 0, 0 };
		int[] colors = { Color.CYAN, Color.MAGENTA, Color.BLUE };
		switch (type) {
		case CHART_CATEGORIES:
			// Generate labels for the series
			ArrayList<String> labelsList = new ArrayList<String>();
			for (int i = 0; i < currentHistoryList.size(); i++) {
				HistoryItem item = currentHistoryList.get(i);
				String category = item.getCategory();
				if (!labelsList.contains(category)) {
					labelsList.add(category);
				}
			}
			chartDataset = getCategoriesData();

			// Style the series
			XYSeriesRenderer categoriesRenderer = new XYSeriesRenderer();
			categoriesRenderer.setPointStyle(PointStyle.CIRCLE);
			categoriesRenderer.setFillPoints(true);
			categoriesRenderer.setColor(Color.BLACK);

			// Add series style and labels to the chart renderer
			chartRenderer.addSeriesRenderer(categoriesRenderer);
			for (String category : labelsList) {
				chartRenderer.addYTextLabel(labelsList.indexOf(category),
						category);
			}
			chartRenderer.setYLabels(labelsList.size());

			// Set limits
			limits[2] = chartDataset.getSeriesAt(0).getMinY() - 0.2;
			limits[3] = chartDataset.getSeriesAt(0).getMaxY() + 0.2;
			break;
		case CHART_ACCELEROMETER:
			for (int i = 0; i < 3; i++) {
				XYSeriesRenderer renderer = new XYSeriesRenderer();
				renderer.setPointStyle(PointStyle.CIRCLE);
				renderer.setFillPoints(true);
				renderer.setColor(colors[i]);
				chartRenderer.addSeriesRenderer(renderer);
			}
			chartRenderer.setYLabels(10);

			chartDataset = getAccelerometerData();

			// Set limits
			limits[2] = chartDataset.getSeriesAt(1).getMinY() - 1;
			limits[3] = chartDataset.getSeriesAt(2).getMaxY() + 1;
			break;
		case CHART_GYROSCOPE:
			for (int i = 0; i < 3; i++) {
				XYSeriesRenderer renderer = new XYSeriesRenderer();
				renderer.setPointStyle(PointStyle.CIRCLE);
				renderer.setFillPoints(true);
				renderer.setColor(colors[i]);
				chartRenderer.addSeriesRenderer(renderer);
			}
			chartRenderer.setYLabels(10);

			chartDataset = getGyroscopeData();

			// Set limits
			limits[2] = chartDataset.getSeriesAt(1).getMinY() - 0.5;
			limits[3] = chartDataset.getSeriesAt(2).getMaxY() + 0.5;
			break;
		}
		limits[0] = chartDataset.getSeriesAt(0).getMinX() - 100000;
		limits[1] = chartDataset.getSeriesAt(0).getMaxX() + 100000;

		// Set pan and zoom limits
		chartRenderer.setPanLimits(limits);
		chartRenderer.setZoomLimits(limits);
		chartRenderer.setXAxisMin(limits[0]);
		chartRenderer.setXAxisMax(limits[1]);
		chartRenderer.setYAxisMin(limits[2]);
		chartRenderer.setYAxisMax(limits[3]);

		return ChartFactory.getTimeChartView(getActivity(), chartDataset,
				chartRenderer, getDateFormat(limits[0], limits[1]));

	}

	private XYMultipleSeriesDataset getCategoriesData() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		TimeSeries series = new TimeSeries("Category");

		ArrayList<String> labelsList = new ArrayList<String>();
		for (int i = 0; i < currentHistoryList.size(); i++) {
			HistoryItem item = currentHistoryList.get(i);
			String category = item.getCategory();
			if (!labelsList.contains(category)) {
				labelsList.add(category);
			}
		}

		dataset.addSeries(series);
		for (int i = 0; i < currentHistoryList.size(); i++) {
			HistoryItem item = currentHistoryList.get(i);
			Long timestamp = item.getTimestamp();
			String category = item.getCategory();

			series.add(timestamp.doubleValue(), labelsList.indexOf(category));

		}

		return dataset;
	}

	private XYMultipleSeriesDataset getAccelerometerData() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		TimeSeries avgaSeries = new TimeSeries("Average");
		TimeSeries minaSeries = new TimeSeries("Minimum");
		TimeSeries maxaSeries = new TimeSeries("Maximum");

		dataset.addSeries(avgaSeries);
		dataset.addSeries(minaSeries);
		dataset.addSeries(maxaSeries);
		for (int i = 0; i < currentHistoryList.size(); i++) {
			HistoryItem item = currentHistoryList.get(i);
			Long timestamp = item.getTimestamp();
			MagnitudeFeatures accelFeatures = item.getAccelFeatures();
			Double avga = accelFeatures.getAverage();
			Double maxa = accelFeatures.getMaximum();
			Double mina = accelFeatures.getMinimum();

			avgaSeries.add(timestamp.doubleValue(), avga);
			minaSeries.add(timestamp.doubleValue(), mina);
			maxaSeries.add(timestamp.doubleValue(), maxa);

		}

		return dataset;
	}

	private XYMultipleSeriesDataset getGyroscopeData() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		TimeSeries avggSeries = new TimeSeries("Average");
		TimeSeries mingSeries = new TimeSeries("Minimum");
		TimeSeries maxgSeries = new TimeSeries("Maximum");

		dataset.addSeries(avggSeries);
		dataset.addSeries(mingSeries);
		dataset.addSeries(maxgSeries);
		for (int i = 0; i < currentHistoryList.size(); i++) {
			HistoryItem item = currentHistoryList.get(i);
			Long timestamp = item.getTimestamp();
			MagnitudeFeatures gyroFeatures = item.getGyroFeatures();
			Double avgg = gyroFeatures.getAverage();
			Double maxg = gyroFeatures.getMaximum();
			Double ming = gyroFeatures.getMinimum();

			avggSeries.add(timestamp.doubleValue(), avgg);
			mingSeries.add(timestamp.doubleValue(), ming);
			maxgSeries.add(timestamp.doubleValue(), maxg);

		}

		return dataset;
	}

	private XYMultipleSeriesRenderer getChartRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

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
		renderer.setLabelsTextSize(textSize);
		renderer.setYLabelsAngle(-30.0f);
		renderer.setXLabelsAngle(-30.0f);
		renderer.setXLabelsColor(Color.BLACK);
		renderer.setYLabelsColor(0, Color.BLACK);
		renderer.setXLabelsAlign(Align.RIGHT);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setLegendTextSize(textSize);
		renderer.setFitLegend(true);
		renderer.setMargins(new int[] { marginSizeNormal, marginSizeLarge,
				marginSizeLarge, marginSizeNormal });
		renderer.setPanEnabled(true, false);
		renderer.setZoomEnabled(true, false);
		renderer.setZoomButtonsVisible(false);
		renderer.setBackgroundColor(Color.WHITE);
		renderer.setMarginsColor(Color.WHITE);
		renderer.setGridColor(Color.BLACK);
		renderer.setPointSize(8);
		renderer.setShowGridY(true);
		renderer.setClickEnabled(false);
		renderer.setXLabels(6);

		return renderer;
	}
	
	private String getDateFormat(double min, double max) {
		String format = "HH:mm:ss";
		
		double timespan = max - min;
		if(timespan > 1000 * 60 * 60 * 24) {
			format = "dd MM yyyy";
		}
		
		return format;
	}
	
}
