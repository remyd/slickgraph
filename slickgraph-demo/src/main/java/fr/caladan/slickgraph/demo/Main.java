package fr.caladan.slickgraph.demo;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Demo for basic usage of Slick Graph
 */
public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		Parent root = null;
		try {
			root = (Parent) new FXMLLoader(getClass().getResource("/fr/caladan/slickgraph/demo/Main.fxml")).load();
		} catch (IOException e) {
			System.err.println("Could not launch the demo");
			System.exit(-1);
		}

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("SlickGraph - Demo");
		primaryStage.sizeToScene();
		primaryStage.show();
	}

	/**
	 * Entry point
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
