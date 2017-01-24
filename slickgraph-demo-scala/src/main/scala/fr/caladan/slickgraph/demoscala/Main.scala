package fr.caladan.slickgraph.demoscala

import java.io.IOException

import scala.util.Try
import scala.util.Success
import scala.util.Failure

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene

/**
 * Demo for basic usage of Slick Graph in Scala with ScalaFX
 */
object Main extends JFXApp {
  Try({ new javafx.fxml.FXMLLoader(getClass.getResource("/fr/caladan/slickgraph/demoscala/Main.fxml")).load[javafx.scene.Parent] }) match {
    case Success(root) => {
      stage = new PrimaryStage() {
        title = "SlickGraph - Demo Scala"
        scene = new Scene(root)
      }
    }
    case Failure(e) => {
      println("Could not launch the demo")
      e.printStackTrace
      System.exit(-1)
    }
  }
}
