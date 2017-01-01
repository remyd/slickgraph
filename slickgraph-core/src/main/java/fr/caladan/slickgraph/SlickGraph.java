package fr.caladan.slickgraph;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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

	/** Data to render */
	protected SimpleObjectProperty<List<Double>> dataProperty;
	public List<Double> getData() {
		return dataProperty.get();
	}
	public void setData(List<Double> data) throws Exception {
		// check if the timeseries is valid: timestamps are strictly growing
		int i = 0;
		boolean isValid = true;
		while (i < data.size() - 1 && isValid) {
			isValid = data.get(i) < data.get(i + 1);
			i++;
		}
		if (!isValid) {
			throw new Exception("Timeseries is not valid: timestamps should be strictly increasing");
		}

		dataProperty.set(data);
		if (!data.isEmpty()) {
			start = data.get(0);
			end = data.get(data.size() - 1);
		} else {
			start = -1;
			end = -1;
		}
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
	protected List<Double> histogram;

	/** Histogram values after the convolution */
	protected List<Double> smoothedHistogram;

	/** Maximum value in the histogram */
	protected double histogramMax;

	/** Vertices of the histogram */
	protected List<Vertex> histogramVertices;

	/** Vertices of the graph */
	protected List<Vertex> vertices;

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
		dataProperty = new SimpleObjectProperty<List<Double>>();
		kernelBandWidthProperty = new SimpleDoubleProperty(5.0);
		histogram = new ArrayList<Double>();
		smoothedHistogram = new ArrayList<Double>();
		start = -1;
		end = -1;
		histogramMax = -1;
		histogramVertices = new ArrayList<Vertex>();
		vertices = new ArrayList<Vertex>();
		xScaleProperty = new SimpleDoubleProperty(1.);
		yScaleProperty = new SimpleDoubleProperty(1.);
		timeCursor = new TimeCursor();
		getChildren().add(timeCursor);
		timeCursorVisibleProperty = new SimpleBooleanProperty(true);
		showShadingProperty = new SimpleBooleanProperty(true);
		showCurveProperty = new SimpleBooleanProperty(true);
		curveColorProperty = new SimpleObjectProperty<Color>(Color.rgb(0, 145, 255));

		dataProperty.addListener(e -> {
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
			if (x < histogram.size()) {
				timeCursor.setTooltipText(" y = " +
						smoothedHistogram.get((int) (x * xScaleProperty.get()) +
						(int) Math.floor(3. * kernelBandWidthProperty.get())) * (end - start) / scaledWidth +
						" ");
			}
			timeCursor.setPosition(x, vertices.get((int) (x * xScaleProperty.get())).y / yScaleProperty.get());
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

		setData(data);
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

		for (int i = 0; i < listIndices.size() - 1; i++) {
			histogram.add((listIndices.get(i + 1) - listIndices.get(i)) / (end - start) * scaledWidth);
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
		// clear the vertices list if not empty and initialize all the vertices
		smoothedHistogram.clear();
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
	}

	/** Apply a scaling factor on the vertical coordinate of the vertices to ensure a correct window fitting and compute the Slick Graph shading */
	protected void scaleAndShadeVertices() {
		double max = smoothedHistogram.stream()
				.mapToDouble(i -> i)
				.summaryStatistics()
				.getMax();
		double scalingFactor = 1.2 / (max / histogramMax);

		vertices.clear();
		for (int i = 0; i < smoothedHistogram.size(); i++) {
			double alpha = histogram.get(i) == 0 ? 0. : 1. / (1. + smoothedHistogram.get(i) / histogram.get(i));
			vertices.add(new Vertex(i,
					(1. - (smoothedHistogram.get(i) * scalingFactor) / (max * scalingFactor) * .8) * scaledHeight,
					Color.rgb(0, 0, 0, alpha))
			);
		}

		// update the coordinate of the last vertex
		Vertex lastVertex = vertices.get(vertices.size() - 1);
		lastVertex.y = scaledHeight - lastVertex.y / max * scaledHeight * .8;

		// trim 3 times the kernel bandwidth at each side
		int toTrim = (int) Math.floor(3. * kernelBandWidthProperty.get());
		vertices = vertices.subList(toTrim, vertices.size() - toTrim);
		vertices.forEach(v -> v.x -= toTrim);
	}

	/** Compute the vertices of the graph */
	protected void computeVertices() {
		// nothing to do if not shown yet or not data
		if (canvas.getWidth() == 0. || canvas.getHeight() == 0. || dataProperty.get() == null) {
			return;
		}

		buildHistogram();
		computeConvolution();
		scaleAndShadeVertices();
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
		GraphicsContext gc = canvas.getGraphicsContext2D();

		// clear the canvas
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, scaledWidth, scaledHeight);

		// render the shading
		if (showShadingProperty.get()) {
			vertices.forEach(v -> {
				gc.setStroke(v.color);
				gc.strokeLine(v.x, scaledHeight, v.x, v.y);
			});
		}

		// render the curve
		if (showCurveProperty.get()) {
			gc.setStroke(curveColorProperty.get());
			for (int i = 0; i < vertices.size() - 1; i++) {
				gc.strokeLine(vertices.get(i).x, vertices.get(i).y, vertices.get(i + 1).x, vertices.get(i + 1).y);
			}
		}
	}

}
