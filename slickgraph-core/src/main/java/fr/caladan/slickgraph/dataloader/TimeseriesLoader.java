package fr.caladan.slickgraph.dataloader;

import java.util.List;

import fr.caladan.slickgraph.Timeseries;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;

/** Interface defining the operations the timeseries loaders have to implement */
public interface TimeseriesLoader {

	/** @return Timestamp of the earliest event among the timeseries */
	public double getStartGlobal();

	/** @return Timestamp of the latest event among the timeseries */
	public double getEndGlobal();
	
	/** @return Timestamp of the earliest event of the time window currently visualized */
	public double getStartTimeWindow();
	
	/** @return Timestamp of the latest event of the time window currently being visualized */
	public double getEndTimeWindow();
	
	/** @return Number of time slices (or bins) to use to compute the histograms */
	public double getNbTimeSlices();
	
	/**
	 * Set the number of time slices (or bins) to use to compute the histograms
	 * 
	 * @param nbTimeSlices Number of time slices for the histogram
	 */
	public void setNbTimeSlices(double nbTimeSlices);
	
	/**
	 * @param pixelsToTrim Number of pixels to add to the histogram that will be trimmed at rendering. It is done to avoid the curve to be disformed at the boundaries of the graph
	 */
	public void setPixelsToTrim(int pixelsToTrim);

	/**
	 * List of timeseries to be rendered.
	 * Order of this list corresponds to the order of rendering from bottom to top
	 * 
	 * @return List of timeseries to be rendered
	 */
	public ListProperty<Timeseries> getTimeseries();

	/**
	 * Histograms resulted from the timeseries aggregation.
	 * There is one histogram per timeseries.
	 * Keys are the name of the actors, values are the histograms.
	 * 
	 * @return Histograms resulted from the aggregation
	 */
	public MapProperty<Timeseries, List<Double>> getHistograms();

	/**
	 * Update the time window to be visualized.
	 * Compute the timeseries aggregation based on the new time window.
	 * 
	 * @param start Start timestamp of the new time window
	 * @param end End timestamp of the new time window
	 */
	public void updateTimeWindow(double start, double end);

	/**
	 * Perform a pan
	 *
	 * @param deltaX Horizontal displacement of the mouse cursor
	 */
	public default void pan(double deltaX) {
		double start = getStartTimeWindow();
		double end = getEndTimeWindow();

		double delta = deltaX * (end - start) / getNbTimeSlices();
		start += delta;
		end += delta;

		updateTimeWindow(start, end);
	}

	/**
	 * Perform a zoom
	 *
	 * @param z Y-delta of the zoom
	 */
	public default void zoom(double z) {
		double start = getStartTimeWindow();
		double end = getEndTimeWindow();

		double delta = 50. * (end - start) / getNbTimeSlices();
		if (z > 0) {
			start += delta;
			end -= delta;
		} else {
			start -= delta;
			end += delta;
		}

		updateTimeWindow(start, end);
	}

}
