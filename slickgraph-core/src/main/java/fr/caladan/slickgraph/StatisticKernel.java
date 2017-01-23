package fr.caladan.slickgraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the different statistic kernel available to smooth the graph
 */
public class StatisticKernel {
	
	public enum KernelType {
		GAUSSIAN,
	};
	
	/**
	 * Return the kernel values according to its type and bandwidth
	 * 
	 * @param bandWidth Kernel bandwidth
	 * @param kernelType Kernel type
	 * @return Kernel values
	 */
	public static List<Double> kernelValues(double bandWidth, KernelType kernelType) {
		List<Double> kernelValues;

		switch (kernelType) {
			case GAUSSIAN:
				kernelValues = gaussian(bandWidth);
				break;
			default:
				kernelValues = new ArrayList<Double>();
		}

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
