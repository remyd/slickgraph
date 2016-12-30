package fr.caladan.slickgraph.demo;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import fr.caladan.slickgraph.SlickGraph;

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

	/** Check box controlling the shading */
	@FXML
	private CheckBox showShadingCheckBox;

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

		// bind the checkbox value to the shading property
		slickGraph.showShadingProperty().bind(showShadingCheckBox.selectedProperty());

		origMouseX = 0;

		slickGraph.addEventHandler(ScrollEvent.ANY, e -> {
			// scroll + control updates the smoothing strength (kernel bandwidth used for the convolution step)
			if (e.isControlDown()) {
				double bw = slickGraph.getKernelBandWidth();
				slickGraph.setKernelBandWidth(bw + bw / ((e.getDeltaY() > 0 ? 1 : -1) * 10.));
			// scroll performs a zoom
			} else {
				slickGraph.zoom(e.getDeltaY());
			}
		});

		// save the position of the mouse when pressed
		slickGraph.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> origMouseX = e.getSceneX());

		// dragging the mouse performs a pan
		slickGraph.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			slickGraph.pan(origMouseX - e.getSceneX());
			origMouseX = e.getSceneX();
		});

		// feed the graph
		slickGraph.setData(DataGenerator.generateTimeseries(10000));
	}

}
