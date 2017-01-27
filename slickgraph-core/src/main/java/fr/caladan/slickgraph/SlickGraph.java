package fr.caladan.slickgraph;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import fr.caladan.slickgraph.StatisticKernel.KernelType;
import javafx.animation.AnimationTimer;
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
		this.timeseries.forEach(ts -> ts.selectedProperty().removeListener(propertiesListener));
		this.timeseries.clear();
		this.timeseries.addAll(timeseries);
		this.timeseries.forEach(ts -> ts.selectedProperty().addListener(propertiesListener));

		if (timeseries.isEmpty()) {
			start = -1;
			end = -1;
		} else {
			start = timeseries.get(0).getData().get(0);
			end = timeseries.get(0).getData().get(timeseries.get(0).getData().size() - 1);
		}
	}
	
	/** Type of the kernel to use to smooth the graph */
	protected SimpleObjectProperty<KernelType> kernelTypeProperty;
	public SimpleObjectProperty<KernelType> kernelTypeProperty() {
		return kernelTypeProperty;
	}
	public void setKernelType(KernelType kernelType) {
		kernelTypeProperty.set(kernelType);
	}
	public KernelType getKernelType() {
		return kernelTypeProperty.get();
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

	/** Number of pixels to trim from left and right sides to have an accurate rendering on the borders */
	protected int toTrim;

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

	/** List of alpha values for the SlickGraph shading */
	protected List<Double> slgAlphas;

	/** Background color */
	protected SimpleObjectProperty<Color> backgroundColorProperty;
	public SimpleObjectProperty<Color> backgroundColorProperty() {
		return backgroundColorProperty;
	}
	public Color getBackgroundColor() {
		return backgroundColorProperty.get();
	}
	public void setBackgroundColor(Color backgroundColor) {
		backgroundColorProperty.set(backgroundColor);
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

	/** Listener of the different properties */
	protected InvalidationListener propertiesListener;
	
	/** Atomic variable to know whether the vertices are ready or not */
	protected AtomicBoolean verticesReady;
	
	/** Indicates whether or not the frame needs to be refresh */
	protected AtomicBoolean needsRefresh;
	
	/** Public default constructor - initializes the properties */
	public SlickGraph() {
		super();
		
		canvas = new Canvas();
		getChildren().add(canvas);
		timeseries = FXCollections.observableArrayList();
		kernelBandWidthProperty = new SimpleDoubleProperty(5.0);
		kernelTypeProperty = new SimpleObjectProperty<KernelType>(KernelType.GAUSSIAN);
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
		showShadingProperty = new SimpleBooleanProperty(false);
		slgAlphas = new ArrayList<Double>();
		backgroundColorProperty = new SimpleObjectProperty<Color>(Color.WHITE);
		showCurveProperty = new SimpleBooleanProperty(true);
		curveColorProperty = new SimpleObjectProperty<Color>(Color.BLACK);
		verticesReady = new AtomicBoolean(false);
		needsRefresh = new AtomicBoolean(false);

		// render the graph when a timeseries is added or removed
		timeseries.addListener((ListChangeListener.Change<? extends Timeseries> c) -> computeVertices());

		canvas.widthProperty().addListener(e -> handleHiDPI());
		canvas.heightProperty().addListener(e -> handleHiDPI());
		timeCursor.getCursorLine().endYProperty().bind(canvas.heightProperty());

		kernelBandWidthProperty.addListener(e -> computeVertices());
		
		// mouse event for the time cursor
		EventHandler<? super InputEvent> mouseEventHandler = e -> {
			if (!verticesReady.get()) {
				return;
			}

			double x = Math.max(0, Math.min(canvas.getWidth(), e instanceof MouseEvent ? ((MouseEvent) e).getX() : ((ScrollEvent) e).getX()));
			double value = mapSmoothedHistogram.values().stream()
					.mapToDouble(h -> h.get((int) (x * xScaleProperty.get()) + (int) Math.floor(3. * kernelBandWidthProperty.get())) * (end - start) / scaledWidth)
					.sum();
			timeCursor.setTooltipText(" y = " + value + " ");

			timeCursor.setPosition(
					x,
					mapVertices.get(timeseries.get(timeseries.size() - 1)).get((int) Math.round(x * 2. * xScaleProperty.get()) / 2 * 2).y / yScaleProperty.get()
			);
			
			needsRefresh.set(true);
		};
		addEventHandler(MouseEvent.MOUSE_MOVED, mouseEventHandler);
		addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseEventHandler);
		addEventHandler(ScrollEvent.ANY, mouseEventHandler);

		timeCursor.visibleProperty().bind(timeCursorVisibleProperty);

		// bind the properties setting the visualization parameters
		propertiesListener = e -> needsRefresh.set(true);
		showShadingProperty.addListener(propertiesListener);
		showCurveProperty.addListener(propertiesListener);
		backgroundColorProperty.addListener(propertiesListener);
		curveColorProperty.addListener(propertiesListener);
		
		// launch the rendering loop - 60 fps
		new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (needsRefresh.getAndSet(false)) {
					render();
				}
			}
		}.start();
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
	 * Constructor that initializes the data to visualize and the smoothing method
	 *
	 * @param data Data that represent the time serie to render
	 * @param kernelType Kernel to use for the smoothing step
	 * @throws Exception If the data is not valid (timestamps should be strictly increasing)
	 */

	public SlickGraph(List<Double> data, KernelType kernelType) throws Exception {
		this(data);
		
		kernelTypeProperty.set(kernelType);
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

	/**
	 * Constructor that initializes the size of the graph, the data to visualize and the smoothing method
	 *
	 * @param width Width of the graph
	 * @param height Height of the graph
	 * @param data Data that represent the time serie to render
	 * @param kernelType Kernel to use for the smoothing step
	 * @throws Exception If the data is not valid (timestamps should be strictly increasing)
	 */
	public SlickGraph(double width, double height, List<Double> data, KernelType kernelType) throws Exception {
		this(width, height, data);
		
		kernelTypeProperty.set(kernelType);
	}

	/** Set the scale on the canvas to have a 1:1 pixel mapping */
	protected void handleHiDPI() {
		GraphicsDevice devices[] = null;
		try {
			devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		} catch (HeadlessException e) {
			return;
		}

		GraphicsDevice currentDevice = devices.length > 1 ? devices[1] : devices[0];
		double nativeWidth = currentDevice.getDisplayMode().getWidth();
		double nativeHeight = currentDevice.getDisplayMode().getHeight();
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
	}

	/**
	 * Return the list of the array of events corresponding to the bounds of the pixels in a given time window
	 *
	 * @param start Start timestamp of the time window
	 * @param end End timestamp of the time window
	 * @return
	 */
	protected double[] buildPixelBounds(double start, double end) {
		toTrim = (int) (Math.round(3. * kernelBandWidthProperty.get() / 2.) * 2);
		double timeSliceDuration = (end - start) / (scaledWidth);
		double[] pixelBounds = new double[(int) (scaledWidth + 1 + 2 * toTrim)];

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
	 * Convolve the histogram with a statistic kernel for a given timeseries
	 *
	 * @param timeseries Timeseries whose histogram is to convolve
	 */
	protected void computeConvolution(Timeseries timeseries) {
		List<Double> histogram = mapHistograms.get(timeseries);
		List<Double> smoothedHistogram = new ArrayList<Double>();
		histogram.forEach(i -> smoothedHistogram.add(0.));

		// compute the convolution of the time serie width the kernel
		List<Double> kernelValues = StatisticKernel.kernelValues(kernelBandWidthProperty.get(), kernelTypeProperty.get());
		for (int i = 2; i < histogram.size(); i++) {
			for (int k = 0; k < kernelValues.size(); k++) {
				int j = (int) Math.ceil(i + k - kernelValues.size() / 2.);
				if (j < 0 || j > histogram.size() - 1) {
					continue;
				}
				smoothedHistogram.set(i, smoothedHistogram.get(i) + histogram.get(j) * kernelValues.get(k));
			}
		}

		mapSmoothedHistogram.put(timeseries, smoothedHistogram);
	}

	/** Compute the vertices for the layered rendering */
	protected void computeStackedVertices() {
		mapVertices.clear();

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
		mapVertices.forEach((t, vt) -> {
			vt = vt.subList(toTrim * 2, vt.size() - toTrim * 2);
			vt.forEach(v -> v.x -= toTrim);
			mapVertices.put(t, vt);
		});
	}

	/** Compute the alpha values used for the SlickGraph shading */
	protected void computeSlgAlphas() {
		slgAlphas.clear();

		IntStream.range(0, mapHistograms.get(timeseries.get(0)).size()).forEach(i -> {
			double vh = mapHistograms.values().stream()
					.mapToDouble(h -> h.get(i))
					.sum();
			double vsh = mapSmoothedHistogram.values().stream()
					.mapToDouble(sh -> sh.get(i))
					.sum();

			slgAlphas.add(vh == 0 ? 0. : 1. / (1. + vsh / vh));
		});

		slgAlphas = slgAlphas.subList(toTrim, slgAlphas.size() - toTrim);
	}

	/** Compute the vertices of the graph */
	protected void computeVertices() {
		verticesReady.set(false);

		// nothing to do if not shown yet or not data
		if (canvas.getWidth() == 0. || canvas.getHeight() == 0. || timeseries.isEmpty()) {
			return;
		}

		// aggregate the timeseries
		timeseries.stream().forEach(ts -> {
			synchronized (ts) {
				buildHistogram(ts);
				computeConvolution(ts);
			}
		});

		computeStackedVertices();
		computeSlgAlphas();
		
		verticesReady.set(true);
		needsRefresh.set(true);
	}

	/**
	 * Perform a zoom
	 *
	 * @param z Y-delta of the zoom
	 */
	public void zoom(double z) {
		double delta = 50. * (end - start) / canvas.getWidth();
		if (z > 0) {
			start += delta;
			end -= delta;
		} else {
			start -= delta;
			end += delta;
		}

		computeVertices();
	}

	/**
	 * Perform a pan
	 *
	 * @param deltaX Horizontal displacement of the mouse cursor
	 */
	public void pan(double deltaX) {
		double delta = deltaX * (end - start) / canvas.getWidth();
		start += delta;
		end += delta;

		computeVertices();
	}

	/**
	 * Return the timeseries under the position (x, y)
	 *
	 * @param x Horizontal position
	 * @param y Vertical position
	 * @return Timeseries currently under the position (x, y)
	 */
	public Optional<Timeseries> pickTimeseries(double x, double y) {
		final double ys = y * yScaleProperty.get();
		final int xTab = (int) Math.round(x * 2. * xScaleProperty.get()) / 2 * 2;
		Optional<Timeseries> pickedTs = !verticesReady.get() ?
				Optional.empty() :
				timeseries.stream()
				.filter(ts -> mapVertices.get(ts).get(xTab).y <= ys &&
				mapVertices.get(ts).get(xTab + 1).y >= ys)
				.findFirst();

		// remove the previously selected timeseries and set the new selected one
		timeseries.stream().filter(ts -> ts.isSelected()).findFirst().ifPresent(ts -> ts.setSelected(false));
		pickedTs.ifPresent(ts -> ts.setSelected(true));

		return pickedTs;
	}

	/** Draw the graph */
	protected void render() {
		GraphicsContext gc = canvas.getGraphicsContext2D();

		// clear the canvas
		gc.setFill(backgroundColorProperty.get());
		gc.fillRect(0, 0, scaledWidth, scaledHeight);

		// render the shading
		if (showShadingProperty.get()) {
			// render the shading
			List<Vertex> vertices = mapVertices.get(timeseries.get(timeseries.size() - 1));
			for (int v = 0; v < vertices.size() - 1; v += 2) {
				gc.setStroke(Color.rgb(0, 0, 0, slgAlphas.get(v / 2)));
				gc.strokeLine(vertices.get(v).x, vertices.get(v).y, vertices.get(v + 1).x, scaledHeight);
			}

			// render the curve
			if (showCurveProperty.get()) {
				gc.setStroke(curveColorProperty.get());
				for (int j = 0; j < vertices.size() - 3; j += 2) {
					gc.strokeLine(vertices.get(j).x, vertices.get(j).y, vertices.get(j + 2).x, vertices.get(j + 2).y);
				}
			}
		} else {
			// render the timeseries
			mapVertices.entrySet().forEach(entry -> {
				Timeseries ts = entry.getKey();
				List<Vertex> vertices = mapVertices.get(ts);

				gc.setStroke(ts.isSelected() ? ts.getColor().desaturate() : ts.getColor());
				for (int v = 0; v < vertices.size() - 1; v += 2) {
					gc.strokeLine(vertices.get(v).x, vertices.get(v).y, vertices.get(v + 1).x, vertices.get(v + 1).y);
				}
			});

			// render the curve
			if (showCurveProperty.get()) {
				gc.setStroke(curveColorProperty.get());
				mapVertices.values().forEach(vertices -> {
					for (int j = 0; j < vertices.size() - 3; j += 2) {
						gc.strokeLine(vertices.get(j).x, vertices.get(j).y, vertices.get(j + 2).x, vertices.get(j + 2).y);
					}
				});
			}
		}
	}

}
