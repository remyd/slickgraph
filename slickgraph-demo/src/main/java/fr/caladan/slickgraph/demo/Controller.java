package fr.caladan.slickgraph.demo;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import fr.caladan.slickgraph.SlickGraph;
import fr.caladan.slickgraph.Timeseries;

/**
 * Controller showing how to bind the slick graph component events
 */
public class Controller implements Initializable {

	/** Root pane of the window */
	@FXML
	private AnchorPane root;

	/** Toolbar containing the widgets */
	@FXML
	private ToolBar toolBar;

	/** Slider to control the smoothing strength */
	@FXML
	private Slider smoothingSlider;

	/** Check box controlling the visibility of the shading */
	@FXML
	private CheckBox showShadingCheckBox;

	/** Check box controlling the visibility of the curve */
	@FXML
	private CheckBox showCurveCheckBox;

	/** Check box controlling the visibility of the time cursor */
	@FXML
	private CheckBox showTimeCursorCheckBox;

	/** Slick Graph widget */
	private SlickGraph slickGraph;

	/** Horizontal coordinate of the last mouse event */
	protected double origMouseX;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// layout adjustment due to hidpi settings
		root.heightProperty().addListener(e -> AnchorPane.setTopAnchor(root, toolBar.getHeight()));

		slickGraph = new SlickGraph();
		slickGraph.widthProperty().bind(root.widthProperty());
		slickGraph.heightProperty().bind(root.heightProperty());
		root.getChildren().add(slickGraph);

		// bind the slider value to the kernel bandwidth
		smoothingSlider.setValue(slickGraph.getKernelBandWidth());
		slickGraph.kernelBandwidthProperty().bind(smoothingSlider.valueProperty());

		// bind the different properties to the checkbox values
		slickGraph.showShadingProperty().bind(showShadingCheckBox.selectedProperty());
		slickGraph.showCurveProperty().bind(showCurveCheckBox.selectedProperty());
		slickGraph.timeCursorVisibleProperty().bind(showTimeCursorCheckBox.selectedProperty());

		origMouseX = 0;

		// pick timeseries at the mouse position
		slickGraph.addEventHandler(MouseEvent.MOUSE_MOVED, e -> slickGraph.pickTimeseries(e.getX(), e.getY()));

		// zoom in data on scroll
		slickGraph.addEventHandler(ScrollEvent.ANY, e -> slickGraph.zoom(e.getDeltaY()));

		// save the position of the mouse when pressed
		slickGraph.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> origMouseX = e.getSceneX());

		// dragging the mouse performs a pan
		slickGraph.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			slickGraph.pan(origMouseX - e.getSceneX());
			origMouseX = e.getSceneX();
		});

		// feed the graph
		List<Timeseries> timeseries = new ArrayList<Timeseries>();
		timeseries.add(new Timeseries("my timeseries 1", Color.RED, DataGenerator.generateTimeseries(1000)));
		timeseries.add(new Timeseries("my timeseries 2", Color.GREEN, DataGenerator.generateTimeseries(1000)));
		timeseries.add(new Timeseries("my timeseries 3", Color.BLUE, DataGenerator.generateTimeseries(1000)));
		slickGraph.setTimeseries(timeseries);
	}

}
