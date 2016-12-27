package fr.caladan.slickgraph.demo;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import fr.caladan.slickgraph.SlickGraph;

/**
 * Controller showing how to bind the slick graph component events
 *
 * @author "Rémy Dautriche <remy.dautriche@caladan.fr>"
 */
public class Controller implements Initializable {

	/** Root pane of the window */
	@FXML
	private AnchorPane root;

	/** Slick Graph widget */
	private SlickGraph slickGraph;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		slickGraph = new SlickGraph();
		slickGraph.widthProperty().bind(root.widthProperty());
		slickGraph.heightProperty().bind(root.heightProperty());
		root.getChildren().add(slickGraph);

		slickGraph.setData(DataGenerator.generateTimeseries(1000));
	}

}
