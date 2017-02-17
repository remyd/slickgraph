package fr.caladan.slickgraph.demo;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import fr.caladan.slickgraph.Timeseries;
import fr.caladan.slickgraph.controller.SlickGraphController;
import fr.caladan.slickgraph.dataloader.InMemoryTimeseriesLoader;

/**
 * Controller showing how to bind the slick graph component events
 */
public class Controller extends SlickGraphController {

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

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		// layout adjustment due to hidpi settings
		slgContainer.heightProperty().addListener(e -> AnchorPane.setTopAnchor(slgContainer, toolBar.getHeight()));

		// bind the slider value to the kernel bandwidth
		smoothingSlider.setValue(slickGraph.getKernelBandWidth());
		slickGraph.kernelBandwidthProperty().bind(smoothingSlider.valueProperty());

		// bind the different properties to the checkbox values
		slickGraph.showShadingProperty().bind(showShadingCheckBox.selectedProperty());
		slickGraph.showCurveProperty().bind(showCurveCheckBox.selectedProperty());
		slickGraph.timeCursorVisibleProperty().bind(showTimeCursorCheckBox.selectedProperty());
	}

	/* (non-Javadoc)
	 * @see fr.caladan.slickgraph.controller.SlickGraphController#initializeTimeseriesLoader()
	 */
	@Override
	protected void initializeTimeseriesLoader() {
		List<Timeseries> timeseries = new ArrayList<Timeseries>();
		timeseries.add(new Timeseries("my timeseries 1", Color.RED, DataGenerator.generateTimeseries(1000)));
		timeseries.add(new Timeseries("my timeseries 2", Color.GREEN, DataGenerator.generateTimeseries(1000)));
		timeseries.add(new Timeseries("my timeseries 3", Color.BLUE, DataGenerator.generateTimeseries(1000)));

		timeseriesLoader = new InMemoryTimeseriesLoader(timeseries);
	}

}
