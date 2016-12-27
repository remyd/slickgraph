package fr.caladan.slickgraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Slick Graph is a binning and smoothing technique for time series visualization.
 * Slick Graphs mitigate quantization artifacts by using the smallest possible binning intervals, i.e. pixels.
 * They nonetheless provide smooth variations by using a convolution with a kernel.
 * The filtered-out information that would be lost by this smoothing step is encoded using the luminance channel.
 *
 * @author "Rémy Dautriche <remy.dautriche@caladan.fr>"
 */
public class SlickGraph extends Canvas {

	/** Data to render */
	protected SimpleObjectProperty<List<Double>> dataProperty;
	public List<Double> getData() {
		return dataProperty.get();
	}
	public void setData(List<Double> data) {
		dataProperty.set(data);
		start = data.get(0);
		end = data.get(data.size() - 1);
	}

	/** Bandwidth to use when computing the kernel estimation */
	protected SimpleDoubleProperty kernelBandWidthProperty;
	public void setKernelBandWidth(double kernelBandWidth) {
		kernelBandWidthProperty.set(Math.max(1., kernelBandWidth));
	}
	public double getKernelBandWidth() {
		return kernelBandWidthProperty.get();
	}

	/** Start timestamp */
	protected double start;

	/** End timestamp */
	protected double end;

	/** Histogram computed after aggregation based on the pixels */
	protected List<Double> histogram;

	/** Maximum value in the histogram */
	protected double histogramMax;

	/** Vertices of the graph */
	protected List<Vertex> vertices;

	/** Public default constructor - initializes the properties */
	public SlickGraph() {
		dataProperty = new SimpleObjectProperty<List<Double>>();
		kernelBandWidthProperty = new SimpleDoubleProperty(1.0);
		histogram = new ArrayList<Double>();
		start = -1;
		end = -1;
		histogramMax = -1;
		vertices = new ArrayList<Vertex>();

		InvalidationListener l = observable -> {
			computeVertices();
			render();
		};

		dataProperty.addListener(l);
		widthProperty().addListener(l);
		heightProperty().addListener(l);
	}

	/**
	 * Constructor that initializes the data
	 *
	 * @param data Data that represent the time serie to render
	 */
	public SlickGraph(List<Double> data) {
		this();

		setData(data);
	}

	/**
	 * Return the list of the array of events corresponding to the bounds of the pixels in a given time window
	 *
	 * @param start Start timestamp of the time window
	 * @param end End timestamp of the time window
	 * @return
	 */
	private double[] buildPixelBounds(double start, double end) {
		double timeSliceDuration = (end - start) / getWidth();
		double[] pixelBounds = new double[(int) (getWidth() + 1)];

		for (int i = 0; i < pixelBounds.length; i++) {
			pixelBounds[i] = start + i * timeSliceDuration;
		}

		return pixelBounds;
	}

	/** Compute the aggregation of the timeseries based on the pixels */
	protected void buildHistogram() {
		histogram.clear();

		// build the timestamps at the pixels bounds
		double[] pixelBounds = buildPixelBounds(start, end);

		// build the list of indices that correspond to the pixel bounds
		List<Integer> listIndices = new ArrayList<Integer>();
		List<Double> data = dataProperty.get();
		for (int i = 0; i < pixelBounds.length; i++) {
			int boundEvent = Collections.binarySearch(data, pixelBounds[i]);
			boundEvent = boundEvent >= 0 ? boundEvent : -boundEvent - 1;
			listIndices.add(boundEvent);
		}

		double nbTimeSlices = getWidth();
		for (int i = 0; i < listIndices.size() - 1; i++) {
			histogram.add((listIndices.get(i + 1) - listIndices.get(i)) / (end - start) * nbTimeSlices);
		}

		histogramMax = histogramMax == -1 ? Collections.max(histogram) : histogramMax;
	}

	/**
	 * Compute the Gaussian values
	 *
	 * @return Gaussian values
	 */
	protected List<Double> gaussianKernel() {
		double kernelBandWidth = kernelBandWidthProperty.get();
		double h = 2. * kernelBandWidth * kernelBandWidth;
		double v = 1. / (kernelBandWidth * Math.sqrt(2. * Math.PI));

		int kernelSize = (int) (Math.ceil(kernelBandWidth * 3) * 2 + 1);
		List<Double> gaussianValues = new ArrayList<Double>(kernelSize);
		for (double i = 0; i < kernelSize; i++) {
			gaussianValues.add(Math.exp(-Math.pow(i - kernelSize / 2, 2) / h) * v);
		}

		return gaussianValues;
	}

	/** Convolve the histogram with a statistic kernel */
	protected void computeConvolution() {
		List<Double> gaussianValues = gaussianKernel();

		// compute the convolution of the time serie width the kernel
		for (int i = 2; i < histogram.size(); i++) {
			for (int k = 0; k < gaussianValues.size(); k++) {
				int j = (int) Math.ceil(i + k - gaussianValues.size() / 2.);
				if (j < 0 || j > histogram.size() - 1) {
					continue;
				}
				vertices.get(i).y += histogram.get(j) * gaussianValues.get(k);
			}
		}
	}

	/** Compute the vertices of the graph */
	protected void computeVertices() {
		// nothing to do if not shown yet
		if (getWidth() == 0. || getHeight() == 0.) {
			return;
		}

		// clear the vertices list if not empty and initialize all the vertices
		vertices.clear();
		for (int i = 0; i < getWidth() + 1; i++) {
			vertices.add(new Vertex(i, 0., Color.BLACK));
		}

		buildHistogram();
		computeConvolution();
		double max = vertices.stream()
				.mapToDouble(v -> v.y)
				.summaryStatistics()
				.getMax();

		double scalingFactor = 2. / (max / histogramMax);
		vertices.forEach(v -> v.y *= scalingFactor);

		// compute the color associated to each vertex that encode the difference between the real value and the smoothed value
		double h = getHeight();
		IntStream.range(0, histogram.size()).parallel().forEach(i -> {
			Vertex vertex = vertices.get(i);
			double alpha = histogram.get(i) == 0 ? 0 : 1. / (1 + vertex.y / histogram.get(i));

			vertex.color = Color.rgb(0, 0, 0, alpha);
			vertex.y = (1. - vertex.y) * h;
		});

		// update the coordinate of the last vertex
		Vertex lastVertex = vertices.get(vertices.size() - 1);
		lastVertex.y = h - lastVertex.y;
	}

	/** Draw the graph */
	protected void render() {
		GraphicsContext gc = getGraphicsContext2D();

		// clear the canvas
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, getWidth(), getHeight());

		double h = getHeight();

		// render the shading
		vertices.forEach(v -> {
			gc.setStroke(v.color);
			gc.strokeLine(v.x, h, v.x, v.y);
		});

		// render the curve
		gc.setStroke(Color.BLUE);
		for (int i = 0; i < vertices.size() - 1; i++) {
			gc.strokeLine(vertices.get(i).x, vertices.get(i).y, vertices.get(i + 1).x, vertices.get(i + 1).y);
		}
	}

}
