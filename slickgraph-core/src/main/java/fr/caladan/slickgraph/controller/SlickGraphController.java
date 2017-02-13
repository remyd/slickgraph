package fr.caladan.slickgraph.controller;

import java.net.URL;
import java.util.ResourceBundle;

import fr.caladan.slickgraph.SlickGraph;
import fr.caladan.slickgraph.dataloader.TimeseriesLoader;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

/**
 * Controller managing the events from the view and model
 */
public abstract class SlickGraphController implements Initializable {

	/** Root pane - Slick Graph container */
	@FXML
	protected Pane slgContainer;

	/** SlickGraph node */
	protected SlickGraph slickGraph;

	/** Timeseries loader */
	protected TimeseriesLoader timeseriesLoader;

	/** Horizontal coordinate of the last mouse event */
	protected double origMouseX;
	
	/** Load the timeseries loader to work with */
	protected abstract void initializeTimeseriesLoader();

	/* (non-Javadoc)
	 * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		initializeTimeseriesLoader();

		slickGraph = new SlickGraph();
		insertSlickGraph(true);
		bindEventListeners();

		origMouseX = 0;
	}

	/**
	 * Insert the Slick Graph into the scene
	 *
	 * @param container Pane that will contain the Slick Graph
	 * @param bindDimensions True to make the Slick Graph fitting its container size, false otherwise
	 */
	protected void insertSlickGraph(boolean bindDimensions) {
		slgContainer.getChildren().add(slickGraph);

		if (bindDimensions) {
			slickGraph.widthProperty().bind(slgContainer.widthProperty());
			slickGraph.heightProperty().bind(slgContainer.heightProperty());
		}
		
		timeseriesLoader.setPixelsToTrim(slickGraph.getPixelsToTrim());
	}

	/** Bind the different events to the listeners */
	protected void bindEventListeners() {
		// user events
		slickGraph.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
		slickGraph.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
		slickGraph.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
		slickGraph.addEventHandler(ScrollEvent.ANY, this::onMouseScroll);

		// layout events
		slickGraph.scaledWidthProperty().addListener(e -> {
			timeseriesLoader.setNbTimeSlices(slickGraph.getScaledWidth());
			timeseriesLoader.updateTimeWindow(timeseriesLoader.getStartTimeWindow(), timeseriesLoader.getEndTimeWindow());
		});

		// model events
		slickGraph.pixelsToTrimProperty().addListener(e -> {
			timeseriesLoader.setPixelsToTrim(slickGraph.getPixelsToTrim());
			timeseriesLoader.updateTimeWindow(timeseriesLoader.getStartTimeWindow(), timeseriesLoader.getEndTimeWindow());
		});
		slickGraph.getTimeseries().bind(timeseriesLoader.getTimeseries());
		timeseriesLoader.getHistograms().addListener((histograms, oldValue, newValue) -> slickGraph.update(histograms.getValue()));
	}

	protected void onMousePressed(MouseEvent event) {
		origMouseX = event.getSceneX();
	}

	protected void onMouseMoved(MouseEvent event) {
		slickGraph.pickTimeseries(event.getX(), event.getY());
	}

	protected void onMouseDragged(MouseEvent event) {
		timeseriesLoader.pan(origMouseX - event.getSceneX());
		origMouseX = event.getSceneX();
	}

	protected void onMouseScroll(ScrollEvent event) {
		timeseriesLoader.zoom(event.getDeltaY());
	}

}
