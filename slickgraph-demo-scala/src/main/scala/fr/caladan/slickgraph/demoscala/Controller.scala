package fr.caladan.slickgraph.demoscala

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import java.net.URL
import java.util.ResourceBundle
import javafx.scene.layout.AnchorPane
import javafx.fxml.FXML
import javafx.scene.control.ToolBar
import javafx.scene.control.Slider
import javafx.scene.control.CheckBox

import scala.collection.JavaConverters._

import scalafx.Includes._
import scalafx.scene.Group
import scalafx.scene.paint.Color

import fr.caladan.slickgraph.Timeseries
import fr.caladan.slickgraph.controller.SlickGraphController
import fr.caladan.slickgraph.dataloader.InMemoryTimeseriesLoader

/**
 * Controller showing how to bind the Slick Graph component events
 */
class Controller extends SlickGraphController {
   
  /** Tool bar containing the widgets */
  @FXML
  private var toolBar: ToolBar = _

  /** Slider to control the smoothing strength */
  @FXML
  private var smoothingSlider: Slider = _

  /** Check box controlling the visibility of the shading */
  @FXML
  private var showShadingCheckBox: CheckBox = _

  /** Check box controlling the visibility of the curve */
  @FXML
  private var showCurveCheckBox: CheckBox = _

  /** Check box controlling the visibility of the time cursor */
  @FXML
  private var showTimeCursorCheckBox: CheckBox = _
	
  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    super.initialize(location, resources)

    // layout adjustment due to hidpi settings
    slgContainer.height.onChange{ (_, _, newValue) => AnchorPane.setTopAnchor(slgContainer, toolBar.height()) }

    // bind the slider value to the kernel bandwidth
    smoothingSlider.setValue(slickGraph.getKernelBandWidth)
    slickGraph.kernelBandwidthProperty <== smoothingSlider.value

    // bind the different properties to the checkbox values
    slickGraph.showShadingProperty <== showShadingCheckBox.selected
    slickGraph.showCurveProperty <== showCurveCheckBox.selected
    slickGraph.timeCursorVisibleProperty <== showTimeCursorCheckBox.selected
  }

  override def initializeTimeseriesLoader(): Unit = {
    // build the list of timeseries to visualize
    val colors: List[Color] = Color.Red :: Color.Green :: Color.Blue :: Nil
    val timeseries: Seq[Timeseries] = for (i <- 0 until 3) yield
      new Timeseries(
        "my timeseries " + i,
        colors(i),
        DataGenerator.generateTimeseries(1000).map(java.lang.Double.valueOf(_)).asJava)

    timeseriesLoader = new InMemoryTimeseriesLoader(timeseries.asJava)
  }
  
}
