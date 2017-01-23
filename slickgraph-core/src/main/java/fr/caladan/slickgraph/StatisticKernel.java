package fr.caladan.slickgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.util.Pair;

/**
 * Implements the different statistic kernel available to smooth the graph
 */
public class StatisticKernel {
	
	public enum KernelType {
		GAUSSIAN,
	};
	
	/** Kernel values cached for futur usage */
	protected static Map<Pair<Double, KernelType>, List<Double>> cachedValues = new HashMap<Pair<Double, KernelType>, List<Double>>();
	
	/**
	 * Return the kernel values according to its type and bandwidth
	 * 
	 * @param bandWidth Kernel bandwidth
	 * @param kernelType Kernel type
	 * @return Kernel values
	 */
	public static List<Double> kernelValues(double bandWidth, KernelType kernelType) {
		// return the cached value if any
		Pair<Double, KernelType> key = new Pair<Double, KernelType>(bandWidth, kernelType);
		if (cachedValues.containsKey(key)) {
			return cachedValues.get(key);
		}

		// otherwise, compute the kernel values
		List<Double> kernelValues;
		switch (kernelType) {
			case GAUSSIAN:
				kernelValues = gaussian(bandWidth);
				break;
			default:
				kernelValues = new ArrayList<Double>();
		}
		
		// cache the values
		cachedValues.put(key, kernelValues);

		return kernelValues;
	}
	
	protected static List<Double> gaussian(double bandWidth) {
		double h = 2. * bandWidth * bandWidth;
		double v = 1. / (bandWidth* Math.sqrt(2. * Math.PI));

		int kernelSize = (int) (Math.ceil(bandWidth * 3) * 2 + 1);
		List<Double> gaussianValues = new ArrayList<Double>(kernelSize);
		for (double i = 0; i < kernelSize; i++) {
			gaussianValues.add(Math.exp(-Math.pow(i - kernelSize / 2, 2) / h) * v);
		}

		return gaussianValues;
	}
	
}
