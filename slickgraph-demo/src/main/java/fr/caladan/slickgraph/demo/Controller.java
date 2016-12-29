package fr.caladan.slickgraph.demo;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

	/** Slick Graph widget */
	private SlickGraph slickGraph;

	/** Horizontal coordinate of the last mouse event */
	protected double origMouseX;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		slickGraph = new SlickGraph();
		slickGraph.widthProperty().bind(root.widthProperty());
		slickGraph.heightProperty().bind(root.heightProperty());
		root.getChildren().add(slickGraph);

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
		slickGraph.setData(DataGenerator.generateTimeseries(1000));
	}

}
