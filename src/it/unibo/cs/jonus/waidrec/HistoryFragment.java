/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
public class HistoryFragment extends Fragment {

	private GraphicalView mChart;
	private LinearLayout graphLayout;
	private Spinner historySpinner;
	private Spinner timeSpinner;
	public static final String ARG_HISTORY_ITEMS = "history_items";
	private static final int CHART_CATEGORIES = 0;
	private static final int CHART_ACCELEROMETER = 1;
	private static final int CHART_GYROSCOPE = 2;
	private static final int DATA_TODAY = 0;
	private static final int DATA_WEEK = 1;
	private static final int DATA_MONTH = 2;
	private static final int DATA_ALL = 3;
	ArrayList<VehicleInstance> mHistoryList = new ArrayList<VehicleInstance>();

	private static final String[] allColumnsProjection = {
			DatabaseOpenHelper.COLUMN_TIMESTAMP,
			DatabaseOpenHelper.COLUMN_CATEGORY, DatabaseOpenHelper.COLUMN_AVGA,
			DatabaseOpenHelper.COLUMN_MINA, DatabaseOpenHelper.COLUMN_MAXA,
			DatabaseOpenHelper.COLUMN_STDA, DatabaseOpenHelper.COLUMN_AVGG,
			DatabaseOpenHelper.COLUMN_MING, DatabaseOpenHelper.COLUMN_MAXG,
			DatabaseOpenHelper.COLUMN_STDG };

	private static final String[] timeValues = { "Today", "This Week",
			"This Month", "All Time" };

	private static final String[] dataValues = { "Vehicles", "Accelerometer",
			"Gyroscope" };

	public HistoryFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.history, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
		case R.id.action_delete_history_file:
			// Delete the history and refresh the graph
			Uri uri = Uri.parse(EvaluationsProvider.CONTENT_URI
					+ EvaluationsProvider.PATH_ERASE_EVALUATIONS);
			getActivity().getContentResolver().delete(uri, null, null);

			mHistoryList.clear();
			mChart = getChart(timeSpinner.getSelectedItemPosition());
			graphLayout.removeAllViews();
			graphLayout.addView(mChart);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_history, container,
				false);

		// Populate the spinners for data selection
		historySpinner = (Spinner) rootView.findViewById(R.id.historySpinner);
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
				this.getActivity(), android.R.layout.simple_spinner_item,
				new ArrayList<String>(Arrays.asList(dataValues)));
		spinnerArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		historySpinner.setAdapter(spinnerArrayAdapter);
		historySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

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

		timeSpinner = (Spinner) rootView.findViewById(R.id.timeSpinner);
		ArrayAdapter<String> timeSpinnerAdapter = new ArrayAdapter<String>(
				this.getActivity(), android.R.layout.simple_spinner_item,
				new ArrayList<String>(Arrays.asList(timeValues)));
		timeSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		timeSpinner.setAdapter(timeSpinnerAdapter);
		timeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selected, int position, long id) {
				Long endTime = Long.MAX_VALUE;
				Long startTime = null;
				switch (position) {
				case DATA_TODAY:
					startTime = getPastDay(0);
					break;
				case DATA_WEEK:
					startTime = getPastDay(7);
					break;
				case DATA_MONTH:
					startTime = getPastDay(30);
					break;
				case DATA_ALL:
					startTime = (long) 0;
					break;
				}

				// Get the VehicleInstances from the Content Provider
				Uri uri = Uri.parse(EvaluationsProvider.CONTENT_URI
						+ EvaluationsProvider.PATH_SOME_EVALUATIONS);
				String selection = DatabaseOpenHelper.COLUMN_TIMESTAMP + " >= "
						+ startTime + " AND "
						+ DatabaseOpenHelper.COLUMN_TIMESTAMP + " <= "
						+ endTime;
				Cursor cursor = getActivity().getContentResolver().query(uri,
						allColumnsProjection, selection, null, null);

				// Convert the instances to history items
				mHistoryList = EvaluationsProvider
						.cursorToVehicleInstanceArray(cursor);

				// Refresh the chart
				mChart = getChart(historySpinner.getSelectedItemPosition());
				graphLayout.removeAllViews();
				graphLayout.addView(mChart);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// TODO Auto-generated method stub

			}
		});

		// Add the graph to the layout
		graphLayout = (LinearLayout) rootView.findViewById(R.id.graphLayout);

		return rootView;
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
			for (int i = 0; i < mHistoryList.size(); i++) {
				VehicleInstance item = mHistoryList.get(i);
				String category = item.getCategory();
				if (!labelsList.contains(category)) {
					labelsList.add(category);
				}
			}
			chartDataset = getCategoriesData();

			// Style the series
			XYSeriesRenderer categoriesRenderer = new XYSeriesRenderer();
			categoriesRenderer.setColor(Color.GREEN);
			categoriesRenderer.setLineWidth(2);

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
				renderer.setColor(colors[i]);
				renderer.setLineWidth(2);
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
				renderer.setColor(colors[i]);
				renderer.setLineWidth(2);
				chartRenderer.addSeriesRenderer(renderer);
			}
			chartRenderer.setYLabels(10);

			chartDataset = getGyroscopeData();

			// Set limits
			limits[2] = chartDataset.getSeriesAt(1).getMinY() - 0.5;
			limits[3] = chartDataset.getSeriesAt(2).getMaxY() + 0.5;
			break;
		}
		long maximum = (long) chartDataset.getSeriesAt(0).getMaxX();
		long minimum = (long) chartDataset.getSeriesAt(0).getMinX();
		long modifier = (maximum - minimum) / 1000;
		limits[0] = chartDataset.getSeriesAt(0).getMinX() - modifier * 100;
		limits[1] = chartDataset.getSeriesAt(0).getMaxX() + modifier * 100;

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
		for (int i = 0; i < mHistoryList.size(); i++) {
			VehicleInstance item = mHistoryList.get(i);
			String category = item.getCategory();
			if (!labelsList.contains(category)) {
				labelsList.add(category);
			}
		}

		dataset.addSeries(series);
		for (int i = 0; i < mHistoryList.size(); i++) {
			VehicleInstance item = mHistoryList.get(i);
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
		for (int i = 0; i < mHistoryList.size(); i++) {
			VehicleInstance item = mHistoryList.get(i);
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
		for (int i = 0; i < mHistoryList.size(); i++) {
			VehicleInstance item = mHistoryList.get(i);
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
		renderer.setXLabelsColor(Color.WHITE);
		renderer.setYLabelsColor(0, Color.WHITE);
		renderer.setXLabelsAlign(Align.RIGHT);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setLegendTextSize(textSize);
		renderer.setFitLegend(true);
		renderer.setMargins(new int[] { marginSizeNormal, marginSizeLarge,
				marginSizeLarge, marginSizeNormal });
		renderer.setPanEnabled(true, false);
		renderer.setZoomEnabled(true, false);
		renderer.setZoomButtonsVisible(false);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.DKGRAY);
		renderer.setMarginsColor(Color.BLACK);
		renderer.setGridColor(Color.WHITE);
		renderer.setPointSize(8);
		renderer.setShowGridY(true);
		renderer.setClickEnabled(false);
		renderer.setXLabels(6);

		return renderer;
	}

	private String getDateFormat(double min, double max) {
		String format = "HH:mm:ss";

		double timespan = max - min;
		if (timespan > 1000 * 60 * 60 * 24) {
			format = "dd MM yyyy";
		}

		return format;
	}

	private long getPastDay(int difference) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -difference);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DATE);
		calendar.set(year, month, day, 0, 0, 0);
		return calendar.getTime().getTime();
	}

}
