package fr.caladan.slickgraph.dataloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.caladan.slickgraph.Timeseries;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;

/**
 * Simplest implementation of a timeseries loader.
 * The timeseries are in memory, as a list.
 */
public class InMemoryTimeseriesLoader implements TimeseriesLoader {

	private ListProperty<Timeseries> timeseries;
	/* (non-Javadoc)
	 * @see fr.caladan.slickgraph.dataloader.TimeseriesLoader#getTimeseries()
	 */
	@Override
	public ListProperty<Timeseries> getTimeseries() {
		return timeseries;
	}

	private MapProperty<Timeseries, List<Double>> mapHistograms;
	/* (non-Javadoc)
	 * @see fr.caladan.slickgraph.dataloader.TimeseriesLoader#getHistograms()
	 */
	@Override
	public MapProperty<Timeseries, List<Double>> getHistograms() {
		return mapHistograms;
	}

	/** Timestamp of the earliest event among the timeseries */
	private double startGlobal;
	@Override
	public double getStartGlobal() {
		return startGlobal;
	}

	/** Timestamp of the latest event among the timeseries */
	private double endGlobal;
	@Override
	public double getEndGlobal() {
		return endGlobal;
	}

	/** Timestamp of the earliest event of the time window currently visualized */
	private double startTimeWindow;
	@Override
	public double getStartTimeWindow() {
		return startTimeWindow;
	}

	/** Timestamp of the latest event of the time window currently being visualized */
	private double endTimeWindow;
	@Override
	public double getEndTimeWindow() {
		return endTimeWindow;
	}

	/** Number of time slices (or bins) to use to compute the histograms */
	private double nbTimeSlices;
	@Override
	public double getNbTimeSlices() {
		return nbTimeSlices;
	}
	@Override
	public void setNbTimeSlices(double nbTimeSlices) {
		this.nbTimeSlices = nbTimeSlices;
	}

	/** Number of pixels to trim from left and right sides to have an accurate rendering on the borders */
	protected int pixelsToTrim;
	@Override
	public void setPixelsToTrim(int pixelsToTrim) {
		this.pixelsToTrim = pixelsToTrim;
	}

	/**
	 * Public constructor that initializes the loader with an in-memory list of timeseries
	 *
	 * @param timeseries List of timeseries to work with
	 */
	public InMemoryTimeseriesLoader(List<Timeseries> timeseries) {
		this.timeseries = new SimpleListProperty<Timeseries>();
		this.timeseries.setValue(FXCollections.observableArrayList(timeseries));
		mapHistograms = new SimpleMapProperty<Timeseries, List<Double>>();
		mapHistograms.setValue(FXCollections.observableHashMap());

		startGlobal = this.timeseries.stream()
				.mapToDouble(ts -> ts.getData().stream()
						.mapToDouble(d -> d)
						.summaryStatistics()
						.getMin())
				.summaryStatistics()
				.getMin();
		endGlobal = this.timeseries.stream()
				.mapToDouble(ts -> ts.getData().stream()
						.mapToDouble(d -> d)
						.summaryStatistics()
						.getMax())
				.summaryStatistics()
				.getMax();

		startTimeWindow = startGlobal;
		endTimeWindow = endGlobal;
	}

	/* (non-Javadoc)
	 * @see fr.caladan.slickgraph.dataloader.TimeseriesLoader#updateTimeWindow(double, double)
	 */
	@Override
	public void updateTimeWindow(double start, double end) {
		if (nbTimeSlices <= 0 || start > end || timeseries.isEmpty()) {
			return;
		}

		startTimeWindow = start;
		endTimeWindow = end;

		// aggregate the timeseries
		Map<Timeseries, List<Double>> histograms = new ConcurrentHashMap<Timeseries, List<Double>>();
		timeseries.parallelStream().forEach(ts -> histograms.put(ts, buildHistogram(ts)));
		synchronized (mapHistograms) {
			mapHistograms.getValue().putAll(histograms);
		}
	}

	/**
	 * Return the list of the array of events corresponding to the bounds of the pixels in a given time window
	 *
	 * @param start Start timestamp of the time window
	 * @param end End timestamp of the time window
	 * @return
	 */
	protected double[] buildPixelBounds(double start, double end) {
		double timeSliceDuration = (end - start) / nbTimeSlices;
		double[] pixelBounds = new double[(int) nbTimeSlices + 2 * pixelsToTrim];

		for (int i = 0; i < pixelBounds.length; i++) {
			pixelBounds[i] = start + i * timeSliceDuration;
		}

		return pixelBounds;
	}

	/**
	 * Compute the aggregation of a timeseries based on the pixels
	 *
	 * @param timeseries Timeseries to aggregate
	 * @return Histograms containing the aggregated timeseries
	 */
	protected List<Double> buildHistogram(Timeseries timeseries) {
		List<Double> histogram = new ArrayList<Double>();

		// build the timestamps at the pixels bounds
		double[] pixelBounds = buildPixelBounds(startTimeWindow, endTimeWindow);

		// build the list of indices that correspond to the pixel bounds
		List<Integer> listIndices = new ArrayList<Integer>();
		for (int i = 0; i < pixelBounds.length; i++) {
			int boundEvent = Collections.binarySearch(timeseries.getData(), pixelBounds[i]);
			boundEvent = boundEvent >= 0 ? boundEvent : -boundEvent - 1;
			listIndices.add(boundEvent);
		}
		
		for (int i = 0; i < listIndices.size() - 1; i++) {
			histogram.add((listIndices.get(i + 1) - listIndices.get(i)) / (endTimeWindow - startTimeWindow) * nbTimeSlices);
		}

		return histogram;
	}

}
