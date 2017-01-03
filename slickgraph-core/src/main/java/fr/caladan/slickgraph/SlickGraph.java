package fr.caladan.slickgraph;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Slick Graph is a binning and smoothing technique for time series visualization.
 * Slick Graphs mitigate quantization artifacts by using the smallest possible binning intervals, i.e. pixels.
 * They nonetheless provide smooth variations by using a convolution with a kernel.
 * The filtered-out information that would be lost by this smoothing step is encoded using the luminance channel.
 */
public class SlickGraph extends Group {

	/** Canvas used to render the timeseries */
	protected Canvas canvas;
	public DoubleProperty widthProperty() {
		return canvas.widthProperty();
	}
	public DoubleProperty heightProperty() {
		return canvas.heightProperty();
	}

	/** Timeseries to render */
	protected ObservableList<Timeseries> timeseries;
	public List<Timeseries> getTimeseries() {
		return timeseries;
	}
	public void setTimeseries(List<Timeseries> timeseries) {
		start = timeseries.get(0).getData().get(0);
		end = timeseries.get(0).getData().get(timeseries.get(0).getData().size() - 1);
		this.timeseries.clear();
		this.timeseries.addAll(timeseries);
	}

	/** Bandwidth to use when computing the kernel estimation */
	protected SimpleDoubleProperty kernelBandWidthProperty;
	public SimpleDoubleProperty kernelBandwidthProperty() {
		return kernelBandWidthProperty;
	}
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
	protected Map<Timeseries, List<Double>> mapHistograms;

	/** Histogram values after the convolution */
	protected Map<Timeseries, List<Double>> mapSmoothedHistogram;

	/** Vertices of the histogram */
	protected List<Vertex> histogramVertices;

	/** Vertices of the graph */
	protected Map<Timeseries, List<Vertex>> mapVertices;

	/** Horizontal scale factor */
	protected SimpleDoubleProperty xScaleProperty;

	/** Vertical scale factor */
	protected SimpleDoubleProperty yScaleProperty;

	/** Width (in physical pixels) of the canvas */
	protected double scaledWidth;

	/** Height (in physical pixels) of the canvas */
	protected double scaledHeight;

	/** Time cursor */
	protected TimeCursor timeCursor;

	/** Time cursor visibility property */
	protected SimpleBooleanProperty timeCursorVisibleProperty;
	public SimpleBooleanProperty timeCursorVisibleProperty() {
		return timeCursorVisibleProperty;
	}
	public boolean isTimeCursorVisible() {
		return timeCursorVisibleProperty.get();
	}
	public void setTimeCursorVisible(boolean visible) {
		timeCursorVisibleProperty.set(visible);
	}

	/** Indicates whether hide or show the shading representing the difference between the smoothed and the real value */
	protected SimpleBooleanProperty showShadingProperty;
	public SimpleBooleanProperty showShadingProperty() {
		return showShadingProperty;
	}
	public boolean isShadingShown() {
		return showShadingProperty.get();
	}
	public void setShowShading(boolean showShading) {
		showShadingProperty.set(showShading);
	}

	/** Color of the timeseries outine */
	protected SimpleObjectProperty<Color> curveColorProperty;
	public SimpleObjectProperty<Color> curveColorProperty() {
		return curveColorProperty;
	}
	public Color getCurveColor() {
		return curveColorProperty.get();
	}
	public void setCurveColor(Color curveColor) {
		curveColorProperty.set(curveColor);
	}

	/** Indicates whether hide or show the curve of the graph */
	protected SimpleBooleanProperty showCurveProperty;
	public SimpleBooleanProperty showCurveProperty() {
		return showCurveProperty;
	}
	public boolean isCurveShown() {
		return showCurveProperty.get();
	}
	public void setShowCurve(boolean showCurve) {
		showCurveProperty.set(showCurve);
	}

	/** Public default constructor - initializes the properties */
	public SlickGraph() {
		super();

		canvas = new Canvas();
		getChildren().add(canvas);
		timeseries = FXCollections.observableArrayList();
		kernelBandWidthProperty = new SimpleDoubleProperty(5.0);
		mapHistograms = new HashMap<Timeseries, List<Double>>();
		mapSmoothedHistogram = new HashMap<Timeseries, List<Double>>();
		start = -1;
		end = -1;
		histogramVertices = new ArrayList<Vertex>();
		mapVertices = new HashMap<Timeseries, List<Vertex>>();
		xScaleProperty = new SimpleDoubleProperty(1.);
		yScaleProperty = new SimpleDoubleProperty(1.);
		timeCursor = new TimeCursor();
		getChildren().add(timeCursor);
		timeCursorVisibleProperty = new SimpleBooleanProperty(true);
		showShadingProperty = new SimpleBooleanProperty(true);
		showCurveProperty = new SimpleBooleanProperty(true);
		curveColorProperty = new SimpleObjectProperty<Color>(Color.BLACK);

		// render the graph when a timeseries is added or removed
		timeseries.addListener((ListChangeListener.Change<? extends Timeseries> c) -> {
			computeVertices();
			render();
		});

		canvas.widthProperty().addListener(e -> handleHiDPI());
		canvas.heightProperty().addListener(e -> handleHiDPI());
		timeCursor.getCursorLine().endYProperty().bind(canvas.heightProperty());

		kernelBandWidthProperty.addListener(e -> {
			computeVertices();
			render();
		});

		// mouse event for the time cursor
		EventHandler<? super InputEvent> mouseEventHandler = e -> {
			double x = e instanceof MouseEvent ? ((MouseEvent) e).getX() : ((ScrollEvent) e).getX();
			int histogramSize = mapHistograms.get(timeseries.get(0)).size();
			if (x < histogramSize) {
				double value = mapSmoothedHistogram.values().stream()
						.mapToDouble(h -> h.get((int) (x * xScaleProperty.get()) + (int) Math.floor(3. * kernelBandWidthProperty.get())) * (end - start) / scaledWidth)
						.sum();
				timeCursor.setTooltipText("y = " + value + " ");
			}
			// TODO can throw a NullPointerException
			timeCursor.setPosition(x, mapVertices.get(timeseries.get(timeseries.size() - 1)).get((int) Math.round(x * 2. * xScaleProperty.get()) / 2 * 2).y / yScaleProperty.get());
		};
		addEventHandler(MouseEvent.MOUSE_MOVED, mouseEventHandler);
		addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEventHandler);
		addEventHandler(ScrollEvent.ANY, mouseEventHandler);

		timeCursor.visibleProperty().bind(timeCursorVisibleProperty);

		// bind the properties setting the visualization parameters
		InvalidationListener propertiesListener = e -> render();
		showShadingProperty.addListener(propertiesListener);
		showCurveProperty.addListener(propertiesListener);
		curveColorProperty.addListener(propertiesListener);
	}

	/**
	 * Constructor that initializes the size of the graph
	 *
	 * @param width Width of the graph
	 * @param height Height of the graph
	 */
	public SlickGraph(double width, double height) {
		this();

		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	/**
	 * Constructor that initializes the data to visualize
	 *
	 * @param data Data that represent the time serie to render
	 * @throws Exception If the data is not valid (timestamps should be strictly increasing)
	 */
	public SlickGraph(List<Double> data) throws Exception {
		this();

		List<Timeseries> timeseries = new ArrayList<Timeseries>();
		timeseries.add(new Timeseries(data));
		setTimeseries(timeseries);
	}

	/**
	 * Constructor that initializes the size of the graph and the data to visualize
	 *
	 * @param width Width of the graph
	 * @param height Height of the graph
	 * @param data Data that represent the time serie to render
	 * @throws Exception If the data is not valid (timestamps should be strictly increasing)
	 */
	public SlickGraph(double width, double height, List<Double> data) throws Exception {
		this(data);

		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	/** Set the scale on the canvas to have a 1:1 pixel mapping */
	protected void handleHiDPI() {
		double nativeWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		double nativeHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
		double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();

		double xScale = nativeWidth / screenWidth;
		double yScale = nativeHeight / screenHeight;

		scaledWidth = canvas.getWidth() * xScale;
		scaledHeight = canvas.getHeight() * yScale;

		// back to scale 1:1
		canvas.getGraphicsContext2D().scale(xScaleProperty.get(), yScaleProperty.get());

		// set the new scale
		canvas.getGraphicsContext2D().scale(1. / xScale, 1. / yScale);

		xScaleProperty.set(xScale);
		yScaleProperty.set(yScale);

		// update the view
		computeVertices();
		render();
	}

	/**
	 * Return the list of the array of events corresponding to the bounds of the pixels in a given time window
	 *
	 * @param start Start timestamp of the time window
	 * @param end End timestamp of the time window
	 * @return
	 */
	protected double[] buildPixelBounds(double start, double end) {
		int toTrim = (int) Math.floor(6. * kernelBandWidthProperty.get());
		double timeSliceDuration = (end - start) / (scaledWidth + toTrim);
		double[] pixelBounds = new double[(int) (scaledWidth + 1 + toTrim)];

		for (int i = 0; i < pixelBounds.length; i++) {
			pixelBounds[i] = start + i * timeSliceDuration;
		}

		return pixelBounds;
	}

	/**
	 * Compute the aggregation of a timeseries based on the pixels
	 *
	 * @param timeseries Timeseries to aggregate
	 */
	protected void buildHistogram(Timeseries timeseries) {
		List<Double> histogram = new ArrayList<Double>();

		// build the timestamps at the pixels bounds
		double[] pixelBounds = buildPixelBounds(start, end);

		// build the list of indices that correspond to the pixel bounds
		List<Integer> listIndices = new ArrayList<Integer>();
		for (int i = 0; i < pixelBounds.length; i++) {
			int boundEvent = Collections.binarySearch(timeseries.getData(), pixelBounds[i]);
			boundEvent = boundEvent >= 0 ? boundEvent : -boundEvent - 1;
			listIndices.add(boundEvent);
		}

		for (int i = 0; i < listIndices.size() - 1; i++) {
			histogram.add((listIndices.get(i + 1) - listIndices.get(i)) / (end - start) * scaledWidth);
		}

		mapHistograms.put(timeseries, histogram);

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

	/**
	 * Convolve the histogram with a statistic kernel for a given timeseries
	 *
	 * @param timeseries Timeseries whose histogram is to convolve
	 */
	protected void computeConvolution(Timeseries timeseries) {
		List<Double> histogram = mapHistograms.get(timeseries);
		List<Double> smoothedHistogram = new ArrayList<Double>();
		// TODO can throw a NullPointerException
		histogram.forEach(i -> smoothedHistogram.add(0.));

		// compute the convolution of the time serie width the kernel
		List<Double> gaussianValues = gaussianKernel();
		for (int i = 2; i < histogram.size(); i++) {
			for (int k = 0; k < gaussianValues.size(); k++) {
				int j = (int) Math.ceil(i + k - gaussianValues.size() / 2.);
				if (j < 0 || j > histogram.size() - 1) {
					continue;
				}
				smoothedHistogram.set(i, smoothedHistogram.get(i) + histogram.get(j) * gaussianValues.get(k));
			}
		}

		mapSmoothedHistogram.put(timeseries, smoothedHistogram);
	}

	/** Compute the vertices for the layered rendering */
	protected void computeStackedVertices() {
		mapVertices.clear();

		// TODO can throw a NullPointerException
		double max = IntStream.range(0, mapSmoothedHistogram.get(timeseries.get(0)).size())
		 		.mapToDouble(i -> mapSmoothedHistogram.values().stream().mapToDouble(h -> h.get(i)).sum())
		 		.summaryStatistics()
		 		.getMax();

		// put the first timeseries at the bottom
		Timeseries ts = timeseries.get(0);
		List<Double> smoothedHistogram = mapSmoothedHistogram.get(ts);
		List<Vertex> vertices = new ArrayList<Vertex>();
		for (int i = 0; i < smoothedHistogram.size(); i++) {
			vertices.add(new Vertex(i, (1. - smoothedHistogram.get(i) / max * .8) * scaledHeight, ts.getColor()));
			vertices.add(new Vertex(i, scaledHeight));
		}
		mapVertices.put(ts, vertices);

		// stack the other timeseries
		for (int i = 1; i < timeseries.size(); i++) {
			ts = timeseries.get(i);
			smoothedHistogram = mapSmoothedHistogram.get(ts);
			vertices = new ArrayList<Vertex>();
			List<Vertex> aboveVertices = mapVertices.get(timeseries.get(i - 1));
			for (int j = 0; j < smoothedHistogram.size(); j++) {
				vertices.add(new Vertex(aboveVertices.get(2 * j).x, aboveVertices.get(2 * j).y - smoothedHistogram.get(j) / max * .8 * scaledHeight, ts.getColor()));
				vertices.add(new Vertex(aboveVertices.get(2 * j).x, aboveVertices.get(2 * j).y, ts.getColor()));
			}

			mapVertices.put(ts, vertices);
		}

		// trim 3 times the kernel bandwidth at each side
		int toTrim = (int) (Math.round(3. * kernelBandWidthProperty.get() / 2.) * 2);
		mapVertices.forEach((t, vt) -> {
			vt = vt.subList(toTrim, vt.size() - toTrim);
			vt.forEach(v -> v.x -= toTrim);
			mapVertices.put(t, vt);
		});
	}

	/** Compute the vertices of the graph */
	protected void computeVertices() {
		// nothing to do if not shown yet or not data
		if (canvas.getWidth() == 0. || canvas.getHeight() == 0. || timeseries.isEmpty()) {
			return;
		}

		// aggregate the timeseries
		timeseries.parallelStream().forEach(ts -> {
			buildHistogram(ts);
			computeConvolution(ts);
		});

		computeStackedVertices();
	}

	/**
	 * Perform a zoom
	 *
	 * @param z Y-delta of the zoom
	 */
	public void zoom(double z) {
		double delta = 50. * (end - start) / scaledWidth;
		if (z > 0) {
			start += delta;
			end -= delta;
		} else {
			start -= delta;
			end += delta;
		}

		computeVertices();
		render();
	}

	/**
	 * Perform a pan
	 *
	 * @param deltaX Horizontal displacement of the mouse cursor
	 */
	public void pan(double deltaX) {
		double delta = deltaX * (end - start) / scaledWidth;
		start += delta;
		end += delta;

		computeVertices();
		render();
	}

	/** Draw the graph */
	protected void render() {
		// sanity check
		boolean verticesReady = true;
		int i = 0;
		while (i < timeseries.size() && verticesReady) {
			verticesReady = verticesReady && mapVertices.containsKey(timeseries.get(i));
			i++;
		}

		if (timeseries == null || timeseries.isEmpty() || !verticesReady) {
			return;
		}

		GraphicsContext gc = canvas.getGraphicsContext2D();

		// clear the canvas
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, scaledWidth, scaledHeight);

		// render the shading
		/* if (showShadingProperty.get()) {
			List<Vertex> vertices = mapVertices.get(timeseries.get(2));
			vertices.forEach(v -> {
				gc.setStroke(v.color);
				gc.strokeLine(v.x, scaledHeight, v.x, v.y);
			});
		} */

		timeseries.forEach(ts -> {
			List<Vertex> vertices = mapVertices.get(ts);
			gc.setStroke(ts.getColor());
			for (int v = 0; v < vertices.size() - 1; v += 2) {
				gc.strokeLine(vertices.get(v).x, vertices.get(v).y, vertices.get(v + 1).x, vertices.get(v + 1).y);
			}
		});

		/* mapVertices.forEach((timeseries, vertices) -> {
			vertices.forEach(v -> {
				// System.out.println(v);
				gc.setStroke(v.color);
				gc.strokeLine(v.x, scaledHeight, v.x, v.y);
			});
		}); */

		// render the curve
		if (showCurveProperty.get()) {
			gc.setStroke(Color.BLACK);
			mapVertices.values().forEach(vertices -> {
				for (int j = 0; j < vertices.size() - 3; j += 2) {
					gc.strokeLine(vertices.get(j).x, vertices.get(j).y, vertices.get(j + 2).x, vertices.get(j + 2).y);
				}
			});
		}
	}

}
