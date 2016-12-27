package fr.caladan.slickgraph.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate a timeseries that follows the Poisson distribution
 *
 * @author "Rémy Dautriche <remy.dautriche@caladan.fr>"
 */
public class DataGenerator {

	protected static double poisson(double rate) {
		return -Math.log(1.0 - Math.random()) / rate;
	}

	/**
	 * Generate a timeseries of a given size
	 *
	 * @param size Number of time points in the timeseries
	 * @return Timeseries
	 */
	public static List<Double> generateTimeseries(int size) {
		List<Double> data = new ArrayList<Double>();

		data.add(0.01);
		for (int i = 1; i < size; i++) {
			data.add(data.get(i - 1) + poisson(0.01));
		}

		return data;
	}

}
